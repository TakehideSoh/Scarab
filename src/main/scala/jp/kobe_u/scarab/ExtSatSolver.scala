package jp.kobe_u.scarab

import java.io.IOException
import java.lang.management.ManagementFactory
import scala.io.Source
import scala.sys.process.Process
import scala.concurrent._
import scala.concurrent.duration._
import ExecutionContext.Implicits.global

class ExtSatSolver(cmd: String, var fileName: String = "", keep: Boolean = false) extends SatSolver {
  def this(cmd: String) = this(cmd, "", false)
  def this(cmd: String, keep: Boolean) = this(cmd, "", keep)
  def this(cmd: String, fileName: String) = this(cmd, fileName, false)

  if (fileName == "") {
    val pid = ManagementFactory.getRuntimeMXBean.getName.split("@").head
    fileName = s"/tmp/scarab${pid}"
  }

  val satFileProblem = new FileProblem(new java.io.File(s"${fileName}.cnf"))
  val outFile = new java.io.File(s"${fileName}.out")
  val logFile = new java.io.File(s"${fileName}.log")

  if (!keep) {
    satFileProblem.getCnfFile.deleteOnExit
    outFile.deleteOnExit
    logFile.deleteOnExit
  }

  var nofVariables = 0
  var nofClauses = 0
  var stat = Map[String, Number]()
  var modelArray: Array[Int] = Array()
  var tout: Int = -1

  protected def getModelFromOutFile: Boolean = {
    val source = Source.fromFile(outFile.getAbsolutePath())
    source.getLines.map(_.trim).next match {
      case "SAT" => {
        modelArray = source.getLines.map(_.trim).next.split(" ").dropRight(1).map(_.toInt)
        true
      }
      case "UNSAT" => false
      case _       => throw new java.lang.Exception("Illegal output file: 1st line should be SAT or UNSAT")
    }
  }

  protected def run(execCMD: String) {
    var exitValue = 1
    println(s"$execCMD")
    //    val logger = new scala.sys.process.FileProcessLogger(logFile)
    val logger = scala.sys.process.ProcessLogger(line => println(line), line => println(line))
    val process = Process(execCMD).run(logger)
    val SATsolving = Future { exitValue = process.exitValue }

    try {
      Await.result(SATsolving, if (tout < 0) Duration.Inf else Duration(tout, SECONDS))
      //      logger.flush	
      //      logger.close
      //      Process(s"cat ${logFile}").run
      // check whether SAT solving process is successfully done or not.
      if (exitValue == 1)
        throw new java.lang.InternalError(s"Unexpected Error is happen while executing the command: ${execCMD}")
    } catch {
      case e: java.util.concurrent.TimeoutException => {
        //        logger.flush
        //        logger.close
        Process(s"cat ${logFile}").run
        println(s"TIMEOUT. Scarab is killing process of: ${cmd}")
        process.destroy
        throw new org.sat4j.specs.TimeoutException
      }
    }
  }

  protected def runExtSolver = {
    val execCMD = s"${cmd} ${satFileProblem.getAbsolutePath} ${outFile.getAbsolutePath}"

    run(execCMD)
    getModelFromOutFile
  }

  def reset: Unit = {
    nofVariables = 0
    nofClauses = 0
  }
  def newVar(n: Int) =
    nofVariables += 1
  def setNumberOfVariables(n: Int) =
    nofVariables = n
  def addAllClauses(clauses: Seq[Seq[Int]]): Unit =
    for (clause <- clauses) addClause(clause, 0)
  def addClause(lits: Seq[Int]) = {
    nofClauses += 1
    satFileProblem.addClause2CnfFile(lits)
  }
  def addClause(lits: Seq[Int], cIndex: Int): Int = {
    nofClauses += 1
    satFileProblem.addClause2CnfFile(lits)
    nofClauses
  }
  def isSatisfiable: Boolean = {
    satFileProblem.nofVariables = nofVariables
    satFileProblem.nofClauses = nofClauses
    satFileProblem.done
    runExtSolver
  }

  def getModelArray: Array[Int] = modelArray
    
  def model(v: Int): Boolean = modelArray(v - 1) > 0
    
  def findModel: Option[Array[Int]] =
    if (isSatisfiable) Option(getModelArray)
    else None
    
  def nVars: Int = nofVariables
    
  def nConstraints: Int = nofClauses
    
  def getStat: Map[String, Number] = stat
    
  def setTimeout(time: Int): Unit = tout = time
    
  def dumpCnf(name: String): Unit =  Process(s"cp ${satFileProblem.getAbsolutePath} ${name}").run
  def dumpCnf: Unit = Process(s"cat ${satFileProblem.getAbsolutePath}").run

