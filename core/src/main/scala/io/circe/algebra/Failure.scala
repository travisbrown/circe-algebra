package io.circe.algebra

sealed abstract class Failure extends Exception

case class DecodingFailure(message: String) extends Failure {
  final override def getMessage: String = message
}

case class OtherFailure(message: String) extends Failure {
  final override def getMessage: String = message
}
