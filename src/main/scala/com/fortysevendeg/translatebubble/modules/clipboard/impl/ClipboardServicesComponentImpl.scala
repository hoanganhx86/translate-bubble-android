/*
 * Copyright (C) 2015 47 Degrees, LLC http://47deg.com hello@47deg.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.fortysevendeg.translatebubble.modules.clipboard.impl

import android.content.{ClipData, ClipboardManager, Context}
import com.fortysevendeg.translatebubble.commons.ContextWrapperProvider
import com.fortysevendeg.translatebubble.modules.clipboard._
import com.fortysevendeg.translatebubble.service._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

trait ClipboardServicesComponentImpl
  extends ClipboardServicesComponent {

  self: ContextWrapperProvider =>

  lazy val clipDataBuilder = new ClipDataBuilder

  lazy val clipboardServices = new ClipboardServicesImpl

  class ClipboardServicesImpl extends ClipboardServices {

    private[this] val urlPattern = "(\\b(https?|ftp|file|ldap)://)[-A-Za-z0-9+&@#/%?=~_|!:,.;]*[-A-Za-z0-9+&@#/%=~_|]".r

    val millisInterval = 1000

    var lastDate: Long = 0

    var clipChangedListener: Option[ClipboardManager.OnPrimaryClipChangedListener] = None

    val clipboardManager: ClipboardManager = {
      contextProvider.application.getSystemService(Context.CLIPBOARD_SERVICE).asInstanceOf[ClipboardManager]
    }

    private def getClipboardManagerPrimaryClip: Option[ClipData] = Option(clipboardManager.getPrimaryClip)

    private def getPrimaryClipItem(clipData: ClipData): Option[ClipData.Item] = Option(clipData.getItemAt(0))

    private def getClipDataItemText(clipDataItem: ClipData.Item): Option[CharSequence] = Option(clipDataItem.getText)

    override def isValidCall: Boolean = {

      val currentMillis = System.currentTimeMillis()

      val currentInterval = currentMillis - lastDate

      val result = for {
        clipData <- getClipboardManagerPrimaryClip
        clipDataItem <- getPrimaryClipItem(clipData)
        text <- getClipDataItemText(clipDataItem)
      } yield text

      result match {
        case Some(text) if isValidText(text.toString) => lastDate = currentMillis; currentInterval > millisInterval
        case _ => false
      }
    }

    override def getText: Service[GetTextClipboardRequest, GetTextClipboardResponse] = request =>
      Future {

        val result = for {
          clipData <- getClipboardManagerPrimaryClip
          clipDataItem <- getPrimaryClipItem(clipData)
          text <- getClipDataItemText(clipDataItem)
        } yield text

        result match {
          case Some(text) if text.toString.length > 0 => GetTextClipboardResponse(Some(text.toString))
          case _ => GetTextClipboardResponse(None)
        }
      }

    override def copyToClipboard: Service[CopyToClipboardRequest, CopyToClipboardResponse] = request =>
      Future {
        val clip = clipDataBuilder.newPlainText(request.text)
        clipboardManager.setPrimaryClip(clip)
        CopyToClipboardResponse()
      }

    def init(listener: ClipboardManager.OnPrimaryClipChangedListener): Unit = {
      clipChangedListener foreach clipboardManager.removePrimaryClipChangedListener
      clipChangedListener = Some(listener)
      clipboardManager.addPrimaryClipChangedListener(listener)
    }

    def destroy(): Unit = {
      clipChangedListener foreach clipboardManager.removePrimaryClipChangedListener
      clipChangedListener = None
    }

    def reset(): Unit = lastDate = 0

    def isValidText(text: String): Boolean = text.trim.nonEmpty && !isTextANumber(text) && !isTextAUrl(text)

    private[this] def isTextANumber(text: String): Boolean = Try(text.toDouble).isSuccess

    private[this] def isTextAUrl(text: String): Boolean = urlPattern.pattern.matcher(text).matches
  }

}

class ClipDataBuilder {

  def newPlainText(text: String) = ClipData.newPlainText("label", text)

}