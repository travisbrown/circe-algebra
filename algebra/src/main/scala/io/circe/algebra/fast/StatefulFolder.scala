package io.circe.algebra.fast

import io.circe.{ CursorOp, DecodingFailure, Json, JsonNumber, JsonObject }
import io.circe.algebra.{ NavigationOp, Op }
import scala.collection.mutable.HashMap

abstract class StatefulFolder[E, Z](c: Json) extends Op.Folder[Unit] {
  def result: Either[E, Z] = if (failed) {
    Left(failure)
  } else {
    Right(value.asInstanceOf[Z])
  }

  protected[this] var value: Any = null
  protected[this] var cursor: Json = c
  protected[this] var failed: Boolean = false
  protected[this] var halted: Boolean = false

  protected[this] var navigationFailure: NavigationOp = null

  protected[this] def failure: E
  protected[this] def fail(message: String, op: NavigationOp = null): Unit

  protected[this] def valueAsUnsafe[A]: A = value.asInstanceOf[A]
  protected[this] def asBooleanUnsafe: Boolean = cursor.asInstanceOf[Json.JBoolean].value
  protected[this] def asNumberUnsafe: JsonNumber = cursor.asInstanceOf[Json.JNumber].value
  protected[this] def asStringUnsafe: String = cursor.asInstanceOf[Json.JString].value
  protected[this] def asObjectUnsafe: JsonObject = cursor.asInstanceOf[Json.JObject].value
  protected[this] def asArrayUnsafe: Vector[Json] = cursor.asInstanceOf[Json.JArray].value

  final def onReadNull: Unit = if (cursor.isNull) {
    if (!failed) value = ()
  } else fail("Expected null")

  final def onReadBoolean: Unit = if (cursor.isBoolean) {
    if (!failed) value = asBooleanUnsafe
  } else fail("Expected boolean")

  final def onReadNumber: Unit = if (cursor.isNumber) {
    if (!failed) value = asNumberUnsafe
  } else fail("Expected number")

  final def onReadString: Unit = if (cursor.isString) {
    if (!failed) value = asStringUnsafe
  } else fail("Expected string")

  final def onReadLong: Unit = if (cursor.isNumber) {
    asNumberUnsafe.toLong match {
      case Some(v) => if (!failed) value = v
      case None => fail("Expected long")
    }
  } else fail("Expected number")

  final def onReadDouble: Unit = if (cursor.isNumber) {
    value = asNumberUnsafe.toDouble
  } else fail("Expected number")

  final def onInspectNavigationFailure: Unit = {
    failed = false
    halted = false
    value = Option(navigationFailure)
  }

  final def onPure[A](a: A): Unit = {
    value = a
  }

  final def onMap[A, B](opA: Op[A], f: A => B, isBracketed: Boolean): Unit = {
    val orig = cursor

    opA.fold(this)
    if (!failed) value = f(valueAsUnsafe[A])
    if (isBracketed) cursor = orig
  }

  final def onBind[A, B](opA: Op[A], f: A => Op[B], isBracketed: Boolean): Unit = {
    val orig = cursor

    opA.fold(this)
    if (!failed) f(valueAsUnsafe[A]).fold(this)
    if (isBracketed) cursor = orig
  }

  final def onJoin[A, B](opA: Op[A], opB: Op[B], isBracketed: Boolean): Unit = {
    val orig = cursor
    opA.fold(this)

    if (!halted || navigationFailure.ne(null)) {
      val x = value
      opB.fold(this)

      if (!failed) value = (x, value)
    }
    if (isBracketed) cursor = orig
  }

  final def onThen[A, B](opA: Op[A], opB: Op[B], isBracketed: Boolean): Unit = {
    val orig = cursor

    opA.fold(this)
    if (!halted || navigationFailure.ne(null)) opB.fold(this)
    if (isBracketed) cursor = orig
  }
}

object StatefulFolder {
  abstract class NoHistory[E, Z](c: Json) extends StatefulFolder[E, Z](c) {
    protected[this] def fail(message: String, op: NavigationOp = null): Unit = {
      navigationFailure = op
      onFail(DecodingFailure(message, Nil))
    }

    final def onDownField(key: String): Unit = if (cursor.isObject) {
      asObjectUnsafe(key) match {
        case Some(v) =>
          cursor = v
          if (!failed) value = ()
        case None =>
          fail(s"Expected key: $key", Op.DownField(key))
      }
    } else fail("Expected object")

