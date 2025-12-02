# Scarab JAR Usage Guide

## Fat JAR Created Successfully! ✓

A standalone fat JAR has been created at:
```
scarab.jar (7.7 MB)
```

This JAR includes:
- Scarab library (all classes)
- SAT4j solver (embedded)
- JNA library (for CaDiCaL integration)
- All dependencies

## Quick Start

### View Usage Instructions

Simply run the JAR to see complete usage instructions:
```bash
java -jar scarab.jar
```

### Run CaDiCaL Tests

To test the CaDiCaL integration:
```bash
java -jar scarab.jar --test
```

## Using Scarab as a Library

### Method 1: Add to Classpath

Create your Scala program:
```scala
// MyProgram.scala
import jp.kobe_u.scarab.dsl._

object MyProgram extends App {
  int('x, 1, 10)
  int('y, 1, 10)
  add('x + 'y === 15)
  add('x > 'y)

  if (find) {
    println(s"x = ${solution.intMap('x)}")
    println(s"y = ${solution.intMap('y)}")
  }
}
```

Run it:
```bash
scala -cp scarab.jar MyProgram.scala
```

Or compile and run:
```bash
scalac -cp scarab.jar MyProgram.scala
scala -cp .:scarab.jar MyProgram
```

### Method 2: SBT Unmanaged Dependency

Copy `scarab.jar` to your project's `lib/` directory:
```bash
mkdir -p myproject/lib
cp scarab.jar myproject/lib/
```

SBT will automatically add it to your classpath.

### Method 3: As a Java Library

You can also use Scarab from Java:
```java
// Example.java
import jp.kobe_u.scarab.*;

public class Example {
    public static void main(String[] args) {
        Scarab scarab = new Scarab();
        // ... use Scarab API
    }
}
```

Compile and run:
```bash
javac -cp scarab.jar Example.java
java -cp .:scarab.jar Example
```

## Using CaDiCaL Solver

The JAR includes JNA bindings for CaDiCaL. To use it:

1. **Install CaDiCaL library** (already done on this system):
   ```bash
   # Library is at ~/.local/lib/libcadical.so
   ```

2. **Use in your code**:
   ```scala
   import jp.kobe_u.scarab.dsl._

   use(new CaDiCaLSolver())  // Instead of default Sat4j

   // Define your problem as usual
   int('x, 1, 10)
   add('x > 5)
   if (find) println(solution)
   ```

3. **Run with library path**:
   ```bash
   # Library is auto-detected in ~/.local/lib
   scala -cp scarab.jar YourProgram.scala
   ```

## Example Programs

### N-Queens Problem

```scala
// NQueens.scala
import jp.kobe_u.scarab.dsl._

object NQueens extends App {
  val n = 8

  // Create variables for each queen's position
  for (i <- 1 to n) int(Symbol(s"q$i"), 1, n)

  // All queens in different columns
  add(alldiff((1 to n).map(i => Symbol(s"q$i"))))

  // All queens in different diagonals
  add(alldiff((1 to n).map(i => Symbol(s"q$i") + i)))
  add(alldiff((1 to n).map(i => Symbol(s"q$i") - i)))

  if (find) {
    println("Solution found:")
    for (i <- 1 to n) {
      val col = solution.intMap(Symbol(s"q$i"))
      println(("." * (col - 1)) + "Q" + ("." * (n - col)))
    }
  }
}
```

Run:
```bash
scala -cp scarab.jar NQueens.scala
```

### Graph Coloring

```scala
// GraphColoring.scala
import jp.kobe_u.scarab.dsl._

object GraphColoring extends App {
  val nodes = 1 to 5
  val edges = Seq((1,2), (1,5), (2,3), (2,4), (3,4), (4,5))
  val colors = 3

  // Each node has a color
  for (n <- nodes) int(Symbol(s"color$n"), 1, colors)

  // Adjacent nodes have different colors
  for ((i, j) <- edges) {
    add(Symbol(s"color$i") !== Symbol(s"color$j"))
  }

  if (find) {
    println("Coloring found:")
    for (n <- nodes) {
      println(s"Node $n: Color ${solution.intMap(Symbol(s"color$n"))}")
    }
  }
}
```

## JAR Contents

The fat JAR includes:
- **Scarab classes**: All constraint programming functionality
- **SAT4j**: Embedded SAT solver (no external dependencies)
- **JNA**: For native library access (CaDiCaL)
- **Scala library**: Not included (use `-cp` with scala command)

Total: 3,274 classes/resources

## Performance Tips

1. **Use CaDiCaL for large instances**:
   ```scala
   use(new CaDiCaLSolver())  // Often faster than Sat4j
   ```

2. **Choose the right Sat4j variant**:
   ```scala
   use(new Sat4j("glucose"))  // Good for industrial instances
   ```

3. **Set timeouts for large problems**:
   ```scala
   timeLimit(60)  // 60 seconds
   ```

## Troubleshooting

### "libcadical.so not found"

If you get an error about CaDiCaL library:
```bash
# Set library path
export LD_LIBRARY_PATH=$HOME/.local/lib:$LD_LIBRARY_PATH
scala -cp scarab.jar YourProgram.scala
```

Or just use Sat4j (works without any external dependencies):
```scala
use(new Sat4j())  // Default, always works
```

### "NoClassDefFoundError: scala..."

Make sure to use `scala` command, not just `java`:
```bash
scala -cp scarab.jar YourProgram.scala  # Correct
java -cp scarab.jar YourProgram         # Won't work - no Scala runtime
```

### Classpath Issues

If classes are not found:
```bash
# Linux/Mac
scala -cp .:scarab.jar MyProgram

# Windows
scala -cp .;scarab.jar MyProgram
```

## Distribution

You can distribute `scarab.jar` as a single file. Users need:
- Java 8 or later
- Scala runtime (for Scala programs)
- Optional: CaDiCaL library for native solver support

## Building from Source

To rebuild the fat JAR:
```bash
cd /home/soh/02_prog/Scarab
sbt clean assembly
```

The JAR will be created at: `target/scala-2.12/scarab.jar`

## Additional Resources

- **README.md**: General Scarab documentation
- **CADICAL_README.md**: CaDiCaL integration guide
- **CADICAL_INSTALLATION.md**: Installation verification
- **Project website**: http://tsoh.org/scarab/
- **GitHub**: https://github.com/TakehideSoh/Scarab

## License

Scarab: BSD New License
CaDiCaL: MIT License
SAT4j: EPL 1.0

---

**Ready to use!** The fat JAR contains everything you need to start constraint programming with Scarab. 🚀
