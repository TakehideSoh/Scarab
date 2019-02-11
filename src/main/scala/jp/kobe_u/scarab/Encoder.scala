package jp.kobe_u.scarab

/**
 * `Encoder` is an abstract class for encoding CSP to SAT.
 * Currently, only the [[jp.kobe_u.scarab.OrderEncoder]] is its implementation.
 *
 * The `encodeCSP` method encodes the whole CSP when it is called at the first time
 * (or whenever there is some change (addition or rollback of
 * constraints) in CSP).
 * Generated clauses are added to the SAT solver by the `addClause` method of this class
 * which calls the `addClause` method of [[jp.kobe_u.scarab.SatSolver]].
 *
 * ==Implementation Issues==
 * ===Encoding integer variables===
 * When encoding an integer variable to SAT, a multiple number of SAT variables is required in general.
 * Each integer variable `x` has its ''code'' which is the minimum SAT variable number used for `x`.
 * Following the classical DIMACS CNF format, the code is a positive number beginning from one.
 * In `encodeCSP` method,  the code is assigned for each integer variable and it is resistered to `varCodeMap`.
 * The code value regisitered in `varCodeMap` can be obtained by `code` method.
 *
 * The following methods should be implemented in the subclass.
 *   - `satVariablesSize(x: Var): Int` : returns the number of SAT variables required to encode `x`.
 *   - `encode(x: Var): Seq[Seq[Int]]` : returns the list of clauses required to encode `x`.
 *
 * ===Encoding Boolean variables===
 * In `encodeCSP` method,  the code is assigned for each Boolean variable and it is resistered to `boolCodeMap`.
 * The code value regisitered in `boolCodeMap` can be obtained by `code` method.
 *
 * ===Encoding constraints===
 * It might be necessary to perform preprocessing to the given CSP before encoding.
 * [[jp.kobe_u.scarab.Simplifier]] does such a transformation to clausal form, that is,
 * constraints are transformed to `Seq[Seq[Literal]]` data.
 *
 * The following methods should be implemented in the subclass.
 *   - `encode(lit: Literal, clause0: Seq[Int]): Seq[Seq[Int]]`
 *   - `add(c: Constraint): Unit`
 *
 * ===Encoding CSP===
 * The `encodeCSP` method performs the encoding of the whole CSP when
 * it is called at the first time (or whenever there is some change (addition or rollback of constraints) in CSP)
 * It is implemented as follows.
 *   1. For each integer variable `x` in the CSP, its code is registered to `varCodeMap`, and
 *      `satVariableCount` is increased by `satVariablesSize(x)`.
 *   1. For each Boolean variable `p` in the CSP, its code is registered to `boolCodeMap`, and
 *      `satVariableCount` is increased by one.
 *   1. For each integer variable `x` in the CSP, `encode(x)` is called.
 *   1. All constraints are removed from the CSP.
 *   1. For each constraint `c` removed from the CSP, `add(c)` is called.
 *
 * ===Decoding===
 * The following methods should be implemented in the subclass.
 *   - `decode(x: Var): Int` : returns the value of `x` from the satisfiable assignment found by the SAT solver.
 *
 * @param csp the CSP to be encoded
 * @param satSolver the SAT solver where the generated clauses are stored
 */
abstract class Encoder(csp: CSP, val satSolver: SatSolver) {
  /**
   * # SAT variables. This is incremented when:
   *  - CSP variables are encoded.
   *  - while Tseitin translation
   */
  var satVariableCount = 0

  /**
   * # SAT clauses. This is incremented when:
   *  - satSolver.addClause is called.
   */
  var satClauseCount = 0

  /** Map: Integer Variable -> its first code (Dimacs Variable Number) */
  var varCodeMap: Map[Var, Int] = Map.empty

  /** Map: Bool -> code (Dimacs Variable Number) */
  var boolCodeMap: Map[Bool, Int] = Map.empty

  /**
   * Map: Constraint index -> Constraint Component
   *  Currently, constraint component is defined when csp.add is called.
   *  Constraint index of axiom clauses are 1.
   */
  var constIndexMap: Map[Int, Constraint] = Map.empty

  /** Constraint index counter. */
  var constIndex = 1

  /** Map: Const Index to Normalized Const Index */
  val normConst = scala.collection.mutable.HashMap.empty[Int, commitPoint]

