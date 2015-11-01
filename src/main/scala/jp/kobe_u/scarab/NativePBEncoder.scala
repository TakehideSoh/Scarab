package jp.kobe_u.scarab

import org.sat4j.core.VecInt

class NativePBEncoder(csp: CSP, satSolver: SatSolver) extends OrderEncoder(csp, satSolver) {

  def useNativePB(sum: Sum) = {
    if (sum.coef.size <= 3)
      false
    else
      sum.coef.forall(n => csp.dom(n._1).binary)
  }

  override def toLeZero(c: Constraint): Constraint = c match {
    case lit: LeZero => lit
    case GeZero(sum) => LeZero(sum.neg)
    case And(cs @ _*) => And(cs.map(toLeZero(_)))
    case Or(cs @ _*) => Or(cs.map(toLeZero(_)))
    case EqZero(sum) => And(LeZero(sum), LeZero(sum.neg))
    case NeZero(sum) => Or(LeZero(sum + 1), LeZero(sum.neg + 1))
    case lit => lit
  }

  override def add(c: Constraint) {
    constIndex += 1
    constIndexMap += constIndex -> c    
    val cc = toLeZero(c)
    for (lits <- simplifier.simplify(cc) if lits.size > 0) {
      if (lits.size == 1) {
        csp.add(lits.head)
        val clauses = encodePB(lits, true)
        if (!clauses.isEmpty)
          addAllClauses(clauses, constIndex)
      } else {
        csp.add(Or(lits))
        val clauses = encodePB(lits, false)
        addAllClauses(clauses, constIndex)
      }
    }
  }

  def encodePB(c: Seq[Literal], single: Boolean): Seq[Seq[Int]] =
    if (c.isEmpty) Seq(Seq.empty)
    else {
      for {
        clause0 <- this.encodePB(c.tail, single)
        clause <- encodePB(c.head, clause0, single)
      } yield clause
    }

  def encodePB(lit: Literal, clause0: Seq[Int], single: Boolean): Seq[Seq[Int]] = lit match {
    case p: Bool => Seq(code(p) +: clause0)
    case Not(p) => Seq(-code(p) +: clause0)
    case LeZero(sum) => {
      val axs = sum.coef.toSeq.map(xa => (xa._2, xa._1))
      if (useNativePB(sum)) {
        var vec = axs.map(n => if (n._1 < 0) -1 * code(n._2) else code(n._2)).toArray
        var coef = axs.map(n => Math.abs(n._1)).toSeq
        val left_b = -1 * sum.b + axs.filter(i => i._1 > 0).foldLeft(0)((n, z) => n + z._1 * -1)
        val degree = left_b * -1

        def add {
          if (left_b * -1 > 0) {
            if (degree == 1)
              satSolver.addClause(vec, 0)
            else if (coef.forall(i => i == 1))
              satSolver.addAtLeast(vec, degree)
            else
              satSolver.addPB(vec, coef, degree)
          }
        }
        if (single) {
          add
          Seq.empty
        } else {
          val p = this.newBool
          vec = vec :+ code(p)
          coef = coef :+ left_b * -1
          add
          Seq(-code(p) +: clause0)
        }
      } else {
        encodeLe(axs, -sum.b, clause0)
      }
    }
    case lit => throw new Exception(s"NativePBEncoder detects not-acceptable Literal $lit")
  }

}