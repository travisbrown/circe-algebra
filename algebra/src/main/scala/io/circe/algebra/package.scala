package io.circe

import cats.data.NonEmptyList
import cats.instances.either._
import io.circe.numbers.BiggerDecimal

package object algebra {
  def readNull: Op[Unit] = Op.ReadNull
  def readBoolean: Op[Boolean] = Op.ReadBoolean
  def readNumber: Op[BiggerDecimal] = Op.ReadNumber
  def readString: Op[String] = Op.ReadString
  def readLong: Op[Long] = Op.ReadLong
  def readDouble: Op[Double] = Op.ReadDouble

  def downField(key: String): Op[Unit] = Op.DownField(key)
  def downAt(index: Int): Op[Unit] = Op.DownAt(index)

  def readFields[A](opA: Op[A]): Op[Vector[(String, A)]] = Op.ReadFields(opA)
  def readValues[A](opA: Op[A]): Op[Vector[A]] = Op.ReadValues(opA)

  def read[A](implicit decodeA: Decoder[A]): Op[A] = decodeA.op
  def get[A](key: String)(implicit decodeA: Decoder[A]): Op[A] = downField(key).andThen(decodeA.op).bracket

  object interpreters {
    val either: CirceInterpreter[Either[DecodingFailure, ?]] = new CirceInterpreter[Either[DecodingFailure, ?]]
    val failFast: Interpreter[Either[DecodingFailure, ?], Json] = fast.FailFastInterpreter
    val accumulating: Interpreter[Either[NonEmptyList[DecodingFailure], ?], Json] = fast.ErrorAccumulatingInterpreter
    val failFastWithHistory: Interpreter[Either[DecodingFailure, ?], Json] = history.FailFastInterpreter
    val accumulatingWithHistory: Interpreter[Either[NonEmptyList[DecodingFailure], ?], Json] =
      history.ErrorAccumulatingInterpreter
  }
}
