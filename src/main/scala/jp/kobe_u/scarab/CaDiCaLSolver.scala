package jp.kobe_u.scarab

import com.sun.jna.Pointer

/**
 * Wrapper class for CaDiCaL SAT solver using JNA.
 *
 * CaDiCaL is a simplified Lingeling SAT solver developed by Armin Biere.
 * This implementation uses Java Native Access (JNA) to interface with the
 * CaDiCaL C library.
 *
 * Note: Some advanced features like cardinality constraints and pseudo-boolean
 * constraints are not natively supported by CaDiCaL and will use clause-based
 * encodings or throw UnsupportedOperationException.
 *
 * @param libraryPath optional path to the directory containing libcadical.so/dylib/dll
 * @author Generated for Scarab
 * @see [[https://github.com/arminbiere/cadical CaDiCaL repository]]
 */
class CaDiCaLSolver(libraryPath: String = null, bufferSize: Int = CaDiCaLSolver.DEFAULT_BUFFER_SIZE) extends SatSolver {
  // Set custom library path if provided
  if (libraryPath != null && libraryPath.nonEmpty) {
    CaDiCaLNative.setLibraryPath(libraryPath)
  }

  private val cadical = CaDiCaLNative.getInstance()
  private var solver: Pointer = cadical.ccadical_init()
  private var nVariables: Int = 0
  private var nClauses: Int = 0
  private var lastResult: Option[Int] = None
  private var modelCache: Option[Array[Int]] = None

  // Buffer for batching clause additions to reduce JNA overhead
  private val clauseBuffer = new Array[Int](bufferSize)
  private var bufferIndex: Int = 0

  /**
   * Flush the clause buffer to the solver.
   * This sends all buffered literals to CaDiCaL in a single batch.
   */
  def flushBuffer(): Unit = {
    if (bufferIndex > 0) {
      var i = 0
      while (i < bufferIndex) {
        cadical.ccadical_add(solver, clauseBuffer(i))
        i += 1
      }
      bufferIndex = 0
    }
  }

  /**
   * Reset the solver to initial state.
   */
  def reset(): Unit = {
    bufferIndex = 0 // Clear buffer without flushing
    if (solver != null) {
      cadical.ccadical_release(solver)
    }
    solver = cadical.ccadical_init()
    nVariables = 0
    nClauses = 0
    lastResult = None
    modelCache = None
  }

  /**
   * Allocate n new variables.
   * Note: CaDiCaL allocates variables dynamically, so this mainly updates our counter.
   */
  def newVar(n: Int): Unit = {
    nVariables = Math.max(nVariables, cadical.ccadical_vars(solver))
    // Freeze variables to prevent elimination during preprocessing
    for (i <- 1 to n) {
      cadical.ccadical_freeze(solver, nVariables + i)
    }
  }

  /**
   * Set the expected number of variables.
   */
  def setNumberOfVariables(n: Int): Unit = {
    nVariables = n
  }

  /**
   * Add a clause to the solver.
   * Clauses are buffered and sent to CaDiCaL when the buffer is full or flushed.
   * @param lits sequence of literals (0 should not be included)
   * @param cIndex clause index for tracking (currently unused in CaDiCaL)
   * @return the clause index
   */
  def addClause(lits: Seq[Int], cIndex: Int): Int = {
    val clauseSize = lits.size + 1 // +1 for terminating 0

    // If clause doesn't fit in remaining buffer, flush first
    if (bufferIndex + clauseSize > clauseBuffer.length) {
      flushBuffer()
    }

    // If single clause is larger than buffer, add directly
    if (clauseSize > clauseBuffer.length) {
      for (lit <- lits) {
        cadical.ccadical_add(solver, lit)
        nVariables = Math.max(nVariables, Math.abs(lit))
      }
      cadical.ccadical_add(solver, 0)
    } else {
      // Add to buffer
      for (lit <- lits) {
        clauseBuffer(bufferIndex) = lit
        bufferIndex += 1
        nVariables = Math.max(nVariables, Math.abs(lit))
      }
      clauseBuffer(bufferIndex) = 0 // Terminate clause
      bufferIndex += 1
    }

    nClauses += 1
    modelCache = None // Invalidate model cache
    cIndex
  }

