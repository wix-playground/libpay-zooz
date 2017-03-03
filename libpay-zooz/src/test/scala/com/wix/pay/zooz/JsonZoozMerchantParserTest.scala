package com.wix.pay.zooz

import com.wix.pay.zooz.parsers.JsonZoozMerchantParser
import org.specs2.mutable.SpecWithJUnit

class JsonZoozMerchantParserTest extends SpecWithJUnit {
  val parser = JsonZoozMerchantParser
  val programId = "someProgramId"
  val programKey = "someProgramKey"

  "stringify and then parse" should {
    "return an order similar to the original one" in {
      val merchant = ZoozMerchant(programId, programKey)
      val merchantKey = parser.stringify(merchant)

      parser.parse(merchantKey) mustEqual merchant
    }
  }
}
