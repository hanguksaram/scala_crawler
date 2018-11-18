package com.crawler
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.syntax._

import util.control.Breaks._
import java.io.{BufferedReader, InputStream, InputStreamReader}
import java.util.stream.Collectors

import scala.annotation.tailrec
import scala.concurrent.Future




object Parser {
  import scala.concurrent.ExecutionContext.Implicits.global
  private val urlRegexPattern = raw"https?://(www.)?[-a-zA-Z0-9@:%._+~#=]{2,256}.[a-z]{2,6}\b([-a-zA-Z0-9@:%_+.~#?&=]*)".r
  private val titleRegexPattern = raw"<title>(.*?)</title>".r.unanchored
  private val terminalRegexPattern = raw"<body>(.*?)</body>".r.unanchored

  def parseJsonClientUrls(reader: BufferedReader): (Option[List[String]], Option[Error]) = {
    val requestBody = Stream.continually(reader.readLine()).takeWhile(_ != null).mkString("\n")
    decode[List[String]](requestBody) match {
      case Right(urls) =>
         if (urls.forall(urlRegexPattern.findFirstIn(_).isDefined)) (Option(urls.distinct), None) else (None, Option(Error("urls provided dont match to url regex pattern")))
      case Left(_) => (None, Option(Error("bad json structure")))
    }
  }
  def parseSiteName(reader: BufferedReader): Future[Option[String]] = Future {
    @tailrec def parseSiteNameUtil(): Option[String] = {
      val line = reader.readLine()
      line match {
        case titleRegexPattern(name) => Option(name)
        case terminalRegexPattern(_*) => None
        //грохнуть цикл по окончании хмтла если по какой-то причине тэга <body> не оказалось
        case null => None
        case _ => parseSiteNameUtil()
      }
    }
    parseSiteNameUtil()
    }
  }




