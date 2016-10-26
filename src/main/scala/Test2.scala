import jp.kobe_u.scarab._

object Test2 {

  def main(args: Array[String]) {
    val csp = CSP()
    val satSolver = new Sat4j("xplain")
    val encoder = new OrderEncoder(csp,satSolver)
    val solver = new Solver(csp,satSolver,encoder)
    
    val b1 = Bool("b1")
    val b2 = Bool("b2")    
    val b3 = Bool("b3")        
    val b4 = Bool("b4")
    
    csp.bool(b1) ; csp.bool(b2) ; csp.bool(b3) ; csp.bool(b4) ; 
    csp.add(b1 || b2)
    csp.add(Not(b1))    
    csp.add(Not(b2))        
    csp.add(Not(b1) || Not(b3))            
    
    solver.find
 
    for (i <- solver.minExplain)
      println(i)
    
    
    
    
  }
}