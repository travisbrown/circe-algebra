package io.circe.algebra.fast

import cats.data.NonEmptyList
import io.circe.{ DecodingFailure, Json }
import io.circe.algebra.{ DirectInterpreter, Op }
import scala.collection.mutable.Builder

abstract class ErrorAccumulatingInterpreter extends DirectInterpreter[Either[NonEmptyList[DecodingFailure], ?], Json] {
  self =>
  protected[this] def folder[Z](j: Json): StatefulFolder[NonEmptyList[DecodingFailure], Z]

  def apply[A](op: Op[A])(j: Json): Either[NonEmptyList[DecodingFailure], A] = {
    val state = folder(j)
    op.fold(state)
    state.result
  }

  protected[this] trait ErrorAccumulatingStatefulFolder[Z] { self: StatefulFolder[NonEmptyList[DecodingFailure], Z] =>
    final def failure: NonEmptyList[DecodingFailure] =
      NonEmptyList.fromListUnsafe(value.asInstanceOf[Builder[DecodingFailure, List[DecodingFailure]]].result)

    final def onHandle[A](opA: Op[A], f: DecodingFailure => Op[A], isBracketed: Boolean): Unit = {
      val orig = cursor

      opA.fold(this)
      if (failed) f(failure.head).fold(this)
      if (isBracketed) cursor = orig
    }

    final def onFail(failure: DecodingFailure): Unit = {
      if (!failed) {
        value = List.newBuilder[DecodingFailure]
      }
      value.asInstanceOf[Builder[DecodingFailure, List[DecodingFailure]]] += failure
      failed = true
    }
  }
}

object ErrorAccumulatingInterpreter {
  object noHistory extends ErrorAccumulatingInterpreter {
    protected[this] def folder[Z](j: Json): StatefulFolder[NonEmptyList[DecodingFailure], Z] =
      new StatefulFolder.NoHistory[NonEmptyList[DecodingFailure], Z](j) with ErrorAccumulatingStatefulFolder[Z] {}
  }

  object withHistory extends ErrorAccumulatingInterpreter {
    protected[this] def folder[Z](j: Json): StatefulFolder[NonEmptyList[DecodingFailure], Z] =
      new StatefulFolder.WithHistory[NonEmptyList[DecodingFailure], Z](j) with ErrorAccumulatingStatefulFolder[Z] {}
  }
}
