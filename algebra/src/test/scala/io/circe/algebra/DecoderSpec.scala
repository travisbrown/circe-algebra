package io.circe.algebra

import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.laws.discipline.{ ApplicativeErrorTests, MonadTests }
import io.circe.DecodingFailure
import io.circe.algebra.instances._
import io.circe.testing.instances._
import org.scalatest.funsuite.AnyFunSuite
import org.typelevel.discipline.scalatest.Discipline

class DecoderSpec extends AnyFunSuite with Discipline {
  // Doesn't work at the moment because of `tailRecM`.
  // checkAll("MonadError[Decoder]", MonadErrorTests[Decoder, Failure].monadError[Int, Int, Int])

  checkAll("Monad[Decoder]", MonadTests[Decoder].stackUnsafeMonad[Int, Int, Int])
  checkAll("ApplicativeError[Ops]", ApplicativeErrorTests[Decoder, DecodingFailure].applicativeError[Int, Int, Int])
}
