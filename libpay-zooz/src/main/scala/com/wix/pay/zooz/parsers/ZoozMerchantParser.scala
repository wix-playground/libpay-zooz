package com.wix.pay.zooz.parsers

import com.wix.pay.zooz.ZoozMerchant

trait ZoozMerchantParser {
  def parse(merchantKey: String): ZoozMerchant
  def stringify(merchant: ZoozMerchant): String
}
