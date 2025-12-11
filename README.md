# Scarab

This page details **Scarab**, a prototyping tool for developing SAT-based CP systems. 
Features of Scarab are follows:

-   **Expressiveness** Rich constraint modeling language.
-   **Efficiency** Optimized order encoding and native handling of BC/PB on Sat4j.
-   **Multiple SAT solvers** Supports Sat4j (default) and CaDiCaL via JNA.
-   **Customizability** Its core part is written in around 1000 lines of Scala.
-   **Portability** Run on JVM.

## How to use it

### Build from source

```bash
sbt assembly                   # Build for default Scala version (2.13)
sbt +assembly                  # Build for all Scala versions (2.12, 2.13)
```

The jar file will be generated at `target/scala-2.1x/scarab.jar`.

### Example

Write your own Scarab program. For instance, let's write a program solving Pandiagonal Latin Square **PLS(n)**:
- a problem of placing different n numbers into n × n matrix
- such that each number is occurring exactly once
- for each row, column, diagonally down right, and diagonally up right.

```scala
import jp.kobe_u.scarab._
import dsl._

var n: Int = 5
for (i <- 1 to n; j <- 1 to n)  int('x(i,j),1,n)
for (i <- 1 to n) {
  add(alldiff((1 to n).map(j => 'x(i,j))))
  add(alldiff((1 to n).map(j => 'x(j,i))))
  add(alldiff((1 to n).map(j => 'x(j,(i+j-1)%n+1))))
  add(alldiff((1 to n).map(j => 'x(j,(i+(j-1)*(n-1))%n+1))))}

if (find)  println(solution.intMap)
```

Save this program as **pls.sc** and run it:

```bash
scala -cp scarab.jar pls.sc
```

## Documentation

- [CaDiCaL Integration](./docs/CADICAL_README.md) - Using CaDiCaL SAT solver
- [JAR Usage Guide](./docs/JAR_USAGE.md) - How to use the fat JAR
- [Docker Usage](./docs/DOCKER_README.md) - Running Scarab in Docker

## Note

- This software is distributed under the [BSD License](http://opensource.org/licenses/bsd-license.php). See [LICENSE](./LICENSE) file.
- scarab.jar includes Sat4j package and Sugar for the ease of use.
  - We really appreciate the developers of Sat4j and Sugar!
  - Sat4j used for inference engine.
  - Sugar used for preprocessor (from 1.5.4).

# Release Note

- [2025.12.11] Version 1.9.7-SNAPSHOT
  - Use sat4j from Maven Central instead of local sources
  - Update to Scala 2.12/2.13, sbt 1.10.7, sbt-assembly 2.3.1
  - Add unit tests for CaDiCaLSolver, Sat4j, Sat4jPB

- [2025.12.02] Version 1.9.7-SNAPSHOT
  - Added CaDiCaL SAT solver support via JNA (alternative to Sat4j)
  - Code cleanup and refactoring

- [2019.02.11] Version 1.9.x
  - Support cross Scala versions: 2.10, 2.11, 2.12
  - Assembled jar no longer includes Scala library

- [2015.11.01] Sat4j and Sat4j-PB (Rev2428) are included.

- [2015.06.14] Version 1.6.9 is released.

- [2015.05.25] Version 1.6.8 is released.

- [2015.02.08] Version 1.5.7 is released.
  - To run this version, Scala 2.11.* or higher is required.
  - Addition of new functions.
    - UNSAT Core detection in CSP level.
    - Nested commit.
    - Built-in optimization function.
  - Refactoring for some parts.

- [2015.01.09] Version 1.5.6 is released.
  - To run this version, Scala 2.11.* or higher is required.
  - Support non-contiguous domain.
  - Performance improvement.
    - Order Encoding Module is tuned.
    - Native PB Constraint is tuned.

# Publications

-   Scarab: A Rapid Prototyping Tool for SAT-based Constraint Programming Systems (Tool Paper)
    -   Takehide Soh, Naoyuki Tamura, and Mutsunori Banbara
    -   In the Proceedings of the 16th International Conference on Theory and Applications of Satisfiability Testing (SAT 2013), LNCS 7962, pp. 429-436, 2013.
-   System Architecture and Implementation of a Prototyping Tool for SAT-based Constraint Programming Systems
    -   Takehide Soh, Naoyuki Tamura, Mutsunori Banbara, Daniel Le Berre, and Stéphanie Roussel
    -   In the Proceedings of Pragmatics of SAT 2013 (PoS-13), 14 pages, July 2013.

# Related Tools

- [CaDiCaL](https://github.com/arminbiere/cadical) - SAT solver by Armin Biere
- [JSR 331](http://jcp.org/en/jsr/detail?id=331) - Java Specification Requests: Constraint Programming API