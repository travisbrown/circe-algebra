package io.circe.algebra

import io.circe.numbers.BiggerDecimal

sealed abstract class Op[A] {
  def isReadingOp: Boolean
  def isNavigationOp: Boolean

  // These definitions are for the sake of convenience—we could get them via Cats syntax.
  def map[B](f: A => B): Op[B] = Op.Map(this, f)
  def flatMap[B](f: A => Op[B]): Op[B] = Op.Bind(this, f)
  def product[B](opB: Op[B]): Op[(A, B)] = Op.Join(this, opB)
  def map2[B, C](opB: Op[B])(f: (A, B) => C): Op[C] = product(opB).map(p => f(p._1, p._2))
}

sealed abstract class ReadingOp[A] extends Op[A] {
  final def isReadingOp: Boolean = true
  final def isNavigationOp: Boolean = false
}

sealed abstract class NavigationOp extends Op[Unit] {
  final def isReadingOp: Boolean = true
  final def isNavigationOp: Boolean = false
}

sealed abstract class CompositionOp[A] extends Op[A] {
  final def isReadingOp: Boolean = false
  final def isNavigationOp: Boolean = false
}

object Op extends OpInstances {
  // Primitive reading operations.
  case object ReadNull                             extends ReadingOp[Unit]
  case object ReadBoolean                          extends ReadingOp[Boolean]
  case object ReadNumber                           extends ReadingOp[BiggerDecimal]
  case object ReadString                           extends ReadingOp[String]

  // Redundant reading operations designed to support optimizations.
  case object ReadLong                             extends ReadingOp[Long]

  // Reading operations for JSON objects and arrays.
  case class ReadFields[A](opA: Op[A])             extends ReadingOp[Vector[(String, A)]]
  case class ReadValues[A](opA: Op[A])             extends ReadingOp[Vector[A]]

  // Navigation operations.
  case class DownField(key: String)                extends NavigationOp
  case class DownAt(index: Int)                    extends NavigationOp

  // Brackets an operation so that any navigation it does is local.
  case class Bracket[A](opA: Op[A])                extends CompositionOp[A]

  // Operations supporting composition.
  case class Pure[A](value: A)                     extends CompositionOp[A]
  case class Fail[A](failure: Failure)             extends CompositionOp[A]
  case class Map[A, B](opA: Op[A], f: A => B)      extends CompositionOp[B]
  case class Bind[A, B](opA: Op[A], f: A => Op[B]) extends CompositionOp[B]
  case class Join[A, B](opA: Op[A], opB: Op[B])    extends CompositionOp[(A, B)]
}
