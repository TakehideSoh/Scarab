package jp.kobe_u.scarab

import com.sun.jna.{Library, Native, Pointer}

/**
 * JNA interface to CaDiCaL SAT solver C API.
 *
 * This interface maps to the C API functions provided by libcadical.
 * The CaDiCaL library must be installed and accessible via the library path.
 *
 * @see [[https://github.com/arminbiere/cadical CaDiCaL repository]]
 */
trait CaDiCaLNative extends Library {
  /**
   * Create and initialize a new CaDiCaL solver instance.
   * @return pointer to the solver instance
   */
  def ccadical_init(): Pointer

  /**
   * Release and deallocate a solver instance.
   * @param solver pointer to the solver instance
   */
  def ccadical_release(solver: Pointer): Unit

  /**
   * Add a literal to the current clause.
   * Call with literal 0 to terminate the clause.
   * @param solver pointer to the solver instance
   * @param lit literal to add (positive or negative integer, 0 to terminate clause)
   */
  def ccadical_add(solver: Pointer, lit: Int): Unit

  /**
   * Add an assumption for incremental solving.
   * @param solver pointer to the solver instance
   * @param lit assumption literal
   */
  def ccadical_assume(solver: Pointer, lit: Int): Unit

  /**
   * Solve the formula under current assumptions.
   * @param solver pointer to the solver instance
   * @return 10 for SAT, 20 for UNSAT, 0 for unknown
   */
  def ccadical_solve(solver: Pointer): Int

  /**
   * Get the truth value of a variable in the model.
   * @param solver pointer to the solver instance
   * @param lit variable number (positive integer)
   * @return value of the variable: positive if true, negative if false, 0 if don't care
   */
  def ccadical_val(solver: Pointer, lit: Int): Int

  /**
   * Get whether a literal is a failed assumption.
   * @param solver pointer to the solver instance
   * @param lit literal to check
   * @return non-zero if the literal is a failed assumption
   */
  def ccadical_failed(solver: Pointer, lit: Int): Int

  /**
   * Get the number of active variables.
   * @param solver pointer to the solver instance
   * @return number of active variables
   */
  def ccadical_vars(solver: Pointer): Int

  /**
   * Set a resource limit (time or conflicts).
   * @param solver pointer to the solver instance
   * @param name name of the limit ("conflicts" or "decisions")
   * @param limit limit value
   */
  def ccadical_limit(solver: Pointer, name: String, limit: Int): Unit

  /**
   * Freeze a variable (prevent it from being eliminated).
   * @param solver pointer to the solver instance
   * @param lit variable to freeze
   */
  def ccadical_freeze(solver: Pointer, lit: Int): Unit

  /**
   * Melt a previously frozen variable.
   * @param solver pointer to the solver instance
   * @param lit variable to melt
   */
  def ccadical_melt(solver: Pointer, lit: Int): Unit

  /**
   * Print statistics to stdout.
   * @param solver pointer to the solver instance
   */
  def ccadical_print_statistics(solver: Pointer): Unit
}

/**
 * Companion object providing access to the native library.
 */
object CaDiCaLNative {
  // Automatically add common installation paths to JNA library path
  private def setupLibraryPath(): Unit = {
    val currentPath = System.getProperty("jna.library.path", "")
    val homeDir = System.getProperty("user.home")
    val commonPaths = Seq(
      s"$homeDir/.local/lib",
      "/usr/local/lib",
      "/usr/lib",
      "/usr/lib64"
    )

    val pathsToAdd = commonPaths.filterNot(currentPath.contains)
    if (pathsToAdd.nonEmpty) {
      val newPath = if (currentPath.isEmpty) {
        pathsToAdd.mkString(java.io.File.pathSeparator)
      } else {
        currentPath + java.io.File.pathSeparator + pathsToAdd.mkString(java.io.File.pathSeparator)
      }
      System.setProperty("jna.library.path", newPath)
    }
  }

  lazy val INSTANCE: CaDiCaLNative = {
    setupLibraryPath()
    try {
      Native.load("cadical", classOf[CaDiCaLNative]).asInstanceOf[CaDiCaLNative]
    } catch {
      case e: UnsatisfiedLinkError =>
        throw new RuntimeException(
          "Failed to load CaDiCaL native library. " +
          "Please ensure libcadical.so (Linux), libcadical.dylib (macOS), " +
          "or cadical.dll (Windows) is in your library path.\n" +
          "Searched in: " + System.getProperty("jna.library.path", "(none)") + "\n" +
          "You can install CaDiCaL to ~/.local/lib or set LD_LIBRARY_PATH.", e)
    }
  }
}
