package io.circe.algebra.fast

import io.circe.{ DecodingFailure, Json }
import io.circe.algebra.{ DirectInterpreter, Op }

abstract class FailFastInterpreter extends DirectInterpreter[Either[DecodingFailure, ?], Json] { self =>
  protected[this] def folder[Z](j: Json): StatefulFolder[DecodingFailure, Z]

  def apply[A](op: Op[A])(j: Json): Either[DecodingFailure, A] = {
    val state = folder(j)
    op.fold(state)
    state.result
  }

  protected[this] trait FailFastStatefulFolder[Z] { self: StatefulFolder[DecodingFailure, Z] =>
    final def failure: DecodingFailure = value.asInstanceOf[DecodingFailure]

    final def onHandle[A](opA: Op[A], f: DecodingFailure => Op[A], isBracketed: Boolean): Unit = {
      val orig = cursor

      opA.fold(this)
      if (failed) {
        failed = false
        f(failure).fold(this)
      }
      if (isBracketed) cursor = orig
    }

    final def onFail(failure: DecodingFailure): Unit = {
      value = failure
      failed = true
      halted = true
    }
  }
}

object FailFastInterpreter {
  object noHistory extends FailFastInterpreter {
    protected[this] def folder[Z](j: Json): StatefulFolder[DecodingFailure, Z] =
      new StatefulFolder.NoHistory[DecodingFailure, Z](j) with FailFastStatefulFolder[Z] {}
  }

  object withHistory extends FailFastInterpreter {
    protected[this] def folder[Z](j: Json): StatefulFolder[DecodingFailure, Z] =
      new StatefulFolder.WithHistory[DecodingFailure, Z](j) with FailFastStatefulFolder[Z] {}
  }
}