    final def onDownAt(index: Int): Unit = if (cursor.isArray) {
      val js = asArrayUnsafe

      if (js.size > index) {
        cursor = js(index)
        if (!failed) value = ()
      } else {
        fail(s"Expected index: $index", Op.DownAt(index))
      }
    } else fail("Expected array")

    final def onReadFields[A](opA: Op[A]): Unit = if (cursor.isObject) {
      val orig = cursor
      val o = asObjectUnsafe
      val fs = o.keys.iterator
      val builder = Vector.newBuilder[(String, A)]

      while (fs.hasNext && !halted) {
        val k = fs.next()
        val v = o(k).get
        cursor = v
        opA.fold(this)
        if (!failed) builder += (k -> valueAsUnsafe[A])
      }

      cursor = orig
      if (!failed) value = builder.result()
    } else fail("Expected object")

    final def onReadValues[A](opA: Op[A]): Unit = if (cursor.isArray) {
      val orig = cursor
      val js = asArrayUnsafe.iterator
      val builder = Vector.newBuilder[A]

      while (js.hasNext && !halted) {
        cursor = js.next()
        opA.fold(this)
        if (!failed) builder += valueAsUnsafe[A]
      }

      cursor = orig
      if (!failed) value = builder.result()
    } else fail("Expected array")

    final def onReadMap[A](opA: Op[A]): Unit = if (cursor.isObject) {
      val orig = cursor
      val o = asObjectUnsafe
      val fs = o.keys.iterator
      val builder = new HashMap[String, A]()

      while (fs.hasNext && !halted) {
        val k = fs.next()
        val v = o(k).get
        cursor = v
        opA.fold(this)
        if (!failed) builder(k) = valueAsUnsafe[A]
      }

      cursor = orig
      if (!failed) value = builder.toMap
    } else fail("Expected object")
  }

  abstract class WithHistory[E, Z](c: Json) extends StatefulFolder[E, Z](c) {
    private[this] var history: List[CursorOp] = Nil

    protected[this] def fail(message: String, op: NavigationOp = null): Unit = {
      navigationFailure = op
      val h = history
      onFail(DecodingFailure(message, h))
    }

    final def onDownField(key: String): Unit = if (cursor.isObject) {
      asObjectUnsafe(key) match {
        case Some(v) =>
          cursor = v
          history = CursorOp.DownField(key) :: history
          if (!failed) value = ()
        case None => fail(s"Expected key: $key", Op.DownField(key))
      }
    } else fail("Expected object")

    final def onDownAt(index: Int): Unit = if (cursor.isArray) {
      val js = asArrayUnsafe

      if (js.size > index) {
        cursor = js(index)
        history = CursorOp.DownN(index) :: history
        if (!failed) value = ()
      } else fail(s"Expected index: $index", Op.DownAt(index))
    } else fail("Expected array")

    final def onReadFields[A](opA: Op[A]): Unit = if (cursor.isObject) {
      val orig = cursor
      val o = asObjectUnsafe
      val fs = o.keys.iterator
      val builder = Vector.newBuilder[(String, A)]
      var first = true

      while (fs.hasNext && !halted) {
        val k = fs.next()
        val v = o(k).get
        cursor = v
        history = CursorOp.DownField(k) :: (if (first) history else history.tail)

        opA.fold(this)
        if (!failed) builder += (k -> valueAsUnsafe[A])
        first = false
      }

      cursor = orig
      if (!failed) value = builder.result()
    } else fail("Expected object")

    final def onReadValues[A](opA: Op[A]): Unit = if (cursor.isArray) {
      val orig = cursor
      val js = asArrayUnsafe.iterator
      val builder = Vector.newBuilder[A]
      history = CursorOp.DownArray :: history

      while (js.hasNext && !halted) {
        cursor = js.next()
        opA.fold(this)
        history = CursorOp.MoveRight :: history
        if (!failed) builder += valueAsUnsafe[A]
      }

      cursor = orig
      if (!failed) value = builder.result()
    } else fail("Expected array")

    final def onReadMap[A](opA: Op[A]): Unit = if (cursor.isObject) {
      val orig = cursor
      val o = asObjectUnsafe
      val fs = o.keys.iterator
      val builder = new HashMap[String, A]()
      var first = true

      while (fs.hasNext && !halted) {
        val k = fs.next()
        val v = o(k).get
        cursor = v
        history = CursorOp.DownField(k) :: (if (first) history else history.tail)
        opA.fold(this)
        if (!failed) builder(k) = valueAsUnsafe[A]
        first = false
      }

      cursor = orig
      if (!failed) value = builder.toMap
    } else fail("Expected object")
  }
}
