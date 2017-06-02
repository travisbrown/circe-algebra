package io.circe.algebra

import cats.MonadError

case class Decoder[A](op: Op[A]) extends AnyVal {
  def map[B](f: A => B): Decoder[B] = Decoder(Op.Map(op, f, true))
  def flatMap[B](f: A => Decoder[B]): Decoder[B] = Decoder(Op.Bind[A, B](op, a => f(a).op, true))
}

object Decoder {
  def apply[A](op: Op[A]): Decoder[A] = new Decoder(op.bracket)

  implicit val decodeUnit: Decoder[Unit] = Decoder(Op.ReadNull)
  implicit val decodeString: Decoder[String] = Decoder(Op.ReadString)
  implicit val decodeBoolean: Decoder[Boolean] = Decoder(Op.ReadBoolean)

  implicit val decodeLong: Decoder[Long] = Decoder(Op.ReadLong)
  /**
   * Alternative implementation using the more generic Op.ReadNumber
   *
    Decoder(
      Op.ReadNumber.flatMap {
        _.toLong match {
          case Some(value) => Op.Pure(value)
          case None => Op.Fail(DecodingFailure("Expected JSON number representing a long integer"))
        }
      }
    )
  */

  implicit val decodeDouble: Decoder[Double] = Decoder(Op.ReadNumber).map(_.toDouble)

  implicit def decodeVector[A](implicit decodeA: Decoder[A]): Decoder[Vector[A]] = Decoder(Op.ReadValues(decodeA.op))

  implicit def decodeMap[A](implicit decodeA: Decoder[A]): Decoder[Map[String, A]] =
    Decoder(Op.ReadFields(decodeA.op)).map(_.toMap)

  implicit val decoderMonad: MonadError[Decoder, Failure] = new MonadError[Decoder, Failure] {
    def pure[A](a: A): Decoder[A] = new Decoder(Op.opMonadError.pure(a))
    def flatMap[A, B](a: Decoder[A])(f: A => Decoder[B]): Decoder[B] = a.flatMap(f)

    override def map[A, B](a: Decoder[A])(f: A => B): Decoder[B] = a.map(f)
    override def product[A, B](a: Decoder[A], b: Decoder[B]): Decoder[(A, B)] = Decoder(a.op.product(b.op))

    def raiseError[A](e: Failure): Decoder[A] = new Decoder(Op.opMonadError.raiseError(e))

    def handleErrorWith[A](fa: Decoder[A])(f: Failure => Decoder[A]): Decoder[A] = fa.op match {
      case Op.Fail(failure) => f(failure)
      case other => fa
    }

    def tailRecM[A, B](a: A)(f: A => Decoder[Either[A, B]]): Decoder[B] =
      Decoder(Op.opMonadError.tailRecM(a)(a => f(a).op))
  }
}
