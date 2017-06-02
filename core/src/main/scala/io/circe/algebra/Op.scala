package io.circe.algebra

import io.circe.numbers.BiggerDecimal

sealed abstract class Op[A] {
  def bracket: Op[A]

  // These definitions are for the sake of convenience—we could get them via Cats syntax.
  def map[B](f: A => B): Op[B] = Op.Map(this, f, false)
  def flatMap[B](f: A => Op[B]): Op[B] = Op.Bind(this, f, false)
  def product[B](opB: Op[B]): Op[(A, B)] = Op.Join(this, opB, false)
  def map2[B, C](opB: Op[B])(f: (A, B) => C): Op[C] = product(opB).map(p => f(p._1, p._2))
}

sealed abstract class ReadingOp[A] extends Op[A] {
  final def bracket: Op[A] = this
}

sealed abstract class NavigationOp extends Op[Unit] {
  final def bracket: Op[Unit] = Op.Unit
}

sealed abstract class StrictOp[A] extends Op[A] {
  final def bracket: Op[A] = this
}

sealed abstract class CompositionOp[A] extends Op[A] {
  def isBracketed: Boolean
}

object Op extends OpInstances {
  val Unit = Pure[Unit](())

  // Primitive reading operations.
  case object ReadNull                                                   extends ReadingOp[Unit]
  case object ReadBoolean                                                extends ReadingOp[Boolean]
  case object ReadNumber                                                 extends ReadingOp[BiggerDecimal]
  case object ReadString                                                 extends ReadingOp[String]

  // Redundant reading operations designed to support optimizations.
  case object ReadLong                                                   extends ReadingOp[Long]

  // Reading operations for JSON objects and arrays.
  case class ReadFields[A](opA: Op[A])                                   extends ReadingOp[Vector[(String, A)]]
  case class ReadValues[A](opA: Op[A])                                   extends ReadingOp[Vector[A]]

  // Navigation operations.
  case class DownField(key: String)                                      extends NavigationOp
  case class DownAt(index: Int)                                          extends NavigationOp

  // Operations supporting composition.
  case class Pure[A](value: A)                                           extends StrictOp[A]
  case class Fail[A](failure: Failure)                                   extends StrictOp[A]
  case class Map[A, B](opA: Op[A], f: A => B, isBracketed: Boolean)      extends CompositionOp[B] {
    final def bracket: Op[B] = if (isBracketed) this else copy(isBracketed = true)
  }
  case class Bind[A, B](opA: Op[A], f: A => Op[B], isBracketed: Boolean) extends CompositionOp[B] {
    final def bracket: Op[B] = if (isBracketed) this else copy(isBracketed = true)
  }
  case class Join[A, B](opA: Op[A], opB: Op[B], isBracketed: Boolean)    extends CompositionOp[(A, B)] {
    final def bracket: Op[(A, B)] = if (isBracketed) this else copy(isBracketed = true)
  }
}
