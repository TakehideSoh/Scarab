package jp.kobe_u.scarab

import scala.collection.SortedSet
import scala.collection.immutable.HashMap
import scala.collection.immutable.HashSet

/**
 * Classes for CSP data structures.
 * @author Takehide Soh
 * @author Naoyuki Tamura
 */

/**
 * Abstract class for `Term`s and `Constraint`s.
 *
 * {{{
 *     Expr ::= Term | Constraint
 * }}}
 */
abstract class Expr

/**
 * `Term` is an abstract class for linear arithmetic expressions.
 *
 * {{{
 *     Term ::= Var(String, String*) | Sum(Int, Map[Var,Int])
 * }}}
 * See examples described in [[jp.kobe_u.scarab.csp.Sum]] section.
 */
sealed abstract class Term extends Expr {
  /**
   * Returns the `Sum` representation of the term.
   * It returns a new `Sum` object when the term is a variable, and return itself when the term is a `Sum`.
   */
  def toSum: Sum = this match {
    case x: Var => Sum(x)
    case s: Sum => s
  }
  /** Returns the negation of the term. */
  def unary_- : Sum = toSum.neg
  /** Returns the addition of the term and `b`. */
  def +(b: Int): Sum = toSum.add(b)
  /** Returns the addition of the term and `x`. */
  def +(x: Term): Sum = toSum.add(x.toSum)
  /** Returns the subtraction of `b` from the term. */
  def -(b: Int): Sum = toSum.sub(b)
  /** Returns the subtraction of `x` from the term. */
  def -(x: Term): Sum = toSum.sub(x.toSum)
  /** Returns the multiplication of the term and `a`. */
  def *(a: Int): Sum = toSum.mul(a)
  /** Returns the constraint representing `this <= b`, that is, `LeZero(this - b)`. */
  def <=(b: Int): Constraint = LeZero(toSum.sub(b))
  /** Returns the constraint representing `this <= x`, that is, `LeZero(this - x)`. */
  def <=(x: Term): Constraint = LeZero(toSum.sub(x.toSum))
  /** Returns the constraint representing `this < b`. */
  def <(b: Int): Constraint = this <= b - 1
  /** Returns the constraint representing `this < x`. */
  def <(x: Term): Constraint = this <= x - 1
  /** Returns the constraint representing `this >= b`. */
  def >=(b: Int): Constraint = GeZero(toSum.sub(b))
  /** Returns the constraint representing `this >= x`. */
  def >=(x: Term): Constraint = GeZero(toSum.sub(x.toSum))
  /** Returns the constraint representing `this > b`. */
  def >(b: Int): Constraint = this >= b + 1
  /** Returns the constraint representing `this > x`. */
  def >(x: Term): Constraint = this >= x + 1
  /** Returns the constraint representing `this === b`, that is, `And(this <= b, this >= b)`. */
  def ===(b: Int): Constraint = EqZero(toSum.sub(b))
  /** Returns the constraint representing `this === x`, that is, `And(this <= x, this >= x)`. */
  def ===(x: Term): Constraint = EqZero(toSum.sub(x.toSum))
  /** Returns the constraint representing `this !== b`, that is, `Or(this < b, this > b)`. */
  def !==(b: Int): Constraint = NeZero(toSum.sub(b))
  /** Returns the constraint representing `this !== x`, that is, `Or(this < x, this > x)`. */
  def !==(x: Term): Constraint = NeZero(toSum.sub(x.toSum))
  /** Returns this >= 1 */
  def ? = GeZero(this - 1)
  /** Returns this <= 0 */
  def ! = LeZero(this.toSum)
  /** Returns the value of the term under the given assingment. */
  def value(assignment: Assignment): Int
}

