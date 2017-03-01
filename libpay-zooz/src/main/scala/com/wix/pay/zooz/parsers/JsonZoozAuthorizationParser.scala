package com.wix.pay.zooz.parsers

import com.wix.pay.zooz.ZoozAuthorization
import org.json4s.DefaultFormats
import org.json4s.native.Serialization

object JsonZoozAuthorizationParser extends ZoozAuthorizationParser {
  private implicit val formats = DefaultFormats

  override def parse(authorizationKey: String): ZoozAuthorization = {
    Serialization.read[ZoozAuthorization](authorizationKey)
  }

  override def stringify(authorization: ZoozAuthorization): String = {
    Serialization.write(authorization)
  }
}
