package jp.kobe_u.scarab

import jp.kobe_u.scarab._

object Tool {
  def tt(map: Map[Var, Int], csp: CSP): (Term, Int, Int) = {
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

}