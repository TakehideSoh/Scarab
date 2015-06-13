package jp.kobe_u.scarab.solver

import scala.collection.JavaConversions
import org.sat4j.minisat.SolverFactory
import org.sat4j.minisat.core.{ Solver => MinisatSolver }
import org.sat4j.specs.ISolver
import org.sat4j.specs.ContradictionException
import org.sat4j.specs.TimeoutException
import org.sat4j.core.VecInt
import org.sat4j.tools.xplain.Xplain
import org.sat4j.tools.xplain.Explainer
import org.sat4j.tools.ModelIterator
import org.sat4j.tools.DimacsStringSolver
import org.sat4j.tools.xplain.HighLevelXplain
import org.sat4j.tools.SolutionFoundListener
import org.sat4j.specs.IVecInt
import org.sat4j.tools.AllMUSes

/**
 * Wrapper class of [[http://www.sat4j.org "Sat4j solver"]].
 *
 * `org.sat4j.minisat.SolverFactory.newDefault` is used as the default solver.
 * You can replace it by re-assigning to the variable `solver`.
 *
 * `ContradictionException` and `TimeoutException` might happen when adding clauses or solving the problem.
 * It should be caught at the caller.
 *
 * Sat4j solver. `org.sat4j.minisat.SolverFactory.newDefault` is used as the default solver.
 * The following configuration is available in Sat4j version 2.3.5:
 * MiniSATHeap, Glucose, MiniLearningHeap, MiniLearningHeapEZSimp, MiniLearningHeapExpSimp, MiniLearningHeapRsatExpSimp,
 * MiniLearningHeapRsatExpSimpBiere, MiniLearningHeapRsatExpSimpLuby, MiniLearningHeapEZSimpNoRestarts,
 * DefaultMS21PhaseSaving, DefaultAutoErasePhaseSaving, Glucose21, BestWL, BestHT, Best17, MiniLearningHeapEZSimpLongRestarts,
 * MinOneSolver, Backjumping, SAT, UNSAT, and GreedySolver
 *
 * @author Naoyuki Tamura and Takehide Soh
 * @see [[http://www.sat4j.org "Sat4j web site"]] for more details.
 */
class Sat4j(option: String) extends SatSolver {
  def this() = this("Default")

  var clearlyUNSAT = false
  var xplain: HighLevelXplain[ISolver] = null
  var musListener: SolutionFoundListener = null
  var modelstock: Seq[Boolean] = null
  var nof_vars = 0

  var sat4j = option.capitalize match {
    case "Iterator" => new ModelIterator(SolverFactory.newDefault)
    case "Dimacs"   => new DimacsStringSolver
    case "Xplain" => {
      val xp = new HighLevelXplain[ISolver](SolverFactory.newDefault)
      xplain = xp
      xp
    }
    case "AllXplain" => {
      musListener = new SolutionFoundListener() {
        def onSolutionFound(s: Array[Int]) {
          println("ok1")
        }
        def onSolutionFound(s: IVecInt) {
          println("ok2")
          val localMus = Array.fill(s.size)(0)
          s.copyTo(localMus)

          val tmp = for (i <- localMus) yield i
          println(tmp.mkString(" "))
        }
        def onUnsatTermination() {
          println("ok3")
        }
      }

      val allMuses = new AllMUSes(true, SolverFactory.instance)
      allMuses.computeAllMUSesOrdered(musListener)
      allMuses.getSolverInstance
    }
    case name => SolverFactory.instance.createSolverByName(name)
  }
  private def minisat: MinisatSolver[_] = sat4j match {
    case x: MinisatSolver[_] => x
    case _ => sys.error("org.sat4j.minisat.core.Solver was expected")
  }

  def reset = sat4j.reset

  def newVar(n: Int) = {
    if (xplain != null) sat4j.newVar(n * 4)
    else sat4j.newVar(n)
  }

  //  def addAllClauses(clauses: Seq[Seq[Int]]) =
  //    for (clause <- clauses) addClause(clause)

  def addClause(lits: Seq[Int]) {
    sat4j.addClause(new VecInt(lits.toArray))
  }

  def addClause(lits: Seq[Int], cIndex: Int): Int = {
    try {
      if (xplain != null)
        xplain.addClause(new VecInt(lits.toArray), cIndex)
      else {
//        println(lits)
        sat4j.addClause(new VecInt(lits.toArray))
      }

    } catch {
      case e: ContradictionException         => clearlyUNSAT = true
      case e: java.lang.NoClassDefFoundError => println(s"$e ${lits}")
    }
    cIndex
  }

  def addAtLeast(lits: Seq[Int], degree: Int) =
    sat4j.addAtLeast(new VecInt(lits.toArray), degree)

  def addAtMost(lits: Seq[Int], degree: Int) =
    sat4j.addAtMost(new VecInt(lits.toArray), degree)

  def addExactly(lits: Seq[Int], degree: Int) =
    sat4j.addExactly(new VecInt(lits.toArray), degree)

