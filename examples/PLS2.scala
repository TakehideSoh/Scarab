import jp.kobe_u.scarab.csp._
import jp.kobe_u.scarab.solver._

/*
 * PLS without DSL
 */
object PLS2 {

  def main(args: Array[String]) {

    val csp = CSP()
    val solver = Solver(csp)
    /**
     * for defining solver, we can specify details as follows instead of using pre-configured solver
     *   val satSolver = new Sat4j
     *   val encoder = new OrderEncoder(csp, satSolver) // OrderEncoder, LogEncoder, NativePBEncoder 
     *   val solver = new Solver(csp, satSolver, encoder)
     */

    var n: Int = 5
    for (i <- 1 to n; j <- 1 to n)  csp.int('x(i,j),1,n) 
    for (i <- 1 to n) {
      csp.add(CSP.alldiff((1 to n).map(j => 'x(i,j)),csp))
      csp.add(CSP.alldiff((1 to n).map(j => 'x(j,i)),csp))
      csp.add(CSP.alldiff((1 to n).map(j => 'x(j,(i+j-1)%n+1)),csp))
      csp.add(CSP.alldiff((1 to n).map(j => 'x(j,(i+(j-1)*(n-1))%n+1)),csp))}

    if (solver.find) println(solver.solution.intMap)
  }

}
