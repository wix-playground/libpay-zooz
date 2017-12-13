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

      authorize() must beSuccessfulAuthorizationWith(authorizationCode, paymentToken)
    }

    "fail with PaymentRejectedException for rejected transactions" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) getsRejectedWith errorMessage

      authorize() must beRejectedWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when opening a payment fails" in new ctx {
      givenOpenPaymentRequest getsAnErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when opening a payment fails fatally" in new ctx {
      givenOpenPaymentRequest getsAFatalErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when adding a payment method fails" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) getsAnErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when adding a payment method fails fatally" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) getsAFatalErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when authorization fails" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) getsAnErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when authorization fails fatally" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) getsAFatalErrorWith errorMessage

      authorize() must failWithMessage(errorMessage)
    }
  }

  "capture request" should {
    "successfully yield a captureCode upon a valid request" in new ctx {
      givenCaptureRequest(paymentToken) returns captureCode

      capture(paymentToken) must succeedWith(captureCode)
    }

    "fail with PaymentRejectedException for rejected transactions" in new ctx {
      givenCaptureRequest(paymentToken) getsRejectedWith errorMessage

      capture(paymentToken) must beRejectedWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when capture fails" in new ctx {
      givenCaptureRequest(paymentToken) getsAnErrorWith errorMessage

      capture(paymentToken) must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when capture fails fatally" in new ctx {
      givenCaptureRequest(paymentToken) getsAFatalErrorWith errorMessage

      capture(paymentToken) must failWithMessage(errorMessage)
    }
  }

  "voidAuthorization request" should {
    "successfully yield a voidReferenceId upon a valid request" in new ctx {
      givenVoidRequest(paymentToken) returns voidReferenceId

      void(paymentToken) must succeedWith(voidReferenceId)
    }

    "fail with PaymentRejectedException for rejected transactions" in new ctx {
      givenVoidRequest(paymentToken) getsRejectedWith errorMessage

      void(paymentToken) must beRejectedWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when void fails" in new ctx {
      givenVoidRequest(paymentToken) getsAnErrorWith errorMessage

      void(paymentToken) must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when capture fails fatally" in new ctx {
      givenVoidRequest(paymentToken) getsAFatalErrorWith errorMessage

      void(paymentToken) must failWithMessage(errorMessage)
    }
  }

  "sale request" should {
    "successfully yield an authorization key upon a valid request" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) returns authorizationCode
      givenCaptureRequest(paymentToken) returns captureCode

      sale() must succeedWith(captureCode)
    }

    "fail with PaymentRejectedException when authorization is rejected" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) getsRejectedWith errorMessage

      sale() must beRejectedWithMessage(errorMessage)
    }

    "fail with PaymentRejectedException when capture is rejected" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) returns authorizationCode
      givenCaptureRequest(paymentToken) getsRejectedWith errorMessage

      sale() must beRejectedWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when opening a payment fails" in new ctx {
      givenOpenPaymentRequest getsAnErrorWith errorMessage

      sale() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when opening a payment fails fatally" in new ctx {
      givenOpenPaymentRequest getsAFatalErrorWith errorMessage

      sale() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when adding a payment method fails" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) getsAnErrorWith errorMessage

      sale() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when adding a payment method fails fatally" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) getsAFatalErrorWith errorMessage

      sale() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when authorization fails" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) getsAnErrorWith errorMessage

      sale() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when authorization fails fatally" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) getsAFatalErrorWith errorMessage

      sale() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when capture fails" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) returns authorizationCode
      givenCaptureRequest(paymentToken) getsAnErrorWith errorMessage

      sale() must failWithMessage(errorMessage)
    }

    "fail with PaymentErrorException when capture fails fatally" in new ctx {
      givenOpenPaymentRequest returns paymentToken
      givenAddPaymentMethodRequest(paymentToken) returns paymentMethodToken
      givenAuthorizationRequest(paymentToken, paymentMethodToken) returns authorizationCode
      givenCaptureRequest(paymentToken) getsAFatalErrorWith errorMessage

      sale() must failWithMessage(errorMessage)
    }
  }

  step {
    driver.stop()
  }

  trait ctx extends Scope with ZoozTestSupport {
    val gateway = new ZoozGateway(s"http://localhost:$probePort")

    driver.reset()

    def givenOpenPaymentRequest = driver.anOpenPaymentRequest(programId, programKey, somePayment, someDeal)

    def givenAddPaymentMethodRequest(paymentToken: String) = driver.anAddPaymentMethodRequest(programId, programKey, someCreditCard, someCustomer, paymentToken)

    def givenAuthorizationRequest(paymentToken: String, paymentMethodToken: String) =
      driver.anAuthorizationRequest(programId, programKey, somePayment, paymentToken, paymentMethodToken)

    def givenCaptureRequest(paymentToken: String) = driver.aCaptureRequest(programId, programKey, somePayment, paymentToken)

    def givenVoidRequest(paymentToken: String) = driver.aVoidRequest(programId, programKey, paymentToken)

    def authorize() = gateway.authorize(someMerchantStr, someCreditCard, somePayment, Some(someCustomer), Some(someDeal))
    def capture(paymentToken: String) = gateway.capture(someMerchantStr, authorization(authorizationCode, paymentToken), somePayment.currencyAmount.amount)
    def void(paymentToken: String) = gateway.voidAuthorization(someMerchantStr, authorization(authorizationCode, paymentToken))
    def sale() = gateway.sale(someMerchantStr, someCreditCard, somePayment, Some(someCustomer), Some(someDeal))
  }
}
