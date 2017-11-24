package io.circe.algebra

import cats.MonadError

trait MonadErrorHelpers[F[_], E] {
  def F: MonadError[F, E]

  def fromOption[A](option: Option[A])(failure: E): F[A] = option match {
    case Some(value) => F.pure(value)
    case None => F.raiseError(failure)
  }

  def fromEither[A](either: Either[E, A]): F[A] = either match {
    case Right(value) => F.pure(value)
    case Left(failure) => F.raiseError(failure)
  }
}
