package io.circe.algebra

import io.circe.DecodingFailure
import io.circe.numbers.BiggerDecimal

sealed abstract class Op[A] {
  def bracket: Op[A]

  // Convenience methods for source compatibility with the current cursor API.
  final def downField(key: String): Op[Unit] = then(Op.DownField(key))
  final def downAt(index: Int): Op[Unit] = then(Op.DownAt(index))
  final def as[B](implicit decodeB: Decoder[B]): Op[B] = then(decodeB.op).bracket
  final def get[B](key: String)(implicit decodeB: Decoder[B]): Op[B] =
    then(Op.DownField(key)).then(decodeB.op).bracket

  // These definitions are for the sake of convenience—we could get them via Cats syntax.
  final def map[B](f: A => B): Op[B] = Op.Mapper(this, f, false)
  final def flatMap[B](f: A => Op[B]): Op[B] = Op.Bind(this, f, false)
  final def product[B](opB: Op[B]): Op[(A, B)] = Op.Join(this, opB, false)
  final def map2[B, C](opB: Op[B])(f: (A, B) => C): Op[C] = product(opB).map(p => f(p._1, p._2))
  final def then[B](opB: Op[B]): Op[B] = Op.Then(this, opB, false)

  def fold[Z](folder: Op.Folder[Z]): Z
}

sealed abstract class ReadingOp[A] extends Op[A] {
  final def bracket: Op[A] = this
}

sealed abstract class NavigationOp extends Op[Unit] {
  final def bracket: Op[Unit] = Op.Unit
}

sealed abstract class StrictOp[A] extends Op[A] {
  final def bracket: Op[A] = this
}

sealed abstract class CompositionOp[A] extends Op[A] {
  def isBracketed: Boolean
}

object Op extends OpInstances {
  val Unit: Op[Unit] = Pure[Unit](())

  abstract class Folder[Z] {
    def onReadNull: Z
    def onReadBoolean: Z
    def onReadNumber: Z
    def onReadString: Z
    def onReadLong: Z
    def onReadDouble: Z
    def onReadFields[A](opA: Op[A]): Z
    def onReadValues[A](opA: Op[A]): Z
    def onReadMap[A](opA: Op[A]): Z
    def onDownField(key: String): Z
    def onDownAt(index: Int): Z
    def onPure[A](value: A): Z
    def onFail(failure: DecodingFailure): Z
    def onMap[A, B](opA: Op[A], f: A => B, isBracketed: Boolean): Z
    def onBind[A, B](opA: Op[A], f: A => Op[B], isBracketed: Boolean): Z
    def onJoin[A, B](opA: Op[A], opB: Op[B], isBracketed: Boolean): Z
    def onThen[A, B](opA: Op[A], opB: Op[B], isBracketed: Boolean): Z
  }

  // Primitive reading operations.
  final case object ReadNull                                                   extends ReadingOp[Unit] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadNull
  }
  final case object ReadBoolean                                                extends ReadingOp[Boolean] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadBoolean
  }
  final case object ReadNumber                                                 extends ReadingOp[BiggerDecimal] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadNumber
  }
  final case object ReadString                                                 extends ReadingOp[String] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadString
  }

  // Redundant reading operations designed to support optimizations.
  final case object ReadLong                                                   extends ReadingOp[Long] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadLong
  }
  final case object ReadDouble                                                 extends ReadingOp[Double] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadDouble
  }

  // Reading operations for JSON objects and arrays.
  final case class ReadFields[A](opA: Op[A])                                   extends ReadingOp[Vector[(String, A)]] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadFields(opA)
  }
  final case class ReadValues[A](opA: Op[A])                                   extends ReadingOp[Vector[A]] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadValues(opA)
  }
  final case class ReadMap[A](opA: Op[A])                                      extends ReadingOp[Map[String, A]] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onReadMap(opA)
  }

  // Navigation operations.
  final case class DownField(key: String)                                      extends NavigationOp {
    final def fold[Z](folder: Folder[Z]): Z = folder.onDownField(key)
  }
  final case class DownAt(index: Int)                                          extends NavigationOp {
    final def fold[Z](folder: Folder[Z]): Z = folder.onDownAt(index)
  }

  // Operations supporting composition.
  final case class Pure[A](value: A)                                           extends StrictOp[A] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onPure(value)
  }
  final case class Fail[A](failure: DecodingFailure)                           extends StrictOp[A] {
    final def fold[Z](folder: Folder[Z]): Z = folder.onFail(failure)
  }
  final case class Mapper[A, B](opA: Op[A], f: A => B, isBracketed: Boolean)   extends CompositionOp[B] {
    final def bracket: Op[B] = if (isBracketed) this else copy(isBracketed = true)
    final def fold[Z](folder: Folder[Z]): Z = folder.onMap(opA, f, isBracketed)
  }
  final case class Bind[A, B](opA: Op[A], f: A => Op[B], isBracketed: Boolean) extends CompositionOp[B] {
    final def bracket: Op[B] = if (isBracketed) this else copy(isBracketed = true)
    final def fold[Z](folder: Folder[Z]): Z = folder.onBind(opA, f, isBracketed)
  }
  final case class Join[A, B](opA: Op[A], opB: Op[B], isBracketed: Boolean)    extends CompositionOp[(A, B)] {
    final def bracket: Op[(A, B)] = if (isBracketed) this else copy(isBracketed = true)
    final def fold[Z](folder: Folder[Z]): Z = folder.onJoin(opA, opB, isBracketed)
  }
  final case class Then[A, B](opA: Op[A], opB: Op[B], isBracketed: Boolean) extends CompositionOp[B] {
    final def bracket: Op[B] = if (isBracketed) this else copy(isBracketed = true)
    final def fold[Z](folder: Folder[Z]): Z = folder.onThen(opA, opB, isBracketed)
  }
}
