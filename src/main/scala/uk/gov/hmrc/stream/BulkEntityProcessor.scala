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

import play.api.libs.iteratee.{Enumerator, Iteratee}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait BulkEntityProcessing[E] extends EntityCharProcessing[E] {
  def list(source: => Enumerator[Array[Byte]], deliminator: Char, convertToEntity: String => E): Future[List[E]]

  def listf(sourcef: => Future[Enumerator[Array[Byte]]], deliminator: Char, convertToEntity: String => E): Future[List[E]]

  def list(source: => Iterator[Char], deliminator: Char, convertToEntity: String => E): List[E]

  //TODO: Rename/restructure these functions to a more overloading approach
  def usingIteratorOfChars(source: => Iterator[Char], deliminator: Char, convertToEntity: String => E): Iterator[E]

  def usingEnumOfByteArrays(source: => Enumerator[Array[Byte]], deliminator: Char, convertToEntity: String => E): Future[Iterator[E]]

  def usingFutureEnumOfByteArrays(source: => Future[Enumerator[Array[Byte]]], deliminator: Char, convertToEntity: String => E): Future[Iterator[E]]
}

class BulkEntityProcessor[E] extends BulkEntityProcessing[E] {

  private def byteArrayToChar = Iteratee.getChunks[Array[Byte]].map(chunk => chunk).map(byteArrays => byteArrays.toIterator.map(byteArray => byteArray.map(_.toChar)))

  private def caterForMissingTrailingDeliminator(convertToEntity: String => E)(processingResult: EntityContainer[E]): List[E] = {
    if (processingResult.partialEntityData.nonEmpty) processingResult.constructedEntities :+ convertToEntity(processingResult.partialEntityData)
    else processingResult.constructedEntities
  }

  def list(source: => Enumerator[Array[Byte]], deliminator: Char, convertToEntity: String => E): Future[List[E]] = {
    val streamDataToEntities = processEntityData[E](deliminator, convertToEntity) _
    val tidyProcessedData = caterForMissingTrailingDeliminator(convertToEntity) _
    source run byteArrayToChar map {
      charArrayIterator =>
        val processedInputData = charArrayIterator.foldLeft(EntityContainer("", List[E]()))((entityRepo, streamCharacterArray) => {
          streamCharacterArray.foldLeft(entityRepo)((entityContainer, streamCharacter) => {
            streamDataToEntities(entityContainer, streamCharacter)
          })
        })
        tidyProcessedData(processedInputData)
    }
  }

  def listf(sourcef: => Future[Enumerator[Array[Byte]]], deliminator: Char, convertToEntity: String => E): Future[List[E]] = {
    sourcef flatMap {
      source =>
        list(source, deliminator, convertToEntity)
    }
  }

  def list(source: => Iterator[Char], deliminator: Char, convertToEntity: String => E): List[E] = {
    val streamDataToEntities = processEntityData[E](deliminator, convertToEntity) _
    val tidyProcessedData = caterForMissingTrailingDeliminator(convertToEntity) _
    val processedInputData = source.foldLeft(EntityContainer[E]("", List[E]()))((entityContainer, streamCharacter) => {
      streamDataToEntities(entityContainer, streamCharacter)
    })
    tidyProcessedData(processedInputData)
  }

  def usingIteratorOfChars(source: => Iterator[Char], deliminator: Char, convertToEntity: String => E): Iterator[E] = {
    new CharsIterator(source, deliminator, convertToEntity)
  }

  def usingEnumOfByteArrays(source: => Enumerator[Array[Byte]], deliminator: Char, convertToEntity: String => E): Future[Iterator[E]] = {
    val byteArrayToChar = Iteratee.getChunks[Array[Byte]].map(chunk => chunk).map(byteArrays => byteArrays.toIterator.map(byteArray => byteArray.map(_.toChar)))

    source run byteArrayToChar map {
      charArrayIterator =>
        new CharArraysIterator[E](charArrayIterator, deliminator, convertToEntity)
    }
  }

  def usingXML(source: => Enumerator[Array[Byte]], startElementSequence: String, endElement: String): Future[Iterator[String]] = {
    val byteArrayToChar = Iteratee.getChunks[Array[Byte]].map(chunk => chunk).map(byteArrays => byteArrays.toIterator.map(byteArray => byteArray.map(_.toChar)))

    source run byteArrayToChar map {
      charArrayIterator =>
        new CharArraysIteratorElementAware(charArrayIterator, startElementSequence, endElement)
    }
  }

