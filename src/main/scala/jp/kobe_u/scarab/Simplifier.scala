package jp.kobe_u.scarab

/**
 * `Simplifier` is a class for translating CSP to clausal form.
 */
class Simplifier(val encoder: Encoder) {
  def flattenOr(c: Constraint): Seq[Constraint] = c match {
    case _: Literal => Seq(c)
    case _: And => Seq(c)
    case Or(cs @ _*) => cs.flatMap(flattenOr(_))
  }

  def isSimpleLiteral(lit: Literal): Boolean = lit match {
    case _: Bool | _: Not => true
    case LeZero(sum) => sum.coef.size <= 1
    case GeZero(sum) => sum.coef.size <= 1
    case EqZero(sum) => sum.coef.size <= 1
    case NeZero(sum) => sum.coef.size <= 1
  }

  def isSimpleClause(lits: Seq[Literal]): Boolean =
    lits.count(!isSimpleLiteral(_)) <= 1

  def tseitin(c: Constraint): (Literal, Seq[Or]) = c match {
    case lit: Literal => (lit, Seq.empty)
    case And(cs @ _*) => {
      val p = encoder.newBool
      (p, cs.map(Or(Not(p), _)))
    }
    case Or(cs @ _*) => {
      val p = encoder.newBool
      (p, Seq(Or(Not(p) +: cs)))
    }
  }

  def toCNF(c: Constraint): Seq[Seq[Literal]] = c match {
    case lit: Literal => Seq(Seq(lit))
    case And(cs @ _*) => cs.flatMap(toCNF(_))
    case _: Or => {
      val cs = flattenOr(c)
      val ts = cs.map(tseitin(_))
      val clause = ts.map(_._1)
      clause +: ts.flatMap(_._2).flatMap(toCNF(_))
    }
  }

  def simplify(c: Constraint): Seq[Seq[Literal]] =
    toCNF(c).flatMap { lits =>
      if (isSimpleClause(lits))
        Seq(lits)
      else {
        val ts = lits.map { lit =>
          if (isSimpleLiteral(lit))
            (lit, None)
          else {
            encoder.satSolver.nextFreeVarID(true)
            val p = encoder.newBool
            (p, Some(Seq(Not(p), lit)))
          }
        }
        val clause = ts.map(_._1)
        clause +: ts.flatMap(_._2)
      }
    }
}