/**
 * `Var` is a case class for integer variables.
 *
 * New variable is generated by applying any objects as arguments to an existing variable.
 * {{{
 *     scala> val x = Var("x")    // Creates a new variable
 *     x: jp.kobe_u.scarab.csp.Var = x
 *
 *     scala> val x12 = x(1,2)    // Creates another new variable
 *     x12: jp.kobe_u.scarab.csp.Var = x(1,2)
 * }}}
 *
 * Implicit conversion from Scala Symbol to Var is defined in csp package object.
 * Therefore, the following convention can be used when `jp.kobe_u.scarab.csp._` is imported.
 * {{{
 *     scala> val x12 = 'x(1,2)
 *     x12: jp.kobe_u.scarab.csp.Var = x(1,2)
 * }}}
 *
 * Anonymous variable is generated by calling `Var` with no arguments.
 * `TMP_I_n` is used as the name of anonymous variables (`n` begins from 1).
 * The value of `aux` is set to true for anonymous variables.
 * {{{
 *     scala> val y = Var()       // Creates an anonymous variable
 *     y: jp.kobe_u.scarab.csp.Var = TMP_I_1
 *
 *     scala> y.aux
 *     res1: Boolean = true
 * }}}
 * @constructor Creates a new variable of the given name and suffixes.
 * @param name header name of the variable
 * @param is suffixes of the variable
 */
case class Var(name: String, is: String*) extends Term {
  /** This is set to true for anonymous variables. */
  var isAux = false
  /** Returns a new variable appending `is1` as suffixes. */
  def apply(is1: Any*) =
    Var(name, is ++ is1.map(_.toString): _*)
  /** Returns the addition of the term and `b`. */
  def add(b: Int): Sum = this + b
  /** Returns the addition of the term and `x`. */
  def add(x: Term): Sum = this + x.toSum
  /** Returns the subtraction of `b` from the term. */
  def sub(b: Int): Sum = this - b
  /** Returns the subtraction of `x` from the term. */
  def sub(x: Term): Sum = this - x.toSum
  /** Returns the multiplication of the term and `a`. */
  def mul(a: Int): Sum = this * a

  def value(assignment: Assignment) = assignment.intMap(this)
  override def toString =
    if (is.size == 0) name else is.mkString(name + "(", ",", ")")
}
/** Companion object of `Var`. */
object Var {
  private var count = 0
  /** Returns a new anonymous variable. */
  def apply() = {
    count += 1; val x = new Var("TMP_I_" + count); x.isAux = true
    x
  }
}

/**
 * `Sum` is a case class for linear expressions.
 * `Sum` object consists of an integer constant `b` and a mapping `coef` representing the coefficients of integer variables.
 * It reprensents the linear summmation b + a1*x1 + a2*x2 + ... + an*xn when
 * `coef` is a map Map(x1 -> a1, x2 -> a2, ..., xn -> an).
 * {{{
 *     scala> val x = Var("x")
 *     scala> val sum = Sum(1, Map(x(1)->1, x(2)->2))
 *     sum: jp.kobe_u.scarab.csp.Sum = Sum(1+x(1)+2*x(2))
 * }}}
 * When `Sum` object is constructed directly, the values of `coef` must not be zero.
 * {{{
 *     scala> Sum(1, Map(x(1)->1, x(2)->0))
 *     java.lang.IllegalArgumentException: requirement failed: Coefficients of Sum should not be zero.
 *
 *     scala> Sum(1, Map(x(1)->1, x(2)->0).filter(_._2 != 0))
 *     res0: jp.kobe_u.scarab.csp.Sum = Sum(1+x(1))
 * }}}
 *
 * Companion object `Sum` provides several easy-to-use construction methods for `Sum`.
 * {{{
 *     scala> Sum(1)            // Sum(1)
 *     scala> Sum(x(1), x(2))   // Sum(+x(1)+x(2))
 *     scala> Sum(x(1), x(1))   // Sum(+2*x(1))
 * }}}
 *
 * Operations of addition, subtraction, negation, and constant multiplication are defined on `Sum` objects.
 * When coefficients become zero, they are removed from the result.
 * {{{
 *     scala> Sum(x).add(1)         // Sum(1+x)
 *     scala> Sum(x).add(1).sub(x)  // Sum(1)
 *     scala> Sum(x).mul(3)         // Sum(+3*x)
 * }}}
 *
 * Infix and prefix operators defined in `Term` class can be used.
 * {{{
 *     scala> x + 1                 // Sum(1+1*x)
 *     scala> x + 1 - x             // Sum(1)
 *     scala> x * 3                 // Sum(0+3*x)
 * }}}
 *
 * @constructor Creates a new `Sum` of the given constant and variables with specified coefficients.
 * @param b constant part of the summation
 * @param coef map consisting of variables and their coefficients
 */
