package io.circe.algebra

case class Decoder[A](op: Op[A]) extends AnyVal {
  def map[B](f: A => B): Decoder[B] = Decoder(op.map(f))
  def flatMap[B](f: A => Decoder[B]): Decoder[B] = Decoder(op.flatMap(a => f(a).op))
}

object Decoder {
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

  implicit val decodeDouble: Decoder[Double] = Decoder(Op.ReadNumber.map(_.toDouble))

  implicit def decodeVector[A](implicit decodeA: Decoder[A]): Decoder[Vector[A]] = Decoder(Op.ReadValues(decodeA.op))

  implicit def decodeMap[A](implicit decodeA: Decoder[A]): Decoder[Map[String, A]] =
    Decoder(Op.ReadFields(decodeA.op).map(_.toMap))
}
