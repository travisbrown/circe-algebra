package io.circe

import cats.Id
import cats.instances.either._
import io.circe.numbers.BiggerDecimal

package object algebra {
  def readNull: Op[Unit] = Op.ReadNull
  def readBoolean: Op[Boolean] = Op.ReadBoolean
  def readNumber: Op[BiggerDecimal] = Op.ReadNumber
  def readString: Op[String] = Op.ReadString
  def downField(key: String): Op[Unit] = Op.DownField(key)
  def downAt(index: Int): Op[Unit] = Op.DownAt(index)

  def readFields[A](opA: Op[A]): Op[Vector[(String, A)]] = Op.ReadFields(opA)
  def readValues[A](opA: Op[A]): Op[Vector[A]] = Op.ReadValues(opA)

  def read[A](implicit decodeA: Decoder[A]): Op[A] = decodeA.op
  def get[A](key: String)(implicit decodeA: Decoder[A]): Op[A] = downField(key).flatMap(_ => decodeA.op).bracket

  val jsonEither: CirceInterpreter[Either[Failure, ?]] = new CirceInterpreter[Either[Failure, ?]]
  val jsonYolo: Interpreter[Id, Json] = CirceYoloInterpreter
}
