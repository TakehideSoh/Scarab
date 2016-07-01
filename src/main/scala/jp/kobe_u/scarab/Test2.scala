package jp.kobe_u.scarab

object Test2 {

  def main(args: Array[String]) {
    val csp = CSP()
    val satSolver = new Sat4j
    val encoder = new OrderEncoder(csp,satSolver)
    val solver = new Solver(csp,satSolver,encoder)
    
    csp.int('x,0,3)
    csp.int('y,0,3)
    
    csp.add('x + 'y === 2)
    
    solver.find
 
    
    
    
    
  }
}