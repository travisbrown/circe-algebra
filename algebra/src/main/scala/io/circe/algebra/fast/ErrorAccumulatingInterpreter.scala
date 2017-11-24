package io.circe.algebra.fast

import cats.data.NonEmptyList
import io.circe.{ DecodingFailure, Json }
import io.circe.algebra.{ DirectInterpreter, Op }
import scala.collection.mutable.Builder

final object ErrorAccumulatingInterpreter
    extends DirectInterpreter[Either[NonEmptyList[DecodingFailure], ?], Json] { self =>
  def apply[A](op: Op[A])(j: Json): Either[NonEmptyList[DecodingFailure], A] = {
    val state = new ErrorAccumulatingStatefulFolder[A](j)
    op.fold(state)
    state.result
  }

  private[this] class ErrorAccumulatingStatefulFolder[Z](c: Json)
      extends StatefulFolder[NonEmptyList[DecodingFailure], Z](c) {
    final def failure: NonEmptyList[DecodingFailure] =
      NonEmptyList.fromListUnsafe(value.asInstanceOf[Builder[DecodingFailure, List[DecodingFailure]]].result)

    final def onFail(failure: DecodingFailure): Unit = {
      if (!failed) {
        value = List.newBuilder[DecodingFailure]
      }
      value.asInstanceOf[Builder[DecodingFailure, List[DecodingFailure]]] += failure
      failed = true
    }
  }
}
