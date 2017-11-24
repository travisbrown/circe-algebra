package io.circe.algebra

import io.circe.Error

trait ParsingInterpreter[F[_], J] { self: Interpreter[F, J] with MonadErrorHelpers[F, Error] =>
}
