package jp.kobe_u.scarab

/**
  * `OrderEncoder` is a class for translating CSP to SAT by order encoding.
  */
class OrderEncoder(csp: CSP, satSolver: SatSolver) extends Encoder(csp, satSolver) {
  val simplifier = new Simplifier(this)

  // x <= b
  def le(x: Var, b: Int): Int = {
    if (b < lb(x)) FalseLit
    else if (b >= ub(x)) TrueLit
    else code(x) + csp.dom(x).pos(b)
  }

  // a * x <= b
  def le(a: Int, x: Var, b: Int): Int =
    if (a > 0) le(x, floorDiv(b, a))
    else -le(x, ceilDiv(b, a) - 1)

  def satVariablesSize(x: Var) = {
    if (csp.dom(x).isContiguous)
      csp.dom(x).ub - csp.dom(x).lb
    else
      csp.dom(x).domain.size - 1
  }

  def encode(x: Var): Seq[Seq[Int]] = {
    if (csp.dom(x).isContiguous)
      for (b <- lb(x) + 1 to ub(x) - 1)
        yield Seq(-le(x, b - 1), le(x, b))
    else {
      val d = csp.dom(x).domain.toSeq
      for (i <- 1 to d.size - 2)
        yield Seq(-le(x, d(i - 1)), le(x, d(i)))
    }
  }

  def range(v: Var, LB: Int, UB: Int) = {
    if (csp.dom(v).isContiguous) LB to UB
    else csp.dom(v).domain.filter(i => (LB <= i) && (i <= UB))
  }

  /*
   * core part of order encoding
   */
  def encodeLe(axs: Seq[(Int, Var)], c: Int, clause0: Seq[Int]): Seq[Seq[Int]] = axs match {
    case Seq() => if (c >= 0) Seq.empty else Seq(clause0)
    case Seq((a, x)) => Seq(clause0 :+ le(a, x, c))
    case Seq((a, x), axs1@_*) => {
      if (a > 0) {
        val ub0 = floorDiv(c - lb(axs1), a)
        val LB = lb(x)
        val UB = math.min(ub(x), ub0)

        if (LB > UB) return Seq(clause0)

        val cs = for {
          b <- range(x, LB, UB)
          lit = le(x, b - 1)
          if lit != TrueLit
          clause <- encodeLe(axs1, c - a * b, lit +: clause0)
        } yield clause

        if (ub(x) > ub0) cs ++ Seq(clause0 :+ le(x, ub0))
        else cs

      } else {
        val lb0 = floorDiv(c - lb(axs1), a)
        val LB = math.max(lb(x), lb0)
        val UB = ub(x)

        if (LB > UB) return Seq(clause0)
        val cs = for {
          b <- range(x, LB, UB)
          lit = -le(x, b)
          if lit != TrueLit
          clause <- encodeLe(axs1, c - a * b, lit +: clause0)
        } yield clause
        if (lb(x) < lb0) cs ++ Seq(clause0 :+ -le(x, lb0 - 1))
        else cs
      }
    }
  }

  def encode(lit: Literal, clause0: Seq[Int]): Seq[Seq[Int]] = lit match {
    case p: Bool => Seq(code(p) +: clause0)
    case Not(p) => Seq(-code(p) +: clause0)
    case LeZero(sum) => {
      val axs = sum.coef.toSeq.map(xa => (xa._2, xa._1)).sortWith((a, b) =>
        csp.dom(a._2).size == csp.dom(b._2).size match {
          case true => Math.abs(a._1) > Math.abs(b._1)
          case false => csp.dom(a._2).size < csp.dom(b._2).size
        })
      encodeLe(axs, -sum.b, clause0)
    }
    case _ => throw new Exception("OrderEncoder cannot accept EqZero and NeZero Literal.")
  }

  def add(c: Constraint) {
    constIndex += 1
    constIndexMap += constIndex -> c
    for (lits <- simplifier.simplify(toLeZero(c)) if lits.size > 0) {
      if (lits.size == 1)
        csp.add(lits(0))
      else
        csp.add(Or(lits))

      val clauses = encode(lits)
      addAllClauses(clauses, constIndex)
    }
  }

  def extractAssumpLits(cs: Seq[Constraint]): Seq[Int] = {
    var ls: Set[Int] = Set.empty
    var p: Option[Bool] = None
    for (c <- cs)
      for (lits <- simplifier.simplify(toLeZero(c))) {
        if (lits.size == 1) {
          val i = lits(0) match {
            case q: Bool => code(q)
            case Not(q) => -code(q)
            case LeZero(sum) => {
              if (sum.coef.size == 1) {
                val x = sum.coef.keys.head
                le(sum.coef(x), x, -sum.b)
              } else {
                p = p orElse Option(newBool)
                csp.add(Or(Not(p.get), LeZero(sum)))
                code(p.get)
              }
            }
            case e => throw new java.lang.Exception(s"Unexpected type is detected in extractAssumpLits: ${e}")
          }
          if (i != TrueLit) ls += i
        } else {
          p = p orElse Option(newBool)
          csp.add(Or(lits :+ Not(p.get)))
        }
      }
    super.encodeCSP
    p match {
      case None => ls.toSeq
      case Some(b) => (ls + code(b)).toSeq
    }
  }

  def decode(x: Var) = {
    if (csp.dom(x).isContiguous)
      (lb(x) to ub(x) - 1).find(b => satSolver.model(le(x, b))).getOrElse(ub(x))
    else {
      val d = csp.dom(x).domain.toSeq.dropRight(1)
      d.find(i => satSolver.model(le(x, i))).getOrElse(ub(x))
    }
  }

}
