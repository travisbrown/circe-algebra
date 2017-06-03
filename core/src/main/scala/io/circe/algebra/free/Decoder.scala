package io.circe.algebra.free

case class Decoder[A](op: Op.OpF[A]) {
  def map[B](f: A => B): Decoder[B] = Decoder(op.map(f))
  def flatMap[B](f: A => Decoder[B]): Decoder[B] = Decoder(op.flatMap(a => f(a).op))
}

object Decoder {
  def apply[A](implicit decodeA: Decoder[A]): Decoder[A] = decodeA

  implicit val decodeUnit: Decoder[Unit] = Decoder(Op.readNull)
  implicit val decodeString: Decoder[String] = Decoder(Op.readString)
  implicit val decodeBoolean: Decoder[Boolean] = Decoder(Op.readBoolean)
  implicit val decodeLong: Decoder[Long] = Decoder(Op.readNumber.map(_.toLong))
  implicit val decodeDouble: Decoder[Double] = Decoder(Op.readNumber.map(_.toDouble))

  implicit def decodeVector[A](implicit decodeA: Decoder[A]): Decoder[Vector[A]] = Decoder(Op.readValues(decodeA))
  implicit def decodeList[A](implicit decodeA: Decoder[A]): Decoder[List[A]] = decodeVector(decodeA).map(_.toList)

  implicit def decodeMap[A](implicit decodeA: Decoder[A]): Decoder[Map[String, A]] =
    Decoder(Op.readFields(decodeA).map(_.toMap))
}
