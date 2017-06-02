package io.circe.algebra

import io.circe.numbers.BiggerDecimal

sealed abstract class Op[A] {
  // These definitions are for the sake of convenience—we could get them via Cats syntax.
  def map[B](f: A => B): Op[B] = Op.Map(this, f)
  def flatMap[B](f: A => Op[B]): Op[B] = Op.Bind(this, f)
  def product[B](opB: Op[B]): Op[(A, B)] = Op.Join(this, opB)
  def map2[B, C](opB: Op[B])(f: (A, B) => C): Op[C] = product(opB).map(p => f(p._1, p._2))
}

object Op extends OpInstances {
  // Primitive reading operations.
  case object ReadNull                             extends Op[Unit]
  case object ReadBoolean                          extends Op[Boolean]
  case object ReadNumber                           extends Op[BiggerDecimal]
  case object ReadString                           extends Op[String]

  // Redundant reading operations designed to support optimizations.
  case object ReadLong                             extends Op[Long]

  // Reading operations for JSON objects and arrays.
  case class ReadFields[A](opA: Op[A])             extends Op[Vector[(String, A)]]
  case class ReadValues[A](opA: Op[A])             extends Op[Vector[A]]

  // Navigation operations.
  case class DownField(key: String)                extends Op[Unit]
  case class DownAt(index: Int)                    extends Op[Unit]

  // Brackets an operation so that any navigation it does is local.
  case class Bracket[A](opA: Op[A])                extends Op[A]

  // Operations supporting composition.
  case class Pure[A](value: A)                     extends Op[A]
  case class Fail[A](failure: Failure)             extends Op[A]
  case class Map[A, B](opA: Op[A], f: A => B)      extends Op[B]
  case class Bind[A, B](opA: Op[A], f: A => Op[B]) extends Op[B]
  case class Join[A, B](opA: Op[A], opB: Op[B])    extends Op[(A, B)]
}
