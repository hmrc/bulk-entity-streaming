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

import play.api.Play.current
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.ws.{WS, WSResponseHeaders}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.stream.CharEntityProcessor

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UsingReactiveBulkCharacterDataStream[T] extends CharEntityProcessor[T] {
  private[stream] def reactiveSourceData(resourceLocation: String): Future[(WSResponseHeaders, Enumerator[Array[Byte]])] = WS.url(resourceLocation).getStream

  def processEntitiesWith(deliminator: Char, resourceLocation: String)(f: T => Unit)(implicit hc: HeaderCarrier): Future[Unit] = {
    val byteArrayProcessor = Iteratee.foreach[Array[Byte]](chunkedArray => chunkedArray.map(_.toChar).map{char => processCharacter(deliminator, f)(char)})

    reactiveSourceData(resourceLocation).map {
      case (_, responseByteArray) => responseByteArray run byteArrayProcessor
    }
  }
}
