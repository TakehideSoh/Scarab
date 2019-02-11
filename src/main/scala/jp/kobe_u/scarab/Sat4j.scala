package jp.kobe_u.scarab

import org.sat4j.minisat.SolverFactory
import org.sat4j.minisat.core.{Solver => MinisatSolver}
import org.sat4j.specs.ISolver
import org.sat4j.specs.ContradictionException
import org.sat4j.core.VecInt
import org.sat4j.tools.ModelIterator
import org.sat4j.tools.DimacsStringSolver
import org.sat4j.tools.xplain.HighLevelXplain
import org.sat4j.tools.SolutionFoundListener
import org.sat4j.specs.IVecInt
import org.sat4j.tools.AllMUSes

import scala.collection.JavaConverters._

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
 * @author Takehide Soh and Naoyuki Tamura
 * @see [[http://www.sat4j.org "Sat4j web site"]] for more details.
 */
class Sat4j(option: String) extends SatSolver {
  def this() = this("Default")

  var clearlyUNSAT = false
  var xplain: Option[HighLevelXplain[ISolver]] = None
  var musListener: Option[SolutionFoundListener] = None
  var modelstock: Option[Seq[Boolean]] = None
  var nof_vars = 0

  val sat4j = option.capitalize match {
    case "Iterator" => new ModelIterator(SolverFactory.newDefault)
    case "Dimacs" => new DimacsStringSolver
    case "Xplain" => {
      val xp = new HighLevelXplain[ISolver](SolverFactory.newDefault)
      xplain = Option(xp)
      xp
    }
    case "AllXplain" => {
      musListener = Option(new SolutionFoundListener() {
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
      })

      val allMuses = new AllMUSes(true, SolverFactory.instance)
      allMuses.computeAllMUSesOrdered(musListener.get)
      allMuses.getSolverInstance
    }
    case name => SolverFactory.instance.createSolverByName(name)
  }

  private def minisat: MinisatSolver[_] = sat4j match {
    case x: MinisatSolver[_] => x
    case _ => sys.error("org.sat4j.minisat.core.Solver was expected")
  }

  def reset = {
    sat4j.reset
    clearlyUNSAT = false
  }

  def newVar(n: Int): Unit =
    xplain match {
      case Some(_) => sat4j.newVar(n)
      case None => sat4j.newVar(n)
    }

  def addClause(lits: Seq[Int]) {
    sat4j.addClause(new VecInt(lits.toArray))
  }

  def nextFreeVarID(reserve: Boolean) = sat4j.nextFreeVarId(reserve)

  def addClause(lits: Seq[Int], cIndex: Int): Int = {
    try {
      xplain match {
        case Some(xp) => {
          //          println(s"${lits.mkString(" v ")} ::: with index " + cIndex)
          xp.addClause(new VecInt(lits.toArray), cIndex)
        }
        case None => sat4j.addClause(new VecInt(lits.toArray))
      }
    } catch {
      case e: ContradictionException => clearlyUNSAT = true
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

  def addPB(lits: Seq[Int], coef: Seq[Int], degree: Int) =
    sat4j.addConstr(new NativePB(minisat, minisat.dimacs2internal(new VecInt(lits.toArray)), coef, degree))

  def addConstr(c: org.sat4j.specs.Constr) = {
    sat4j.addConstr(c)
  }

  def getVocabulary =
    minisat.getVocabulary

  def dimacs2internal(ints: org.sat4j.specs.IVecInt) =
    minisat.dimacs2internal(ints)

  def isSatisfiable =
    !clearlyUNSAT && sat4j.isSatisfiable
  def isSatisfiable(assumps: Seq[Int]) = {
    !clearlyUNSAT && sat4j.isSatisfiable(new VecInt(assumps.toArray))
  }

  def getModelArray: Array[Int] = // return model array starting with 0, i.e., here 0 means variable numbered 1 in DIMACS
    sat4j.model

  def model(v: Int) = { // return truth value assigned to variable numbered n in DIMACS
    //    println(v)
    modelstock match {
      case Some(ms) => ms(v - 1)
      case None => sat4j.model(v)
    }
  }

  // def findModel: Array[Int] =
  //    sat4j.findModel
  //  def findModel(assumps: Seq[Int]): Array[Int] =
  //    sat4j.findModel(new VecInt(assumps.toArray))

  def findMinimalModel(ps: Seq[Int]): Option[Seq[Boolean]] = {
    if (!isSatisfiable) return None

    var ts = ps.filter(i => if (i < 0) !sat4j.model(math.abs(i)) else sat4j.model(i)).map(-_)
    var fs = ps.filter(i => if (i < 0) sat4j.model(math.abs(i)) else !sat4j.model(i)).map(-_)

    val nofVars = sat4j.nextFreeVarId(false) - 1
    modelstock = Option((1 to nofVars).map(sat4j.model(_)))
    sat4j.addClause(new VecInt(ts.toArray))

    while (isSatisfiable(fs)) {
      modelstock = Option((1 to nofVars).map(sat4j.model(_)))
      ts = ps.filter(i => if (i < 0) !sat4j.model(math.abs(i)) else sat4j.model(i)).map(-_)
      fs = ps.filter(i => if (i < 0) sat4j.model(math.abs(i)) else !sat4j.model(i)).map(-_)
      if (ts.isEmpty)
        return modelstock
      else
        sat4j.addClause(new VecInt(ts.toArray))
    }
    modelstock
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
        case (true, true) =>
        case (false, false) =>
      }
    }
    bb
  }

  def nVars = sat4j.nVars

  def nConstraints = sat4j.nConstraints

  def getStat = {
    import scala.collection.JavaConverters._
    sat4j.getStat.asScala
  }

  def setTimeout(time: Int) = {
    if (time > 0)
      sat4j.setTimeout(time)
  }

  def clearLearntClauses = sat4j.clearLearntClauses

  def dumpStat = printStatCore(System.out)
  /* dump Statistics */
  private def printStatCore(file: java.io.OutputStream) = {
    import java.io.PrintWriter
    val out = new PrintWriter(new java.io.BufferedOutputStream(file))
    val header = s"#variables: ${sat4j.nVars}, #clauses: ${nConstraints}"
    out.println(header)
    sat4j.printInfos(out)
    sat4j.printStat(out)
    out.flush
  }

  def dumpStat(filePath: String) = {
    val fos = new java.io.FileOutputStream(new java.io.File(filePath))
    printStatCore(fos)
    fos.close
  }
  def dumpStat(pw: java.io.PrintWriter) = {
    sat4j.printInfos(pw)
    sat4j.printStat(pw)
  }

  /* dump CNF */
  private def dumpCnfCore(file: java.io.OutputStream) = sat4j match {
    case sol: org.sat4j.tools.DimacsStringSolver => {
      var header = Seq("p", "cnf", sat4j.nVars, nConstraints).mkString(" ")
      val n = 64
      while (header.length() < n) header += " "

      if (sol.getOut.charAt(0) == 'p') sol.getOut.delete(0, n)
      sol.getOut.insert(0, header)
      import java.io.PrintWriter
      val out = new PrintWriter(new java.io.BufferedOutputStream(file))
      out.println(sol.getOut)
      out.flush
    }
    case _ => {
      throw new UnsupportedOperationException("This SAT Solver does not support printFile method")
    }
  }
  def dumpCnf = dumpCnfCore(System.out)
  def dumpCnf(filePath: String) = {
    val fos = new java.io.FileOutputStream(new java.io.File(filePath))
    dumpCnfCore(fos)
    fos.close
  }
  def dumpCnf(pw: java.io.PrintWriter) = dumpCnfCore(pw.asInstanceOf[java.io.OutputStream])

  def setNumberOfVariables(n: Int): Unit = { nof_vars = n }

  def minExplain: Array[Int] = {
    xplain match {
      case Some(xp) =>
        //        sat4j.unsatExplanation

        val tmp = xp.minimalExplanation

        //        for (i <- tmp)
        //          println(i)

        tmp

      case None =>
        throw new UnsupportedOperationException("This SAT Solver does not support minimal explanation.")
    }
  }

  def minAllExplain {
    val allMuses = new AllMUSes(true, SolverFactory.instance)
    musListener match {
      case Some(ml) =>
        allMuses.computeAllMSS(ml)
        allMuses.computeAllMUSesOrdered(ml)
      //    allMuses.computeAllMUSesOrdered(musListener)
      case None =>
        throw new UnsupportedOperationException("This SAT Solver does not support minimal all explanation.")
    }
  }

}
