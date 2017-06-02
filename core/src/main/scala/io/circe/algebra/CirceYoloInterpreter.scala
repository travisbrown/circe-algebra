package io.circe.algebra

import cats.Id
import io.circe.Json
import io.circe.numbers.BiggerDecimal

object CirceYoloInterpreter extends Interpreter[Id, Json] { self =>
  import Op._

  type S[x] = Json => (Json, x)

  def runS[A](s: Json => (Json, A))(j: Json): A = s(j)._2

  def apply[A](op: Op[A]): Json => (Json, A) = op match {
    case ReadNull          => j => (j, readNull(j))
    case ReadBoolean       => j => (j, readBoolean(j))
    case ReadNumber        => j => (j, readNumber(j))
    case ReadLong          => j => (j, readLong(j))
    case ReadDouble        => j => (j, readDouble(j))
    case ReadString        => j => (j, readString(j))
    case DownField(key)    => j => (downField(key)(j), ())
    case DownAt(index)     => j => (downAt(index)(j), ())
    case ReadFields(opA)   => j => (j, readFields(opA)(j))
    case ReadValues(opA)   => j => (j, readValues(opA)(j))

    case Pure(value)       => j => (j, value)
    case Fail(failure)     => throw failure

    case Map(opA, f, false)    => j => self(opA)(j) match { case (j1, a) => (j1, f(a)) }
    case Bind(opA, f, false)   => j => self(opA)(j) match { case (j1, a) => self(f(a))(j1) }
    case Join(opA, opB, false) => j => {
      val (j1, a) = self(opA)(j)
      val (j2, b) = self(opB)(j1)
      (j2, (a, b))
    }

    case Map(opA, f, true)     => j => (j, f(self(opA)(j)._2))
    case Bind(opA, f, true)    => j => self(opA)(j) match {
      case (newJ, a) => (j, self(f(a))(newJ)._2)
    }
    case Join(opA, opB, true)  => j => (j, (self(opA)(j)._2, self(opB)(j)._2))
  }

  def readNull(j: Json): Unit = if (j.isNull) () else throw new Exception("Something bad happened")
  def readBoolean(j: Json): Boolean = j.asInstanceOf[Json.JBoolean].b
  def readNumber(j: Json): BiggerDecimal = j.asInstanceOf[Json.JNumber].n.toBiggerDecimal
  def readLong(j: Json): Long = j.asInstanceOf[Json.JNumber].n.toLong.get
  def readDouble(j: Json): Double = j.asInstanceOf[Json.JNumber].n.toDouble
  def readString(j: Json): String = j.asInstanceOf[Json.JString].s
  def downField(key: String)(j: Json): Json = j.asInstanceOf[Json.JObject].o(key).get
  def downAt(index: Int)(j: Json): Json = j.asInstanceOf[Json.JArray].a(index)
  def readFields[A](opA: Op[A])(j: Json): Iterable[(String, A)] = new Iterable[(String, A)] {
    private[this] val d: Json => A = self(opA)(_)._2
    private[this] val fs = j.asInstanceOf[Json.JObject].o.toVector

    def iterator: Iterator[(String, A)] = fs.iterator.map {
      case (k, v) => (k -> d(v))
    }
  }
  def readValues[A](opA: Op[A])(j: Json): Iterable[A] = new Iterable[A] {
    private[this] val d: Json => A = self(opA)(_)._2
    val js = j.asInstanceOf[Json.JArray].a

    def iterator: Iterator[A] = js.iterator.map(d)
  }
}