case class Sum(b: Int, coef: Map[Var, Int]) extends Term {
  require(coef.values.forall(_ != 0), "Coefficients of Sum should not be zero.")
  /** Returns the negation of this, that is, `-this`. */
  def neg = Sum(-b, coef.map { case (x, a) => (x, -a) })
  /** Returns the addition of this and `b`. */
  def add(b1: Int): Sum = Sum(b + b1, coef)
  /** Returns the addition of this and `a*x`.  When `a` is omitted 1 is used. */
  def add(x: Var, a: Int = 1): Sum = {
    val a1 = coef.getOrElse(x, 0) + a
    val coef1 = if (a1 == 0) coef - x else coef + (x -> a1)
    Sum(b, coef1)
  }
  /** Returns the addition of this and `that`. */
  def add(that: Sum): Sum =
    that.coef.foldLeft(this)((sum, xa) => sum.add(xa._1, xa._2)).add(that.b)
  /** Returns the subtraction of `b` from this. */
  def sub(b1: Int): Sum = add(-b1)
  /** Returns the subtraction of `a*x` from this.  When `a` is omitted 1 is used. */
  def sub(x: Var, a: Int = 1): Sum = add(x, -a)
  /** Returns the subtraction of `that` from this. */
  def sub(that: Sum): Sum = add(that.neg)
  /** Returns the multiplication of this by `a`. */
  def mul(a: Int) =
    if (a == 0) Sum.zero else Sum(a * b, coef.map(xa => (xa._1, a * xa._2)))
  def value(assignment: Assignment) =
    b + coef.map(xa => xa._1.value(assignment) * xa._2).sum
  override def toString = {
    def s(a: Int, x: Var) =
      if (a == 0) ""
      else if (a < 0) "-" + (if (a == -1) "" else -a + "*") + x.toString
      else "+" + (if (a == 1) "" else a + "*") + x.toString
    ((if (b == 0) "" else b.toString) +: coef.map(xa => s(xa._2, xa._1)).toSeq).mkString(productPrefix + "(", "", ")")
  }
}

/** Companion object of `Sum`. */
object Sum {
  /** Constant value 0. */
  val zero = new Sum(0, Map.empty)
  /** Returns the sum of `b` and variables `xs`. */
  def apply(b: Int, xs: Var*): Sum = xs.foldLeft(zero.add(b))(_.add(_))
  /** Returns the sum of `x` and variables `xs`. */
  def apply(x: Var, xs: Var*): Sum = (x +: xs).foldLeft(zero)(_.add(_))
  /** Returns the sum of `s1`, `s2` and terms `ss`. */
  def apply(s1: Term, s2: Term, ss: Term*): Sum = (s1 +: (s2 +: ss)).foldLeft(zero)((z, n) => z + n)
  def apply(s: Sum): Sum = s
  // /** Returns the sum of the elements of `xs`. */
  // // def apply(xs: Seq[Var]): Sum = xs.foldLeft(zero)(_.add(_))
  /** Returns the sum of the elements of `ss`. */
  def apply(ss: Seq[Term]): Sum = ss.foldLeft(zero)((z, n) => z + n)
}

