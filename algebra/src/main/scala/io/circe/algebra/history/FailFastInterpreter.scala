package io.circe.algebra.history

import io.circe.{ DecodingFailure, Json }
import io.circe.algebra.{ DirectInterpreter, Op }

final object FailFastInterpreter extends DirectInterpreter[Either[DecodingFailure, ?], Json] { self =>
  def apply[A](op: Op[A])(j: Json): Either[DecodingFailure, A] = {
    val state = new FailFastStatefulFolder[A](j)
    op.fold(state)
    state.result
  }

  private[this] class FailFastStatefulFolder[Z](c: Json) extends StatefulFolder[DecodingFailure, Z](c) {
    final def failure: DecodingFailure = value.asInstanceOf[DecodingFailure]

    final def onHandle[A](opA: Op[A], f: DecodingFailure => Op[A], isBracketed: Boolean): Unit = {
      val orig = cursor

      opA.fold(this)
      if (failed) f(failure).fold(this)
      if (isBracketed) cursor = orig
    }

    final def onFail(failure: DecodingFailure): Unit = {
      value = failure
      failed = true
      halted = true
    }
  }
}