  /**
   * Represents the integer constant for true literal (Integer.MAX_VALUE).
   * It is only used during the encoding process, and does not appear in the generated clause finally.
   */
  val TrueLit = Integer.MAX_VALUE

  /**
   * Represents the integer constant for false literal (-Integer.MAX_VALUE).
   * It is only used during the encoding process, and does not appear in the generated clause finally.
   */
  val FalseLit = -TrueLit

  /** # of Integer Variables which have encoded into SAT.  */
  /** # of Bool which have encoded into SAT.  */
  /** # of constraints which have encoded into SAT */
  object eState {
    var v = 0; var b = 0; var c = 0
    def reset { v = 0; b = 0; c = 0 }
    def update {
      v = csp.variables.size
      b = csp.bools.size
      c = csp.constraints.size
      csp.cStack = for (cp <- csp.cStack) yield if (normConst.contains(cp.c)) normConst(cp.c) else cp
    }
  }

  /**
   * Reset the followings: satSolver, satVariableCount, satClauseCount, varCodeMap, boolCodeMap, eState
   */
  def reset {
    satSolver.reset
    satVariableCount = 0
    satClauseCount = 0
    varCodeMap = Map.empty
    boolCodeMap = Map.empty
    eState.reset
  }

  /**
   * Adds SAT clause by using `addClause` method of [[jp.kobe_u.scarab.SatSolver]] after removing `TrueLit` and `FalseLit`.
   * When the clause contains `TrueLit`, the clause is not added.
   * When the clause only contains `FalseLit`, two clauses 1 and -1
   * are added.
   */
  def addClause(clause: Seq[Int], cIndex: Int) {
    val clause1 = clause.filter(_ != FalseLit)
    if (clause1.contains(TrueLit)) {
    } else if (clause1.isEmpty) {
      //      println(s"${constIndexMap(cIndex)} is obviously inconsistent!")
      satSolver.addClause(Seq(1), cIndex)
      satSolver.addClause(Seq(-1), cIndex)
      satClauseCount += 2
    } else {
      satSolver.addClause(clause1, cIndex)
      satClauseCount += 1
    }
  }

  /** Adds all clauses by calling `addClause`. */
  def addAllClauses(clauses: Seq[Seq[Int]], cIndex: Int) {
    if (clauses.isEmpty)
      addClause(Seq.empty[Int], cIndex)
    for (clause <- clauses if !clause.contains(TrueLit))
      addClause(clause, cIndex)
  }

  /** Creates a new Boolean variable, adds it to the CSP, and registers it to `boolCodeMap`. */
  def newBool = {
    val p = csp.newBool
    boolCodeMap += p -> (satVariableCount + 1)
    satVariableCount += 1
    p
  }

  /** Returns the lower bound value of `x`. */
  def lb(x: Var): Int = csp.dom(x).lb

  /** Returns the upper bound value of `x`. */
  def ub(x: Var): Int = csp.dom(x).ub

  /** Returns the lower bound value of `a*x`. */
  def lb(a: Int, x: Var): Int = if (a > 0) a * lb(x) else a * ub(x)

  /** Returns the upper bound value of `a*x`. */
  def ub(a: Int, x: Var): Int = if (a > 0) a * ub(x) else a * lb(x)

  /** Returns the lower bound value of the linear summation. */
  def lb(axs: Seq[(Int, Var)]): Int = axs.map { case (a, x) => lb(a, x) }.sum

  /** Returns the upper bound value of the linear summation. */
  def ub(axs: Seq[(Int, Var)]): Int = axs.map { case (a, x) => ub(a, x) }.sum

  /**
   * Returns the value of `floor(b / a)`.
   * This is used because math.floor is slow.
   */
  def floorDiv(b: Int, a: Int) =
    if (a > 0)
      if (b >= 0) b / a else (b - a + 1) / a
    else if (b >= 0) (b - a - 1) / a else b / a

  /**
   * Returns the value of `ceil(b / a)`.
   * This is used because math.ceil is slow.
   */
  def ceilDiv(b: Int, a: Int) =
    if (a > 0)
      if (b >= 0) (b + a - 1) / a else b / a
    else if (b >= 0) b / a else (b + a + 1) / a

  /** Returns the number of SAT variables required to encode `x`. */
  def satVariablesSize(x: Var): Int

