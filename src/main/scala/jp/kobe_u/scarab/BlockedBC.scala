package jp.kobe_u.scarab

import org.sat4j.minisat.constraints.card.AtLeast

import org.sat4j.core.VecInt
import org.sat4j.minisat.core.{ ILits, Undoable, Solver => MinisatSolver }
import org.sat4j.specs.{
  Constr,
  ContradictionException,
  IVecInt,
  IteratorInt,
  MandatoryLiteralListener,
  Propagatable,
  UnitPropagationListener,
  ISolver,
  VarMapper
}

/*
 * It reprensents a Blocked Boolean cardinality b v (x1 + x2 + ... + xn >= degree)
 */
class BlockedBC(sat: ISolver, ps: IVecInt, degree: Int) extends Constr with Propagatable with Undoable {
  def getAssertionLevel(x: IVecInt, y: Int): Int = ???
  def isSatisfied(): Boolean = ???
  def toString(x: VarMapper): String = ???

  val minisat: MinisatSolver[_] = sat match {
    case x: MinisatSolver[_] => x
    case _                   => sys.error("org.sat4j.minisat.core.Solver was expected")
  }

  val voc = minisat.getVocabulary
  var falsifiedRHS = 0

  val l0 = new Array[Int](ps.size)
  ps.moveTo(this.l0)

  val blit = l0.head
  val lits = l0.tail

  val n = lits.size

  val maxFalsified = n

  register

  /* (1) register */
  def register {
    l0.foreach(q => voc.watch(q ^ 1, this))
    for (q <- l0 if voc.isFalsified(q)) {
      if (q == blit) falsifiedRHS += degree 
      else falsifiedRHS += 1
      voc.undos(q ^ 1).push(this)
    }
  }

  /* (2) propagete */
  def propagate(s: UnitPropagationListener, p: Int): Boolean = {
    
    voc.watch(p, this)
    val value = if ((p ^ 1) == blit) degree else 1

    if (falsifiedRHS + value > maxFalsified) return false

    falsifiedRHS += value

    voc.undos(p).push(this)

    if ((falsifiedRHS + degree) <= maxFalsified) return true // optional 

    if (voc.isUnassigned(l0(0)) && falsifiedRHS + degree > maxFalsified && !s.enqueue(l0(0), this))
      return false

    if (falsifiedRHS == maxFalsified) {
      var cnt = 1
      try {
        while (true) {
          if (voc.isUnassigned(l0(cnt)) && !s.enqueue(l0(cnt), this))
            return false
          cnt += 1
        }
      } catch {
        case e: java.lang.ArrayIndexOutOfBoundsException => // loop finished!
      }
    }
    true
  }

  /* (3) clacReason */
  def calcReason(p: Int, outReason: IVecInt) {
    var c = 0
    outReason.push(l0(0) ^ 1)
    c += degree
    var cnt = 1
    try {
      while (true) {
        if (voc.isFalsified(l0(cnt))) {
          outReason.push(l0(cnt) ^ 1)
          c += 1
          if (c > maxFalsified) return
        }
        cnt += 1
      }
    } catch {
      case e: java.lang.ArrayIndexOutOfBoundsException => // loop finished!
    }
  }

  def canBePropagatedMultipleTimes = true

  def undo(p: Int) = if ((p ^ 1) == blit) falsifiedRHS -= degree else falsifiedRHS -= 1

  /*
   * 20 more methods are needed. Almost of them can be written by one-liner.
   */
  def canBeSatisfiedByCountingLiterals = false
  def requiredNumberOfSatisfiedLiterals = 0
  def propagatePI(l: MandatoryLiteralListener, p: Int) = {
    throw new UnsupportedOperationException("Not implemented yet!");
  }

  def remove(upl: UnitPropagationListener) {
    for (q <- this.l0)
      voc.watches(q ^ 1).remove(this);
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
    var c = if (p == ILits.UNDEFINED) -1 else 0;
    val vlits: IVecInt = new VecInt(this.lits);
    val it: IteratorInt = trail.iterator();
    while (it.hasNext) {
      val q = it.next();
      if (vlits.contains(q ^ 1)) {
        outReason.push(q);
        c += 1
        if (c > this.maxFalsified) {
          return ;
        }
      }
    }
  }
}
