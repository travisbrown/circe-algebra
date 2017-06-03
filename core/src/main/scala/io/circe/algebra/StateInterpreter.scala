package io.circe.algebra

import cats.MonadError
import cats.data.StateT
import cats.syntax.semigroupal._
import io.circe.Error
import io.circe.numbers.BiggerDecimal

abstract class StateInterpreter[F[_], J](implicit M: MonadError[F, Error]) extends Interpreter[F, J] { self =>
  import Op._

  type S[x] = StateT[F, J, x]

  final def apply[A](op: Op[A])(j: J): F[A] = compile(op).runA(j)

  final def compile[A](op: Op[A]): StateT[F, J, A] = op match {
    case ReadNull        => StateT.inspectF(readNull)
    case ReadBoolean     => StateT.inspectF(readBoolean)
    case ReadNumber      => StateT.inspectF(readNumber)
    case ReadLong        => StateT.inspectF(readLong)
    case ReadDouble      => StateT.inspectF(readDouble)
    case ReadString      => StateT.inspectF(readString)
    case DownField(key)  => StateT.modifyF(downField(key))
    case DownAt(index)   => StateT.modifyF(downAt(index))
    case ReadFields(opA) => StateT.get[F, J].flatMapF(readFields(opA))
    case ReadValues(opA) => StateT.get[F, J].flatMapF(readValues(opA))
    case ReadMap(opA)    => StateT.get[F, J].flatMapF(readMap(opA))

    case Pure(value)           => StateT.pure(value)
    case Fail(failure)         => StateT.lift(M.raiseError(failure))
    case Mapper(opA, f, false) => self.compile(opA).map(f)
    case Bind(opA, f, false)   => self.compile(opA).flatMap(a => self.compile(f(a)))
    case Join(opA, opB, false) => self.compile(opA).product(self.compile(opB))
    case Then(opA, opB, false) => StateT.get[F, J].flatMap(j => self.compile(opA).flatMap(_ => self.compile(opB)))
    case Mapper(opA, f, true)  => StateT.get[F, J].flatMap(j => self.compile(opA).map(f).modify(_ => j))
    case Bind(opA, f, true)    =>
      StateT.get[F, J].flatMap(j => self.compile(opA).flatMap(a => self.compile(f(a))).modify(_ => j))
    case Join(opA, opB, true)  =>
      StateT.get[F, J].flatMap(j => self.compile(opA).product(self.compile(opB)).modify(_ => j))
    case Then(opA, opB, true)  =>
      StateT.get[F, J].flatMap(j => self.compile(opA).flatMap(_ => self.compile(opB)).modify(_ => j))
  }

  def readNull(j: J): F[Unit]
  def readBoolean(j: J): F[Boolean]
  def readNumber(j: J): F[BiggerDecimal]
  def readLong(j: J): F[Long]
  def readDouble(j: J): F[Double]
  def readString(j: J): F[String]
  def downField(key: String)(j: J): F[J]
  def downAt(index: Int)(j: J): F[J]
  def readFields[A](opA: Op[A])(j: J): F[Vector[(String, A)]]
  def readValues[A](opA: Op[A])(j: J): F[Vector[A]]
  def readMap[A](opA: Op[A])(j: J): F[Map[String, A]]
}
