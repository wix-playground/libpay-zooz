package com.wix.pay.zooz

import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.Deal
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class ZoozGatewayTest extends SpecWithJUnit {
  "authorize request" should {
    "fail if credit card is missing CSC" in new ctx {
      authorize(creditCard = someCreditCard.withoutCsc) must
        failWithMessage("Credit Card CSC is mandatory for ZooZ!")
    }

    "fail if credit card is missing holderName" in new ctx {
      authorize(creditCard = someCreditCard.withoutHolderName) must
        failWithMessage("Credit Card holder name is mandatory for ZooZ!")
    }

    "fail if deal is missing" in new ctx {
      authorize(deal = None) must
        failWithMessage("Deal is mandatory for ZooZ!")
    }

    "fail if deal invoiceId is missing" in new ctx {
      authorize(deal = Some(someDeal.withoutInvoiceId)) must
        failWithMessage("Deal invoiceId is mandatory for ZooZ!")
    }
  }

  trait ctx extends Scope with ZoozTestSupport {
    val gateway = new ZoozGateway("")

    def authorize(creditCard: CreditCard = someCreditCard, deal: Option[Deal] = Some(someDeal)) =
      gateway.authorize(someMerchantStr, creditCard, somePayment, Some(someCustomer), deal)
  }
}
