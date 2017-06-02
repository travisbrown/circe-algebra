package io.circe.algebra

trait ParsingInterpreter[F[_], J] { self: Interpreter[F, J] with MonadErrorHelpers[F, Failure] =>
  def parse(doc: String): F[J]

  def parseAndDecode[A](doc: String)(implicit decodeA: Decoder[A]): F[A] = F.flatMap(parse(doc))(decode[A])
}