  def usingXMLf(sourcef: => Future[Enumerator[Array[Byte]]], startElementSequence: String, endElement: String): Future[Iterator[String]] = {
    val byteArrayToChar = Iteratee.getChunks[Array[Byte]].map(chunk => chunk).map(byteArrays => byteArrays.toIterator.map(byteArray => byteArray.map(_.toChar)))

    sourcef flatMap {
      source => source run byteArrayToChar map {
        charArrayIterator =>
          new CharArraysIteratorElementAware(charArrayIterator, startElementSequence, endElement)
      }
    }
  }

  def usingFutureEnumOfByteArrays(source: => Future[Enumerator[Array[Byte]]], deliminator: Char, convertToEntity: String => E): Future[Iterator[E]] = {
    source.flatMap {
      usingEnumOfByteArrays(_, deliminator, convertToEntity)
    }
  }
}

private class CharArraysIteratorElementAware(source: Iterator[Array[Char]], startElementSequence: String, endElement: String) extends Iterator[String] {

  private val characterQueue = new scala.collection.mutable.Queue[Char]()
  private val unidentifiedCharacterQueue = new scala.collection.mutable.Queue[Char]()
  private val entityQueue = scala.collection.mutable.Queue[String]()

  override def hasNext: Boolean = source.hasNext || characterQueue.nonEmpty || entityQueue.nonEmpty

  override def next(): String = {
    val streamDataToEntities = processElementData(startElementSequence, endElement) _

    def processNextArray = source.next().foldLeft(ElementContainer(emptyUnidentifiedCharacterQueue(), emptyCharacterQueue(), List[String]()))((entityContainer, streamCharacter) => {
      streamDataToEntities(entityContainer, streamCharacter)
    })

    if (source.hasNext && entityQueue.isEmpty) {
      val latestEntityDataFetched = processNextArray
      latestEntityDataFetched.constructedEntities.foreach(entityQueue.enqueue(_))
      latestEntityDataFetched.partialXmlData.fold()(x => x.toCharArray.foreach(characterQueue.enqueue(_)))
      latestEntityDataFetched.unidentifiedCandidateData.fold()(z => z.toCharArray.foreach(unidentifiedCharacterQueue.enqueue(_)))
    }

    if (entityQueue.nonEmpty) return entityQueue.dequeueFirst(_ => true).getOrElse(throw new RuntimeException("The Iterator is out of sync! The hasNext returned true but the next function cannot find anything to return!"))
    if (entityQueue.isEmpty && !source.hasNext && characterQueue.nonEmpty) return emptyCharacterQueue().fold("")(x => x)
    next()
  }

  private def processElementData(startElementSequence: String, endElement: String)(entityContainer: ElementContainer[String], input: Char): ElementContainer[String] = {

    def allocateInputCharacter(entityContainer: ElementContainer[String], input: Char): ElementContainer[String] = {
      entityContainer match {
        case ElementContainer(None, Some(xml), _) => entityContainer.copy(partialXmlData = Some(xml.concat(input.toString)))
        case ElementContainer(Some(data), None, _) => entityContainer.copy(unidentifiedCandidateData = Some(data.concat(input.toString)))
        case _ => entityContainer.copy(unidentifiedCandidateData = Some(input.toString))
      }
    }

    def processAllocatedDataContainer(allocatedDataContainer: ElementContainer[String], startElementSequence: String, endElement: String): ElementContainer[String] = {
      allocatedDataContainer match {
        case ElementContainer(None, Some(partialXml), _) => {
          if (partialXml.contains(endElement)) {
            allocatedDataContainer.copy(None, None, allocatedDataContainer.constructedEntities :+ partialXml)
          } else {
            allocatedDataContainer
          }
        }
        case ElementContainer(Some(unidentifiedData), None, _) => {
          if (unidentifiedData.contains(startElementSequence)) {
            allocatedDataContainer.copy(None, substring(unidentifiedData, startElementSequence), allocatedDataContainer.constructedEntities)
          } else {
            allocatedDataContainer
          }
        }
        case _ => allocatedDataContainer
      }
    }

    def substring(input: String, startElementPattern: String): Option[String] = {
      if (input.contains(startElementPattern)) Some(input.substring(input.indexOf(startElementPattern))) else  None
    }
    processAllocatedDataContainer(allocateInputCharacter(entityContainer, input), startElementSequence, endElement)
  }

