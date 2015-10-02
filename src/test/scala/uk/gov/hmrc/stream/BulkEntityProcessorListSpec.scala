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

import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.iteratee.Enumerator
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent._

class BulkEntityProcessorListSpec extends WordSpecLike with Matchers with FutureAwaits with DefaultAwaitTimeout with Eventually {
  "BulkEntityList using a Byte Array Enumerator" should {
    "exhaust the source data and return a list of converted entities" in {

      def sourceData(): Enumerator[Array[Byte]] = {
        val byteArray1: Array[Byte] = Array('a', 'b', ';', 'c', 'd', ';')
        val byteArray2: Array[Byte] = Array('e', 'f', ';', 'g', 'h', ';')
        val byteArray3: Array[Byte] = Array('i', 'j', ';', 'k', 'l', ';')

        Enumerator(byteArray1, byteArray2, byteArray3)
      }

      val deliminator = ';'
      def converter(rawData: String): String = rawData

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      val result = await(bulkEntityProcessor.list(sourceData(), deliminator, converter _))
      result.size shouldBe 6
      result.contains("ab") shouldBe true
      result.contains("cd") shouldBe true
      result.contains("ef") shouldBe true
      result.contains("gh") shouldBe true
      result.contains("ij") shouldBe true
      result.contains("kl") shouldBe true
    }

    "exhaust the source data and return a list of converted entities when a trailing deliminator is missing" in {

      def sourceData(): Enumerator[Array[Byte]] = {
        val byteArray1: Array[Byte] = Array('a', 'b', ';', 'c', 'd', ';')
        val byteArray2: Array[Byte] = Array('e', 'f', ';', 'g', 'h', ';')
        val byteArray3: Array[Byte] = Array('i', 'j', ';', 'z', 'z')

        Enumerator(byteArray1, byteArray2, byteArray3)
      }

      val deliminator = ';'
      def converter(rawData: String): String = rawData

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      val result = await(bulkEntityProcessor.list(sourceData(), deliminator, converter _))
      result.size shouldBe 6
      result.contains("ab") shouldBe true
      result.contains("cd") shouldBe true
      result.contains("ef") shouldBe true
      result.contains("gh") shouldBe true
      result.contains("ij") shouldBe true
      result.contains("zz") shouldBe true
    }
  }

  "BulkEntityList using a Future of a Byte Array Enumerator" should {
    "exhaust the source data and return a list of converted entities" in {

      def sourceData(): Future[Enumerator[Array[Byte]]] = {
        val byteArray1: Array[Byte] = Array('a', 'b', ';', 'c', 'd', ';')
        val byteArray2: Array[Byte] = Array('e', 'f', ';', 'g', 'h', ';')
        val byteArray3: Array[Byte] = Array('i', 'j', ';', 'k', 'l', ';')

        Future.successful(Enumerator(byteArray1, byteArray2, byteArray3))
      }

      val deliminator = ';'
      def converter(rawData: String): String = rawData

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      val result = await(bulkEntityProcessor.listf(sourceData(), deliminator, converter _))
      result.size shouldBe 6
      result.contains("ab") shouldBe true
      result.contains("cd") shouldBe true
      result.contains("ef") shouldBe true
      result.contains("gh") shouldBe true
      result.contains("ij") shouldBe true
      result.contains("kl") shouldBe true
    }

    "exhaust the source data and return a list of converted entities when a trailing deliminator is missing" in {

      def sourceData(): Future[Enumerator[Array[Byte]]] = {
        val byteArray1: Array[Byte] = Array('a', 'b', ';', 'c', 'd', ';')
        val byteArray2: Array[Byte] = Array('e', 'f', ';', 'g', 'h', ';')
        val byteArray3: Array[Byte] = Array('i', 'j', ';', 'y', 'y')

        Future.successful(Enumerator(byteArray1, byteArray2, byteArray3))
      }

      val deliminator = ';'
      def converter(rawData: String): String = rawData

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      val result = await(bulkEntityProcessor.listf(sourceData(), deliminator, converter _))
      result.size shouldBe 6
      result.contains("ab") shouldBe true
      result.contains("cd") shouldBe true
      result.contains("ef") shouldBe true
      result.contains("gh") shouldBe true
      result.contains("ij") shouldBe true
      result.contains("yy") shouldBe true
    }
  }

  "BulkEntityList using a Iterator or Chars" should {
    "exhaust the source data and return a list of converted entities" in {

      def sourceData(): Iterator[Char] = {
        List[Char]('a', 'b', ';', 'c', 'd', ';', 'e', 'f', ';', 'g', 'h', ';', 'i', 'j', ';', 'k', 'l', ';').iterator
      }

      val deliminator = ';'
      def converter(rawData: String): String = rawData

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      val result = bulkEntityProcessor.list(sourceData(), deliminator, converter _)
      result.size shouldBe 6
      result.contains("ab") shouldBe true
      result.contains("cd") shouldBe true
      result.contains("ef") shouldBe true
      result.contains("gh") shouldBe true
      result.contains("ij") shouldBe true
      result.contains("kl") shouldBe true
    }

    "exhaust the source data and return a list of converted entities when a trailing deliminator is missing" in {

      def sourceData(): Iterator[Char] = {
        List[Char]('a', 'b', ';', 'c', 'd', ';', 'e', 'f', ';', 'g', 'h', ';', 'i', 'j', ';', 'y', 'y').iterator
      }

      val deliminator = ';'
      def converter(rawData: String): String = rawData

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      val result = bulkEntityProcessor.list(sourceData(), deliminator, converter _)
      result.size shouldBe 6
      result.contains("ab") shouldBe true
      result.contains("cd") shouldBe true
      result.contains("ef") shouldBe true
      result.contains("gh") shouldBe true
      result.contains("ij") shouldBe true
      result.contains("yy") shouldBe true
    }
  }
}
