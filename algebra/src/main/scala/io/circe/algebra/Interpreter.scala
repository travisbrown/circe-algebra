package io.circe.algebra

abstract class Interpreter[F[_], J] { self =>
  type S[_]

  def compile[A](op: Op[A]): S[A]
  def apply[A](op: Op[A])(j: J): F[A]

  final def decode[A](j: J)(implicit decodeA: Decoder[A]): F[A] = self(decodeA.op)(j)
}

abstract class DirectInterpreter[F[_], J] extends Interpreter[F, J] {
  type S[x] = J => F[x]

  final def compile[A](op: Op[A]): J => F[A] = apply(op)
}
