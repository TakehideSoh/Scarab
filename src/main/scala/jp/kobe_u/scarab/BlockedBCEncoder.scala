package jp.kobe_u.scarab

import org.sat4j.core.VecInt

class BlockedBCEncoder(csp: CSP, satSolver: SatSolver) extends OrderEncoder(csp, satSolver) {

  def useNativeBC(sum: Sum) =
    {
      if (sum.coef.size <= 2)
        false
      else
        sum.coef.forall(n => csp.dom(n._1).binary)
    }

  override def toLeZero(c: Constraint): Constraint = c match {
    case lit: LeZero  => lit
    case GeZero(sum)  => LeZero(sum.neg)
    case And(cs @ _*) => And(cs.map(toLeZero(_)))
    case Or(cs @ _*)  => Or(cs.map(toLeZero(_)))
    case EqZero(sum)  => And(LeZero(sum), LeZero(sum.neg))
    case NeZero(sum)  => Or(LeZero(sum + 1), LeZero(sum.neg + 1))
    case lit          => lit
  }

  def isBlockedLeZero(lits: Seq[Literal]) = lits match {
    case Seq(Not(p), LeZero(sum)) => sum.coef.size > 2
    case _                        => false
  }

  override def add(c: Constraint) {
    constIndex += 1
    constIndexMap += constIndex -> c
    val cc = toLeZero(c)
    for (lits <- simplifier.simplify(cc) if lits.size > 0) {
      if (lits.size == 1) {
        csp.add(lits.head)
        val clauses = encodeBC(lits, true)
        if (!clauses.isEmpty)
          addAllClauses(clauses, constIndex)
      } else if (isBlockedLeZero(lits)) {
        //        println("match")

        csp.add(Or(lits))

        lits match {
          case Seq(Not(p), LeZero(sum)) => {
            val axs = sum.coef.toSeq.map(xa => (xa._2, xa._1))
            var vec = axs.map(n => if (n._1 < 0) -1 * code(n._2) else code(n._2)).toArray
            val left_b = -1 * sum.b + axs.filter(i => i._1 > 0).foldLeft(0)((n, z) => n + z._1 * -1)
            val degree = left_b * -1

            satSolver.addBBC(-code(p), vec, degree)

          }
        }

      } else {
        csp.add(Or(lits))
        val clauses = encodeBC(lits, false)
        addAllClauses(clauses, constIndex)
      }
    }
  }

  // native PB や BC では余計な変数が出ている．．．
  def encodeBC(c: Seq[Literal], single: Boolean): Seq[Seq[Int]] =
    if (c.isEmpty) Seq(Seq.empty)
    else {
      for {
        clause0 <- this.encodeBC(c.tail, single)
        clause <- encodeBC(c.head, clause0, single)
      } yield clause
    }

  def encodeBC(lit: Literal, clause0: Seq[Int], single: Boolean): Seq[Seq[Int]] = lit match {
    case p: Bool => Seq(code(p) +: clause0)
    case Not(p)  => Seq(-code(p) +: clause0)
    case LeZero(sum) => {
      val axs = sum.coef.toSeq.map(xa => (xa._2, xa._1))
      if (useNativeBC(sum)) {

        // normalize --> all coefficients are positive value.
        var vec = axs.map(n => if (n._1 < 0) -1 * code(n._2) else code(n._2)).toArray
        var coef = axs.map(n => Math.abs(n._1)).toSeq
        val left_b = -1 * sum.b + axs.filter(i => i._1 > 0).foldLeft(0)((n, z) => n + z._1 * -1)
        val degree = left_b * -1

        if (single) {
          if (degree == 1) 
            satSolver.addClause(vec, 0)
          else
            satSolver.addAtLeast(vec, degree)
          Seq.empty
        } else {
          val p = this.newBool
          satSolver.addBBC(-code(p), vec, degree)
          Seq(code(p) +: clause0)
        }
      } else {
        encodeLe(axs, -sum.b, clause0)
      }
    }
    case lit => throw new Exception(s"BlockedBCEncoder detects not-acceptable Literal $lit")
  }

}