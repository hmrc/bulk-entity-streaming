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


trait UsingEntityIterator[T] {

  implicit def convert(json: String): T

  private[stream] def sourceData(resourceLocation: String): Iterator[Char] = scala.io.Source.fromURL(resourceLocation).iter

  def entities(deliminator: Char, resourceLocation: String)(implicit hc: HeaderCarrier, converter: String => T): Iterator[T] = {
    new TransactionIterator(deliminator, sourceData(resourceLocation))
  }
}

private class TransactionIterator[T](deliminator: Char, streamIterator: Iterator[Char])(implicit hc: HeaderCarrier, converter: String => T) extends Iterator[T] with CharEntityProcessor[T] {
  override implicit def convert(data: String): T = converter(data)

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
