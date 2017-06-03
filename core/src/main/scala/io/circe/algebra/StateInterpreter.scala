package io.circe.algebra

import cats.MonadError
import cats.data.StateT

abstract class StateInterpreter[F[_], J](implicit M: MonadError[F, Failure]) extends Interpreter[F, J] { self =>
  import Op._

  type S[x] = StateT[F, J, x]

  def apply[A](op: Op[A])(j: J): F[A] = compile(op).runA(j)

  def compile[A](op: Op[A]): StateT[F, J, A] = op match {
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

    case Pure(value)           => StateT.pure(value)
    case Fail(failure)         => StateT.lift(M.raiseError(failure))
    case Map(opA, f, false)    => self.compile(opA).map(f)
    case Bind(opA, f, false)   => self.compile(opA).flatMap(a => self.compile(f(a)))
    case Join(opA, opB, false) => self.compile(opA).product(self.compile(opB))
    case Map(opA, f, true)     => StateT.get[F, J].flatMap(j => self.compile(opA).map(f).modify(_ => j))
    case Bind(opA, f, true)    => StateT.get[F, J].flatMap(j => self.compile(opA).flatMap(a => self.compile(f(a))).modify(_ => j))
    case Join(opA, opB, true)  => StateT.get[F, J].flatMap(j => self.compile(opA).product(self.compile(opB)).modify(_ => j))
  }
}
