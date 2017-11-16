package io.circe.algebra

import cats.{ ~> }
import cats.data.StateT
import cats.implicits._
import io.circe.{ DecodingFailure, Error, Json }

package object free {
  def decode[A](j: Json)(implicit decodeA: Decoder[A]): Either[Error, A] =
    decodeA.op.foldMap[EitherState](either).runA(j)

  type EitherState[A] = StateT[Either[Error, ?], Json, A]

  val either: Op ~> EitherState = new (Op ~> EitherState) {
    def inspect[A](message: String)(f: Json => Option[A]): EitherState[A] =
      StateT.inspectF[Either[Error, ?], Json, A](f(_).fold[Either[Error, A]](
        Left(DecodingFailure(message, Nil))
      )(Right(_)))

    def apply[A](fa: Op[A]): EitherState[A] = fa match {
      case ReadNull => inspect("Expected null")(j => if (j.isNull) Some(()) else None)
      case ReadBoolean => inspect("Expected boolean")(_.asBoolean)
      case ReadNumber => inspect("Expected number")(_.asNumber.flatMap(_.toBigDecimal))
      case ReadString => inspect("Expected string")(_.asString)

      case ReadFields(decodeA) => StateT.inspectF(
        (_: Json).asObject match {
          case Some(o) => o.toVector.traverse {
            case (k, v) => decodeA.op.foldMap[EitherState](either).runA(v).right.map(k -> _)
          }
          case None => Left(DecodingFailure("Expected object", Nil))
        }
      )

      case ReadValues(decodeA) => StateT.inspectF(
        (_: Json).asArray match {
          case Some(a) => a.traverse(decodeA.op.foldMap[EitherState](either).runA(_))
          case None => Left(DecodingFailure("Expected array", Nil))
        }
      )

      case DownField(key) => StateT.modifyF(
        (_: Json).asObject.flatMap(_(key)) match {
          case Some(v) => Right(v)
          case None => Left(DecodingFailure("Expected object", Nil))
        }
      )
      case DownAt(index) => StateT.modifyF(
        (_: Json).asArray.flatMap(_.lift(index)) match {
          case Some(v) => Right(v)
          case None => Left(DecodingFailure("Expected array", Nil))
        }
      )
      case Bracket(op) => StateT.inspectF(op.foldMap[EitherState](either).runA(_: Json))
      case Fail(failure) => StateT.lift[Either[Error, ?], Json, A](Left(failure))
    }
  }
}
