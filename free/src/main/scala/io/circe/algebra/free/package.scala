package io.circe.algebra

import cats.{ ~>, Monad, Traverse }
import cats.data.{ IndexedStateT, StateT }
import io.circe.{ DecodingFailure, Error, Json }

package object free {
  type ErrorOr[A] = Either[Error, A]
  type ErrorOrState[A] = StateT[ErrorOr, Json, A]

  private[this] implicit val errorOrMonad: Monad[ErrorOr] =
    cats.instances.either.catsStdInstancesForEither[Error]

  private[this] implicit val errorOrStateMonad: Monad[ErrorOrState] =
    IndexedStateT.catsDataMonadForIndexedStateT[ErrorOr, Json](errorOrMonad)

  private[this] def traverseVector[A, B](as: Vector[A])(f: A => ErrorOr[B]): Either[Error, Vector[B]] =
    (cats.instances.vector.catsStdInstancesForVector: Traverse[Vector]).traverse[ErrorOr, A, B](as)(f)

  def decode[A](j: Json)(implicit decodeA: Decoder[A]): Either[Error, A] =
    decodeA.op.foldMap[ErrorOrState](eitherInterpreter).runA(j)

  val eitherInterpreter: Op ~> ErrorOrState = new (Op ~> ErrorOrState) {
    def inspect[A](message: String)(f: Json => Option[A]): ErrorOrState[A] =
      StateT.inspectF[ErrorOr, Json, A](
        f(_).fold[Either[Error, A]](
          Left(DecodingFailure(message, Nil))
        )(Right(_))
      )

    def apply[A](fa: Op[A]): ErrorOrState[A] = fa match {
      case ReadNull => inspect("Expected null")(j => if (j.isNull) Some(()) else None)
      case ReadBoolean => inspect("Expected boolean")(_.asBoolean)
      case ReadNumber => inspect("Expected number")(_.asNumber.flatMap(_.toBigDecimal))
      case ReadString => inspect("Expected string")(_.asString)

      case ReadFields(decodeA) => StateT.inspectF(
        (_: Json).asObject match {
          case Some(o) => traverseVector(o.toVector) {
            case (k, v) =>
              decodeA.op.foldMap[ErrorOrState](eitherInterpreter).runA(v)(errorOrMonad).right.map(k -> _)
          }
          case None => Left(DecodingFailure("Expected object", Nil))
        }
      )

      case ReadValues(decodeA) => StateT.inspectF(
        (_: Json).asArray match {
          case Some(a) => traverseVector(a)(decodeA.op.foldMap[ErrorOrState](eitherInterpreter).runA(_))
          case None => Left(DecodingFailure("Expected array", Nil))
        }
      )

      case DownField(key) => StateT.modifyF[ErrorOr, Json](
        _.asObject.flatMap(_(key)) match {
          case Some(v) => Right(v)
          case None => Left(DecodingFailure("Expected object", Nil))
        }
      )

      case DownAt(index) => StateT.modifyF[ErrorOr, Json](
        (_: Json).asArray.flatMap(_.lift(index)) match {
          case Some(v) => Right(v)
          case None => Left(DecodingFailure("Expected array", Nil))
        }
      )

      case Bracket(op) => StateT.inspectF(op.foldMap[ErrorOrState](eitherInterpreter).runA(_: Json))
      case Fail(failure) => StateT.lift[ErrorOr, Json, A](Left(failure))
    }
  }
}