  //  def addConstr(c: org.sat4j.specs.Constr) =
  //    sat4j.addConstr(c)

  def addPB(lits: Seq[Int], coef: Seq[Int], degree: Int) = {
    sat4j.addConstr(new NativePB(minisat, minisat.dimacs2internal(new VecInt(lits.toArray)), coef, degree))

  }
  def addConstr(c: org.sat4j.specs.Constr) = {
    sat4j.addConstr(c)
  }

  def getVocabulary =
    minisat.getVocabulary

  def dimacs2internal(ints: org.sat4j.specs.IVecInt) =
    minisat.dimacs2internal(ints)

  def isSatisfiable =
    !clearlyUNSAT && sat4j.isSatisfiable
  def isSatisfiable(assumps: Seq[Int]) =
    !clearlyUNSAT && sat4j.isSatisfiable(new VecInt(assumps.toArray))
  def model: Array[Int] =
    sat4j.model

  def model(v: Int) = {
    if (modelstock != null) {
      modelstock(v - 1)
    } else {
      sat4j.model(v)
    }

  }

  //  def findModel: Array[Int] =
  //    sat4j.findModel
  //  def findModel(assumps: Seq[Int]): Array[Int] =
  //    sat4j.findModel(new VecInt(assumps.toArray))

  def findMinimalModel(ps: Seq[Int]): Option[Seq[Boolean]] = {
    if (!isSatisfiable) return None

    var ts = ps.filter(i => if (i < 0) !sat4j.model(math.abs(i)) else sat4j.model(i)).map(-_)
    var fs = ps.filter(i => if (i < 0) sat4j.model(math.abs(i)) else !sat4j.model(i)).map(-_)

//    println(s"ps $ps")
//    println(s"ts $ts")
//    println(s"fs $fs")    
    
    modelstock = (1 to nof_vars).map(sat4j.model(_))

    sat4j.addClause(new VecInt(ts.toArray))
    while (isSatisfiable(fs)) {
      modelstock = (1 to nof_vars).map(sat4j.model(_))
      ts = ps.filter(i => if (i < 0) !sat4j.model(math.abs(i)) else sat4j.model(i)).map(-_)
      fs = ps.filter(i => if (i < 0) sat4j.model(math.abs(i)) else !sat4j.model(i)).map(-_)
      //      ts = ps.filter(i => sat4j.model(i)).map(-_)
      //      fs = ps.filter(i => !sat4j.model(i)).map(-_)
      sat4j.addClause(new VecInt(ts.toArray))
    }

    Option(modelstock)
  }

  def findBackbone(ps: Seq[Int]): Set[Int] = {
    var bb = Set.empty[Int]
    for (p <- ps) {
      val res1 = isSatisfiable(Seq(p))
      val res2 = isSatisfiable(Seq(-p))

      (res1, res2) match {
        case (true, false) => {
          bb += p
          addClause(Seq(p))
        }
        case (false, true) => {
          bb += -p
          addClause(Seq(-p))
        }
        case (true, true)   =>
        case (false, false) =>
      }
    }
    bb
  }

  def nVars =
    sat4j.nVars
  def nConstraints =
    sat4j.nConstraints
  def getStat =
    JavaConversions.mapAsScalaMap(sat4j.getStat).toMap
  def setTimeout(time: Int) = {
    if (time > 0)
      sat4j.setTimeout(time)
  }
  def clearLearntClauses =
    sat4j.clearLearntClauses
  def printInfos(out: java.io.PrintWriter) =
    sat4j.printInfos(out)
  def printStat(out: java.io.PrintWriter) =
    sat4j.printStat(out)

  def dumpCNF = sat4j match {
    case sol: org.sat4j.tools.DimacsStringSolver => {
      print(Seq("p", "cnf", sat4j.nVars, nConstraints).mkString(" "))
      (sol.getOut)
    }
    case _ => {
      throw new UnsupportedOperationException("This SAT Solver does not support printFile method")
    }
  }

  def writeCNF(name: String, vars: Int) = sat4j match {
    case sol: org.sat4j.tools.DimacsStringSolver => {
      sol.getOut.insert(0, Seq("p", "cnf", sat4j.nVars, nConstraints).mkString(" "))
      import java.io.PrintWriter
      val out = new PrintWriter(name)
      out.println(sol.getOut)
      out.close
    }
    case _ => {
      throw new UnsupportedOperationException("This SAT Solver does not support printFile method")
    }
  }

  def setNumberOfVariables(n: Int): Unit = { nof_vars = n }

  def minExplain: Array[Int] = {
    if (xplain != null) {
      sat4j.unsatExplanation
      xplain.minimalExplanation
    } else
      throw new UnsupportedOperationException("This SAT Solver does not support minimal explanation.")
  }

  def minAllExplain {
    println("ok")
    val allMuses = new AllMUSes(true, SolverFactory.instance)
    allMuses.computeAllMSS(musListener);
    allMuses.computeAllMUSesOrdered(musListener)
    //    allMuses.computeAllMUSesOrdered(musListener)
  }

}
