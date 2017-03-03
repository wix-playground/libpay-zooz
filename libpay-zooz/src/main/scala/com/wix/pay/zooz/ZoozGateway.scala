package com.wix.pay.zooz

import com.google.api.client.http._
import com.google.api.client.http.javanet.NetHttpTransport
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{Customer, Deal, Payment}
import com.wix.pay.zooz.ZoozGateway.productionEndpoint
import com.wix.pay.zooz.parsers.{JsonZoozAuthorizationParser, JsonZoozMerchantParser, ZoozAuthorizationParser, ZoozMerchantParser}
import com.wix.pay.{PaymentErrorException, PaymentGateway, PaymentRejectedException}
import org.json4s._
import org.json4s.native.JsonMethods.{parse, _}

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

    val ZoozMerchant(programId, programKey) = merchantParser.parse(merchantKey)

    val paymentToken = openPayment(programId = programId, programKey = programKey, payment, deal.get)
    val paymentMethodToken = addPaymentMethod(programId = programId, programKey = programKey, creditCard, customer, paymentToken)
    val authorizationCode = authorize(
      programId = programId,
      programKey = programKey,
      payment = payment,
      paymentToken = paymentToken,
      paymentMethodToken = paymentMethodToken
    )

    authorizationParser.stringify(ZoozAuthorization(authorizationCode))
  }

  override def capture(merchantKey: String, authorizationKey: String, amount: Double): Try[String] = ???

  override def sale(merchantKey: String, creditCard: CreditCard, payment: Payment, customer: Option[Customer], deal: Option[Deal]): Try[String] = ???

  override def voidAuthorization(merchantKey: String, authorizationKey: String): Try[String] = ???

  private def validateParams(creditCard: CreditCard, deal: Option[Deal]): Unit = {
    require(creditCard.csc.isDefined, "Credit Card CSC is mandatory for ZooZ!")
    require(creditCard.holderName.isDefined, "Credit Card holder name is mandatory for ZooZ!")
    require(deal.isDefined, "Deal is mandatory for ZooZ!")
    require(deal.get.invoiceId.isDefined, "Deal invoiceId is mandatory for ZooZ!")
  }

  private def openPayment(programId: String, programKey: String, payment: Payment, deal: Deal): String = {
    val content = requestBuilder.openPaymentRequest(payment, deal)
    val response = post(programId, programKey, content)
    (response \ "responseObject" \ "paymentToken").extract[String]
  }

  private def addPaymentMethod(programId: String,
                               programKey: String,
                               creditCard: CreditCard,
                               customer: Option[Customer],
                               paymentToken: String): String = {
    val content = requestBuilder.addPaymentMethodRequest(creditCard, customer, paymentToken)
    val response = post(programId, programKey, content)
    (response \ "responseObject" \ "paymentMethodToken").extract[String]
  }

  private def authorize(programId: String,
                        programKey: String,
                        payment: Payment,
                        paymentToken: String,
                        paymentMethodToken: String) = {
    val content = requestBuilder.authorizeRequest(payment, paymentToken = paymentToken, paymentMethodToken = paymentMethodToken)
    val response = post(programId, programKey, content)
    (response \ "responseObject" \ "authorizationCode").extract[String]
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

  private def withExceptionHandling(f: => String): Try[String] = {
    Try {
      f
    } recover {
      case e: PaymentRejectedException => throw e
      case e: PaymentErrorException => throw e
      case e => throw PaymentErrorException(e.getMessage, e)
    }
  }
}

object ZoozGateway {
  val productionEndpoint = "https://app.zooz.com"
  val sandboxEndpoint = "https://sandbox.zooz.com"
}