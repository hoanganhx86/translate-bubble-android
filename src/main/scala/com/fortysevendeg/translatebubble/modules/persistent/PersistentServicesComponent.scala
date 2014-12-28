package com.fortysevendeg.translatebubble.modules.persistent

import com.fortysevendeg.translatebubble.service._
import com.fortysevendeg.translatebubble.utils.TranslateUIType._

trait PersistentServices {
  def getLanguages: Service[GetLanguagesRequest, GetLanguagesResponse]
  def disable30MinutesTranslation(): Unit
  def enableTranslation(): Unit
  def disableTranslation(): Unit
  def getTypeTranslateUI(): TypeTranslateUI
  def isTranslationEnable(): Boolean
  def isHeadsUpEnable(): Boolean
}

trait PersistentServicesComponent {
  val persistentServices: PersistentServices
}
