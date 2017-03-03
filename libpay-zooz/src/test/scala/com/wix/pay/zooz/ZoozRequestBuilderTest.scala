package com.wix.pay.zooz

import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.Customer
import org.json4s.{JObject, JString}
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class ZoozRequestBuilderTest extends SpecWithJUnit {
  "ZoozRequestBuilder" should {
    "call customerLoginIdGenerator for customerLoginId" in new ctx {
      openPaymentRequest().customerLoginId must beGeneratedCustomerLoginId
    }

    "use the default email value for email when customer is missing" in new ctx {
      addPaymentMethodRequest(withCustomer = None).email must beDefaultEmail
    }

    "use the default email value for email when customer email is missing" in new ctx {
      addPaymentMethodRequest(withCustomer = Some(someCustomer.withoutEmail)).email must beDefaultEmail
    }

    "use the default email value for email when customer email is empty" in new ctx {
      addPaymentMethodRequest(withCustomer = Some(someCustomer.withEmail(""))).email must beDefaultEmail
    }
  }

  trait ctx extends Scope with ZoozTestSupport {
    val defaultEmail = randomStringWithLength(6)
    val builder = new ZoozRequestBuilder(customerLoginId, defaultEmail)

    def openPaymentRequest() = builder.openPaymentRequest(somePayment, someDeal)

    def addPaymentMethodRequest(withCreditCard: CreditCard = someCreditCard, withCustomer: Option[Customer] = Some(someCustomer)) =
      builder.addPaymentMethodRequest(withCreditCard, withCustomer, paymentToken)

    def beGeneratedCustomerLoginId = beEqualTo(JString(customerLoginId))
    def beDefaultEmail = beEqualTo(JString(defaultEmail))

    implicit class JObjectTestExtensions(o: JObject) {
      def email = o \ "email"

      def customerLoginId = o \ "customerDetails" \ "customerLoginID"
    }
  }
}
