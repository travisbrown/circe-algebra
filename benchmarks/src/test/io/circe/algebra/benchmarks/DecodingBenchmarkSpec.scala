package io.circe.algebra.benchmarks

import org.scalatest.FlatSpec

class DecodingBenchmarkSpec extends FlatSpec {
  val benchmark: DecodingBenchmark = new DecodingBenchmark

  "The decoding benchmark" should "correctly decode Foos using circe" in {
    assert(benchmark.decodeC === Foo.example)
  }

  it should "correctly decode Foos using circe-algebra in Either mode" in {
    assert(benchmark.decodeEitherA === Foo.example)
  }

  it should "correctly decode Foos using circe-algebra in yolo mode" in {
    assert(benchmark.decodeYoloA === Foo.example)
  }
}