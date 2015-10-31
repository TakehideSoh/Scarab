package jp.kobe_u.scarab

import scala.language.implicitConversions

/** CSP package of scarab
 * More explanations can be found in [[jp.kobe_u.scarab]].
 * @author Naoyuki Tamura (Kobe Univesity, Japan)
 */
package object csp {
  /** Implicit conversion of Scala Symbol to Var */
  implicit def symbol2var(s: Symbol) = Var(s.name)
}
