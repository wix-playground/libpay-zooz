package com.wix.pay.zooz

import com.wix.pay.zooz.parsers.JsonZoozAuthorizationParser
import org.specs2.mutable.SpecWithJUnit

class JsonZoozAuthorizationParserTest extends SpecWithJUnit {
  val parser = JsonZoozAuthorizationParser
  val authorizationCode = "someAuthorizationCode"

  "stringify and then parse" should {
    "return an order similar to the original one" in {
      val authorization = ZoozAuthorization(authorizationCode)
      val authorizationKey = parser.stringify(authorization)

      parser.parse(authorizationKey) mustEqual authorization
    }
  }
}
