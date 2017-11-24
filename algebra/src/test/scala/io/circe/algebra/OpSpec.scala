package io.circe.algebra

import cats.instances.int._
import cats.instances.tuple._
import cats.laws.discipline.MonadTests
import io.circe.algebra.instances._
import org.scalatest.FunSuite
import org.typelevel.discipline.scalatest.Discipline

class OpSpec extends FunSuite with Discipline {
  // Doesn't work at the moment because of `tailRecM`.
  // checkAll("MonadError[Ops]", MonadErrorTests[Op, Failure].monadError[Int, Int, Int])
  checkAll("Monad[Op", MonadTests[Op].stackUnsafeMonad[Int, Int, Int])
}