/**
 * `Constraint` is an abstract class for constraints.
 *
 * {{{
 *     Constraint ::= Literal | And(Constraint*) | Or(Constraint*)
 *     Literal    ::= Bool(String, String*) | Not(Bool) | LeZero(Sum)
 * }}}
 *
 * The following is an example describing constraints.
 * {{{
 *     scala> 'x + 'y === 7
 *     res0: jp.kobe_u.scarab.csp.Constraint = And(LeZero(Sum(-7+x+y)),LeZero(Sum(7-x-y)))
 *
 *     scala> 'x * 2 + 'y * 4 === 20
 *     res1: jp.kobe_u.scarab.csp.Constraint = And(LeZero(Sum(-20+2*x+4*y)),LeZero(Sum(20-2*x-4*y)))
 *
 *     scala> 'x + 1 <= 'y || 'y + 2 <= 'x
 *     res2: jp.kobe_u.scarab.csp.Or = Or(LeZero(Sum(1+x-y)),LeZero(Sum(2+y-x)))
 *
 *     scala> ! ('x + 1 <= 'y || 'y + 2 <= 'x)
 *     res3: jp.kobe_u.scarab.csp.Constraint = And(LeZero(Sum(-x+y)),LeZero(Sum(-1-y+x)))
 * }}}
 *
 * Constraints are always represented in Negative Normal Forms (NNFs)
 * since `Not` can be applied only to `Bool`s.
 */
sealed abstract class Constraint extends Expr {
  /** Returns the negation of this constraint. */
  def unary_! : Constraint = this match {
    case p: Bool      => Not(p)
    case Not(p)       => p
    case LeZero(sum)  => GeZero(sum.sub(1))
    case GeZero(sum)  => LeZero(sum.add(1))
    case EqZero(sum)  => NeZero(sum)
    case NeZero(sum)  => EqZero(sum)
    case And(cs @ _*) => Or(cs.map(!_))
    case Or(cs @ _*)  => And(cs.map(!_))
  }
  /** Returns the conjunction of this and `c`. */
  def &&(c: Constraint) = And(this, c)
  /** Returns the disjunction of this and `c`. */
  def ||(c: Constraint) = Or(this, c)
  /** Returns the constraint representing `this ==> c`, that is `! this || c`. */
  def ==>(c: Constraint) = Or(!this, c)
  /** Returns the constraint representing `this ==> c`, that is `! this || c`. */
  def <==>(c: Constraint) = And(Or(!this, c), Or(this, !c))
  /** Returns the truth value of the constraint under the given assignment. */
  def value(assignment: Assignment): Boolean
}

/**
 * `Literal` is an abstract class for literals.
 *
 * {{{
 *     Literal    ::= Bool(String, String*) | Not(Bool) | LeZero(Sum)
 * }}}
 */
sealed abstract class Literal extends Constraint

/**
 * `Bool` is a case class for Boolean variables.
 *
 * New variable is generated by applying any objects as arguments to an existing variable.
 * {{{
 *     scala> val p = Bool("p")   // Creates a new variable
 *     p: jp.kobe_u.scarab.csp.Bool = p
 *
 *     scala> val p12 = p(1,2)    // Creates another new variable
 *     p12: jp.kobe_u.scarab.csp.Bool = p(1,2)
 * }}}
 * Anonymous variable is generated by calling `Bool` with no arguments.
 * `TMP_B_n` is used as the name of anonymous variables (`n` begins from 1).
 * The value of `aux` is set to true for anonymous variables.
 * {{{
 *     scala> val q = Bool()      // Creates an anonymous variable
 *     q: jp.kobe_u.scarab.csp.Bool = TMP_B_1
 *
 *     scala> q.aux
 *     res1: Boolean = true
 * }}}
 * @constructor Creates a new variable of the given name and suffixes.
 * @param name header name of the variable
 * @param is suffixes of the variable
 */
case class Bool(name: String, is: String*) extends Literal {
  /** This is set to true for anonymous variables. */
  var isAux = false
  /** Returns a new variable appending `is1` as suffixes. */
  def apply(is1: Any*) =
    Bool(name, is ++ is1.map(_.toString): _*)
  def value(assignment: Assignment) = assignment.boolMap(this)
  override def toString =
    if (is.size == 0) name else is.mkString(name + "(", ",", ")")
}
/** Companion object of `Bool`. */
object Bool {
  private var count = 0
  /** Returns a new anonymous variable. */
  def apply() = {
    count += 1; val p = new Bool("TMP_B_" + count); p.isAux = true
    p
  }
}

