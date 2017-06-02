package io.circe.algebra

import cats.MonadError

abstract class OpInstances {
  import Op._

  implicit val opMonad: MonadError[Op, Failure] = new MonadError[Op, Failure] {
    def pure[A](value: A): Op[A] = Pure(value)
    def flatMap[A, B](opA: Op[A])(f: A => Op[B]): Op[B] = opA.flatMap(f)

    override def map[A, B](opA: Op[A])(f: A => B): Op[B] = opA.map(f)
    override def product[A, B](opA: Op[A], opB: Op[B]): Op[(A, B)] = opA.product(opB)

    def raiseError[A](e: Failure): Op[A] = Fail(e)

    def handleErrorWith[A](fa: Op[A])(f: Failure => Op[A]): Op[A] = fa match {
      case Fail(failure) => f(failure)
      case other => other
    }

    // TODO: This isn't actually stack-safe.
    def tailRecM[A, B](a: A)(f: A => Op[Either[A, B]]): Op[B] = tailRecMHelper(f(a))(f)

    private[this] def mapHelper[A, B, C](m: Map[C, Either[A, B]])(f: A => Op[Either[A, B]]): Bind[C, B] = Bind(
      m.opA,
      m.f(_) match {
        case Left(a) => tailRecM(a)(f)
        case Right(b) => Pure(b)
      }
    )

    private[this] def bindHelper[A, B, C](m: Bind[C, Either[A, B]])(f: A => Op[Either[A, B]]): Bind[C, B] = Bind(
      m.opA,
      m.f(_).flatMap {
        case Left(a) => tailRecM(a)(f)
        case Right(b) => Pure(b)
      }
    )

    @annotation.tailrec
    private[this] def tailRecMHelper[A, B](opA: Op[Either[A, B]])(f: A => Op[Either[A, B]]): Op[B] = opA match {
      case Pure(Left(a))  => tailRecM(a)(f)
      case Pure(Right(b)) => Pure(b)
      case Fail(failure)  => Fail(failure)
      case m @ Map(_, _)  => mapHelper(m)(f)
      case b @ Bind(_, _) => bindHelper(b)(f)
      case Bracket(opA)   => tailRecMHelper(opA)(f)
    }
  }
}
