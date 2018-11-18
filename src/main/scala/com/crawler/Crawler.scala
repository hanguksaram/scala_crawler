package com.crawler

import java.security.cert.PKIXRevocationChecker

import dispatch._
import Defaults._

import concurrent.Future
import scala.collection.mutable.ListBuffer
import scala.util.control.NonFatal

class Crawler(requestTimeout: Int) {

  private val httpClientBuilder = Http.withConfiguration(config =>
    config.setFollowRedirect(true).setConnectTimeout(requestTimeout)
  )
  def getSitesIndexPage(urls: List[String]) = {
    Future {
      val siteRequesters = urls.map(siteUrl => httpClientBuilder(
        url(siteUrl) OK (response => (response.getUri, Utils.getStream(response.getResponseBodyAsStream))))
      .recover {
        case NonFatal(e) => (url(siteUrl).toRequest.getUri, Utils.getStream(e))
      }).to[ListBuffer]
      val res =
        for (siteRequest <- siteRequesters)
          yield
            for {
              result <- siteRequest
              siteName <- result._2 match {
              case Left(error) => Future.successful(Option(error.errorName))
              case Right(stream) => Utils.handleStream[Option[String]](stream, Parser.parseSiteName)
            }
            } yield (result._1.toUrl, siteName.getOrElse("site name not found"))

      val results =
        for (names <- Future.sequence(res))
          yield names.toMap
      results()
    }







//    siteName.getOrElse("site name not found")
  }
}
