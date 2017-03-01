package com.wix.pay.zooz.parsers

import com.wix.pay.zooz.ZoozAuthorization

trait ZoozAuthorizationParser {
  def parse(authorizationKey: String): ZoozAuthorization
  def stringify(authorization: ZoozAuthorization): String
}