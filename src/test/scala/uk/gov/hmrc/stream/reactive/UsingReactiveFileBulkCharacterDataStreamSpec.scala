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

import org.scalatest.concurrent.Eventually
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.iteratee.Enumerator
import play.api.libs.ws.WSResponseHeaders
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.RequestId
import uk.gov.hmrc.stream.EntityCounter

class UsingReactiveFileBulkCharacterDataStreamSpec extends WordSpecLike with Matchers with FutureAwaits with DefaultAwaitTimeout with Eventually {

   class DummyWSResponseHeaders extends WSResponseHeaders {
     override def status: Int = 200

     override def headers: Map[String, Seq[String]] = Map.empty
   }

   "Processing a stream of data" should {
     "execute a function for each deliminator segregated entity found in the data source" in new UsingReactiveFileBulkCharacterDataStream[String] {
       override implicit def convert(input: String): String = input

       override def reactiveSourceData(resourceLocation: File): Enumerator[Array[Byte]] = {

         val byteArray1: Array[Byte] = Array('a', 'b', ';', 'c', 'd', ';')
         val byteArray2: Array[Byte] = Array('e', 'f', ';', 'g', 'h', ';')
         val byteArray3: Array[Byte] = Array('i', 'j', ';', 'k', 'l', ';')

         Enumerator(byteArray1, byteArray2, byteArray3)
       }

       val counterInstance = new EntityCounter
       implicit val hc = new HeaderCarrier(requestId = Some(RequestId("someRequestId")))

       await(processEntitiesWith(';', new File("someLocation"))(counterInstance.keepTrackOfCallCount))
       eventually(timeout(Span(1, Seconds)))  {
         counterInstance.counter shouldBe 6
       }
     }
   }
 }