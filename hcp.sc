import jp.kobe_u.scarab._ , dsl._
import scala.io.Source

case class Graph(var nodes: Set[Int] = Set.empty, var edges: Set[(Int, Int)] = Set.empty) {

  def edge(n1: Int, n2: Int) = if (n1 < n2) (n1, n2) else (n2, n1)

  private var adjacentMap: Map[Int, Set[Int]] = Map.empty
  private def addAdjacent(n1: Int, n2: Int) =
    adjacentMap += n1 -> (adjacentMap.getOrElse(n1, Set.empty) + n2)

  def addNode(n1: Int) = nodes += n1
  def addEdge(n1: Int, n2: Int) =
    if (n1 != n2) {
      edges += edge(n1, n2)
      addAdjacent(n1, n2)
      addAdjacent(n2, n1)
    }
  def adjacent(n: Int) = adjacentMap(n)
  def adjacentEdge(n: Int) = adjacent(n).map(n2 => edge(n, n2))
}

object Graph {
  def parse(source: Source): Graph = {
    val graph = Graph()
    val re = """e\s+(\d+)\s+(\d+)""".r
    for (line <- source.getLines.map(_.trim)) {
      line match {
        case re(s1, s2) => {
          val n1 = s1.toInt; graph.addNode(n1)
          val n2 = s2.toInt; graph.addNode(n2)
          graph.addEdge(n1, n2)
        }
        case _ =>
      }
    }
    graph
  }
}

def getCycle(node: Int, initial: Int, cycle: List[Int]): List[Int] = {
  val node2: Int = graph.adjacent(node).find(node2 => solver.solution('arc(node, node2)) > 0).get
  if (node2 == initial) node2 :: cycle
  else getCycle(node2, initial, node2 :: cycle)
}

def getCycles: Set[List[Int]] = {
  var cycles: Set[List[Int]] = Set.empty
  var nodes = graph.nodes
  while (!nodes.isEmpty) {
    val node = nodes.head
    val cycle = getCycle(node, node, List(node))
    cycles += cycle
    nodes --= cycle
  }
  cycles
}

def define = {
  for ((n1, n2) <- graph.edges) {
    int('arc(n1, n2), 0, 1)
    int('arc(n2, n1), 0, 1)
    add('arc(n1, n2) + 'arc(n2, n1) <= 1)
  }
  for (n1 <- graph.nodes) {
    val nodes = graph.adjacent(n1).toSeq
    add(Sum(nodes.map(i => 'arc(i, n1))) === 1)
    add(Sum(nodes.map(i => 'arc(n1, i))) === 1)
  }
}

def addBlockingClauses(cycle: List[Int]) {
  val ceArcs = for (edge <- cycle.sliding(2).toList) 
             yield (edge(0), edge(1))
  add(Or(ceArcs.map(i => 'arc(i._1, i._2) <= 0)))
  add(Or(ceArcs.map(i => 'arc(i._2, i._1) <= 0)))
}

val graph = Graph.parse(Source.fromFile(args(0)))

use(new Sat4j("Glucose21"))
use(new NativePBEncoder(csp, satSolver))

define

while (solver.find) {
  val cycles: Set[List[Int]] = getCycles
  if (cycles.size == 1) {
    println("A Hamiltonian Cycle is Found")
    println(Some(cycles.head).get.mkString(" "))
    System.exit(0)
  } else
    for (cycle <- cycles)
      addBlockingClauses(cycle)
}
println("This Graph has no Hamiltonian Cycle")
