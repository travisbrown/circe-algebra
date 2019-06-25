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

class OpSpec extends AnyFunSuite with Discipline {
  // Doesn't work at the moment because of `tailRecM`.
  // checkAll("MonadError[Ops]", MonadErrorTests[Op, Failure].monadError[Int, Int, Int])

  checkAll("Monad[Op]", MonadTests[Op].stackUnsafeMonad[Int, Int, Int])
  checkAll("ApplicativeError[Ops]", ApplicativeErrorTests[Op, DecodingFailure].applicativeError[Int, Int, Int])
}
