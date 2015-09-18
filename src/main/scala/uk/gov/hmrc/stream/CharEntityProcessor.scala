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
import uk.gov.hmrc.play.http.logging.RequestId

import scala.collection.mutable
import scala.collection.mutable.HashMap

trait CharEntityProcessor[T] extends EntityConverter[T] {

  def processCharacter(deliminator: Char, entityProcessor: T => Unit)(characterFromStream: Char)(implicit hc: HeaderCarrier): Unit = {
    entityLocator(deliminator)(characterFromStream) match {
      case Some(entityFound) => entityProcessor(entityFound.toString())
      case None => //do nothing
    }
  }

  def parseEntity(deliminator: Char)(characterFromStream: Char)(implicit hc: HeaderCarrier): Option[T] = {
    entityLocator(deliminator)(characterFromStream) match {
      case Some(entityFound) => Some(entityFound.toString())
      case None => None
    }
  }

  def flush()(implicit hc: HeaderCarrier): T = {
    entityCharacterAccumulator.remove(requestId).fold("")(_.toString())
  }

  def isEntityDataAvailable()(implicit hc: HeaderCarrier): Boolean = {
    entityCharacterAccumulator.get(requestId).fold(false)(data => data.nonEmpty)
  }

  private val entityCharacterAccumulator = new mutable.HashMap[RequestId, StringBuilder]

  private def entityLocator(deliminator: Char)(characterFromStream: Char)(implicit hc: HeaderCarrier): Option[StringBuilder] = {
    if (characterFromStream == deliminator) {
      entityCharacterAccumulator.remove(requestId)
    } else {
      entityCharacterAccumulator.get(requestId) match {
        case Some(partialEntity) => entityCharacterAccumulator put(requestId, partialEntity.append(characterFromStream.toString))
        case None => entityCharacterAccumulator put(requestId, new StringBuilder(characterFromStream.toString))
      }
      None
    }
  }

  private def requestId(implicit hc: HeaderCarrier): RequestId = {
    hc.requestId.getOrElse(throw new RuntimeException("Unable to process file. RequestId is missing!"))
  }
}