/**
 * `Not` is a case class for negations of Boolean variables.
 *
 * Prefix operator `!` defined in `Term` class can be used to create this object.
 * {{{
 *     scala> val p = Bool("p")
 *     scala> Not(p)               // Not(p)
 *     scala> ! p                  // Not(p)
 * }}}
 *
 * @constructor Creates a new `Not` of the given Boolean variable.
 * @param p Boolean variable
 */
case class Not(p: Bool) extends Literal {
  def value(assignment: Assignment) = !p.value(assignment)
}

/**
 * `LeZero` is a case class representing the constraint meaning the given linear summation is less than or equal to zero, that is, `sum <= 0`.
 *
 * Infix operators defined in `Term` class can be used to create this object.
 * {{{
 *     scala> val x = Var("x")
 *     scala> x(1) <= 1               // LeZero(Sum(-1+x(1)))
 *     scala> x(1) > x(2)             // LeZero(Sum(1-x(1)+x(2)))
 * }}}
 *
 * @constructor Creates a new `LeZero` constraint meaning the given linear summation is less than or equal to zero.
 * @param sum linear summation
 */
case class LeZero(sum: Sum) extends Literal {
  def value(assignment: Assignment) = sum.value(assignment) <= 0
}

/**
 * `GeZero` is a case class representing the constraint meaning the given linear summation is less than or equal to zero, that is, `sum >= 0`.
 *
 * Infix operators defined in `Term` class can be used to create this object.
 * {{{
 *     scala> val x = Var("x")
 *     scala> x(1) >= 1               // GeZero(Sum(-1+x(1)))
 *     scala> x(1) > x(2)             // GeZero(Sum(1+x(1)-x(2)))
 * }}}
 *
 * @constructor Creates a new `GeZero` constraint meaning the given linear summation is greater than or equal to zero.
 * @param sum linear summation
 */
case class GeZero(sum: Sum) extends Literal {
  def value(assignment: Assignment) = sum.value(assignment) >= 0
}

/**
 * `EqZero` is a case class representing the constraint meaning the given linear summation is equal to zero, that is, `sum == 0`.
 *
 * Infix operators defined in `Term` class can be used to create this object.
 * {{{
 *     scala> val x = Var("x")
 *     scala> x(1) === 1               // EqZero(Sum(-1+x(1)))
 * }}}
 *
 * @constructor Creates a new `EqZero` constraint meaning the given linear summation is equal to zero.
 * @param sum linear summation
 */
case class EqZero(sum: Sum) extends Literal {
  def value(assignment: Assignment) = sum.value(assignment) == 0
}

/**
 * `NeZero` is a case class representing the constraint meaning the given linear summation is not equal to zero, that is, `sum != 0`.
 *
 * Infix operators defined in `Term` class can be used to create this object.
 * {{{
 *     scala> val x = Var("x")
 *     scala> x(1) !== 1               // NeZero(Sum(-1+x(1)))
 * }}}
 *
 * @constructor Creates a new `NeZero` constraint meaning the given linear summation is not equal to zero.
 * @param sum linear summation
 */
case class NeZero(sum: Sum) extends Literal {
  def value(assignment: Assignment) = sum.value(assignment) != 0
}

/**
 * `And` is a case class representing the conjunction of constraints.
 *
 * Infix operators defined in `Constraint` class can be used to create this object.
 * The operator `===` on terms also create an `And` object.
 * {{{
 *     scala> val x = Var("x")
 *     scala> x(1) <= 1 && x(1) > x(2)  // And(LeZero(Sum(-1+x(1))),LeZero(Sum(1-x(1)+x(2))))
 *     scala> x(1) === 1                // And(LeZero(Sum(-1+x(1))),LeZero(Sum(1-x(1))))
 * }}}
 *
 * @constructor Creates a new `And` constraint representing the conjuction of the elements of `cs`.
 * @param cs elements of the conjunction
 */
case class And(cs: Constraint*) extends Constraint {
  def value(assignment: Assignment) = cs.forall(_.value(assignment))
  override def toString = cs.mkString(productPrefix + "(", ",", ")")
}
/** Companion object of `And`. */
object And {
  /** Returns the conjunction of the elements of `cs`. */
  def apply(cs: TraversableOnce[Constraint]) = new And(cs.toSeq: _*)
}

