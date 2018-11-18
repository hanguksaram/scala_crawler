package com.crawler

import java.security.cert.PKIXRevocationChecker

import dispatch._
import Defaults._

import concurrent.Future
import scala.collection.mutable.ListBuffer

class Crawler(requestTimeout: Int) {

  private val httpClientBuilder = Http.withConfiguration(config =>
    config.setFollowRedirect(true).setConnectTimeout(requestTimeout)
  )
  def getSitesIndexPage(urls: List[String]) = {
    Future {
      val siteRequesters = urls.map(siteUrl => httpClientBuilder(
        url(siteUrl) OK (response => (response.getUri, response.getResponseBodyAsStream)))).to[ListBuffer]
      val res =
        for (siteRequest <- siteRequesters)
          yield
            for {
              result <- siteRequest
              siteName <- Utils.handleStream[Option[String]](result._2, Parser.parseSiteName)
            } yield (result._1.toUrl, siteName.getOrElse("site name not found"))
      val results =
        for (names <- Future.sequence(res))
          yield names.toMap
      results()
    }







//    siteName.getOrElse("site name not found")
  }
}
