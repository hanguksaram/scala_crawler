package com.crawler

import java.io.{BufferedReader, InputStream, InputStreamReader}

import scala.concurrent.Future

final case class Error(errorName: String)
object Utils {
  import scala.concurrent.ExecutionContext.Implicits.global
  def handleStream[T](stream: InputStream, streamHandler: BufferedReader => T, fatal: => Boolean = false): Future[T] = {
    val reader = new BufferedReader(new InputStreamReader(stream))
    var readResult: Future[T] = null
    try {
      readResult = Future(streamHandler(reader))
      readResult
    }
    finally {
//        readResult.onComplete(_ => reader.close())
    }
  }
  //костыль для рабочего рековера стримовых футуров
  def getStream(stream: InputStream): Either[Error, InputStream] = {
    if (stream != null) Right(stream) else Left(Error("someerror"))
  }

}
