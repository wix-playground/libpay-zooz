package com.wix.pay.zooz

import com.wix.pay.zooz.parsers.JsonZoozAuthorizationParser
import org.specs2.mutable.SpecWithJUnit

class JsonZoozAuthorizationParserTest extends SpecWithJUnit {
  val parser = JsonZoozAuthorizationParser
  val authorizationCode = "someAuthorizationCode"
  val paymentToken = "somePaymentToken"

  "stringify and then parse" should {
    "return an order similar to the original one" in {
      val authorization = ZoozAuthorization(authorizationCode, paymentToken)
      val authorizationKey = parser.stringify(authorization)

      parser.parse(authorizationKey) mustEqual authorization
    }
  }
}
