import jp.kobe_u.scarab._, dsl._

object HowToWriteProblems {

  def LatinSquare(n: Int) {
    for (i <- 1 to n; j <- 1 to n) int('x(i, j), 1, n)
    for (i <- 1 to n) {
      add(alldiff((1 to n).map(j => 'x(i, j))))
      add(alldiff((1 to n).map(j => 'x(j, i))))
      add(alldiff((1 to n).map(j => 'x(j, (i + j - 1) % n + 1))))
      add(alldiff((1 to n).map(j => 'x(j, (i + (j - 1) * (n - 1)) % n + 1))))
    }

    if (find) println(solution)
    reset
  }

  def SquarePacking(n: Int, s: Int) {
    for (i <- 1 to n) { int('x(i), 0, s - i); int('y(i), 0, s - i) }
    for (i <- 1 to n; j <- i + 1 to n)
      add(('x(i) + i <= 'x(j)) || ('x(j) + j <= 'x(i)) || ('y(i) + i <= 'y(j)) || ('y(j) + j <= 'y(i)))

    if (find) println(solution)
    reset
  }

  def LangfordPair_Model1(n: Int) {
    for (i <- 1 to 2 * n) int('x(i), 1, n)
    for (i <- 1 to n)
      add(Or(for (j <- 1 to 2 * n - i - 1) yield And(('x(j) === 'x(j + i + 1)), ('x(j) === i))))

    if (find) println(solution)
    reset
  }

  def LangfordPair_Model2(n: Int) {
    for (i <- 1 to 2 * n) int('x(i), 1, n)
    for (i <- 1 to n)
      add(Or(for (j <- 1 to 2 * n - i - 1) yield And(('x(j) === 'x(j + i + 1)), ('x(j) === i))))

    if (find) println(solution)
    reset
  }

  def GraphColoring {
    val nodes = Seq(1, 2, 3, 4, 5)
    val edges = Seq((1, 2), (1, 5), (2, 3), (2, 4), (3, 4), (4, 5))
    var maxColor = 4;

    int('color, 1, maxColor)
    for (i <- nodes) int('n(i), 1, maxColor)
    for (i <- nodes) add('n(i) <= 'color)
    for ((i, j) <- edges) add('n(i) !== 'n(j))

    while (find('color <= maxColor)) {
      println(solution)
      maxColor -= 1
    }
    reset
  }

  def MagicSquare {
    val xs = for (i <- 1 to 3; j <- 1 to 3) yield csp.int('x(i, j), 1, 9)
    add(alldiff(xs))

    for (i <- 1 to 3)
      add(Sum((1 to 3).map(j => 'x(i, j))) === 15)
    for (j <- 1 to 3)
      add(Sum((1 to 3).map(i => 'x(i, j))) === 15)
    add(Sum((1 to 3).map(i => 'x(i, i))) === 15)
    add(Sum((1 to 3).map(i => 'x(i, 4 - i))) === 15)

    if (find) println(solution)
    reset
  }

  def AlphameticProblem {
    val base = 10

    for (v <- Seq('s, 'i, 'f, 't)) yield int(v, 1, base - 1) // S, I, F and T are not zero
    for (v <- Seq('a, 'u, 'n, 'r, 'e)) yield int(v, 0, base - 1) // others can be zero
    for (v <- Seq('c1, 'c2, 'c3)) yield int(v, 0, 2) // carries

    add('t + 's + 'n === 'e + 'c1 * base)
    add('a + 'i + 'u + 'c1 === 'u + 'c2 * base)
    add('s + 'f + 'c2 === 'r + 'c3 * base)
    add('c3 === 't)

    val vars: Seq[Var] = Seq('s, 'i, 'f, 't, 'a, 'u, 'n, 'r, 'e)
    add(alldiff(vars))

    if (find) println(solution.intMap)
    reset
  }

  def OpenShopScheduling {
    use(new Sat4j("glucose"))

    val pt = Seq(
      Seq(661, 6, 333),
      Seq(168, 489, 343),
      Seq(171, 505, 324))

    val n = pt.size
    val lb = pt.map(_.sum).max
    var ub = (0 until n).map(k => (0 until n).map(i => pt(i)((i + k) % n)).max).sum

    int('makespan, lb, ub)

    for (i <- 0 until n; j <- 0 until n) {
      int('s(i, j), 0, ub)
      add('s(i, j) + pt(i)(j) <= 'makespan)
    }
    for (i <- 0 until n) {
      for (j <- 0 until n; l <- j + 1 until n)
        add('s(i, j) + pt(i)(j) <= 's(i, l) ||
          's(i, l) + pt(i)(l) <= 's(i, j))
    }
    for (j <- 0 until n) {
      for (i <- 0 until n; k <- i + 1 until n)
        add('s(i, j) + pt(i)(j) <= 's(k, j) ||
          's(k, j) + pt(k)(j) <= 's(i, j))
    }

    while (find('makespan <= ub)) {
      println(solution)
      val end = (for (i <- 0 until n; j <- 0 until n)
        yield solution.intMap('s(i, j)) + pt(i)(j)).max
      ub = end - 1
      println(ub)
    }
    reset
  }

  def Colored_N_Queen(n: Int) {
    val c = n

    use(new Sat4j("glucose"))

    for (i <- 1 to n; color <- 1 to c)
      int('q(i, color), 1, c)

    for (color <- 1 to c) {
      add(alldiff((1 to n).map(i => 'q(i, color))))
      add(alldiff((1 to n).map(i => 'q(i, color) + i)))
      add(alldiff((1 to n).map(i => 'q(i, color) - i)))
    }

    for (i <- 1 to n)
      add(alldiff((1 to c).map(color => 'q(i, color))))

    if (find) {
      for (color <- 1 to c) {
        for (row <- 1 to n) {
          var seq: Seq[Int] = Seq.empty
          for (column <- 1 to n)
            if (encoder.decode('q(row, color)) == column)
              seq = seq :+ color
            else
              seq = seq :+ 0
          println(seq.mkString(" "))
        }
        println("-----------------")
      }
    }
    reset
  }

  def main(args: Array[String]) {
    LatinSquare(5)
    SquarePacking(15, 36)
    LangfordPair_Model1(4)
    LangfordPair_Model2(4)
    GraphColoring
    MagicSquare
    AlphameticProblem
    OpenShopScheduling
    Colored_N_Queen(5)

  }

}