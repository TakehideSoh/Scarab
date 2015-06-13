package jp.kobe_u.scarab.solver

import jp.kobe_u.scarab.csp._

/** `SatSolver` is an abstract class for SAT solver.
 * Currently, only the [[jp.kobe_u.scarab.solver.Sat4j]] is its implementation.
 */
trait SatSolver {
  /* Basic Interface */
  def newVar(n: Int): Unit
  def setNumberOfVariables(n: Int): Unit
  def addClause(lits: Seq[Int], cIndex: Int): Int
  def addClause(lits: Seq[Int])
  def isSatisfiable: Boolean
  def model: Array[Int]
  def model(v: Int): Boolean
  def reset: Unit  
  
  /* Advanced Interface */
  def isSatisfiable(assumps: Seq[Int]): Boolean  
  def addAtLeast(lits: Seq[Int], degree: Int): Unit
  def addAtMost(lits: Seq[Int], degree: Int): Unit
  def addExactly(lits: Seq[Int], degree: Int): Unit
  def addPB(lits: Seq[Int], coef: Seq[Int], degree: Int): Unit
  def minExplain: Array[Int]
  def minAllExplain: Unit
  def clearLearntClauses: Unit
  def findMinimalModel(ps: Seq[Int]): Option[Seq[Boolean]]
  def findBackbone(ps: Seq[Int]): Set[Int]

  /* Supplemental Interface */
  def nVars: Int
  def nConstraints: Int
  def setTimeout(time: Int)
  
  def printInfos(out: java.io.PrintWriter)
  def printStat(out: java.io.PrintWriter)
  def writeCNF(name: String, vars: Int)
  def dumpCNF
  
}