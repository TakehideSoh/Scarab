package jp.kobe_u.scarab

/**
 * Classes for CSP solvers.
 * @author Takehide Soh and Naoyuki Tamura
 */

/**
 * `Solver` is a class for CSP solvers.
 * It encodes the given CSP to a SAT instance by using the specified encoder,
 * and solves the encoded SAT instance by using the specified SAT solver.
 * [[jp.kobe_u.scarab.OrderEncoder]] is used as the default encoder, and
 * [[jp.kobe_u.scarab.Sat4j]] is used as the default SAT solver.
 *
 * Typical usage of the solver is as follows.
 * {{{
 *     val solver = Solver(csp)     // using Companion Object
 *     if (solver.find) {
 *       println(solver.solution)
 *     }
 * }}}
 *
 * The `find` method does the following.
 *   1. When it is called at the first time, the whole CSP is encoded by the `encodeCSP` method of [[jp.kobe_u.scarab.Encoder]],
 *      and generated clauses are added to the SAT solver by the `addClause` method of [[jp.kobe_u.scarab.SatSolver]].
 *   1. The `isSatisfiable` method of [[jp.kobe_u.scarab.SatSolver]] is called.
 *   1. When a solution is found, the `decode` method of [[jp.kobe_u.scarab.Encoder]] is called to get the solution
 *      and the solution is set to the `solution` variable.
 *      Otherwise, the `solution` variable is set to `None`.
 *
 * The `add` method does the following.
 *   1. The constraint is preprocessed before encoding, and the translated constraints are added to the CSP.
 *      [[jp.kobe_u.scarab.Simplifier]] is the preprocessor of [[jp.kobe_u.scarab.OrderEncoder]].
 *   1. The translated constraints are encoded by [[jp.kobe_u.scarab.Encoder]],
 *      and generated clauses are added to the SAT solver by the `addClause` method of [[jp.kobe_u.scarab.SatSolver]].
 *
 * {{{
 *     val solver = new Solver(csp)
 *     if (solver.find) {
 *       solver.add(new constraint to be added)
 *       if (solver.find) {
 *         println(solver.solution)
 *       }
 *     }
 * }}}
 *
 * The SAT solver keeps working during the above process.
 * Therefore, we can get the benefit of reusing learnt clauses of the SAT solver at the second `find`.
 *
 * @constructor Creates a new solver
 * @param csp the CSP to be solved
 * @param satSolver the SAT solver to be used
 * @see [[jp.kobe_u.scarab]].
 */