/**
 * `Or` is a case class representing the disjunction of constraints.
 *
 * Infix operators defined in `Constraint` class can be used to create this object.
 * The operator `!==` on terms also create an `Or` object.
 * {{{
 *     scala> val x = Var("x")
 *     scala> x(1) <= 1 || x(1) > x(2)  // Or(LeZero(Sum(-1+x(1))),LeZero(Sum(1-x(1)+x(2))))
 *     scala> x(1) !== 1                // Or(LeZero(Sum(+x(1))),LeZero(Sum(2-x(1))))
 * }}}
 *
 * @constructor Creates a new `Or` constraint representing the disjunction of the elements of `cs`.
 * @param cs elements of the disjunction
 */
case class Or(cs: Constraint*) extends Constraint {
  def value(assignment: Assignment) = cs.exists(_.value(assignment))
  override def toString = cs.mkString(productPrefix + "(", ",", ")")
}
/** Companion object of `Or`. */
object Or {
  /** Returns the disjunction of the elements of `cs`. */
  def apply(cs: TraversableOnce[Constraint]) = new Or(cs.toSeq: _*)
}

/**
 * `Domain` is a case class representing the domain of variables.
 * It is required to be `lb <= ub`.
 * 
 * (case of contiguous {5, 6, 7, 8})
 * value 5 6 7 8
 *   pos 0 1 2 3
 * 
 * (case of non-contiguous {3, 5, 7, 9})
 * value 3 5 7 9
 *   pos 0 1 2 3
 *    ar 3 4 5 6 7 8 9
 *       0 1 2 3 4 5 6 // ar のインデックス
 *  ar値 0 0 1 1 2 2 3 // ar の値
 *  
 */
case class Domain private (lb: Int, ub: Int, size: Int, private val domainOpt: Option[Seq[Int]]) {
  require(lb <= ub)
  private var ar: Array[Int] = Array.fill[Int](ub - lb + 1)(0)
  val binary = lb == 0 && ub == 1
  private var hs = HashSet.empty[Int]

  val offset = lb
  domainOpt match {
    case None => ()
    case Some(domain) =>
      domain.foreach(i => hs += i)
      var cnt = -1
      for (i <- lb to ub) {
        if (hs.contains(i)) {
          cnt += 1
          ar(i - offset) = cnt
        } else {
          ar(i - offset) = cnt
        }
      }
  }
  val isContiguous = domainOpt.isEmpty
  val domain: Seq[Int] = if (isContiguous) Seq.empty else domainOpt.get 

//  def isContiguous = iscontiguous 
//  def domain: Seq[Int] = domainOpt.get 
  def pos(value: Int): Int = if (isContiguous) value - lb else ar(value - offset)

  def show {
    val s1 = for (a <- ar) yield a
    println(s1.mkString(" "))
  }
  
  override def toString =
    domainOpt match {
      case None                              => s"Domain($lb to $ub)"
      case Some(domain) if domain.size <= 10 => s"Domain(${domain.mkString(",")})"
      case Some(domain) =>
        s"Domain(${domain(0)}, ${domain(1)}, ..., ${domain(domain.size - 2)}, ${domain(domain.size - 1)})"
    }
}

object Domain {
  /**
   * Creates a domain.
   * @param lb the lower bound value of the domain
   * @param ub the upper bound value of the domain
   */
  def apply(lb: Int, ub: Int): Domain = Domain((lb to ub).toSeq)
  def apply(domain0: Seq[Int]): Domain = {
    val sorted = domain0.sorted
    val lb = sorted.head
    val ub = sorted.last
    require(lb <= ub)
    val size = sorted.size
    val cntgs = (ub == lb || ub - lb + 1 == size)
    Domain(lb, ub, size,
      if (cntgs) None
      else Some(sorted))
  }
}

