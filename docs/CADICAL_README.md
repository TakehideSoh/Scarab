# CaDiCaL SAT Solver Integration for Scarab

This document describes how to use CaDiCaL SAT solver with Scarab via JNA (Java Native Access).

## Overview

CaDiCaL is a simplified reimplementation of the Lingeling SAT solver by Armin Biere. This integration allows you to use CaDiCaL as a backend SAT solver in Scarab applications as an alternative to Sat4j.

## Installation

### 1. Install CaDiCaL Library

You need to install the CaDiCaL shared library on your system.

#### On Linux/macOS:

```bash
# Clone the repository
git clone https://github.com/arminbiere/cadical.git
cd cadical

# Build and install
./configure
make
sudo make install

# Or install to a custom location
./configure --prefix=$HOME/.local
make
make install
```

#### On Ubuntu/Debian:

```bash
# You may also try installing via package manager if available
sudo apt-get install libcadical-dev
```

### 2. Set Library Path

Ensure the library is in your system's library path:

#### Linux:
```bash
export LD_LIBRARY_PATH=/usr/local/lib:$LD_LIBRARY_PATH
# Or if installed to custom location:
export LD_LIBRARY_PATH=$HOME/.local/lib:$LD_LIBRARY_PATH
```

#### macOS:
```bash
export DYLD_LIBRARY_PATH=/usr/local/lib:$DYLD_LIBRARY_PATH
```

#### Windows:
Add the directory containing `cadical.dll` to your PATH environment variable.

## Usage

### Basic Usage

```scala
import jp.kobe_u.scarab._

// Create a CaDiCaL solver instance
val solver = new CaDiCaLSolver()

// Add clauses (DIMACS format)
solver.addClause(Seq(1, 2, 3))      // x1 ∨ x2 ∨ x3
solver.addClause(Seq(-1, -2))       // ¬x1 ∨ ¬x2

// Solve
if (solver.isSatisfiable) {
  println("SAT")
  println(s"x1 = ${solver.model(1)}")
  println(s"x2 = ${solver.model(2)}")
  println(s"x3 = ${solver.model(3)}")
} else {
  println("UNSAT")
}
```

### Using with Scarab DSL

```scala
import jp.kobe_u.scarab._
import jp.kobe_u.scarab.dsl._

// Use CaDiCaL as the backend solver
use(new CaDiCaLSolver())

// Define your CSP as usual
int('x, 1, 10)
int('y, 1, 10)
add('x + 'y === 15)

if (find) {
  println(s"x = ${solution.intMap('x)}")
  println(s"y = ${solution.intMap('y)}")
}
```

### Incremental Solving with Assumptions

```scala
val solver = new CaDiCaLSolver()

// Add base clauses
solver.addClause(Seq(1, 2))
solver.addClause(Seq(-2, 3))

// Solve with assumptions
if (solver.isSatisfiable(Seq(1))) {
  println("SAT with x1=true")
}

if (solver.isSatisfiable(Seq(-1, -3))) {
  println("SAT with x1=false and x3=false")
}
```

## Features

### Supported Operations

- ✅ Basic SAT solving (clauses)
- ✅ Incremental solving with assumptions
- ✅ Model extraction
- ✅ Cardinality constraints (via encoding)
- ✅ Pseudo-boolean constraints (via encoding)
- ✅ Timeout/resource limits
- ✅ Variable freezing/melting
- ✅ Statistics

### Unsupported Operations

The following features are not supported by CaDiCaL's C API:

- ❌ UNSAT core extraction (`minExplain`)
- ❌ MUS enumeration (`minAllExplain`)
- ❌ CNF dumping to file (`dumpCnf`)

For these features, use Sat4j instead:
```scala
use(new Sat4j("xplain"))  // For UNSAT cores
```

## Performance Notes

- CaDiCaL is generally faster than Sat4j on industrial SAT instances
- For CSP problems, performance depends on the encoding
- Cardinality and PB constraints are encoded to clauses, which may be less efficient than Sat4j's native support
- For problems requiring frequent UNSAT core extraction, Sat4j is recommended

## Comparison: CaDiCaL vs Sat4j

| Feature | CaDiCaL | Sat4j |
|---------|---------|-------|
| Speed on industrial instances | ⚡⚡⚡ Faster | ⚡⚡ Fast |
| Native PB constraints | ❌ No | ✅ Yes |
| UNSAT cores | ❌ No | ✅ Yes |
| CNF dumping | ❌ No | ✅ Yes |
| Memory usage | Lower | Higher |
| Installation | Requires native lib | Pure Java |

## Troubleshooting

### Error: Failed to load CaDiCaL native library

**Problem:** JNA cannot find `libcadical.so`/`libcadical.dylib`/`cadical.dll`

**Solutions:**
1. Verify the library is installed: `ldconfig -p | grep cadical` (Linux)
2. Check your library path is set correctly
3. Try specifying the path explicitly:
   ```bash
   java -Djna.library.path=/path/to/cadical/lib -jar yourapp.jar
   ```

### Error: UnsatisfiedLinkError

**Problem:** The library is found but cannot be loaded

**Solutions:**
1. Verify the library matches your system architecture (32-bit vs 64-bit)
2. Check library dependencies: `ldd /path/to/libcadical.so` (Linux)
3. Ensure you have the C++ standard library installed

### Warning: clearLearntClauses not supported

**Explanation:** CaDiCaL's C API doesn't expose learned clause management. This warning is informational and can be ignored.

## Testing

Run the test suite:

```bash
cd /home/soh/02_prog/Scarab
sbt "runMain CaDiCaLTest"
```

This will run three tests:
1. Simple 3-SAT problem
2. Graph coloring problem
3. CSP solving with DSL

## Examples

See `CaDiCaLTest.scala` for complete examples including:
- Basic SAT solving
- Graph coloring
- Integration with Scarab DSL

## References

- CaDiCaL repository: https://github.com/arminbiere/cadical
- JNA documentation: https://github.com/java-native-access/jna
- Scarab documentation: http://tsoh.org/scarab/

## License

The CaDiCaL integration code follows the same license as Scarab (BSD New).
CaDiCaL itself is licensed under the MIT license.
