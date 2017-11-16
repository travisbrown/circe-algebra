package io.circe.algebra.benchmarks

import org.scalatest.FlatSpec

class DecodingBenchmarkSpec extends FlatSpec {
  val benchmark: DecodingBenchmark = new DecodingBenchmark

  "The decoding benchmark" should "correctly decode Foos using circe in fail-fast mode" in {
    assert(benchmark.decodeFailFastWithCursors === Foo.example)
  }

  it should "correctly decode Foos using circe in error-accumulation mode" in {
    assert(benchmark.decodeAccumulatingWithCursors === Foo.example)
  }

  it should "correctly decode Foos using circe-algebra in Either mode" in {
    assert(benchmark.decodeIntoEither === Foo.example)
  }

  it should "correctly decode Foos using circe-algebra in fail-fast mode" in {
    assert(benchmark.decodeFailFast === Foo.example)
  }

  it should "correctly decode Foos using circe-algebra in error-accumulation mode" in {
    assert(benchmark.decodeAccumulating === Foo.example)
  }

  it should "correctly decode Foos using circe-algebra in fail-fast mode with history" in {
    assert(benchmark.decodeFailFastWithHistory === Foo.example)
  }

  it should "correctly decode Foos using circe-algebra in error-accumulation mode with history" in {
    assert(benchmark.decodeAccumulatingWithHistory === Foo.example)
  }

  it should "correctly decode Foos using a Free algebra" in {
    assert(benchmark.decodeFree === Foo.example)
  }
}
