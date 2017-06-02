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
  def readBoolean(j: Json): F[Boolean] = fromOption(j.asBoolean)(DecodingFailure("Expected boolean"))
  def readNumber(j: Json): F[BiggerDecimal] =
    fromOption(j.asNumber.map(_.toBiggerDecimal))(DecodingFailure("Expected number"))
  def readLong(j: Json): F[Long] = fromOption(j.asNumber.flatMap(_.toLong))(DecodingFailure("Expected number"))
  def readDouble(j: Json): F[Double] = fromOption(j.asNumber.map(_.toDouble))(DecodingFailure("Expected number"))
  def readString(j: Json): F[String] = fromOption(j.asString)(DecodingFailure("Expected null"))

  def downField(key: String)(j: Json): F[Json] =
    fromOption(j.asObject.flatMap(_(key)))(DecodingFailure(s"Expected object with key $key"))

  def downAt(index: Int)(j: Json): F[Json] = fromOption(j.asArray.flatMap(_.lift(index)))(
    DecodingFailure(s"Expected array with at least ${ index + 1} elements")
  )

  def readFields[A](opA: Op[A])(j: Json): F[Iterable[(String, A)]] =
    fromOption(j.asObject)(DecodingFailure("Expected object")).flatMap(
      _.toVector.traverse {
        case (k, v) => self.apply(opA).runA(v).map(k -> _)
      }.map(_.toIterable)
    )

  def readValues[A](opA: Op[A])(j: Json): F[Iterable[A]] =
    fromOption(j.asArray)(DecodingFailure("Expected array")).flatMap(_.traverse(self.apply(opA).runA).map(_.toIterable))
}