class Solver(
  val csp: CSP,
  val satSolver: SatSolver = new Sat4j(),
  val encoder: Encoder) {
  /**
   * Returns the solution (assignment) found by `find` method.
   * It will return `null` if no solutions are found.
   * Use `solutionOpt` to avoid `null`.
   */
  def solution: Assignment = solutionOpt.orNull

  /**
   * Returns the solution (assignment) found by `find` method wrapped in `Option`.
   * It will return `None` if no solutions are found.
   */
  def solutionOpt: Option[Assignment] = _solutionOpt
  private[this] var _solutionOpt: Option[Assignment] = None

  /**
   * Returns satisfiability (whether CSP is SAT or UNSAT).
   *  In case of SAT, CSP solution is decoded from SAT solution.
   *
   *  @return Boolean
   */
  def isSatisfiable = {
    val result = satSolver.isSatisfiable
    _solutionOpt = if (result) Option(encoder.decode) else None
    result
  }

  /**
   *  Returns satisfiability (whether CSP is SAT or UNSAT).
   *  This method is more or less `encodeCSP` plus `isSatisfiable`.
   *
   *  @return Boolean
   */
  def find = {
    encoder.encodeCSP
    isSatisfiable
  }

  /**
   *  Add Constraints that blocks the latest solution.
   */
  def addBlockConstraint {
    _solutionOpt match {
      case Some(n) => {
        val cs1 = for (x <- csp.variables if !x.isAux)
          yield (x !== x.value(_solutionOpt.get))
        val cs2 = for (p <- csp.bools if !p.isAux)
          yield if (_solutionOpt.get(p)) Not(p) else p
        csp.add(Or(Or(cs1), Or(cs2)))
      }
      case None =>
    }
  }

  /**
   *  Returns satisfiability (whether CSP is SAT or UNSAT) after blocking the latest solution.
   *  That is, find another solution.
   *  This method must be called after `find` is called.
   *
   *  @return Boolean
   */
  def findNext = {
    addBlockConstraint
    find
  }

  /**
   *  Returns satisfiability (whether CSP is SAT or UNSAT) after blocking the latest solution.
   *  it is identical to `findNext`. The difference is that it find next solution without adding blocking constraints by using
   *  Sat4j's special enumerator.
   *
   *  To use this,  Sat4j("iterate") must be used as SAT solver.
   *  While this method, satSolver.model is called since Sat4j("iterate") needs calling model() for enumeration.
   *
   *  @return Boolean
   */
  def findNext4s = {
    encoder.encodeCSP
    val result = satSolver.isSatisfiable
    // Sat4j("iterate") needs calling model() for enumeration
    if (result) satSolver.getModelArray
    _solutionOpt = if (result) Option(encoder.decode) else None
    result
  }

  /** Return whether CSP with assumption is SAT or not. If SAT then solution is constructed. */
  def isSatisfiable(cons: Seq[Constraint]) = {
    val result = satSolver.isSatisfiable(encoder.extractAssumpLits(cons))
    _solutionOpt = if (result) Option(encoder.decode) else None
    result
  }

  /**
   *  Return whether CSP with assumption is SAT or not. If SAT then solution is constructed.
   *   This method is more or less `encodeCSP` plus `isSatisfiable(cons: Seq[Constraint])`.
   */
  def find(cons: Seq[Constraint]): Boolean = {
    encoder.encodeCSP
    isSatisfiable(cons)
  }

  private def gen(x: Var) = Seq.range(encoder.code(x), encoder.code(x) + encoder.satVariablesSize(x))
  
  /**
   *  Return minimal model according to a given set of Boolean variables
   */
  def findMinimal(bs: Seq[Bool] = Seq.empty, is: Seq[Var] = Seq.empty): Boolean = {
    encoder.encodeCSP
    val ps = bs.map(b => encoder.code(b)) ++ is.flatMap(i => gen(i).map(-_))

    val result = satSolver.findMinimalModel(ps)
    _solutionOpt = if (result != None) Option(encoder.decode) else None
    result != None
  }


  /**
   *  Return maximal model according to a given set of Boolean variables
   */
  def findMaximal(bs: Seq[Bool] = Seq.empty, is: Seq[Var] = Seq.empty): Boolean = {
    encoder.encodeCSP
    val ps = bs.map(b => encoder.code(b)) ++ is.flatMap(i => gen(i))

    val result = satSolver.findMinimalModel(ps)
    _solutionOpt = if (result != None) Option(encoder.decode) else None
    result != None
  }

  /**
   *  Return minimal model according to a given set of Boolean variables
   */
  def findBackbone(bs: Seq[Bool] = Seq.empty, is: Seq[Var] = Seq.empty): Set[Literal] = {
    val ps = bs.map(b => encoder.code(b)) ++ is.flatMap(i => gen(i))
    
    var out = Set.empty[Literal]
    encoder.encodeCSP
    val res = satSolver.findBackbone(ps)
    for (b <- bs) {
      res.find(i => math.abs(i) == encoder.code(b)) match {
        case Some(n) => if (n < 0) out += Not(b) else out += b
        case None    =>
      }
    }
    out
  }

  /**
   *  Return whether CSP with assumption is SAT or not. If SAT then solution is constructed.
   *   This method is more or less `encodeCSP` plus `isSatisfiable(con: Constraint)`.
   */
  def find(con: Constraint): Boolean = find(Seq(con))

  /**
   * Reset the followings:
   * - encoder
   * - satSolver
   * And rollback
   * - csp
   */
  def reset {
    encoder.reset
    satSolver.reset
    csp.reset
    _solutionOpt = None
  }

  /**
   * Return a minimal set of UNSAT components of CSP.
   * Each component is defined at the addition of CSP.
   */
  def minExplain = for (i <- satSolver.minExplain if i != 0) yield encoder.constIndexMap.getOrElse(i, Bool("Axiom"))

  /**
   * Return a minimal set of UNSAT components of CSP.
   * Each component is defined at the addition of CSP.
   */
  def minAllExplain = satSolver.minAllExplain

  /**
   * Return the optimal value of `v`
   *
   * Options of strategy:
   * - `dec` does decremental search from upper bound.
   * - `inc1` does incremental search from lb by using commit and rollback.
   * - `inc2` does incremental search from lb by using assumptions.
   * - `bin1` does binary search from lb and ub by using commit and rollback.
   * - `bin2` does binary search from lb and ub by using assumptions.
   *
   */
  def optimize(v: Var, lb: Int, ub: Int, strategy: String = "default"): Int = strategy match {
    case "dec"          => decrementalSearch(v, ub)
    case "inc1"         => incrementalSearch1(v, lb)
    case "inc2" | "inc" => incrementalSearch2(v, lb)
    case "bin1"         => binarySearch1(v, lb, ub)
    case "bin2" | "bin" => binarySearch2(v, lb, ub)
    case "default"      => binarySearch2(v, lb, ub)
    case ex             => throw new IllegalArgumentException("no such optimization heuristics $ex")
  }

  private def decrementalSearch(v: Var, ub0: Int) = {
    csp.add(v <= ub0)
    var lb = csp.dom(v).lb; var ub = ub0; var sol = ub0
    while (lb < ub && find) {
      sol = encoder.decode(v)
      //      println(s"lb: $lb, ub: $sol")
      ub = sol - 1
      csp.add(v <= ub)
    }
    sol
  }

  private def incrementalSearch1(v: Var, lb0: Int) = {
    var lb = lb0; var ub = csp.dom(v).ub
    println(s"lb: $lb, ub: $ub")
    csp.commit
    csp.add(v <= lb)
    while (lb < ub && !find) {
      csp.rollback
      lb += 1
      csp.add(v <= lb)
      //      println(s"lb: $lb, ub: $ub")
    }
    lb
  }

  private def incrementalSearch2(v: Var, lb0: Int) = {
    var lb = lb0; var ub = csp.dom(v).ub
    println(s"lb: $lb, ub: $ub")
    while (lb < ub && !find(Seq(v <= lb))) {
      lb += 1
      //      println(s"lb: $lb, ub: $ub")
    }
    lb
  }

  private def binarySearch1(v: Var, lb0: Int, ub0: Int) = {
    var lb = lb0; var ub = ub0
    while (lb < ub) {
      var size = (lb + ub) / 2
      csp.commit
      csp.add(v <= size)
      if (find) {
        ub = size; csp.commit;
      } else {
        lb = size + 1; csp.rollback;
      }
      //      println(s"lb: $lb, ub: $ub")
    }
    ub
  }

  private def binarySearch2(v: Var, lb0: Int, ub0: Int) = {
    var lb = lb0; var ub = ub0
    while (lb < ub) {
      var size = (lb + ub) / 2
      if (find(v <= size)) ub = size
      else lb = size + 1
      //      println(s"lb: $lb, ub: $ub")
    }
    ub
  }

}

/**
 * Companion Object for CSP Solver.
 *
 * {{{
 *    val solver = Solver(csp)
 *    if (solver.find) {
 *      println(solver.solution)
 *    }
 * }}}
 * [[jp.kobe_u.scarab.Solver]] is returned as the default solver.
 * @see [[jp.kobe_u.scarab.Solver]]
 */
object Solver {
  def apply(csp: CSP): Solver = {
    val satSolver = new Sat4j()
    val encoder = new OrderEncoder(csp, satSolver)
    val solver = new Solver(csp, satSolver, encoder)
    solver
  }
}
