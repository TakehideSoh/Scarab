package jp.kobe_u.scarab

/**
 * Trait for Scarab DSL which provides methods for CSP and CSP solver.
 */
trait ScarabTrait {
  
  def csp: CSP
  def satSolver: SatSolver
  def encoder: Encoder
  def solver: Solver

  def use(newEncoder: Encoder): Unit
  def use(newSatSolver: SatSolver): Unit
  def use(newSolver: Solver): Unit

  /* */
  def abc = println("ok")
  /* */
  def int(x: Var, a: Int, b: Int) = csp.int(x,a,b)
  /* */
  def int(x: Var, d: Seq[Int]) = csp.int(x,d)
  /* */
  def boolInt(x: Var) = csp.boolInt(x)
  /* */
  def bool(p: Bool) = csp.bool(p)
  /* */  
  def add(c: Constraint) = csp.add(c)
  /* */  
  def dom(v: Var) = csp.dom(v)
  /* */  
  def commit = csp.commit
  /* */  
  def show = csp.show
  /* */  
  def rollback = csp.rollback
  /* */
  def find = solver.find
  /* */
  def isSatisfiable = solver.isSatisfiable
  /* */
  def findNext = solver.findNext
  /* */
  def find(con: Constraint) = solver.find(con)
  /* */
  def find(cons: Seq[Constraint]) = solver.find(cons)
  /* */
  def findMinimal(bs: Seq[Bool] = Seq.empty, is: Seq[Var] = Seq.empty): Boolean = solver.findMinimal(bs,is)
  /* */
  def findMaximal(bs: Seq[Bool] = Seq.empty, is: Seq[Var] = Seq.empty): Boolean = solver.findMaximal(bs,is)
  /* */
  def solution = solver.solution
  /* */
  def reset = solver.reset
  /* */
  def minExplain = solver.minExplain
  /* */
  def minAllExplain = solver.minAllExplain
  /* */
  def encodeCSP = encoder.encodeCSP
  /* */
  def optimize(v: Var): Int = solver.optimize(v, csp.dom(v).lb, csp.dom(v).ub, "default") 
  /* */
  def optimize(v: Var, how: String): Int = solver.optimize(v, csp.dom(v).lb, csp.dom(v).ub, how)
  /* */
  def optimize(v: Var, lb: Int, ub: Int, how: String = "default"): Int = solver.optimize(v, lb, ub, how)
  /* */
  def timeLimit(sec: Int) = satSolver.setTimeout(sec)
  /* */
  def alldiff(xs: Term*) = Tools.alldiff(xs, csp)
  /* */
  def alldiff(xs: Iterable[Term]) = Tools.alldiff(xs.toSeq, csp)
  /* */
  // def alldiff(xs: Iterable[Var]) = Tools.alldiff(xs.toSeq, csp)
  /* */
  def dumpCnf = { encoder.encodeCSP ; satSolver.dumpCnf }
  /* */
  def dumpCnf(filePath: String) = { encoder.encodeCSP ; satSolver.dumpCnf(filePath) }
  /* */
  def dumpStat = satSolver.dumpStat
  /* */
  def dumpStat(filePath: String) = satSolver.dumpStat(filePath)
}

class Scarab (val csp: CSP, 
	      var satSolver: SatSolver,
	      var encoder: Encoder,
	      var solver: Solver) extends ScarabTrait {

  /* */
  def this(csp: CSP, satSolver: SatSolver, encoder: Encoder) = 
    this(csp, satSolver, encoder, new Solver(csp,satSolver,encoder))
  /* */
  def this(csp: CSP, satSolver: SatSolver) = 
    this(csp, satSolver, new OrderEncoder(csp,satSolver))
  /* */
  def this(csp: CSP) = this(csp, new Sat4j())
  /* */
  def this() = this(CSP())

  /* */
  def use(newEncoder: Encoder) = {
    encoder = newEncoder
    satSolver = encoder.satSolver
    solver = new Solver(csp,satSolver,encoder)
  }
  /* */
  def use(newSatSolver: SatSolver) = {
    satSolver = newSatSolver
    encoder = new OrderEncoder(csp,satSolver)
    solver = new Solver(csp,satSolver,encoder)
  }
  /* */
  def use(newSolver: Solver) = 
    solver = newSolver
}

/**
 * Singleton for Scarab Applications.
 */
object dsl extends ScarabTrait {
  import scala.util.DynamicVariable    
  val scarabVar = new DynamicVariable[Scarab](new Scarab)
  
  /* */
  def csp = scarabVar.value.csp
  /* */
  def satSolver = scarabVar.value.satSolver
  /* */
  def encoder = scarabVar.value.encoder
  /* */
  def solver = scarabVar.value.solver

  /* */
  def use(newEncoder: Encoder) = scarabVar.value.use(newEncoder)
  /* */
  def use(newSatSolver: SatSolver) = scarabVar.value.use(newSatSolver)
  /* */
  def use(newSolver: Solver): Unit = scarabVar.value.use(newSolver)
  /* */
  def using(scarab: Scarab = new Scarab)(block: => Unit) = 
    scarabVar.withValue(scarab) { block }

}

/**
 * Singleton for Scarab Applications.
 */
@deprecated("sapp is replaced by dsl","v1-6-9")
object sapp extends ScarabTrait {
  import scala.util.DynamicVariable    
  val scarabVar = new DynamicVariable[Scarab](new Scarab)
  
  /* */
  def csp = scarabVar.value.csp
  /* */
  def satSolver = scarabVar.value.satSolver
  /* */
  def encoder = scarabVar.value.encoder
  /* */
  def solver = scarabVar.value.solver

  /* */
  def use(newEncoder: Encoder) = scarabVar.value.use(newEncoder)
  /* */
  def use(newSatSolver: SatSolver) = scarabVar.value.use(newSatSolver)
  /* */
  def use(newSolver: Solver): Unit = scarabVar.value.use(newSolver)
  /* */
  def using(scarab: Scarab = new Scarab)(block: => Unit) = 
    scarabVar.withValue(scarab) { block }

}
