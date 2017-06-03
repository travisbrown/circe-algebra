package io.circe.algebra

import cats.MonadError
import cats.instances.vector._
import cats.syntax.flatMap._
import cats.syntax.functor._
import cats.syntax.traverse._
import io.circe.Json
import io.circe.numbers.BiggerDecimal

class CirceInterpreter[F[_]](implicit M: MonadError[F, Failure]) extends StateInterpreter[F, Json]
    with ParsingInterpreter[F, Json] with MonadErrorHelpers[F, Failure] { self =>
  val F: MonadError[F, Failure] = M

  def parse(doc: String): F[Json] = fromEither(io.circe.jawn.parse(doc).left.map(_ => OtherFailure("Parsing failure")))

  def readNull(j: Json): F[Unit] = if (j.isNull) F.pure(()) else F.raiseError(DecodingFailure("Expected null"))

  def readBoolean(j: Json): F[Boolean] =
    if (j.isBoolean) F.pure(j.asInstanceOf[Json.JBoolean].b) else F.raiseError(DecodingFailure("Expected boolean"))

  def readNumber(j: Json): F[BiggerDecimal] =
    if (j.isNumber) F.pure(j.asInstanceOf[Json.JNumber].n.toBiggerDecimal) else {
      F.raiseError(DecodingFailure("Expected number"))
    }

  def readLong(j: Json): F[Long] =
    if (j.isNumber) fromOption(j.asInstanceOf[Json.JNumber].n.toLong)(DecodingFailure("Expected number")) else {
      F.raiseError(DecodingFailure("Expected number"))
    }

  def readDouble(j: Json): F[Double] =
    if (j.isNumber) F.pure(j.asInstanceOf[Json.JNumber].n.toDouble) else {
      F.raiseError(DecodingFailure("Expected number"))
    }

  def readString(j: Json): F[String] =
    if (j.isString) F.pure(j.asInstanceOf[Json.JString].s) else F.raiseError(DecodingFailure("Expected null"))

  def downField(key: String)(j: Json): F[Json] =
    if (j.isObject) {
      fromOption(j.asInstanceOf[Json.JObject].o(key))(DecodingFailure(s"Expected object with key $key"))
    } else F.raiseError(DecodingFailure(s"Expected object with key $key"))

  def downAt(index: Int)(j: Json): F[Json] = fromOption(j.asArray.flatMap(_.lift(index)))(
    DecodingFailure(s"Expected array with at least ${ index + 1} elements")
  )

  def readFields[A](opA: Op[A])(j: Json): F[Vector[(String, A)]] = {
    val s: Json => F[A] = self.apply(opA)

    fromOption(j.asObject)(DecodingFailure("Expected object")).flatMap(
      _.toVector.traverse {
        case (k, v) => s(v).map(k -> _)
      }
    )
  }

  def readValues[A](opA: Op[A])(j: Json): F[Vector[A]] = {
    val s: Json => F[A] = self.apply(opA)

    fromOption(j.asArray)(DecodingFailure("Expected array")).flatMap(_.traverse(s))
  }
}
