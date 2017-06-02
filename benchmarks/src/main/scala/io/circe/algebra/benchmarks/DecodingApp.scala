package io.circe.algebra.benchmarks

import io.circe.algebra.jsonYolo

object DecodingApp {
  def main(args: Array[String]): Unit = {
    var total = 0L
    (1 to 1000000).foreach { i =>
      val res = jsonYolo.decode[Foo](Foo.exampleJson)
      total += res.v.head

      if (i % 1000 == 0) println(i)
    }

    println(total)
  }
}
