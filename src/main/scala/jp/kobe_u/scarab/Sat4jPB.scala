package jp.kobe_u.scarab

import org.sat4j.pb.SolverFactory
import org.sat4j.pb._
import org.sat4j.core.VecInt

class Sat4jPB extends SatSolver {

//  val sat4jpb: IPBSolver = SolverFactory.instance.createSolverByName("Default")
  val sat4jpb: IPBSolver = SolverFactory.instance.createSolverByName("CuttingPlanes")    
  var nof_vars = 0

  /* Basic Interface */
  def newVar(n: Int) {
    sat4jpb.newVar(n)
  }

  def setNumberOfVariables(n: Int) {
    nof_vars = n
  }

  def addClause(lits: Seq[Int], cIndex: Int): Int = {
    val coef = Seq.fill[Int](lits.size)(1).toArray
    sat4jpb.addAtLeast(new VecInt(lits.toArray), new VecInt(coef), 1)
    cIndex
  }

  def addClause(lits: Seq[Int]) {
    val coef = Seq.fill[Int](lits.size)(1).toArray
    sat4jpb.addAtLeast(new VecInt(lits.toArray), new VecInt(coef), 1)
  }

  def isSatisfiable: Boolean = sat4jpb.isSatisfiable

  def getModelArray: Array[Int] = sat4jpb.model()

  def model(v: Int): Boolean = sat4jpb.model(v)

  def reset: Unit = sat4jpb.reset

  /* Advanced Interface */
  def isSatisfiable(assumps: Seq[Int]): Boolean = sat4jpb.isSatisfiable(new VecInt(assumps.toArray))

  def addAtLeast(lits: Seq[Int], degree: Int) {
    val coef = Seq.fill[Int](lits.size)(1).toArray
    sat4jpb.addAtLeast(new VecInt(lits.toArray), new VecInt(coef), degree)
  }

  def addAtMost(lits: Seq[Int], degree: Int) {
    val coef = Seq.fill[Int](lits.size)(1).toArray
    sat4jpb.addAtMost(new VecInt(lits.toArray), new VecInt(coef), degree)
  }
  def addExactly(lits: Seq[Int], degree: Int) {
    val coef = Seq.fill[Int](lits.size)(1).toArray
    sat4jpb.addExactly(new VecInt(lits.toArray), new VecInt(coef), degree)
  }
  
  def addBBC(block: Int, lits: Seq[Int], degree: Int) = ???

  def addPB(lits: Seq[Int], coef: Seq[Int], degree: Int) {
    sat4jpb.addAtLeast(new VecInt(lits.toArray), new VecInt(coef.toArray), degree)
  }
  def minExplain: Array[Int] = { throw new Exception("this method is not implemented in Sat4jPB") }
  def minAllExplain: Unit = { throw new Exception("this method is not implemented in Sat4jPB") }
  def clearLearntClauses: Unit = sat4jpb.clearLearntClauses
  def findMinimalModel(ps: Seq[Int]): Option[Seq[Boolean]] = { throw new Exception("this method is not implemented in Sat4jPB") }
  def findBackbone(ps: Seq[Int]): Set[Int] = { throw new Exception("this method is not implemented in Sat4jPB") }

  /* Supplemental Interface */
  def nVars: Int = sat4jpb.nVars
  def nextFreeVarID(reserve:Boolean) = sat4jpb.nextFreeVarId(reserve)
  
  
  def nConstraints: Int = sat4jpb.nConstraints
  def setTimeout(time: Int) = { throw new Exception("this method is not implemented in Sat4jPB") }

  def dumpStat = { throw new Exception("this method is not implemented in Sat4jPB") }
  def dumpStat(filePath: String) = { throw new Exception("this method is not implemented in Sat4jPB") }
  def dumpCnf = { throw new Exception("this method is not implemented in Sat4jPB") }
  def dumpCnf(filePath: String) = { throw new Exception("this method is not implemented in Sat4jPB") }

}