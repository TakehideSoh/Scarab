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

class BlockedBCver0(sat: ISolver, ps: IVecInt, degree: Int) extends Constr with Propagatable with Undoable {
  def getAssertionLevel(x: IVecInt, y: Int): Int = ???
  def isSatisfied(): Boolean = ???
  def toString(x: VarMapper): String = ???

  val minisat: MinisatSolver[_] = sat match {
    case x: MinisatSolver[_] => x
    case _                   => sys.error("org.sat4j.minisat.core.Solver was expected")
  }

  val voc = minisat.getVocabulary
  var counter = 0
  
  val l0 = new Array[Int](ps.size)
  ps.moveTo(this.l0)

  val blit = l0.head
  val lits = l0.tail
  
  val maxUnsatisfied = lits.size - degree  
  
//  println(s"blit: $blit")
//  println(s"lits: ${lits.mkString(" ")}")

  register

  /*
   * (1)
   */
  def register {
    // for Boolean cardinality
    lits.foreach(q => voc.watch(q ^ 1, this))
    for (q <- lits if voc.isFalsified(q)) {
      counter += 1
      voc.undos(q ^ 1).push(this)
    }

    // for Blocked Literal
    voc.watch(blit ^ 1, this)
    if (voc.isFalsified(blit))
      voc.undos(blit ^ 1).push(this)
  }

  /*
   * (2)
   */
  def propagate(s: UnitPropagationListener, p: Int): Boolean = {
    def propBlockLit: Boolean = {
      if (counter > maxUnsatisfied)
        return false
      else if (counter == maxUnsatisfied) {
        try {
          var cnt = 0
          while (true) {
            if (voc.isUnassigned(lits(cnt)))
              if (!s.enqueue(lits(cnt), this))
                return false
            cnt += 1
          }
        } catch {
          case e: java.lang.ArrayIndexOutOfBoundsException => // loop finished!
        }
        return true
      } else {
        return true
      }
    }
    def propBCLit: Boolean = {
      if (voc.isSatisfied(blit)) { // (1) blit is true
//        println(s"BC: (1) blit is true")
        return true
      } else if (voc.isFalsified(blit)) { // (2) blit is false
//        println(s"BC: (2) blit is false")
        if (counter >= maxUnsatisfied) return false

        counter += 1
        voc.undos(p).push(this)

        if (counter < maxUnsatisfied) return true
        try {
          var cnt = 0
          while (true) {
            if (voc.isUnassigned(lits(cnt)))
              if (!s.enqueue(lits(cnt), this))
                return false
            cnt += 1
          }
        } catch {
          case e: java.lang.ArrayIndexOutOfBoundsException => // loop finished!
        }
        return true

      } else { // (3) blit is Unassigned
        if (counter >= maxUnsatisfied)
          return s.enqueue(blit, this)

        counter += 1

        voc.undos(p).push(this)

        return true
      }

    }
    voc.watch(p, this)
    
//    println(p)

    if ((p ^ 1) == blit)
      return propBlockLit
    else
      return propBCLit

  }

  /*
   * (3)
   */
  def calcReason(p: Int, outReason: IVecInt) {
    var c = if (p == ILits.UNDEFINED) -1 else 0 // undefined の時はこの制約が最初?
    outReason.push(blit ^ 1)
    for (q <- lits if voc.isFalsified(q)) {

      outReason.push(q ^ 1)

      c += 1
      if (c > maxUnsatisfied) return
    }
  }

  def canBePropagatedMultipleTimes = true

  
  
  def undo(p: Int) = if ((p ^ 1) != blit) counter -= 1

  /*
   * 20 more methods are needed. Almost of them can be written by one-liner.
   */
  def canBeSatisfiedByCountingLiterals = false
  def requiredNumberOfSatisfiedLiterals = 0
  def propagatePI(l: MandatoryLiteralListener, p: Int) = {
    throw new UnsupportedOperationException("Not implemented yet!");
  }

  def remove(upl: UnitPropagationListener) {
    for (q <- this.lits)
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
        if (c > this.maxUnsatisfied) {
          return ;
        }
      }
    }
  }
}