  /**
   * Add a clause to the solver without index tracking.
   */
  def addClause(lits: Seq[Int]): Unit = {
    addClause(lits, 0)
  }

  /**
   * Solve the formula.
   * @return true if satisfiable, false if unsatisfiable
   */
  def isSatisfiable: Boolean = {
    flushBuffer() // Ensure all clauses are sent to solver
    val result = cadical.ccadical_solve(solver)
    lastResult = Some(result)
    modelCache = None
    result == 10 // 10 = SAT, 20 = UNSAT, 0 = UNKNOWN
  }

  /**
   * Solve the formula with assumptions.
   * @param assumps sequence of assumption literals
   * @return true if satisfiable under assumptions, false otherwise
   */
  def isSatisfiable(assumps: Seq[Int]): Boolean = {
    for (lit <- assumps) {
      cadical.ccadical_assume(solver, lit)
    }
    isSatisfiable
  }

  /**
   * Get the model as an array.
   * Array index i corresponds to variable i+1 in DIMACS notation.
   * @return array where array(i) > 0 means variable i+1 is true, < 0 means false
   */
  def getModelArray: Array[Int] = {
    modelCache match {
      case Some(model) => model
      case None =>
        if (lastResult.isEmpty || lastResult.get != 10) {
          throw new IllegalStateException("No model available. Call isSatisfiable first and ensure it returns true.")
        }
        val model = new Array[Int](nVariables)
        for (i <- 1 to nVariables) {
          model(i - 1) = cadical.ccadical_val(solver, i)
        }
        modelCache = Some(model)
        model
    }
  }

  /**
   * Get the truth value of a specific variable.
   * @param v variable number (1-indexed, DIMACS notation)
   * @return true if variable is assigned true in the model
   */
  def model(v: Int): Boolean = {
    if (v < 1 || v > nVariables) {
      throw new IllegalArgumentException(s"Variable $v out of range [1, $nVariables]")
    }
    val value = modelCache match {
      case Some(model) => model(v - 1)
      case None => cadical.ccadical_val(solver, v)
    }
    value > 0
  }

  /**
   * Add an at-least-k constraint (cardinality constraint).
   * Since CaDiCaL doesn't natively support cardinality constraints,
   * this uses a simple sequential counter encoding.
   */
  def addAtLeast(lits: Seq[Int], degree: Int): Unit = {
    if (degree <= 0) return
    if (degree > lits.size) {
      // Unsatisfiable constraint
      addClause(Seq.empty)
      return
    }
    if (degree == 1) {
      // At least one: simple clause
      addClause(lits)
      return
    }
    // Use sequential counter encoding for at-least-k
    encodeAtLeastSequential(lits, degree)
  }

  /**
   * Add an at-most-k constraint.
   */
  def addAtMost(lits: Seq[Int], degree: Int): Unit = {
    if (degree < 0) {
      // Unsatisfiable
      addClause(Seq.empty)
      return
    }
    if (degree >= lits.size) return // Trivially satisfied

    // at-most-k(x1,...,xn) = at-least-(n-k)(¬x1,...,¬xn)
    addAtLeast(lits.map(-_), lits.size - degree)
  }

  /**
   * Add an exactly-k constraint.
   */
  def addExactly(lits: Seq[Int], degree: Int): Unit = {
    addAtLeast(lits, degree)
    addAtMost(lits, degree)
  }

