package com.wix.pay.zooz

import com.wix.pay.zooz.testkit.ZoozDriver
import org.specs2.mutable.SpecWithJUnit
import org.specs2.specification.Scope

class ZoozGatewayIT extends SpecWithJUnit {
  val probePort = 10001
  val driver = new ZoozDriver(probePort)

  step {
    driver.start()
  }

  sequential

  "authorize request" should {
    "successfully yield an authorization key upon a valid request" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) returns authorizationCode

      authorize() must beSuccessfulAuthorizationWith(authorizationCode)
    }

    "fail with PaymentRejectedException for rejected transactions" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) isRejectedWith errorMessage

      authorize() must beRejectedWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when opening a payment fails" in new ctx {
      givenOpenPaymentRequest isAnErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when adding a payment method fails" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) isAnErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when authorization fails" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) isAnErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }
  }

  step {
    driver.stop()
  }

  trait ctx extends Scope with ZoozTestSupport {
    private val requestBuilder = new ZoozRequestBuilder(customerLoginIdGenerator = customerLoginId)
    val gateway = new ZoozGateway(s"http://localhost:$probePort", requestBuilder = requestBuilder)

    driver.reset()

    def givenOpenPaymentRequest = driver.anOpenPaymentRequest(programId, programKey, payment, someDeal, customerLoginId)

    def givenAddPaymentMethodRequest(paymentToken: String) = driver.anAddPaymentMethodRequest(programId, programKey, someCreditCard, someCustomer, paymentToken)

    def givenAuthorizationRequest(paymentToken: String, paymentMethodToken: String) =
      driver.anAuthorizationRequest(programId, programKey, payment, paymentToken, paymentMethodToken)

    def authorize() = gateway.authorize(someMerchantStr, someCreditCard, payment, Some(someCustomer), Some(someDeal))
  }
}
