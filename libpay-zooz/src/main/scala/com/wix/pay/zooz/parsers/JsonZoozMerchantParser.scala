package com.wix.pay.zooz.parsers

import com.wix.pay.zooz.ZoozMerchant
import org.json4s.DefaultFormats
import org.json4s.native.Serialization

object JsonZoozMerchantParser extends ZoozMerchantParser {
  private implicit val formats = DefaultFormats

  override def parse(merchantKey: String): ZoozMerchant = {
    Serialization.read[ZoozMerchant](merchantKey)
  }

  override def stringify(merchant: ZoozMerchant): String = {
    Serialization.write(merchant)
  }
}