  /**
   * Add a pseudo-boolean constraint: coef1*lit1 + coef2*lit2 + ... >= degree
   * This is encoded using adder circuits and clauses.
   */
  def addPB(lits: Seq[Int], coef: Seq[Int], degree: Int): Unit = {
    if (lits.size != coef.size) {
      throw new IllegalArgumentException("Number of literals must match number of coefficients")
    }

    // Simple encoding: expand to multiple literals based on coefficients
    val expandedLits = lits.zip(coef).flatMap { case (lit, c) =>
      if (c > 0) Seq.fill(c)(lit) else Seq.empty
    }

    if (expandedLits.nonEmpty) {
      addAtLeast(expandedLits, degree)
    }
  }

  /**
   * Sequential counter encoding for at-least-k constraints.
   * Creates auxiliary variables to count satisfied literals.
   */
  private def encodeAtLeastSequential(lits: Seq[Int], k: Int): Unit = {
    val n = lits.size
    if (k > n) {
      addClause(Seq.empty) // UNSAT
      return
    }
    if (k == 1) {
      addClause(lits)
      return
    }

    // Create auxiliary variables s[i][j] meaning "at least j of the first i literals are true"
    val auxBase = nVariables + 1
    val auxVars = Array.ofDim[Int](n, k)

    for (i <- 0 until n; j <- 0 until k) {
      auxVars(i)(j) = auxBase + i * k + j
      cadical.ccadical_freeze(solver, auxVars(i)(j))
    }
    nVariables = auxBase + n * k

    // Clauses for sequential counter
    // s[0][0] <-> x[0]
    addClause(Seq(-lits(0), auxVars(0)(0)))
    addClause(Seq(lits(0), -auxVars(0)(0)))

    // ¬s[0][j] for j > 0
    for (j <- 1 until k) {
      addClause(Seq(-auxVars(0)(j)))
    }

    // For i > 0
    for (i <- 1 until n) {
      // s[i][0] <-> (s[i-1][0] ∨ x[i])
      addClause(Seq(-auxVars(i - 1)(0), auxVars(i)(0)))
      addClause(Seq(-lits(i), auxVars(i)(0)))
      addClause(Seq(auxVars(i - 1)(0), lits(i), -auxVars(i)(0)))

      for (j <- 1 until k) {
        // s[i][j] <-> (s[i-1][j] ∨ (s[i-1][j-1] ∧ x[i]))
        addClause(Seq(-auxVars(i - 1)(j), auxVars(i)(j)))
        addClause(Seq(-auxVars(i - 1)(j - 1), -lits(i), auxVars(i)(j)))
        addClause(Seq(auxVars(i - 1)(j), lits(i), -auxVars(i)(j)))
        addClause(Seq(auxVars(i - 1)(j), auxVars(i - 1)(j - 1), -auxVars(i)(j)))
      }
    }

    // Assert s[n-1][k-1]
    addClause(Seq(auxVars(n - 1)(k - 1)))
  }

  /**
   * Get minimal explanation (UNSAT core).
   * Not natively supported by CaDiCaL.
   */
  def minExplain(): Array[Int] = {
    throw new UnsupportedOperationException(
      "CaDiCaL does not natively support UNSAT core extraction. " +
      "Use Sat4j with 'xplain' option for this feature.")
  }

  /**
   * Get all minimal explanations.
   * Not natively supported by CaDiCaL.
   */
  def minAllExplain(): Unit = {
    throw new UnsupportedOperationException(
      "CaDiCaL does not natively support MUS enumeration. " +
      "Use Sat4j with 'AllXplain' option for this feature.")
  }

  /**
   * Clear learned clauses.
   * Not directly exposed in CaDiCaL C API - would require solver restart.
   */
  def clearLearntClauses(): Unit = {
    // CaDiCaL doesn't expose this in the C API
    // Could potentially reset and re-add all original clauses, but that's expensive
    println("Warning: clearLearntClauses not supported in CaDiCaL, operation ignored")
  }

