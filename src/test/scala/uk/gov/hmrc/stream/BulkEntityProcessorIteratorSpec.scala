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

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

class BulkEntityProcessorIteratorSpec extends WordSpecLike with Matchers with FutureAwaits with DefaultAwaitTimeout with Eventually {
  "BulkEntityIterator using a Byte Array Enumerator" should {
    "exhaust the source data and return a list of converted entities" in {

      def sourceData(): Enumerator[Array[Byte]] = {
        val byteArray1: Array[Byte] = Array('a', 'b', ';', 'c', 'd', ';')
        val byteArray2: Array[Byte] = Array('e', 'f', ';', 'g', 'h', ';')
        val byteArray3: Array[Byte] = Array('i', 'j', ';', 'k', 'l', ';')

        Enumerator(byteArray1, byteArray2, byteArray3)
      }

      val deliminator = ';'
      def converter(rawData: String): String = rawData

      val counterInstance = new EntityCounterWithDataCheck
      def validateData = counterInstance.keepTrackOfCallCount(List("ab", "cd", "ef", "gh", "ij", "kl")) _

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      await(bulkEntityProcessor.usingEnumOfByteArrays(sourceData(), deliminator, converter).map {
        iterator => iterator.foreach(validateData)
      })

      counterInstance.counter shouldBe 6
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

      val counterInstance = new EntityCounterWithDataCheck
      def validateData = counterInstance.keepTrackOfCallCount(List("ab", "cd", "ef", "gh", "ij", "zz")) _

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      await(bulkEntityProcessor.usingEnumOfByteArrays(sourceData(), deliminator, converter).map {
        iterator => iterator.foreach(validateData)
      })

      counterInstance.counter shouldBe 6
    }

    "BulkEntityIterator using a Future of a Byte Array Enumerator" should {
      "exhaust the source data and return a list of converted entities" in {

        def sourceData(): Future[Enumerator[Array[Byte]]] = {
          val byteArray1: Array[Byte] = Array('a', 'b', ';', 'c', 'd', ';')
          val byteArray2: Array[Byte] = Array('e', 'f', ';', 'g', 'h', ';')
          val byteArray3: Array[Byte] = Array('i', 'j', ';', 'k', 'l', ';')

          Future.successful(Enumerator(byteArray1, byteArray2, byteArray3))
        }

        val deliminator = ';'
        def converter(rawData: String): String = rawData

        val counterInstance = new EntityCounterWithDataCheck
        def validateData = counterInstance.keepTrackOfCallCount(List("ab", "cd", "ef", "gh", "ij", "kl")) _

        val bulkEntityProcessor = new BulkEntityProcessor[String]()
        await(bulkEntityProcessor.usingFutureEnumOfByteArrays(sourceData(), deliminator, converter).map {
          iterator => iterator.foreach(validateData)
        })

        counterInstance.counter shouldBe 6
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

        val counterInstance = new EntityCounterWithDataCheck
        def validateData = counterInstance.keepTrackOfCallCount(List("ab", "cd", "ef", "gh", "ij", "yy")) _

        val bulkEntityProcessor = new BulkEntityProcessor[String]()
        await(bulkEntityProcessor.usingFutureEnumOfByteArrays(sourceData(), deliminator, converter).map {
          iterator => iterator.foreach(validateData)
        })

        counterInstance.counter shouldBe 6
      }
    }

    "BulkEntityIterator using a Iterator or Chars" should {
      "exhaust the source data and return a list of converted entities" in {

        def sourceData(): Iterator[Char] = {
          List[Char]('a', 'b', ';', 'c', 'd', ';', 'e', 'f', ';', 'g', 'h', ';', 'i', 'j', ';', 'k', 'l', ';').iterator
        }

        val deliminator = ';'
        def converter(rawData: String): String = rawData

        val counterInstance = new EntityCounterWithDataCheck
        def validateData = counterInstance.keepTrackOfCallCount(List("ab", "cd", "ef", "gh", "ij", "kl")) _
        val bulkEntityProcessor = new BulkEntityProcessor[String]()
        bulkEntityProcessor.usingIteratorOfChars(sourceData(), deliminator, converter).foreach(validateData)

        counterInstance.counter shouldBe 6
      }

      "exhaust the source data and return a list of converted entities when a trailing deliminator is missing" in {

        def sourceData(): Iterator[Char] = {
          List[Char]('a', 'b', ';', 'c', 'd', ';', 'e', 'f', ';', 'g', 'h', ';', 'i', 'j', ';', 'y', 'y').iterator
        }

        val deliminator = ';'
        def converter(rawData: String): String = rawData

        val counterInstance = new EntityCounterWithDataCheck
        def validateData = counterInstance.keepTrackOfCallCount(List("ab", "cd", "ef", "gh", "ij", "yy")) _

        val bulkEntityProcessor = new BulkEntityProcessor[String]()
        bulkEntityProcessor.usingIteratorOfChars(sourceData(), deliminator, converter).foreach(validateData)

        counterInstance.counter shouldBe 6
      }
    }
  }
}

private class EntityCounterWithDataCheck {
  var counter: Integer = 0

  def keepTrackOfCallCount(expectedData: List[String])(input: String): Unit = {
    counter = counter + 1
    if(!expectedData.contains(input)) throw new RuntimeException(s"Data [$input] was not expected!")
  }
}