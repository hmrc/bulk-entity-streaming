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

import org.scalatest.{Matchers, WordSpecLike}
import uk.gov.hmrc.play.audit.http.HeaderCarrier
import uk.gov.hmrc.play.http.logging.RequestId

class CharEntityProcessorSpec extends WordSpecLike with Matchers {
  "Processing each Character" should {
    "only execute function when a deliminator is found with a trailing deliminator" in new CharEntityProcessor[String] {
      override implicit def convert(input: String): String = input

      val counterInstance = new EntityCounter
      implicit val hc = new HeaderCarrier(requestId = Some(RequestId("someRequestId")))

      val functionToTest = processCharacter(',', counterInstance.keepTrackOfCallCount) _

      functionToTest('a')
      functionToTest('b')
      functionToTest('c')
      functionToTest(',')
      functionToTest('d')
      functionToTest('e')
      functionToTest('f')
      functionToTest(',')
      functionToTest('g')
      functionToTest('h')
      functionToTest('i')
      functionToTest(',')

      counterInstance.counter shouldBe 3
    }

    "only execute function when a deliminator is found without a trailing deliminator" in new CharEntityProcessor[String] {
      override implicit def convert(input: String): String = input

      val counterInstance = new EntityCounter
      implicit val hc = new HeaderCarrier(requestId = Some(RequestId("someRequestId")))

      val functionToTest = processCharacter(',', counterInstance.keepTrackOfCallCount) _

      functionToTest('a')
      functionToTest('b')
      functionToTest('c')
      functionToTest(',')
      functionToTest('d')
      functionToTest('e')
      functionToTest('f')
      functionToTest(',')
      functionToTest('g')
      functionToTest('h')
      functionToTest('i')

      counterInstance.counter shouldBe 2
    }

    "abort process when a request id is not present" in new CharEntityProcessor[String] {
      override implicit def convert(input: String): String = input

      val counterInstance = new EntityCounter
      implicit val hcMissingRequestId = new HeaderCarrier
      val functionToTest = processCharacter(',', counterInstance.keepTrackOfCallCount) _

      intercept[RuntimeException] {
        functionToTest('a')
      }.getMessage shouldBe "Unable to process file. RequestId is missing!"
    }
  }
}
