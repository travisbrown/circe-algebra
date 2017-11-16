package io.circe.algebra.free

import cats.free.Free
import io.circe.DecodingFailure

sealed abstract class Op[A]

final case object ReadNull extends Op[Unit]
final case object ReadBoolean extends Op[Boolean]
final case object ReadNumber extends Op[BigDecimal]
final case object ReadString extends Op[String]
final case class ReadFields[A](decodeA: Decoder[A]) extends Op[Vector[(String, A)]]
final case class ReadValues[A](decodeA: Decoder[A]) extends Op[Vector[A]]
final case class DownField(key: String) extends Op[Unit]
final case class DownAt(index: Int) extends Op[Unit]
final case class Bracket[A](op: Op.OpF[A]) extends Op[A]
final case class Fail[A](failure: DecodingFailure) extends Op[A]

object Op {
  type OpF[A] = Free[Op, A]

  val readNull: OpF[Unit] = Free.liftF(ReadNull)
  val readBoolean: OpF[Boolean] = Free.liftF(ReadBoolean)
  val readNumber: OpF[BigDecimal] = Free.liftF(ReadNumber)
  val readString: OpF[String] = Free.liftF(ReadString)
  def readFields[A](decodeA: Decoder[A]): OpF[Vector[(String, A)]] =
    Free.liftF[Op, Vector[(String, A)]](ReadFields(decodeA))
  def readValues[A](decodeA: Decoder[A]): OpF[Vector[A]] = Free.liftF[Op, Vector[A]](ReadValues(decodeA))
  def downField(key: String): OpF[Unit] = Free.liftF(DownField(key))
  def downAt(index: Int): OpF[Unit] = Free.liftF(DownAt(index))
  def bracket[A](op: OpF[A]): OpF[A] = Free.liftF[Op, A](Bracket(op))
  def fail[A](failure: DecodingFailure): OpF[A] = Free.liftF[Op, A](Fail(failure))

  def get[A](key: String)(implicit decodeA: Decoder[A]): OpF[A] = downField(key).flatMap(_ => decodeA.op)
}
