package uk.gov.hmrc.stream

import java.io.File
import java.util.concurrent.TimeUnit

import org.scalatest.concurrent.Eventually
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.iteratee.Enumerator
import play.api.test.{DefaultAwaitTimeout, FutureAwaits}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

class BulkEntityElementIteratorSpec extends WordSpecLike with Matchers with FutureAwaits with DefaultAwaitTimeout with Eventually {
  "Element Iterator" should {
    "return the element data specified only" in {
      def sourceData(): Enumerator[Array[Byte]] = {
        //<table:table table:name table:name="Other_Grants_V2" blah blah blah blah </table:table>
        val byteArray0: Array[Byte] = Array('<', 'x', 'a', 'b', 'l', 'e', ':', 't', 'a', 'b', 'l', 'e', ' ')
        val byteArray1: Array[Byte] = Array('<', 't', 'a', 'b', 'l', 'e', ':', 't', 'a', 'b', 'l', 'e', ' ')
        val byteArray2: Array[Byte] = Array('b', 'l', 'a', 'h', 'b', 'l', 'a', 'h', 'A')
        val byteArray3: Array[Byte] = Array('<', '/', 't', 'a', 'b', 'l', 'e', '>')
        val byteArray4: Array[Byte] = Array('<', 't', 'a', 'b', 'l', 'e', ':', 't', 'a', 'b', 'l', 'e', ' ')
        val byteArray5: Array[Byte] = Array('b', 'l', 'a', 'h', 'b', 'l', 'a', 'h', 'B')
        val byteArray6: Array[Byte] = Array('<', '/', 't', 'a', 'b', 'l', 'e', '>', 'Z')

        Enumerator(byteArray0, byteArray1, byteArray2, byteArray3, byteArray4, byteArray5, byteArray6)
      }

      val element = "<table:table"
      val endElementPattern = "</table>"
      def converter(rawData: String): String = rawData

      val counterInstance = new EntityCounterWithDataCheck
//      def validateData = counterInstance.keepTrackOfCallCount(List("ab", "cd", "ef", "gh", "ij", "kl")) _

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      await(bulkEntityProcessor.usingXML(sourceData(), element, endElementPattern).map {
        iterator => iterator.foreach(x => println("Consumer Data [ " + x + " ]"))
      })
    }

    "return the element data specified only from a file" in {
      def sourceData(): Enumerator[Array[Byte]] = {
        //<table:table table:name table:name="Other_Grants_V2" blah blah blah blah </table:table>
        val byteArray0: Array[Byte] = Array('<', 'x', 'a', 'b', 'l', 'e', ':', 't', 'a', 'b', 'l', 'e', ' ')
        val byteArray1: Array[Byte] = Array('<', 't', 'a', 'b', 'l', 'e', ':', 't', 'a', 'b', 'l', 'e', ' ')
        val byteArray2: Array[Byte] = Array('b', 'l', 'a', 'h', 'b', 'l', 'a', 'h', 'A')
        val byteArray3: Array[Byte] = Array('<', '/', 't', 'a', 'b', 'l', 'e', '>')
        val byteArray4: Array[Byte] = Array('<', 't', 'a', 'b', 'l', 'e', ':', 't', 'a', 'b', 'l', 'e', ' ')
        val byteArray5: Array[Byte] = Array('b', 'l', 'a', 'h', 'b', 'l', 'a', 'h', 'B')
        val byteArray6: Array[Byte] = Array('<', '/', 't', 'a', 'b', 'l', 'e', '>', 'Z')

        Enumerator(byteArray0, byteArray1, byteArray2, byteArray3, byteArray4, byteArray5, byteArray6)
      }

      // /Users/davesammut/Downloads/content.xml

      def source3() = Enumerator.fromFile(new File("/Users/davesammut/Downloads/content_subset.xml"))

      val startElementPattern = "<table:table-row"
      val endElementPattern = "</table:table-row>"
      def converter(rawData: String): String = rawData

      val counterInstance = new EntityCounterWithDataCheck
      //      def validateData = counterInstance.keepTrackOfCallCount(List("ab", "cd", "ef", "gh", "ij", "kl")) _

      val bulkEntityProcessor = new BulkEntityProcessor[String]()
      await(bulkEntityProcessor.usingXML(source3(), startElementPattern, endElementPattern).map {
        iterator => iterator.foreach(x => println("Consumer Data [ " + x + " ]"))
      }, 4, TimeUnit.MINUTES)
    }
  }
}
