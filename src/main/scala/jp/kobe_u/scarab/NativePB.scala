package jp.kobe_u.scarab

import org.sat4j.core.VecInt
import org.sat4j.minisat.core.{ ILits, Undoable, Solver => MinisatSolver }
import org.sat4j.specs.{ Constr, ContradictionException, IVecInt, IteratorInt, MandatoryLiteralListener,
  Propagatable, UnitPropagationListener, ISolver, VarMapper }

/**
 * `Native PB` is a case class for a Pseudo Boolean constraint.
 * `Native PB` object consists of the vocabulary of satSolver, a vector of SAT variables, a sequence of coefficient, and a threshold degree.
 * It reprensents the linear comparison a1*x1 + a2*x2 + ... + an*xn >= -1 * b
 */
case class NativePB(sat: ISolver, ps: IVecInt, var coef: Seq[Int], var degree: Int) extends Constr with Propagatable with Undoable {
  def getAssertionLevel(x: IVecInt, y: Int): Int = ???
  def isSatisfied(): Boolean = ???
  def toString(x: VarMapper): String = ???

  var tmpCoef = Seq.empty[Int]

  coef = coef.map(i => if (i > degree) degree else i)
//  if (coef.min != 1) normalizeGCD

  val minisat: MinisatSolver[_] = sat match {
    case x: MinisatSolver[_] => x
    case _ => sys.error("org.sat4j.minisat.core.Solver was expected")
  }

  val voc = minisat.getVocabulary
  val maxUnsatisfied = coef.sum - degree
  var counter = 0
  var coefMap: Map[Int, Int] = Map.empty
  var lits = new Array[Int](ps.size)
  val maxCoef = coef.max
  ps.moveTo(this.lits)
  
// println(s"${coef.mkString(" ")} >= ${degree}")  
  register
//   println(s"registerd. ${coef.mkString(" ")} >= ${degree}")  

//  private def normalizeGCD {
//    var gcd: BigInt = coef(0);
//    for (i <- 1 until coef.size) {
//      gcd = BigInt.int2bigInt(coef(i)).gcd(gcd)
//      if (gcd.equals(BigInt.int2bigInt(1))) return
//    }
//    //    println(s"gcd ${gcd}")
//    for (i <- 0 until coef.size)
//      tmpCoef = tmpCoef :+ (BigInt.int2bigInt(coef(i)) / gcd).toInt
//
//    coef = tmpCoef
//    degree = ((BigInt.int2bigInt(degree) + gcd - 1) / gcd).toInt
//  }

  def toDimacs(p: Int) = if ((p & 1) == 0) 1 * (p >> 1) else -1 * (p >> 1)
  /*
   * (1)
   */
  def register {
    val coefTotal = coef.sum

    // check inconsistency    
    if (coefTotal < degree) { // PB is obviously UNSAT 
      sat.addClause(new VecInt(Seq(1).toArray))
      sat.addClause(new VecInt(Seq(-1).toArray))
    }

    // for all lits
    for (i <- 0 until lits.size) {

      // (a) watch each literal --- リテラルが偽になることを監視する
      voc.watch(lits(i) ^ 1, this)

      // (b) construct map: literals -> coefficients 
      coefMap = coefMap + ((lits(i) >> 1) -> coef(i))

      // (c) check literals already falsified
      if (voc.isFalsified(lits(i))) {
        voc.undos(lits(i) ^ 1).push(this)
        counter += coef(i)
      }

      // (d) unit clauses: literals must be satisfied
      if (coefTotal - coef(i) < degree) {
        sat.addClause(new VecInt(Seq(toDimacs(lits(i))).toArray))
      }
    }

  }

  /*
   * (2) 
   * voc.watch(p, this) <----------------- watch p
   * voc.undos(p).push(this) <------------ add this NativePB constraint to undo-list
   * 
   * (counter + value + maxCoef < maxUnsatisfied) <--- Condition1: propagation OK, no more propagation
   * (counter + value > maxUnsatisfied) <------------- Condition2: conflict
   * (counter + value + l > maxUnsatisfied) <--------- Condition3: propagation
   */
  def propagate(s: UnitPropagationListener, p: Int): Boolean = {
    voc.watch(p, this) 
    val value = coefMap(p >> 1) // p は符号が反対になっているので >> 1 して PB 中のリテラルを示すようにする

    if (counter + value > maxUnsatisfied) return false

    counter += value

    if ((counter + maxCoef) <= maxUnsatisfied) {
      voc.undos(p).push(this)
      return true
    }

    voc.undos(p).push(this)

    var cnt = 0
    try {
      while (true) {
        if (voc.isUnassigned(lits(cnt)) && counter + coef(cnt) > maxUnsatisfied)
          if (!s.enqueue(lits(cnt), this))
            return false
        cnt += 1
      }
    } catch {
      case e: java.lang.ArrayIndexOutOfBoundsException => // loop finished!
    }
    true
  }

  /*
   * (3)
   */
  def calcReason(p: Int, outReason: IVecInt) {
    var c = 0
    var cnt = 0
    try {
      while (true) {
        if (voc.isFalsified(lits(cnt))) {
          outReason.push(lits(cnt) ^ 1)
          c += coef(cnt)
          if (c > maxUnsatisfied) return
        }
        cnt += 1
      }
    } catch {
      case e: java.lang.ArrayIndexOutOfBoundsException => // loop finished!
    }
    //    for (i <- 0 until lits.size if voc.isFalsified(lits(i))) {
    //      outReason.push(lits(i) ^ 1)
    //      c += coef(i)
    //      if (c > maxUnsatisfied) return
    //    }
  }

  def canBePropagatedMultipleTimes = true
  def undo(p: Int) = counter -= coefMap(p >> 1)

  // 20 more methods are needed. Almost of them can be written by one-liner. 
  def canBeSatisfiedByCountingLiterals = true
  def requiredNumberOfSatisfiedLiterals = 0
  def propagatePI(l: MandatoryLiteralListener, p: Int) = {
    throw new UnsupportedOperationException("Not implemented yet!");
  }
  def remove(upl: UnitPropagationListener) {
    for (q <- this.lits) voc.watches(q ^ 1).remove(this);
  }
  def simplify: Boolean = false
  def learnt = false
  def getActivity = 0.0
  def setActivity(d: Double) = println("setActivity")
  def incActivity(claInc: Double) = println("incActivity")
  def locked = true
  def setLearnt() = throw new UnsupportedOperationException()
  def size = lits.size
  def get(i: Int) = lits(i)
  def rescaleBy(d: Double) = throw new UnsupportedOperationException()
  def assertConstraint(s: UnitPropagationListener) = throw new UnsupportedOperationException()
  def assertConstraintIfNeeded(s: UnitPropagationListener) = throw new UnsupportedOperationException()
  def forwardActivity(claInc: Double) {}
  def toConstraint = this
  def calcReasonOnTheFly(p: Int, trail: IVecInt, outReason: IVecInt) {
    System.out.println("calcReasonOnTheFly");
    var c = if (p == ILits.UNDEFINED) -1 * coefMap(p >> 1) else 0
    val vlits: IVecInt = new VecInt(this.lits)
    val it: IteratorInt = trail.iterator()
    while (it.hasNext) {
      val q = it.next()
      if (vlits.contains(q ^ 1)) {
        outReason.push(q)
        c += coefMap(q >> 1)
        if (c > this.maxUnsatisfied) {
          return
        }
      }
    }
  }
}