/**
 * `Assignment` is a case class representing the assignment for integer variables and Boolean variables.
 * The assignment is returned from the [[jp.kobe_u.scarab.solver.Solver]] as a solution.
 *
 * {{{
 *     scala> val x = Var("x")
 *     scala> val y = Var("y")
 *     scala> val assignment = Assignment(Map(x->1, y->2), Map.empty)
 *     scala> (x + y).value(assignment)
 *     res0: Int = 3
 * }}}
 * @constructor Creates a new assignment consisting of `intMap` and `boolMap`.
 * @param intMap the assignment for integer variables
 * @param boolMap the assignment for Boolean variables
 * @see The `value` method of [[jp.kobe_u.scarab.csp.Term]].
 * @see The `value` method of [[jp.kobe_u.scarab.csp.Constraint]].
 * @see The `verify` method of [[jp.kobe_u.scarab.csp.CSP]].
 * @see The `solution` variable of [[jp.kobe_u.scarab.solver.Solver]].
 */
case class Assignment(intMap: Map[Var, Int], boolMap: Map[Bool, Boolean]) {
  /** Returns the value of the given term under this assignment. */
  def apply(x: Term): Int = x.value(this)
  /** Returns the truth value of the given constraint under this assignment. */
  def apply(c: Constraint): Boolean = c.value(this)
}

/**
 * `CSP` is a case class representing the CSP (Constraint Satisfaction Problem).
 *
 * It consists of integer variables (`variables`),
 * boolean variables (`bools`),
 * domain function (`dom`), and
 * constraints (`constraints`).
 * Variables of these members are declared by `var`.   Therefore, CSP is a mutable object.
 *
 * {{{
 *     val csp = CSP()                  // Create a new CSP
 *     val x = csp.int(Var("x"), 1, 9)  // declare a variable x in 1..9.
 *     val y = csp.int(Var("y"), 1, 9)  // declare a variable y in 1..9.
 *     csp.add(x + y === 7)             // add a constraint of x === y.
 * }}}
 *
 * Implicit conversion from Scala Symbol to Var is defined in [[jp.kobe_u.scarab.csp]] package object.
 * Therefore, the above program can be written as follows when `jp.kobe_u.scarab.csp._` is imported.
 *
 * {{{
 *     val csp = CSP()
 *     csp.int('x, 1, 9)
 *     csp.int('y, 1, 9)
 *     csp.add('x + 'y === 7)
 * }}}
 *
 * When the CSP is encoded by [[jp.kobe_u.scarab.solver.Encoder]],
 * the contents of the CSP (that is, variables, bools, dom, and constraints) will be modified.
 *
 * @constructor Creates a new `CSP` consisting of specified variables, bools, domain, and constraints.
 * @param variables integer variables of the CSP
 * @param bools Boolean variables of the CSP
 * @param dom domain function of integer variables of the CSP
 * @param constraints constraints of the CSP
 * @see [[jp.kobe_u.scarab]].
 */