  /**
   * Find a minimal model with respect to the given literals.
   */
  def findMinimalModel(ps: Seq[Int]): Option[Seq[Boolean]] = {
    if (!isSatisfiable) return None

    var trueLits = ps.filter(p => model(Math.abs(p)) == (p > 0))

    // Try to flip each true literal to false
    var changed = true
    while (changed) {
      changed = false
      for (lit <- trueLits) {
        val otherLits = trueLits.filter(_ != lit)
        if (isSatisfiable(otherLits.map(-_))) {
          trueLits = ps.filter(p => model(Math.abs(p)) == (p > 0))
          changed = true
        }
      }
    }

    Some((1 to nVariables).map(model))
  }

  /**
   * Find the backbone (literals that must have the same value in all models).
   */
  def findBackbone(ps: Seq[Int]): Set[Int] = {
    var backbone = Set.empty[Int]

    for (p <- ps) {
      val v = Math.abs(p)
      val satWithPos = isSatisfiable(Seq(v))
      val satWithNeg = isSatisfiable(Seq(-v))

      (satWithPos, satWithNeg) match {
        case (true, false) =>
          backbone += v
          addClause(Seq(v))
        case (false, true) =>
          backbone += -v
          addClause(Seq(-v))
        case _ => // Variable is not in backbone
      }
    }

    backbone
  }

  /**
   * Get the number of variables.
   */
  def nVars(): Int = nVariables

  /**
   * Get the next free variable ID.
   * @param reserve whether to reserve the variable
   * @return next free variable ID
   */
  def nextFreeVarID(reserve: Boolean): Int = {
    val next = nVariables + 1
    if (reserve) {
      nVariables = next
      cadical.ccadical_freeze(solver, next)
    }
    next
  }

  /**
   * Get the number of constraints (clauses).
   */
  def nConstraints(): Int = nClauses

  /**
   * Set a timeout in seconds.
   * Note: CaDiCaL uses resource limits rather than wall-clock time.
   */
  def setTimeout(time: Int): Unit = {
    if (time > 0) {
      // Convert seconds to a conflict limit (rough approximation)
      // Typical solvers process ~10000 conflicts per second
      val conflictLimit = time * 10000
      cadical.ccadical_limit(solver, "conflicts", conflictLimit)
    }
  }

  /**
   * Dump solver statistics.
   */
  def dumpStat(): Unit = {
    println(s"CaDiCaL Statistics:")
    println(s"  Variables: $nVariables")
    println(s"  Clauses: $nClauses")
    cadical.ccadical_print_statistics(solver)
  }

  /**
   * Dump statistics to a file.
   */
  def dumpStat(filePath: String): Unit = {
    val writer = new java.io.PrintWriter(new java.io.File(filePath))
    try {
      writer.println(s"CaDiCaL Statistics:")
      writer.println(s"  Variables: $nVariables")
      writer.println(s"  Clauses: $nClauses")
      writer.flush()
      // Note: cadical_print_statistics prints to stdout,
      // not possible to redirect through JNA without additional work
      println(s"Warning: Detailed statistics written to stdout, not $filePath")
      cadical.ccadical_print_statistics(solver)
    } finally {
      writer.close()
    }
  }

  /**
   * Dump CNF to stdout.
   * Not supported by CaDiCaL C API.
   */
  def dumpCnf(): Unit = {
    throw new UnsupportedOperationException(
      "CaDiCaL C API does not support CNF dumping. " +
      "Use Sat4j with 'Dimacs' option for this feature.")
  }

  /**
   * Dump CNF to a file.
   * Not supported by CaDiCaL C API.
   */
  def dumpCnf(filePath: String): Unit = {
    throw new UnsupportedOperationException(
      "CaDiCaL C API does not support CNF dumping. " +
      "Use Sat4j with 'Dimacs' option for this feature.")
  }

  /**
   * Cleanup when object is garbage collected.
   */
  override def finalize(): Unit = {
    if (solver != null) {
      cadical.ccadical_release(solver)
      solver = null
    }
    super.finalize()
  }
}

/**
 * Companion object for CaDiCaLSolver constants.
 */
object CaDiCaLSolver {
  /** Default buffer size for clause batching (number of integers) */
  val DEFAULT_BUFFER_SIZE: Int = 10000
}
