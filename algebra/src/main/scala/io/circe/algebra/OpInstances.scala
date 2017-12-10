package io.circe.algebra

import cats.MonadError
import io.circe.DecodingFailure

abstract class OpInstances {
  import Op._

  implicit val opMonadError: MonadError[Op, DecodingFailure] = new MonadError[Op, DecodingFailure] {
    def pure[A](value: A): Op[A] = Pure(value)
    def flatMap[A, B](opA: Op[A])(f: A => Op[B]): Op[B] = opA.flatMap(f)

    override def map[A, B](opA: Op[A])(f: A => B): Op[B] = opA.map(f)
    override def product[A, B](opA: Op[A], opB: Op[B]): Op[(A, B)] = opA.product(opB)

    def raiseError[A](e: DecodingFailure): Op[A] = Fail(e)

    def handleErrorWith[A](fa: Op[A])(f: DecodingFailure => Op[A]): Op[A] = Handle[A](fa, f, false)

    // TODO: This isn't actually stack-safe.
    def tailRecM[A, B](a: A)(f: A => Op[Either[A, B]]): Op[B] = tailRecMHelper(f(a))(f)

    private[this] def mapHelper[A, B, C](o: Mapper[C, Either[A, B]])(f: A => Op[Either[A, B]]): Bind[C, B] = Bind(
      o.opA,
      o.f(_) match {
        case Left(a) => tailRecM(a)(f)
        case Right(b) => Pure(b)
      },
      false
    )

    private[this] def bindHelper[A, B, C](o: Bind[C, Either[A, B]])(f: A => Op[Either[A, B]]): Bind[C, B] = Bind(
      o.opA,
      o.f(_).flatMap {
        case Left(a) => tailRecM(a)(f)
        case Right(b) => Pure(b)
      },
      false
    )

    private[this] def thenHelper[A, B, C](o: Then[C, Either[A, B]])(f: A => Op[Either[A, B]]): Bind[C, B] = Bind(
      o.opA,
      _ => o.opB.flatMap {
        case Left(a) => tailRecM(a)(f)
        case Right(b) => Pure(b)
      },
      false
    )

    private[this] def tailRecMHelper[A, B](opA: Op[Either[A, B]])(f: A => Op[Either[A, B]]): Op[B] = opA match {
      case Pure(Left(a))       => tailRecM(a)(f)
      case Pure(Right(b))      => Pure(b)
      case Fail(failure)       => Fail(failure)
      case m @ Mapper(_, _, _) => mapHelper(m)(f)
      case b @ Bind(_, _, _)   => bindHelper(b)(f)
      case Handle(o, h, b) => Handle(tailRecMHelper(o)(f), df => tailRecMHelper(h(df))(f), b)
      case t @ Then(_, _, _)   => thenHelper(t)(f)
    }
  }
}
