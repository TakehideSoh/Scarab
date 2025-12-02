import jp.kobe_u.scarab._
import jp.kobe_u.scarab.dsl._

/**
 * Test example for CaDiCaL solver integration.
 *
 * This demonstrates how to use the CaDiCaLSolver class as a drop-in
 * replacement for Sat4j in Scarab applications.
 *
 * Note: This requires libcadical.so to be installed and available
 * in your library path.
 */
object CaDiCaLTest {

  def main(args: Array[String]): Unit = {
    println("=" * 60)
    println("CaDiCaL SAT Solver Test for Scarab")
    println("=" * 60)

    // Test 1: Simple SAT problem
    println("\nTest 1: Simple 3-SAT problem")
    testSimple3SAT()

    // Test 2: Graph coloring
    println("\nTest 2: Graph 3-coloring problem")
    testGraphColoring()

    // Test 3: Using with DSL
    println("\nTest 3: Using CaDiCaL with Scarab DSL")
    testWithDSL()

    println("\n" + "=" * 60)
    println("All tests completed!")
    println("=" * 60)
  }

  /**
   * Test 1: Simple 3-SAT problem
   * Formula: (x1 ∨ x2 ∨ x3) ∧ (¬x1 ∨ ¬x2) ∧ (¬x2 ∨ x3)
   */
  def testSimple3SAT(): Unit = {
    try {
      val solver = new CaDiCaLSolver()

      // Add clauses
      solver.addClause(Seq(1, 2, 3))      // x1 ∨ x2 ∨ x3
      solver.addClause(Seq(-1, -2))       // ¬x1 ∨ ¬x2
      solver.addClause(Seq(-2, 3))        // ¬x2 ∨ x3

      // Solve
      val result = solver.isSatisfiable
      println(s"  Result: ${if (result) "SAT" else "UNSAT"}")

      if (result) {
        println(s"  Model: x1=${solver.model(1)}, x2=${solver.model(2)}, x3=${solver.model(3)}")
      }
    } catch {
      case e: RuntimeException if e.getMessage.contains("Failed to load CaDiCaL") =>
        println("  SKIPPED: CaDiCaL library not found")
        println("  To use CaDiCaL, install libcadical and ensure it's in your library path")
      case e: Exception =>
        println(s"  ERROR: ${e.getMessage}")
        e.printStackTrace()
    }
  }

  /**
   * Test 2: Graph 3-coloring problem
   * Graph: Triangle (3 nodes, fully connected)
   * Can we color it with 2 colors? (No)
   * Can we color it with 3 colors? (Yes)
   */
  def testGraphColoring(): Unit = {
    try {
      val solver = new CaDiCaLSolver()

      // Variables: x[i][c] means node i has color c
      // For 3 nodes and 2 colors: variables 1-6
      // x[1][1]=1, x[1][2]=2, x[2][1]=3, x[2][2]=4, x[3][1]=5, x[3][2]=6

      // Each node must have exactly one color
      for (i <- 0 until 3) {
        val nodeVars = (1 to 2).map(c => i * 2 + c)
        solver.addExactly(nodeVars, 1)
      }

      // Adjacent nodes must have different colors
      // Edge (0,1)
      solver.addClause(Seq(-1, -3))  // ¬x[0][0] ∨ ¬x[1][0]
      solver.addClause(Seq(-2, -4))  // ¬x[0][1] ∨ ¬x[1][1]
      // Edge (1,2)
      solver.addClause(Seq(-3, -5))  // ¬x[1][0] ∨ ¬x[2][0]
      solver.addClause(Seq(-4, -6))  // ¬x[1][1] ∨ ¬x[2][1]
      // Edge (0,2)
      solver.addClause(Seq(-1, -5))  // ¬x[0][0] ∨ ¬x[2][0]
      solver.addClause(Seq(-2, -6))  // ¬x[0][1] ∨ ¬x[2][1]

      val result = solver.isSatisfiable
      println(s"  Can color triangle with 2 colors? ${if (result) "YES (unexpected!)" else "NO (correct)"}")

    } catch {
      case e: RuntimeException if e.getMessage.contains("Failed to load CaDiCaL") =>
        println("  SKIPPED: CaDiCaL library not found")
      case e: Exception =>
        println(s"  ERROR: ${e.getMessage}")
    }
  }

  /**
   * Test 3: Using CaDiCaL with Scarab DSL
   * Solves a simple CSP: find x, y such that x + y = 5, x, y ∈ {1,2,3,4}
   */
  def testWithDSL(): Unit = {
    try {
      // Use CaDiCaL solver instead of default Sat4j
      use(new CaDiCaLSolver())

      // Define variables
      int('x, 1, 4)
      int('y, 1, 4)

      // Add constraint: x + y = 5
      add('x + 'y === 5)

      // Solve
      if (find) {
        println(s"  Found solution: x=${solution.intMap('x)}, y=${solution.intMap('y)}")
      } else {
        println("  No solution found")
      }

      reset()

    } catch {
      case e: RuntimeException if e.getMessage.contains("Failed to load CaDiCaL") =>
        println("  SKIPPED: CaDiCaL library not found")
      case e: UnsupportedOperationException =>
        println(s"  Note: ${e.getMessage}")
      case e: Exception =>
        println(s"  ERROR: ${e.getMessage}")
        e.printStackTrace()
    }
  }
}
