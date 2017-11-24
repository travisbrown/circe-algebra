package io.circe.algebra

import cats.instances.either._
import cats.kernel.Eq
import io.circe.{ DecodingFailure, Error, Json }
import io.circe.testing.instances._
import org.scalacheck.{ Arbitrary, Cogen, Gen }

package object instances {
  //implicit val cogenFailure: Cogen[DecodingFailure] = Cogen((_: DecodingFailure).hashCode.toLong)

  implicit val eqFailure: Eq[DecodingFailure] = Eq.fromUniversalEquals

  implicit def arbitraryOp[A: Arbitrary]: Arbitrary[Op[A]] = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[A].map(Op.Pure(_)),
      Arbitrary.arbitrary[DecodingFailure].map(Op.Fail[A](_))
    )
  )

  private[this] def arbitraryValues[A](implicit A: Arbitrary[A]): Stream[A] = Stream.continually(
    A.arbitrary.sample
  ).flatten

  implicit def eqOp[A: Eq]: Eq[Op[A]] = Eq.instance[Op[A]] {
    case (Op.Pure(a), Op.Pure(b)) => Eq[A].eqv(a, b)
    case (a, b) =>
      val js = arbitraryValues[Json].take(8)

      js.forall { j =>
        Eq[Either[Error, A]].eqv(
          interpreters.either.decode(j)(Decoder.instance(a)),
          interpreters.either.decode(j)(Decoder.instance(b))
        )
      }
  }

  implicit def arbitraryAlgebraDecoder[A: Arbitrary]: Arbitrary[Decoder[A]] = Arbitrary(
    Arbitrary.arbitrary[Op[A]].map(Decoder.instance(_))
  )

  implicit def eqAlgebraDecoder[A: Eq]: Eq[Decoder[A]] = Eq.by(_.op)
}