  /*
   * (not-Supported Methods in External SATSolvers) 
   */
  def clearLearntClauses =
    throw new java.lang.UnsupportedOperationException("clearLearntClauses is not supported in External SAT Solvers")
  def dumpStat(filePath: String) =
    throw new java.lang.UnsupportedOperationException("printStat is not supported in External SAT Solvers")
  def dumpStat =
    throw new java.lang.UnsupportedOperationException("printStat is not supported in External SAT Solvers")
  def addAtLeast(lits: Seq[Int], degree: Int): Unit =
    throw new java.lang.UnsupportedOperationException("addAtLeast is not supported in External SAT Solvers")
  def addAtMost(lits: Seq[Int], degree: Int): Unit =
    throw new java.lang.UnsupportedOperationException("addAtMost is not supported in External SAT Solvers")
  def addExactly(lits: Seq[Int], degree: Int): Unit =
    throw new java.lang.UnsupportedOperationException("addExactly is not supported in External SAT Solvers")
  def addPB(lits: Seq[Int], coef: Seq[Int], degree: Int): Unit =
    throw new java.lang.UnsupportedOperationException("addPB is not supported in External SAT Solvers")
  def addBBC(block: Int, lits: Seq[Int], degree: Int): Unit =
    throw new java.lang.UnsupportedOperationException("addBBC is not supported in External SAT Solvers")
  def addConstr(c: org.sat4j.specs.Constr): Unit =
    throw new java.lang.UnsupportedOperationException("addConstr is not supported in External SAT Solvers")
  def isSatisfiable(assumps: Seq[Int]): Boolean =
    throw new java.lang.UnsupportedOperationException("isSatisfiable with Assumption is not supported in External SAT Solvers")
  def findModel(assumps: Seq[Int]): Array[Int] =
    throw new java.lang.UnsupportedOperationException("findModel with Assumption is not supported in External SAT Solvers")
  def minExplain: Array[Int] =
    throw new java.lang.UnsupportedOperationException("minimalExp is not supported in External SAT Solvers")
  def minAllExplain =
    throw new java.lang.UnsupportedOperationException("minimalAllExp is not supported in External SAT Solvers")
  def findMinimalModel(ps: Seq[Int]): Option[Seq[Boolean]] =
    throw new java.lang.UnsupportedOperationException("findMinimalModel is not supported in External SAT Solvers")
  def findBackbone(ps: Seq[Int]): Set[Int] =
    throw new java.lang.UnsupportedOperationException("findBackbone is not supported in External SAT Solvers")
  def nextFreeVarID(reserve: Boolean): Int =
    throw new java.lang.UnsupportedOperationException("nextFreeVarId is not supported in External SAT Solvers")
}

/*
 * Wrapper class of SAT Solver without out files (e.g., lingeling) for external execution (for evaluation of tight and sparse integration)
 * 
 * cmd: path for SAT solvers
 * fileName: filename for *.cnf and *.log
 * options: configuration options
 * keep: flag if one keeps *.cnf and *.log files  
 */
class ExtSolverNoOutFile(cmd: String, fileName: String = "", options: String = "", keep: Boolean = false) extends ExtSatSolver(cmd, fileName, keep) {
  def this(cmd: String) = this(cmd, "", "", false)
  def this(cmd: String, keep: Boolean) = this(cmd, "", "", keep)

  override protected def getModelFromOutFile: Boolean = {
    var result = false
    val source = Source.fromFile(logFile.getAbsolutePath())
    val sol = """v ([0-9 -]+)""".r
    var stringModel = ""

    for (line <- source.getLines.map(_.trim)) {
      line match {
        case "s SATISFIABLE" => {
          result = true
          stringModel = ""
        }
        case "s UNSATISFIABLE" => {
          result = false
          stringModel = ""
        }
        case sol(s) => { stringModel = stringModel + s + " " }
        case _      =>
      }
    }
    if (result)
      modelArray = stringModel.split(" +").map(_.toInt).toArray.dropRight(1)

    result
  }

  override protected def runExtSolver = {
    run(s"${cmd} ${options} ${satFileProblem.getAbsolutePath}")
    getModelFromOutFile
  }
}

/*
 * Wrapper class of Sat4j for external execution (for evaluation of tight and sparse integration)
 * 
 * cmd: path for org.sat4j.core.jar (users also can add java options here, e.g., -Xms4g -Xmx4g)
 * fileName: filename for *.cnf and *.log
 * config: configuration for Sat4j
 * keep: flag if one keeps *.cnf and *.log files  
 */
class ExtSat4j(jarPath: String, fileName: String = "", javaOption: String = "", config: String = "", keep: Boolean = false) extends ExtSatSolver(jarPath, fileName, keep) {
  def this(jarPath: String) = this(jarPath, "", "", "", false)
  def this(jarPath: String, keep: Boolean) = this(jarPath, "", "", "", keep)

  override protected def getModelFromOutFile: Boolean = {
    var result = false
    val source = Source.fromFile(logFile.getAbsolutePath())
    val sol = """v ([0-9 -]+) 0""".r
    for (line <- source.getLines.map(_.trim)) {
      line match {
        case "s SATISFIABLE"   => result = true
        case "s UNSATISFIABLE" => result = false
        case sol(s)            => modelArray = s.split(" ").map(_.toInt).toArray
        case _                 =>
      }
    }
    result
  }

