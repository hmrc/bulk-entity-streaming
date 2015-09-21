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

package uk.gov.hmrc.stream

import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.stream.source.{FileStreamSource, HttpStreamSource}

trait UsingHttpEntityIterator[T] extends UsingEntityIterator[T] with HttpStreamSource
trait UsingFileEntityIterator[T] extends UsingEntityIterator[T] with FileStreamSource

trait UsingEntityIterator[T] extends EntityConverter[T] {

  def bulkEntities(deliminator: Char, sourceData: => Iterator[Char])(implicit hc: HeaderCarrier, converter: String => T): Iterator[T] = {
    new CharEntityIterator(deliminator, sourceData)
  }
}

private class CharEntityIterator[T](deliminator: Char, streamIterator: Iterator[Char])(implicit hc: HeaderCarrier, converter: String => T) extends Iterator[T] with CharEntityProcessor[T] {
  override implicit def convert(entityRawData: String): T = converter(entityRawData)

  override def hasNext: Boolean = streamIterator.hasNext

  override def next(): T = {
    val nextCharacter = streamIterator.next()
    parseEntity(deliminator)(nextCharacter) match {
      case Some(entity) => entity
      case _ if hasNext => next()
      case _ => flush
    }
  }
}
