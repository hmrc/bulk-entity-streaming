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

import java.io.File

import org.scalatest.{Matchers, WordSpecLike}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.RequestId

class UsingFileEntityIteratorSpec extends WordSpecLike with Matchers {

  "Processing a stream of data" should {
    "execute a function for each deliminator segregated entity found in the data source" in new UsingFileEntityIterator[String] {
      override implicit def convert(input: String): String = input

      override def sourceData(resourceLocation: File): Iterator[Char] = {
        List[Char]('a', 'b', ';', 'c', 'd', ';', 'e', 'f', ';', 'g', 'h', ';').iterator
      }

      val counterInstance = new EntityCounter
      implicit val hc = new HeaderCarrier(requestId = Some(RequestId("someRequestId")))

      bulkEntities(';', sourceData(new File("someLocation/file.ext"))).foreach(counterInstance.keepTrackOfCallCount)

      counterInstance.counter shouldBe 4
    }

    "execute a function for each deliminator segregated entity found in the data source with a trailing entity" in new UsingFileEntityIterator[String] {
      override implicit def convert(input: String): String = input

      override def sourceData(resourceLocation: File): Iterator[Char] = {
        List[Char]('a', 'b', ';', 'c', 'd', ';', 'e', 'f', ';', 'g', 'h', ';', 'i', 'j').iterator
      }

      val counterInstance = new EntityCounter
      implicit val hc = new HeaderCarrier(requestId = Some(RequestId("someRequestId")))

      bulkEntities(';', sourceData(new File("someLocation/file.ext"))).foreach(counterInstance.keepTrackOfCallCount)

      counterInstance.counter shouldBe 5
    }
  }
}