  override protected def runExtSolver = {
    if (config == "")
      run(s"java ${javaOption} -jar ${jarPath} ${satFileProblem.getAbsolutePath}")
    else
      run(s"java ${javaOption} -jar ${jarPath} ${config} ${satFileProblem.getAbsolutePath}")
    getModelFromOutFile
  }
}

class FileProblem(cnfFile: java.io.File) {
  import java.io.FileOutputStream
  import java.io.RandomAccessFile
  import java.nio.ByteBuffer
  import java.nio.channels.FileChannel

  val SAT_BUFFER_SIZE = 256 * 1024
  val MAX_SAT_SIZE = 3 * 1024 * 1024 * 1024L

  var nofVariables = 0
  var nofClauses = 0
  var fileSize: Long = 0

  var nofVariablesCommitted = 0
  var nofClausesCommitted = 0
  var fileSizeCommitted: Long = 0

  var satFileChannel: Option[FileChannel] = None
  var satByteBuffer: Option[ByteBuffer] = None

  init

  /* */
  def getCnfFile =
    cnfFile
  /* */
  def setNumberOfVariables(n: Int) =
    nofVariables = n
  /* */
  def setNumberOfClauses(n: Int) =
    nofClauses = n
  /* */
  def getAbsolutePath =
    cnfFile.getAbsolutePath
  /* */
  def open() {
    if (satFileChannel.nonEmpty)
      throw new java.lang.Exception("Internal error: re-opening file " + cnfFile.getAbsolutePath)
    try {
      if (fileSize == 0) {
        satFileChannel = Option((new FileOutputStream(cnfFile.getAbsolutePath)).getChannel)
      } else {
        satFileChannel = Option((new RandomAccessFile(cnfFile.getAbsolutePath, "rw")).getChannel)
        satFileChannel.get.position(fileSize)
      }
      satByteBuffer = Option(ByteBuffer.allocateDirect(SAT_BUFFER_SIZE))
    } catch {
      case e: IOException => throw new IOException
    }
  }
  /* */
  def write(b: Seq[Byte]): Unit = {
    if (satFileChannel.isEmpty)
      open
    val len = b.size
    if (satByteBuffer.get.position() + len > SAT_BUFFER_SIZE)
      flush
    satByteBuffer.get.put(b.toArray)
    fileSize = fileSize + len
    if (fileSize >= MAX_SAT_SIZE)
      throw new java.lang.Exception("Encoding is interrupted because file size becomes too large (" + fileSize + " bytes)")
  }
  /* */
  def write(s: String): Unit =
    write(s.getBytes)
  /* */
  def flush(): Unit =
    satFileChannel match {
      case None => ()
      case Some(fc) =>
        try {
          satByteBuffer.get.flip // limit = position;  position = 0
          fc.write(satByteBuffer.get)
          satByteBuffer.get.clear
        } catch {
          case e: IOException => throw new IOException("IOException is happen while flush FileProblem")
        }
    }

  /* */
  def close(): Unit =
    satFileChannel match {
      case None => ()
      case Some(fc) =>
        try {
          flush
          fc.close
          satFileChannel = None
          satByteBuffer = None
        } catch {
          case e: IOException => throw new IOException("IOException is happen while close FileProblem")
        }
    }

  /* */
  def update(): Unit = {
    val n = 64
    val s: StringBuilder = new StringBuilder
    s.append("p cnf ")
    s.append(nofVariables.toString)
    s.append(" ")
    s.append(nofClauses.toString)
    while (s.length < n - 1)
      s.append(" ");

    s.append("\n");
    val header = s.toString

    if (satFileChannel.nonEmpty) {
      throw new java.lang.Exception("Internal error: updating opening file " + cnfFile.getAbsolutePath)
    }

    try {
      val satFile1: RandomAccessFile = new RandomAccessFile(cnfFile.getAbsolutePath, "rw")
      satFile1.seek(0)
      satFile1.write(header.getBytes())
      if (fileSize == 0)
        fileSize = header.length
      satFile1.setLength(fileSize)
      satFile1.close
    } catch {
      case e: IOException => throw new IOException("IOException is happen while update FileProblem")
    }
  }
  /* */
  def init() = {
    fileSize = 0
    nofVariables = 0
    nofClauses = 0
    update
  }
  /* */
  def done() {
    if (nofClauses == 0) {
      if (nofVariables == 0)
        nofVariables += 1
      addClause2CnfFile(Seq(1, -1))
      nofClauses += 1
    }
    flush
    close
    update
  }
  /* */
  def addComment(comment: String) =
    write("c " + comment + "\n")
  /* */
  def commit() = {
    nofVariablesCommitted = nofVariables
    nofClausesCommitted = nofClauses
    fileSizeCommitted = fileSize
  }
  /* */
  def rollback() = {
    done
    nofVariables = nofVariablesCommitted
    nofClauses = nofClausesCommitted
    fileSize = fileSizeCommitted
    update
  }
  /* */
  def addClause2CnfFile(lits: Seq[Int]) =
    write(lits.mkString("", " ", " 0\n"))
}


