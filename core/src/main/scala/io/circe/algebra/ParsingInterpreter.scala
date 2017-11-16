package io.circe.algebra

import io.circe.Error

trait ParsingInterpreter[F[_], J] { self: Interpreter[F, J] with MonadErrorHelpers[F, Error] =>
  def parse(doc: String): F[J]

  final def parseAndDecode[A](doc: String)(implicit decodeA: Decoder[A]): F[A] = F.flatMap(parse(doc))(decode[A])
}
