package com.wix.pay.zooz

import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.Deal
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class ZoozGatewayTest extends SpecWithJUnit {
  "authorize request" should {
    "fail on invalid merchantKey format" in new ctx {
      authorize(merchantKey = "invalid") must beParseError
    }

    "fail if credit card is missing CSC" in new ctx {
      authorize(creditCard = someCreditCard.withoutCsc) must failWithMessage("Credit Card CSC is mandatory for ZooZ!")
    }

    "fail if credit card is missing holderName" in new ctx {
      authorize(creditCard = someCreditCard.withoutHolderName) must failWithMessage("Credit Card holder name is mandatory for ZooZ!")
    }

    "fail if deal is missing" in new ctx {
      authorize(deal = None) must failWithMessage("Deal is mandatory for ZooZ!")
    }

    "fail if deal invoiceId is missing" in new ctx {
      authorize(deal = Some(someDeal.withoutInvoiceId)) must failWithMessage("Deal invoiceId is mandatory for ZooZ!")
    }
  }

  "capture request" should {
    "fail on invalid merchantKey format" in new ctx {
      capture(merchantKey = "invalid") must beParseError
    }

    "fail on invalid authorizationKey format" in new ctx {
      capture(authorizationKey = "invalid") must beParseError
    }
  }

  "void request" should {
    "fail on invalid merchantKey format" in new ctx {
      void(merchantKey = "invalid") must beParseError
    }

    "fail on invalid authorizationKey format" in new ctx {
      void(authorizationKey = "invalid") must beParseError
    }
  }

  "sale request" should {
    "fail on invalid merchantKey format" in new ctx {
      sale(merchantKey = "invalid") must beParseError
    }

    "fail if credit card is missing CSC" in new ctx {
      sale(creditCard = someCreditCard.withoutCsc) must failWithMessage("Credit Card CSC is mandatory for ZooZ!")
    }

    "fail if credit card is missing holderName" in new ctx {
      sale(creditCard = someCreditCard.withoutHolderName) must failWithMessage("Credit Card holder name is mandatory for ZooZ!")
    }

    "fail if deal is missing" in new ctx {
      sale(deal = None) must failWithMessage("Deal is mandatory for ZooZ!")
    }

    "fail if deal invoiceId is missing" in new ctx {
      sale(deal = Some(someDeal.withoutInvoiceId)) must failWithMessage("Deal invoiceId is mandatory for ZooZ!")
    }
  }

  trait ctx extends Scope with ZoozTestSupport {
    val gateway = new ZoozGateway("")

    def authorize(merchantKey: String = someMerchantStr, creditCard: CreditCard = someCreditCard, deal: Option[Deal] = Some(someDeal)) =
      gateway.authorize(merchantKey, creditCard, somePayment, Some(someCustomer), deal)

    def capture(merchantKey: String = someMerchantStr, authorizationKey: String = authorization(authorizationCode, paymentToken)) =
      gateway.capture(merchantKey, authorizationKey, somePayment.currencyAmount.amount)

    def void(merchantKey: String = someMerchantStr, authorizationKey: String = authorization(authorizationCode, paymentToken)) =
      gateway.voidAuthorization(merchantKey, authorizationKey)

    def sale(merchantKey: String = someMerchantStr, creditCard: CreditCard = someCreditCard, deal: Option[Deal] = Some(someDeal)) =
      gateway.sale(merchantKey, creditCard, somePayment, Some(someCustomer), deal)
  }
}
