package jp.kobe_u.scarab

import jp.kobe_u.scarab._

object Tools {
  @deprecated def tt(map: Map[Var, Int], csp: CSP): (Term, Int, Int) = {
    def lbd(x: Var, a: Int) =
      if (a < 0) csp.dom(x).ub * a else csp.dom(x).lb * a
    def ubd(x: Var, a: Int) =
      if (a < 0) csp.dom(x).lb * a else csp.dom(x).ub * a

    val s = map.keys.toSeq
    if (map.size == 1) {
      (s(0) * map(s(0)), lbd(s(0), map(s(0))), ubd(s(0), map(s(0))))
    } else if (map.size == 2) {
      (s(0) * map(s(0)) + s(1) * map(s(1)), lbd(s(0), map(s(0))) + lbd(s(1), map(s(1))), ubd(s(0), map(s(0))) + ubd(s(1), map(s(1))))
    } else {
      val (xs1, xs2) = map.splitAt(map.size / 2)
      val (psum1, plb1, pub1) = tt(xs1, csp)
      val (psum2, plb2, pub2) = tt(xs2, csp)
      val v1 = csp.int(Var(), plb1, pub1)
      val v2 = csp.int(Var(), plb2, pub2)
      csp.add(v1 === psum1)
      csp.add(v2 === psum2)
      (v1 + v2, plb1 + plb2, pub1 + pub2)
    }
  }
  
  def alldiff(xs: Seq[Term], csp: CSP) = {
    def check(xs: Seq[Term]): Boolean =
      xs.forall(i => i match { case i: Var => true; case _ => false })
    if (check(xs)) {
      var xxs: Seq[Var] = Seq.empty
      for (i <- xs) { i match { case i: Var => xxs = xxs :+ i; case _ => } }
      val lb = for (x <- xxs) yield csp.dom(x).lb
      val ub = for (x <- xxs) yield csp.dom(x).ub
      val ph = And(Or(for (x <- xxs) yield !(x < lb.min + xxs.size - 1)),
        Or(for (x <- xxs) yield !(x > ub.max - xxs.size + 1)))
      def perm = And(for (num <- lb.min to ub.max)
        yield Or(for (x <- xxs) yield x === num))
      val extra = if (ub.max - lb.min + 1 == xxs.size) And(ph, perm) else ph
      And(And(for (Seq(x, y) <- xxs.combinations(2)) yield x !== y), extra)
    } else
      And(for (Seq(x, y) <- xs.combinations(2)) yield x !== y)
  }

  def binTrans(map: Map[Var, Int], csp: CSP): (Term, Int, Int) = {
    def lbd(x: Var, a: Int) =
      if (a < 0) csp.dom(x).ub * a else csp.dom(x).lb * a
    def ubd(x: Var, a: Int) =
      if (a < 0) csp.dom(x).lb * a else csp.dom(x).ub * a

    val s = map.keys.toSeq
    if (map.size == 1) {
      (s(0) * map(s(0)), lbd(s(0), map(s(0))), ubd(s(0), map(s(0))))
    } else if (map.size == 2) {
      (s(0) * map(s(0)) + s(1) * map(s(1)), lbd(s(0), map(s(0))) + lbd(s(1), map(s(1))), ubd(s(0), map(s(0))) + ubd(s(1), map(s(1))))
    } else {
      val (xs1, xs2) = map.splitAt(map.size / 2)
      val (psum1, plb1, pub1) = binTrans(xs1, csp)
      val (psum2, plb2, pub2) = binTrans(xs2, csp)
      val v1 = csp.int(Var(), plb1, pub1)
      val v2 = csp.int(Var(), plb2, pub2)
      csp.add(v1 === psum1)
      csp.add(v2 === psum2)
      (v1 + v2, plb1 + plb2, pub1 + pub2)
    }
  }  

}