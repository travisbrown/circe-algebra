package io.circe.algebra

import cats.MonadError
import cats.instances.vector._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.circe.{ DecodingFailure, Error, Json }
import io.circe.numbers.BiggerDecimal

class CirceInterpreter[F[_]](implicit M: MonadError[F, Error]) extends StateInterpreter[F, Json]
    with ParsingInterpreter[F, Json] with MonadErrorHelpers[F, Error] { self =>
  val F: MonadError[F, Error] = M

  def readNull(j: Json): F[Unit] = if (j.isNull) F.pure(()) else F.raiseError(DecodingFailure("Expected null", Nil))

  def readBoolean(j: Json): F[Boolean] =
    if (j.isBoolean) F.pure(j.asInstanceOf[Json.JBoolean].value) else {
      F.raiseError(DecodingFailure("Expected boolean", Nil))
    }

  def readNumber(j: Json): F[BiggerDecimal] =
    if (j.isNumber) F.pure(j.asInstanceOf[Json.JNumber].value.toBiggerDecimal) else {
      F.raiseError(DecodingFailure("Expected number", Nil))
    }

  def readLong(j: Json): F[Long] =
    if (j.isNumber) {
      fromOption(j.asInstanceOf[Json.JNumber].value.toLong)(DecodingFailure("Expected number", Nil))
    } else {
      F.raiseError(DecodingFailure("Expected number", Nil))
    }

  def readDouble(j: Json): F[Double] =
    if (j.isNumber) F.pure(j.asInstanceOf[Json.JNumber].value.toDouble) else {
      F.raiseError(DecodingFailure("Expected number", Nil))
    }

  def readString(j: Json): F[String] =
    if (j.isString) F.pure(j.asInstanceOf[Json.JString].value) else F.raiseError(DecodingFailure("Expected null", Nil))

  def downField(key: String)(j: Json): F[Json] =
    if (j.isObject) {
      fromOption(j.asInstanceOf[Json.JObject].value(key))(DecodingFailure(s"Expected object with key $key", Nil))
    } else F.raiseError(DecodingFailure(s"Expected object with key $key", Nil))

  def downAt(index: Int)(j: Json): F[Json] = fromOption(j.asArray.flatMap(_.lift(index)))(
    DecodingFailure(s"Expected array with at least ${ index + 1} elements", Nil)
  )

  def readFields[A](opA: Op[A])(j: Json): F[Vector[(String, A)]] = {
    val s: Json => F[A] = self.apply(opA)

    fromOption(j.asObject)(DecodingFailure("Expected object", Nil)).flatMap(
      _.toVector.traverse {
        case (k, v) => s(v).map(k -> _)
      }
    )
  }

  def readValues[A](opA: Op[A])(j: Json): F[Vector[A]] = {
    val s: Json => F[A] = self.apply(opA)

    fromOption(j.asArray)(DecodingFailure("Expected array", Nil)).flatMap(_.traverse(s))
  }

  def readMap[A](opA: Op[A])(j: Json): F[Map[String, A]] = {
    val s: Json => F[A] = self.apply(opA)

    fromOption(j.asObject)(DecodingFailure("Expected object", Nil)).flatMap(
      _.toVector.traverse {
        case (k, v) => s(v).map(k -> _)
      }.map(_.toMap)
    )
  }
}
