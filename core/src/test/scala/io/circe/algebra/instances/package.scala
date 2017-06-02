package io.circe.algebra

import cats.instances.either._
import cats.kernel.Eq
import io.circe.Json
import io.circe.testing.instances._
import org.scalacheck.{ Arbitrary, Cogen, Gen }

package object instances {
  implicit val arbitraryFailure: Arbitrary[Failure] = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[String].map(DecodingFailure(_)),
      Arbitrary.arbitrary[String].map(OtherFailure(_))
    )
  )

  implicit val cogenFailure: Cogen[Failure] = Cogen((_: Failure).hashCode.toLong)

  implicit val eqFailure: Eq[Failure] = Eq.fromUniversalEquals

  implicit def arbitraryOp[A: Arbitrary]: Arbitrary[Op[A]] = Arbitrary(
    Gen.oneOf(
      Arbitrary.arbitrary[A].map(Op.Pure(_)),
      Arbitrary.arbitrary[Failure].map(Op.Fail[A](_))
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
        Eq[Either[Failure, A]].eqv(
          jsonEither.decode(j)(Decoder(a)),
          jsonEither.decode(j)(Decoder(b))
        )
      }
  }

  implicit def arbitraryDecoder[A: Arbitrary]: Arbitrary[Decoder[A]] = Arbitrary(
    Arbitrary.arbitrary[Op[A]].map(Decoder(_))
  )

  implicit def eqDecoder[A: Eq]: Eq[Decoder[A]] = Eq.by(_.op)
}
