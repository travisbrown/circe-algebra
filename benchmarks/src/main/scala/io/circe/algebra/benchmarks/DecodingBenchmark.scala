package io.circe.algebra.benchmarks

import cats.Apply
import io.circe.{ Decoder => DecoderC, Encoder, Json }
import io.circe.algebra.{ Decoder => DecoderA, Op, _ }
import io.circe.syntax._
import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations._

case class Foo(f: Double, m: Map[String, Boolean], v: Vector[Long])

object Foo {
  implicit val encodeFoo: Encoder[Foo] = Encoder.instance {
    case Foo(f, m, v) => Json.obj("f" -> f.asJson, "m" -> m.asJson, "v" -> v.asJson)
  }

  implicit val decodeFooC: DecoderC[Foo] = DecoderC.instance { c =>
    DecoderC.resultInstance.map3(
      c.get[Double]("f"),
      c.get[Map[String, Boolean]]("m"),
      c.get[Vector[Long]]("v")
    )(Foo(_, _, _))
  }

  implicit val decodeFooA: DecoderA[Foo] = DecoderA(
    Apply[Op].map3(
      get[Double]("f"),
      get[Map[String, Boolean]]("m"),
      get[Vector[Long]]("v")
    )(Foo(_, _, _))
  )

  val example: Foo = Foo(
    Double.MaxValue,
    (0 to 10).map(i => i.toString -> (i % 2 == 0)).toMap,
    (1000L to 1010L).toVector
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
@BenchmarkMode(Array(Mode.Throughput))
@OutputTimeUnit(TimeUnit.SECONDS)
class DecodingBenchmark {
  @Benchmark
  def decodeC: Foo = Foo.decodeFooC.decodeJson(Foo.exampleJson).right.get

  @Benchmark
  def decodeYoloA: Foo = jsonYolo.decode[Foo](Foo.exampleJson)

  @Benchmark
  def decodeEitherA: Foo = jsonEither.decode[Foo](Foo.exampleJson).right.get
}
