package io.circe.algebra.benchmarks

import cats.Apply
import io.circe.{ Decoder => DecoderC, Encoder, Json }
import io.circe.algebra.{ Decoder => DecoderA, Op, _ }
import io.circe.algebra.free.{ Decoder => DecoderF, Op => OpF }
import io.circe.syntax._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

case class Foo(f: Double, m: Map[String, Boolean], v: Vector[Long])

object Foo {
  implicit val encodeFoo: Encoder[Foo] = Encoder.instance {
    case Foo(f, m, v) => Json.obj("f" -> f.asJson, "m" -> m.asJson, "nested" -> Json.obj("v" -> v.asJson))
  }

  implicit val decodeFooC: DecoderC[Foo] = DecoderC.instance { c =>
    DecoderC.resultInstance.map3(
      c.get[Double]("f"),
      c.get[Map[String, Boolean]]("m"),
      c.downField("nested").get[Vector[Long]]("v")
    )(Foo(_, _, _))
  }

  implicit val decodeFooA: DecoderA[Foo] = DecoderA.instance(
    Apply[Op].map3(
      ops.get[Double]("f"),
      ops.get[Map[String, Boolean]]("m"),
      ops.downField("nested").get[Vector[Long]]("v")
    )(Foo(_, _, _))
  )

  implicit val decodeFooF: DecoderF[Foo] = DecoderF(
    Apply[OpF.OpF].map3(
      OpF.bracket(OpF.get[Double]("f")),
      OpF.bracket(OpF.get[Map[String, Boolean]]("m")),
      OpF.bracket(OpF.downField("nested").flatMap(_ => OpF.get[Vector[Long]]("v")))
    )(Foo(_, _, _))
  )

  val example: Foo = Foo(
    Double.MaxValue,
    (0 to 100).map(i => i.toString -> (i % 2 == 0)).toMap,
    (1000L to 2010L).toVector
  )
  val exampleJson: Json = example.asJson
}

/**
 * Compare the performance of various interpreters and the current decoding approach.
 *
 * The following command will run the benchmarks with reasonable settings:
 *
 * > sbt "benchmarks/jmh:run -i 10 -wi 10 -f 2 -t 1 io.circe.algebra.benchmarks.DecodingBenchmark"
 */
@State(Scope.Thread)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
class DecodingBenchmark {
  @Benchmark
  def decodeFailFastWithCursors: Foo = Foo.decodeFooC.decodeJson(Foo.exampleJson).right.get

  @Benchmark
  def decodeAccumulatingWithCursors: Foo =
    Foo.decodeFooC.decodeAccumulating(Foo.exampleJson.hcursor).getOrElse(sys.error("should never happen"))

  @Benchmark
  def decodeFailFast: Foo = interpreters.failFast.decode[Foo](Foo.exampleJson).right.get

  @Benchmark
  def decodeAccumulating: Foo = interpreters.accumulating.decode[Foo](Foo.exampleJson).right.get

  @Benchmark
  def decodeFailFastWithHistory: Foo = interpreters.failFast.decode[Foo](Foo.exampleJson).right.get

  @Benchmark
  def decodeAccumulatingWithHistory: Foo = interpreters.accumulating.decode[Foo](Foo.exampleJson).right.get

  @Benchmark
  def decodeIntoEither: Foo = interpreters.either.decode[Foo](Foo.exampleJson).right.get

  @Benchmark
  def decodeFree: Foo = io.circe.algebra.free.decode[Foo](Foo.exampleJson).right.get
}
