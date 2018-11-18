package com.crawler

import java.net.InetSocketAddress
import java.util.concurrent.Executors

import scala.concurrent.ExecutionContext.Implicits.global
import com.sun.net.httpserver.{HttpExchange, HttpHandler, HttpServer}
import io.circe.generic.auto._
import io.circe.syntax._

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

//response entities

final case class Response[T](statusCode: Int, body: T)

object Server {
  private val pool = Executors.newCachedThreadPool()
  private val defaultConfigArgs = Array(8080, 1000)
  def main(args: Array[String]): Unit = {
    if (args.length > 0) {
      val inputedArgs = args.map(arg => Try(arg.toInt))
        .collect {
          case Success(arg) => arg
          case Failure(exception) => throw new IllegalArgumentException("bad args input")
        }
      for (arg <- defaultConfigArgs.indices) {
        if (arg < inputedArgs.length) defaultConfigArgs.update(arg, inputedArgs(arg))
      }
    }
      val server = HttpServer.create(new InetSocketAddress(defaultConfigArgs(0)), 0)
        server.createContext("/", new Initializer(defaultConfigArgs(1)))
        server.setExecutor(pool)
        server.start()
      Runtime.getRuntime.addShutdownHook(new Thread()
        {
          override def run(): Unit = {
            server.stop(1)
          }
       }
      )
    }
  }

class Initializer(requestTimeOut: Int) extends HttpHandler {
  val crawler = new Crawler(requestTimeOut)
  override def handle(t: HttpExchange) {
    try {
    Utils.handleStream[(Option[List[String]], Option[Error])](t.getRequestBody, Parser.parseJsonClientUrls)
        .onComplete {
      case Success(result) => result match {
        case (Some(urls), _) =>
          crawler.getSitesIndexPage(urls).
            onComplete {
              case Success(result) => sendResponse(t, Response(200, result))
              case Failure(error) => sendResponse(t, Response(400, Error(error.getMessage)))
            }

        case (_, Some(error)) => sendResponse(t, Response(400, error))
      }
      case Failure(message) => sendResponse(t, Response(500, Error(message.getMessage)))
    }
    }
    catch {
      case _: Throwable =>
          sendResponse(t, Response(500, Error("server incorrect behaviour, will be terminated")))
            .onComplete {
              _ => System.exit(1)
            }
    }
  }

  private def sendResponse[T](t: HttpExchange, r: Response[T])= {
    Future {
      val headers = t.getResponseHeaders
      headers.add("Content-Type", "application/json")
      val jsonResponse = r.body match {
        case error: Error => error.asJson.spaces2
        case names: Map[String, String] => names.asJson.spaces2
      }
      val bytesBody = jsonResponse.getBytes
      t.sendResponseHeaders(r.statusCode, bytesBody.size)
      val os = t.getResponseBody
      os.write(bytesBody)
      os.close()
    }
  }


}

