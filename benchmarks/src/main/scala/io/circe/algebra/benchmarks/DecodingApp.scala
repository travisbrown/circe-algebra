package io.circe.algebra.benchmarks

import io.circe.algebra.interpreters

object DecodingApp {
  def main(args: Array[String]): Unit = {
    var total = 0L
    (1 to 1000000).foreach { i =>
      val res = interpreters.failFast.decode[Foo](Foo.exampleJson)
      total += res.right.get.v.head

      if (i % 1000 == 0) println(i)
    }

    println(total)
  }
}
