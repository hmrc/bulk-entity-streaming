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

trait UsingHttpBulkCharacterDataStream[T] extends UsingBulkCharacterDataStream[T] with HttpStreamSource
trait UsingFileBulkCharacterDataStream[T] extends UsingBulkCharacterDataStream[T] with FileStreamSource

trait UsingBulkCharacterDataStream[T] extends CharEntityProcessor[T] {

  def processEntitiesWith(deliminator: Char, sourceData: => Iterator[Char])(f: T => Unit)(implicit hc: HeaderCarrier): Unit = {
    val resourceData = sourceData
    Iterator.continually(resourceData.foreach(processCharacter(deliminator, f))).takeWhile(_ => resourceData.hasNext).toList
  }
}






