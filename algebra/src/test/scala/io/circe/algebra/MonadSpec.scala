package io.circe.algebra

import cats.instances.either._
import cats.instances.int._
import cats.instances.tuple._
import cats.instances.unit._
import cats.laws.discipline.MonadErrorTests
import io.circe.DecodingFailure
import io.circe.algebra.instances._
import io.circe.testing.instances._
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class DecoderSpec extends FunSuite with Discipline {
  // Doesn't work at the moment because of `tailRecM`.
  checkAll("MonadError[Decoder]", MonadErrorTests[Decoder, DecodingFailure].monadError[Int, Int, Int])
  //checkAll("Monad[Decoder]", MonadTests[Decoder].stackUnsafeMonad[Int, Int, Int])
}
