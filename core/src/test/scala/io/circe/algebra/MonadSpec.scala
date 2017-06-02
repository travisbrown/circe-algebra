package io.circe.algebra

import cats.instances.int._
import cats.instances.tuple._
import cats.laws.discipline.MonadTests
import io.circe.algebra.instances._
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class DecoderSpec extends FunSuite with Discipline {
  // Doesn't work at the moment because of `tailRecM`.
  // checkAll("MonadError[Decoder]", MonadErrorTests[Decoder, Failure].monadError[Int, Int, Int])
  checkAll("Monad[Decoder]", MonadTests[Decoder].stackUnsafeMonad[Int, Int, Int])
}
