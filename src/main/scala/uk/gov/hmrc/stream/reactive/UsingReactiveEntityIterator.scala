/*
 * Copyright 2015 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.stream.reactive

import java.io.File

import play.api.Play.current
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.ws.{WS, WSResponseHeaders}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.stream.CharEntityProcessor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UsingReactiveEntityIterator[T] extends CharEntityProcessor[T] {

  private[stream] def reactiveSourceData(resourceLocation: String): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = WS.url(resourceLocation).getStream

  def bulkEntities(deliminator: Char, resourceLocation: String)(implicit hc: HeaderCarrier, converter: String => T): Future[Iterator[T]] = {

    val byteArrayToChar = Iteratee.getChunks[Array[Byte]].map(chunk => chunk).map(byteArrays => byteArrays.toIterator.map(byteArray => byteArray.map(_.toChar)))

    reactiveSourceData(resourceLocation).flatMap {
      sourceData =>
        sourceData._2 run byteArrayToChar map {
          charArrayIterator =>
            new CharArrayEntityIterator[T](deliminator, charArrayIterator)
        }
    }
  }
}

trait UsingReactiveFileEntityIterator[T] extends CharEntityProcessor[T] {

  private[stream] def reactiveSourceData(resourceLocation: File): Enumerator[Array[Byte]] = Enumerator.fromFile(resourceLocation)

  def bulkEntities(deliminator: Char, resourceLocation: File)(implicit hc: HeaderCarrier, converter: String => T): Future[Iterator[T]] = {

    def byteArrayToChar = Iteratee.getChunks[Array[Byte]].map(chunk => chunk).map(byteArrays => byteArrays.toIterator.map(byteArray => byteArray.map(_.toChar)))

    reactiveSourceData(resourceLocation) run byteArrayToChar map {
      charArrayIterator =>
        new CharArrayEntityIterator[T](deliminator, charArrayIterator)
    }
  }
}

private class CharArrayEntityIterator[T](deliminator: Char, sourceData: Iterator[Array[Char]])(implicit hc: HeaderCarrier, converter: String => T) extends Iterator[T] with CharEntityProcessor[T] {
  override implicit def convert(entityRawData: String): T = converter(entityRawData)

  override def hasNext: Boolean = sourceData.hasNext || constructedEntities.nonEmpty || isEntityDataAvailable

  val constructedEntities = new scala.collection.mutable.Queue[T]

  override def next(): T = {
    getConstructedEntity match {
      case Some(entity) => entity
      case None => constructEntityFromSourceData() match {
        case Some(entity) => entity
        case None => flushEntity match {
          case Some(entity) => entity
          case _ => throw new RuntimeException("The Iterator is out of sync! The hasNext returned true but the next function cannot find anything to return!")
        }
      }
    }
  }

  private def getConstructedEntity: Option[T] = {
    if(constructedEntities.nonEmpty) Some(constructedEntities.dequeue()) else None
  }

  private def flushEntity: Option[T] = {
    if(isEntityDataAvailable) Some(flush) else None
  }

  private def constructEntityFromSourceData(): Option[T] = {
    if (sourceData.hasNext) {
        sourceData.next().foreach {
        char =>
          parseEntity(deliminator)(char) match {
            case Some(entity) => constructedEntities.+=(entity)
            case _ => // do nothing
          }
      }
    }
    getConstructedEntity
  }
}
