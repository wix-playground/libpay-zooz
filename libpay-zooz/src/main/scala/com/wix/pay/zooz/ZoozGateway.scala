package com.wix.pay.zooz

import com.google.api.client.http._
import com.google.api.client.http.javanet.NetHttpTransport
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{Customer, Deal, Payment}
import com.wix.pay.zooz.ZoozGateway.productionEndpoint
import com.wix.pay.zooz.parsers.{JsonZoozAuthorizationParser, JsonZoozMerchantParser, ZoozAuthorizationParser, ZoozMerchantParser}
import com.wix.pay.{PaymentErrorException, PaymentGateway, PaymentRejectedException}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.util.Try

class ZoozGateway(endpointUrl: String = productionEndpoint,
                  requestFactory: HttpRequestFactory = new NetHttpTransport().createRequestFactory(),
                  requestBuilder: ZoozRequestBuilder = new ZoozRequestBuilder(),
                  merchantParser: ZoozMerchantParser = JsonZoozMerchantParser,
                  authorizationParser: ZoozAuthorizationParser = JsonZoozAuthorizationParser) extends PaymentGateway {
  private implicit val formats = DefaultFormats

  override def authorize(merchantKey: String,
                         creditCard: CreditCard,
                         payment: Payment,
                         customer: Option[Customer],
                         deal: Option[Deal]): Try[String] = withExceptionHandling {
    validateParams(creditCard, deal)

    val context = new RequestContext(merchantKey)

    val paymentToken = context.openPayment(payment, deal.get)
    val paymentMethodToken = context.addPaymentMethod(creditCard, customer, paymentToken)
    val authorizationCode = context.authorize(payment = payment, paymentToken = paymentToken, paymentMethodToken = paymentMethodToken)

    authorizationParser.stringify(ZoozAuthorization(authorizationCode = authorizationCode, paymentToken = paymentToken))
  }

  override def capture(merchantKey: String, authorizationKey: String, amount: Double): Try[String] = withExceptionHandling {
    val context = new RequestContext(merchantKey)
    val authorization = authorizationParser.parse(authorizationKey)

    val captureCode = context.capture(authorization.paymentToken, amount)
    captureCode
  }

  override def sale(merchantKey: String, creditCard: CreditCard, payment: Payment, customer: Option[Customer], deal: Option[Deal]): Try[String] = ???

  override def voidAuthorization(merchantKey: String, authorizationKey: String): Try[String] = ???

  private def validateParams(creditCard: CreditCard, deal: Option[Deal]): Unit = {
    require(creditCard.csc.isDefined, "Credit Card CSC is mandatory for ZooZ!")
    require(creditCard.holderName.isDefined, "Credit Card holder name is mandatory for ZooZ!")
    require(deal.isDefined, "Deal is mandatory for ZooZ!")
    require(deal.get.invoiceId.isDefined, "Deal invoiceId is mandatory for ZooZ!")
  }

  private def withExceptionHandling(f: => String): Try[String] = {
    Try {
      f
    } recover {
      case e: PaymentRejectedException => throw e
      case e: PaymentErrorException => throw e
      case e => throw PaymentErrorException(e.getMessage, e)
    }
  }

  private class RequestContext(merchantKey: String) {
    private val ZoozMerchant(programId, programKey) = merchantParser.parse(merchantKey)

    def openPayment(payment: Payment, deal: Deal): String = {
      val content = requestBuilder.openPaymentRequest(payment, deal)
      getToken(programId, programKey, content, "paymentToken")
    }

    def addPaymentMethod(creditCard: CreditCard,
                                 customer: Option[Customer],
                                 paymentToken: String): String = {
      val content = requestBuilder.addPaymentMethodRequest(creditCard, customer, paymentToken)
      getToken(programId, programKey, content, "paymentMethodToken")
    }

    def authorize(payment: Payment,
                          paymentToken: String,
                          paymentMethodToken: String): String = {
      val content = requestBuilder.authorizeRequest(payment, paymentToken = paymentToken, paymentMethodToken = paymentMethodToken)
      getToken(programId, programKey, content, "authorizationCode")
    }

    def capture(paymentToken: String, amount: Double): String = {
      val content = requestBuilder.captureRequest(paymentToken, amount)
      getToken(programId, programKey, content, "captureCode")
    }

    private def getToken(programId: String, programKey: String, content: JObject, tokenFieldName: String): String = {
      val response = post(programId, programKey, content)
      (response \ "responseObject" \ tokenFieldName).extract[String]
    }

    private def post(programId: String, programKey: String, content: JObject): JValue = {
      val response = requestFactory.buildPostRequest(
        new GenericUrl(s"$endpointUrl/mobile/ZooZPaymentAPI"),
        new ByteArrayContent("application/json", compact(render(content)).getBytes("UTF-8"))
      ).setHeaders(new HttpHeaders()
        .set("ZooZUniqueID", programId)
        .set("ZooZAppKey", programKey)
        .set("ZooZResponseType", "JSon")
      ).execute()

      val responseContent = parse(response.getContent)
      assertResponseOk(responseContent)
      responseContent
    }

    private def assertResponseOk(responseContent: JValue): Unit = {
      val responseStatus = (responseContent \ "responseStatus").extract[Int]
      if (responseStatus != 0) {
        val responseObject = responseContent \ "responseObject"
        val errorMessage = (responseObject \ "errorMessage").extract[String]
        val processorError = responseObject \ "processorError"
        if (processorError != JNothing) {
          val declineReason = processorError \ "declineReason"
          val message = if (declineReason != JNothing) {
            declineReason.extract[String]
          } else {
            s"$errorMessage | declineCode: ${processorError \ "declineCode"}"
          }
          throw PaymentRejectedException(message)
        } else {
          throw PaymentErrorException(errorMessage)
        }
      }
    }
  }
}

object ZoozGateway {
  val productionEndpoint = "https://app.zooz.com"
  val sandboxEndpoint = "https://sandbox.zooz.com"
}