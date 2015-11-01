package jp.kobe_u

package object scarab {
  import scala.language.implicitConversions  
  /** Implicit conversion of Scala Symbol to Var */
  implicit def symbol2var(s: Symbol) = Var(s.name)
}
