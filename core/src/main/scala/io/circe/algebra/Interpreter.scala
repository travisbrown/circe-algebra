package io.circe.algebra

import io.circe.numbers.BiggerDecimal

abstract class Interpreter[F[_], J] { self =>
  type S[_]

  def apply[A](op: Op[A]): S[A]

  def runS[A](s: S[A])(j: J): F[A]
  def decode[A](j: J)(implicit decodeA: Decoder[A]): F[A] = runS(self(decodeA.op))(j)

  def readNull(j: J): F[Unit]
  def readBoolean(j: J): F[Boolean]
  def readNumber(j: J): F[BiggerDecimal]
  def readLong(j: J): F[Long]
  def readString(j: J): F[String]
  def downField(key: String)(j: J): F[J]
  def downAt(index: Int)(j: J): F[J]
  def readFields[A](opA: Op[A])(j: J): F[List[(String, A)]]
  def readValues[A](opA: Op[A])(j: J): F[Vector[A]]
}
