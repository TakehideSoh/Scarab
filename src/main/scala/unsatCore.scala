import org.sat4j.specs.ISolver
import org.sat4j.tools.xplain.HighLevelXplain
import org.sat4j.minisat.SolverFactory
import org.sat4j.core.VecInt

object unsatCore {

  def main(args: Array[String]) {

    test_newVar_normal

    println("=========")

    test_newVar_addition

    println("=========")

    test_newVar_redundant

  }

  def test_newVar_normal {
    val xp = new HighLevelXplain[ISolver](SolverFactory.newDefault)

    println(" " + xp.nextFreeVarId(false))    
    xp.newVar(2)

    println(" " + xp.nextFreeVarId(true))
    xp.addClause(new VecInt(Array(1, 2)), 3)
    println(" " + xp.nextFreeVarId(true))    
    xp.addClause(new VecInt(Array(-1)), 4)
    println(" " + xp.nextFreeVarId(true))    
    xp.addClause(new VecInt(Array(-2)), 5)

    xp.isSatisfiable

    for (i <- xp.minimalExplanation)
      println(i)
  }

  def test_newVar_addition {
    val xp = new HighLevelXplain[ISolver](SolverFactory.newDefault)

    xp.newVar(2)

    xp.addClause(new VecInt(Array(1, 2)), 3)

    xp.newVar(3)

    xp.addClause(new VecInt(Array(-1)), 4)
    xp.addClause(new VecInt(Array(-2)), 5)
    xp.addClause(new VecInt(Array(3, 2)), 6)

    xp.isSatisfiable

    for (i <- xp.minimalExplanation)
      println(i)

  }

  def test_newVar_redundant {
    val xp = new HighLevelXplain[ISolver](SolverFactory.newDefault)

    xp.newVar(2)

    println(" " + xp.nextFreeVarId(true))        

    xp.addClause(new VecInt(Array(1, 2)), 3)

    println(" " + xp.nextFreeVarId(true))        
    xp.newVar(2)

    xp.addClause(new VecInt(Array(-1)), 4)

    println(" " + xp.nextFreeVarId(true))        
    xp.addClause(new VecInt(Array(-2)), 5)

    println(" " + xp.nextFreeVarId(true))        

    xp.isSatisfiable

    for (i <- xp.minimalExplanation)
      println(i)

  }

}
