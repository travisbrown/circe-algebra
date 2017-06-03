package io.circe.algebra

import cats.MonadError
import io.circe.DecodingFailure

class Decoder[A](val op: Op[A]) extends AnyVal {
  def map[B](f: A => B): Decoder[B] = Decoder.instance(Op.Mapper(op, f, true))
  def flatMap[B](f: A => Decoder[B]): Decoder[B] = Decoder.instance(Op.Bind[A, B](op, a => f(a).op, true))
}

object Decoder {
  def apply[A](implicit decodeA: Decoder[A]): Decoder[A] = decodeA
  def instance[A](op: Op[A]): Decoder[A] = new Decoder(op.bracket)

  implicit val decodeUnit: Decoder[Unit] = Decoder.instance(Op.ReadNull)
  implicit val decodeString: Decoder[String] = Decoder.instance(Op.ReadString)
  implicit val decodeBoolean: Decoder[Boolean] = Decoder.instance(Op.ReadBoolean)

  implicit val decodeLong: Decoder[Long] = Decoder.instance(Op.ReadLong)
  implicit val decodeInt: Decoder[Int] = Decoder.instance(
    Op.ReadLong.flatMap {
      case x if x >= Int.MinValue.toLong && x <= Int.MaxValue.toLong => Op.Pure(x.toInt)
      case _ => Op.Fail(DecodingFailure("Expected int", Nil))
    }
  )

  implicit val decodeDouble: Decoder[Double] = Decoder.instance(Op.ReadDouble)

  implicit def decodeVector[A](implicit decodeA: Decoder[A]): Decoder[Vector[A]] =
    Decoder.instance(Op.ReadValues(decodeA.op))
  implicit def decodeList[A](implicit decodeA: Decoder[A]): Decoder[List[A]] = decodeVector(decodeA).map(_.toList)

  implicit def decodeMap[A](implicit decodeA: Decoder[A]): Decoder[Map[String, A]] =
    Decoder.instance(Op.ReadMap(decodeA.op))

  implicit val decoderMonad: MonadError[Decoder, DecodingFailure] = new MonadError[Decoder, DecodingFailure] {
    def pure[A](a: A): Decoder[A] = Decoder.instance(Op.opMonadError.pure(a))
    def flatMap[A, B](a: Decoder[A])(f: A => Decoder[B]): Decoder[B] = a.flatMap(f)

    override def map[A, B](a: Decoder[A])(f: A => B): Decoder[B] = a.map(f)
    override def product[A, B](a: Decoder[A], b: Decoder[B]): Decoder[(A, B)] = Decoder.instance(a.op.product(b.op))

    def raiseError[A](e: DecodingFailure): Decoder[A] = Decoder.instance(Op.opMonadError.raiseError(e))

    def handleErrorWith[A](fa: Decoder[A])(f: DecodingFailure => Decoder[A]): Decoder[A] = fa.op match {
      case Op.Fail(failure) => f(failure)
      case other => fa
    }

    def tailRecM[A, B](a: A)(f: A => Decoder[Either[A, B]]): Decoder[B] =
      Decoder.instance(Op.opMonadError.tailRecM(a)(a => f(a).op))
  }
}