  private[stream] case class ElementContainer[String](unidentifiedCandidateData: Option[String], partialXmlData: Option[String], constructedEntities: List[String])

  private[stream] def emptyUnidentifiedCharacterQueue(): Option[String] = {
    val unidentifiedData = unidentifiedCharacterQueue.dequeueAll(_ => true).foldLeft("")((partialEntityString, character) => {
      partialEntityString + character.toString
    })
    if (!unidentifiedData.isEmpty) Some(unidentifiedData) else None
  }

  private[stream] def emptyCharacterQueue(): Option[String] = {
    val characterData = characterQueue.dequeueAll(_ => true).foldLeft("")((partialEntityString, character) => {
      partialEntityString + character.toString
    })
    if (!characterData.isEmpty) Some(characterData) else None
  }
}

private class CharArraysIterator[T](source: Iterator[Array[Char]], deliminator: Char, convertToEntity: String => T) extends Iterator[T] with EntityCharProcessing[T] with QueueSupport {
  implicit def convert(entityRawData: String): T = convertToEntity(entityRawData)

  override val characterQueue = new scala.collection.mutable.Queue[Char]()
  private val entityQueue = scala.collection.mutable.Queue[T]()

  override def hasNext: Boolean = source.hasNext || characterQueue.nonEmpty || entityQueue.nonEmpty

  override def next(): T = {
    val streamDataToEntities = processEntityData[T](deliminator, convertToEntity) _

    def processNextArray = source.next().foldLeft(EntityContainer(emptyCharacterQueue(), List[T]()))((entityContainer, streamCharacter) => {
      streamDataToEntities(entityContainer, streamCharacter)
    })

    // Process next Chunk only if there's nothing in the entityQueue to serve and unprocessed data is available
    if (source.hasNext && entityQueue.isEmpty) {
      val latestEntityDataFetched = processNextArray
      latestEntityDataFetched.constructedEntities.foreach(entityQueue.enqueue(_))
      latestEntityDataFetched.partialEntityData.toCharArray.foreach(characterQueue.enqueue(_))
    }

    if (entityQueue.nonEmpty) return entityQueue.dequeueFirst(_ => true).getOrElse(throw new RuntimeException("The Iterator is out of sync! The hasNext returned true but the next function cannot find anything to return!"))
    if (entityQueue.isEmpty && !source.hasNext && characterQueue.nonEmpty) return emptyCharacterQueue()

    next()
  }
}

private class CharsIterator[T](source: Iterator[Char], deliminator: Char, converter: String => T) extends Iterator[T] with QueueSupport {
  override val characterQueue = new scala.collection.mutable.Queue[Char]()

  implicit def convert(entityRawData: String): T = converter(entityRawData)

  override def hasNext: Boolean = {
    source.hasNext || characterQueue.nonEmpty
  }

  override def next(): T = {
    val nextCharacter = source.next()

    if (nextCharacter != deliminator) {
      characterQueue.enqueue(nextCharacter)
      if (source.hasNext) next() else emptyCharacterQueue()
    }
    else {
      emptyCharacterQueue()
    }
  }
}

private[stream] trait QueueSupport {
  private[stream] val characterQueue: scala.collection.mutable.Queue[Char]

  private[stream] def emptyCharacterQueue(): String = {
    characterQueue.dequeueAll(_ => true).foldLeft("")((partialEntityString, character) => {
      partialEntityString + character.toString
    })
  }
}

private[stream] case class EntityContainer[T](partialEntityData: String, constructedEntities: List[T])

private[stream] trait EntityCharProcessing[T] {
  def processEntityData[E](deliminator: Char, convertToEntity: String => E)(entityContainer: EntityContainer[E], input: Char): EntityContainer[E] = {
    val entities: List[E] = if (input != deliminator) {
      entityContainer.constructedEntities
    } else {
      val entityDataChars = entityContainer.partialEntityData.toString
      entityContainer.constructedEntities :+ convertToEntity(entityDataChars)
    }
    val entityCharacters: String = if (input != deliminator) {
      entityContainer.partialEntityData.concat(input.toString)
    } else {
      ""
    }
    EntityContainer(entityCharacters, entities)
  }
}
