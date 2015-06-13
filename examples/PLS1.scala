import jp.kobe_u.scarab.csp._
import jp.kobe_u.scarab.solver._
import jp.kobe_u.scarab.sapp._

/*
 * PLS with DSL
 */
object PLS1 {

  def main(args: Array[String]) {
    var n: Int = 5
    for (i <- 1 to n; j <- 1 to n)  int('x(i,j),1,n) 
    for (i <- 1 to n) {
      add(CSP.alldiff((1 to n).map(j => 'x(i,j)),csp))
      add(CSP.alldiff((1 to n).map(j => 'x(j,i)),csp))
      add(CSP.alldiff((1 to n).map(j => 'x(j,(i+j-1)%n+1)),csp))
      add(CSP.alldiff((1 to n).map(j => 'x(j,(i+(j-1)*(n-1))%n+1)),csp))}

    if (find) println(solution.intMap)
  }

}