  /** Returns the code of `x`. */
  def code(x: Var) = varCodeMap(x)

  /** Returns the code of `p`. */
  def code(p: Bool) = boolCodeMap(p)

  def toLeZero(c: Constraint): Constraint = c match {
    case EqZero(sum) => And(LeZero(sum), LeZero(sum.neg))
    case NeZero(sum) => Or(LeZero(sum + 1), LeZero(sum.neg + 1))
    case GeZero(sum) => LeZero(sum.neg)
    case And(cs @ _*) => And(cs.map(toLeZero(_)))
    case Or(cs @ _*) => Or(cs.map(toLeZero(_)))
    case lit => lit
  }

  /** Returns the list of clauses generated by encoding integer variable `x`. */
  def encode(x: Var): Seq[Seq[Int]]

  /**
   * Returns the list of clauses generated by encoding literal `lit`.
   * `clause0` should be added to each clause generated.
   */
  def encode(lit: Literal, clause0: Seq[Int]): Seq[Seq[Int]]

  /** Returns the list of clauses generated by encoding clausal form constraint `c`. */
  def encode(c: Seq[Literal]): Seq[Seq[Int]] = {
    if (c.isEmpty) Seq(Seq.empty)
    else {
      for {
        clause0 <- encode(c.tail)
        clause <- encode(c.head, clause0)
      } yield clause
    }
  }

  /** Adds the constraint to CSP after preprocessing and encodes it. */
  def add(c: Constraint): Unit

  /**
   * Returns the sequence of literals that will be used as assumptions.
   */
  def extractAssumpLits(cs: Seq[Constraint]): Seq[Int]

  /** Encodes the whole CSP at 1st time. Otherwise, only the differences are added. */
  def encodeCSP {
    if (csp.rollbackHappen) {
      //      println("rollback is hapened! care!")
      this.reset
      satSolver.reset
      eState.reset
      csp.rollbackHappen = false
    }

    /** 1. count CNF var. for integer variable */
    for (n <- eState.v to csp.variables.size - 1) {
      varCodeMap += csp.variables(n) -> (satVariableCount + 1)
      satVariableCount += satVariablesSize(csp.variables(n))
    }

    /** 2. count CNF var. for Boolean variable */
    for (n <- eState.b to csp.bools.size - 1) {
      boolCodeMap += csp.bools(n) -> (satVariableCount + 1)
      satVariableCount += 1
    }
    // satSolver.setNumberOfVariables(satVariableCount)

    /**
     * 3. set current variable count
     * Note:
     * this value would be increased since "simplify class" would
     * add auxiliary variables used in Tseitin transformation
     */
    if (satSolver.nextFreeVarID(false) == 1) {
      satSolver.newVar(satVariableCount)
    } else {
      while (satSolver.nextFreeVarID(false) < satVariableCount)
        satSolver.nextFreeVarID(true)
    }

    /** 4. encode integer variable for axiom clauses */
    for (n <- eState.v to csp.variables.size - 1) {
      val axiom = encode(csp.variables(n))
      if (!axiom.isEmpty)
        addAllClauses(axiom, 1)
    }

    /** 5. temporally save current csp to tmp */
    val tmp = csp.constraints

    /** 6. `ALREADY ENCODED Constraints` are added to CSP */
    csp.constraints = csp.constraints.take(eState.c)

    /** 7. `NOT YET ENCODED Constraints` are encoded and added to CSP */
    for (n <- eState.c to tmp.size - 1) {
      add(tmp(n))
      normConst += n + 1 -> commitPoint(csp.bools.size, csp.variables.size, csp.constraints.size)
    }
    eState.update

    // satSolver.setNumberOfVariables(satVariableCount)
  }

  /** Returns the value of `x` from the satisfiable assignment found by the SAT solver. */
  def decode(x: Var): Int

  /** Returns the satisfiable assignment of CSP from the satisfiable assignment found by the SAT solver. */
  def decode: Assignment = {
    var varAssign: Map[Var, Int] = Map.empty
    var boolAssign: Map[Bool, Boolean] = Map.empty
    for (x <- csp.variables)
      varAssign += x -> decode(x)
    for (p <- csp.bools) {
      boolAssign += p -> satSolver.model(boolCodeMap(p))
    }

    Assignment(varAssign, boolAssign)
  }

}