case class CSP(var variables: IndexedSeq[Var] = IndexedSeq.empty,
               var bools: IndexedSeq[Bool] = IndexedSeq.empty,
               var dom: Map[Var, Domain] = Map.empty,
               var constraints: IndexedSeq[Constraint] = IndexedSeq.empty) {

  var boolHash = HashSet.empty[Bool]
  var varHash = HashSet.empty[Var]

  /**
   * Adds the integer variable `x` with the domain `d` to the CSP and returns the variable.
   * It throws IllegalArgumentException when `x` is added twice or more.
   * @param x the integer variable to be added
   * @param d the domain of the integer variable `x`
   * @return the integer variable added
   * @throws IllegalArgumentException when `x` is added twice or more
   */
  def int(x: Var, d: Domain): Var = {
    if (varHash.contains(x))
      throw new IllegalArgumentException("duplicate int " + x)
    varHash += x; variables = variables :+ x; dom += x -> d; x
  }
  /**
   * Adds the integer variable `x` with the domain `Domain(lb, ub)` to the CSP and returns the variable.
   * It throws IllegalArgumentException when `x` is added twice or more.
   * @param x the integer variable to be added
   * @param lb the lower bound of the integer variable `x`
   * @param ub the upper bound of the integer variable `x`
   * @return the integer variable added
   * @throws IllegalArgumentException when `x` is added twice or more
   */
  def int(x: Var, lb: Int, ub: Int): Var =
    int(x, Domain(lb, ub))

  def int(x: Var, d: Seq[Int]): Var =
    int(x, Domain(d))

  /**
   * Adds a new anonymous integer variable with the domain `d` to the CSP and returns the variable.
   * @param d the domain of the anonymous integer variable
   * @return the anonymous integer variable added
   */
  def newInt(d: Domain): Var = int(Var(), d)
  /**
   * Adds a new anonymous integer variable with the domain `Domain(lb, ub)` to the CSP and returns the variable.
   * @param lb the lower bound of the anonymous integer variable
   * @param ub the upper bound of the anonymous integer variable
   * @return the anonymous integer variable added
   */
  def newInt(lb: Int, ub: Int): Var = int(Var(), lb, ub)

  /** Adds a 0-1 integer variable */
  def boolInt(x: Var): Var = int(x, 0, 1)

  /** Adds 0-1 integer variables */
  def boolInt(xs: Iterable[Term]): Iterable[Term] = {
    xs.foreach(_ match {
      case x: Var => boolInt(x)
      case _ =>
        throw new IllegalArgumentException("boolInt: argument of boolInt declaration should be a Var")
    })
    xs
  }

  /**
   * Adds the Boolean variable `p` to the CSP and returns the variable.
   * It throws IllegalArgumentException when `p` is added twice or more.
   * @param p the Boolean variable to be added
   * @return the Boolean variable added
   * @throws IllegalArgumentException when `p` is added twice or more
   */
  def bool(p: Bool): Bool = {
    if (boolHash.contains(p))
      throw new IllegalArgumentException("duplicate bool " + p)
    boolHash += p; bools = bools :+ p; p
  }

  /**
   * Adds a new anonymous Boolean variable to the CSP and returns the variable.
   * @return the anonymous Boolean variable added
   */
  def newBool: Bool = bool(Bool())

  /**
   * Adds the constraint `c` to the CSP and returns the constraint.
   * @return the constraint added
   */
  def add(c: Constraint): Constraint = {
    constraints = constraints :+ c; c
  }

  /**
   * Verifies the CSP is satisfied by the given assignment.
   * It throws java.lang.AssertionError when the CSP is not satisfied by the assignment.
   * Note that the CSP will be modified by [[jp.kobe_u.scarab.solver.Simplifier]] when the CSP is encoded,
   * and the verification is performed on the modified CSP.
   * @param the assignment
   * @throws java.lang.AssertionError when the CSP is not satisfied by the assignment.
   */
  def verify(assignment: Assignment) {
    for (x <- variables)
      assert(dom(x).lb <= x.value(assignment) && x.value(assignment) <= dom(x).ub,
        x + " is not satisfied")
    for (c <- constraints)
      assert(c.value(assignment),
        c + " is not satisfied")
  }

  /** Display CSP */
  def show {
    for (x <- variables) println("int(" + x + "," + dom(x) + ")")
    for (p <- bools) println("bool(" + p + ")")
    for (c <- constraints) println(c)
  }

  var cStack = Seq.empty[commitPoint]
  var rollbackHappen = false

  def reset {
    rollback
    while(!cStack.isEmpty) 
      rollback
    boolHash = HashSet.empty[Bool]
    varHash = HashSet.empty[Var]
    
    rollbackHappen = true    
  }
  
  /**
   * for commit/rollback model
   */
  def commit {
    cStack = commitPoint(bools.size, variables.size, constraints.size) +: cStack
  }

  def rollback {
    if (cStack.isEmpty)
      cStack = commitPoint(0, 0, 0) +: cStack
    //      throw new java.lang.Exception("No Commit Point is Made.")

    val lastCommit = cStack.head
    cStack = cStack.tail

    variables = variables.take(lastCommit.v)
    bools = bools.take(lastCommit.b)
    constraints = constraints.take(lastCommit.c)

    rollbackHappen = true
  }

}

/**
 * Abstract class of global constraints.
 */
sealed abstract class GlobalConstraint extends Constraint

case class commitPoint(var b: Int, var v: Int, var c: Int)



