package io.circe.algebra

import cats.instances.either._
import io.circe.{ DecodingFailure, Json }

package object simple {
  val interpreter: Interpreter[Either[DecodingFailure, ?], Json] = new CirceInterpreter[Either[DecodingFailure, ?]]
}
