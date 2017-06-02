package io.circe.algebra

import cats.Id
import io.circe.Json
import io.circe.numbers.BiggerDecimal

object CirceYoloInterpreter extends Interpreter[Id, Json] { self =>
  import Op._

  type S[x] = Json => (Json, x)

  def runS[A](s: Json => (Json, A))(j: Json): A = s(j)._2

  def apply[A](op: Op[A]): Json => (Json, A) = op match {
    case ReadNull        => j => (j, readNull(j))
    case ReadBoolean     => j => (j, readBoolean(j))
    case ReadNumber      => j => (j, readNumber(j))
    case ReadLong        => j => (j, readLong(j))
    case ReadString      => j => (j, readString(j))
    case DownField(key)  => j => (downField(key)(j), ())
    case DownAt(index)   => j => (downAt(index)(j), ())
    case ReadFields(opA) => j => (j, readFields(opA)(j))
    case ReadValues(opA) => j => (j, readValues(opA)(j))

    case Pure(value)     => j => (j, value)
    case Fail(failure)   => throw failure
    case Map(opA, f)     => j => self(opA)(j) match { case (newJ, a) => (newJ, f(a)) }
    case Bind(opA, f)    => j => self(opA)(j) match { case (newJ, a) => self(f(a))(newJ) }
    case Join(opA, opB)  => j => {
      val (j1, a) = self(opA)(j)
      val (j2, b) = self(opB)(j1)
      (j2, (a, b))
    }

    case Bracket(opA)    => j => self(opA)(j) match { case (_, a) => (j, a) }
  }

  def readNull(j: Json): Unit = if (j.isNull) () else throw new Exception("Something bad happened")
  def readBoolean(j: Json): Boolean = j.asInstanceOf[Json.JBoolean].b
  def readNumber(j: Json): BiggerDecimal = j.asInstanceOf[Json.JNumber].n.toBiggerDecimal
  def readLong(j: Json): Long = j.asInstanceOf[Json.JNumber].n.toLong.get
  def readString(j: Json): String = j.asInstanceOf[Json.JString].s
  def downField(key: String)(j: Json): Json = j.asInstanceOf[Json.JObject].o(key).get
  def downAt(index: Int)(j: Json): Json = j.asInstanceOf[Json.JArray].a(index)
  def readFields[A](opA: Op[A])(j: Json): Vector[(String, A)] = {
    val fs = j.asInstanceOf[Json.JObject].o.toVector
    val builder = Vector.newBuilder[(String, A)]
    val d: Json => A = self(opA)(_)._2

    fs.foreach {
      case (k, v) =>
        builder += (k -> d(v))
    }

    builder.result()
  }
  def readValues[A](opA: Op[A])(j: Json): Vector[A] = {
    val js = j.asInstanceOf[Json.JArray].a
    val builder = Vector.newBuilder[A]
    val d: Json => A = self(opA)(_)._2

    js.foreach { j =>
      builder += d(j)
    }

    builder.result()
  }
}
