package com.wix.pay.zooz.testkit

import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{Customer, Deal, Payment}
import org.json4s.DefaultFormats
import org.json4s.native.Serialization
import spray.http.Uri.Path
import spray.http._

import scala.util.Random

class ZoozDriver(port: Int) {
  private val probe = new EmbeddedHttpProbe(port, EmbeddedHttpProbe.NotFoundHandler)

  def reset(): Unit = probe.reset()

  def start(): Unit = probe.doStart()

  def stop(): Unit = probe.doStop()

  def anOpenPaymentRequest(programId: String,
                           programKey: String,
                           payment: Payment,
                           deal: Deal,
                           customerLoginId: String) =
    OpenPaymentRequest(programId, programKey, payment, deal, customerLoginId)

  def anAddPaymentMethodRequest(programId: String,
                                programKey: String,
                                creditCard: CreditCard,
                                customer: Customer,
                                paymentToken: String) =
    AddPaymentMethodRequest(programId, programKey, creditCard, customer, paymentToken)

  def anAuthorizationRequest(programId: String,
                             programKey: String,
                             payment: Payment,
                             paymentToken: String,
                             paymentMethodToken: String) =
    AuthorizationRequest(programId, programKey, payment, paymentToken, paymentMethodToken)

  def aCaptureRequest(programId: String,
                      programKey: String,
                      payment: Payment,
                      paymentToken: String) =
    CaptureRequest(programId, programKey, payment, paymentToken)

  def aVoidRequest(programId: String,
                   programKey: String,
                   paymentToken: String) =
    VoidRequest(programId, programKey, paymentToken)

  abstract class ZoozRequest(programId: String, programKey: String) {
    protected def expectedJsonBody: Map[String, Any]

    def isAnErrorWith(errorMessage: String): Unit = respondWith(errorResponse(errorMessage))

    def isAFatalErrorWith(errorMessage: String, statusCode: StatusCode = StatusCodes.BadRequest): Unit =
      respondWith(errorResponse(errorMessage), statusCode)

    private def errorResponse(errorMessage: String) = Map(
      "responseStatus" -> -1,
      "responseObject" -> Map(
        "errorMessage" -> errorMessage,
        "errorDescription" -> errorMessage,
        "responseErrorCode" -> Random.nextInt(999999)
      )
    )

    protected def respondWith(content: Map[String, Any], status: StatusCode = StatusCodes.OK): Unit = {
      probe.handlers += {
        case HttpRequest(HttpMethods.POST, uri, headers, entity, _)
          if uri.path == Path("/mobile/ZooZPaymentAPI") && isJson(headers) && isAuthorized(headers) && isStubbedEntity(entity) =>
          HttpResponse(status = status, entity = toJson(content))
      }
    }

    private def isJson(headers: List[HttpHeader]): Boolean = headerExists(headers, "Content-Type", "application/json")

    private def isAuthorized(headers: List[HttpHeader]): Boolean = {
      headerExists(headers, "ZooZUniqueID", programId) && headerExists(headers, "ZooZAppKey", programKey)
    }

    private def headerExists(headers: List[HttpHeader], name: String, value: String) = headers.exists { header =>
      header.name.equalsIgnoreCase(name) && header.value.equalsIgnoreCase(value)
    }

    protected def isStubbedEntity(entity: HttpEntity): Boolean = {
      val actual = toMap(entity)
      val expected = expectedJsonBody
      actual == expected
    }

    implicit val formats = DefaultFormats
    protected def toJson(map: Map[String, Any]): String = Serialization.write(map)
    private def toMap(entity: HttpEntity): Map[String, Any] = Serialization.read[Map[String, Any]](entity.asString)

    protected def randomStringWithLength(length: Int): String = Random.alphanumeric.take(length).mkString
  }

  abstract class ZoozRejectableRequest(programId: String, programKey: String) extends ZoozRequest(programId, programKey) {
    def isRejectedWith(reason: String): Unit = respondWith(rejectResponse(reason))

    private def rejectResponse(reason: String) = Map(
      "responseStatus" -> -1,
      "responseObject" -> Map(
        "processorError" -> Map(
          "processorName" -> "N/A",
          "declineCode" -> Random.nextInt(999),
          "declineReason" -> reason
        ),
        "errorMessage" -> Random.nextString(20),
        "errorDescription" -> Random.nextString(20),
        "responseErrorCode" -> Random.nextInt(999999)
      )
    )
  }

  case class OpenPaymentRequest(programId: String,
                                programKey: String,
                                payment: Payment,
                                deal: Deal,
                                customerLoginId: String)
    extends ZoozRequest(programId, programKey) {

    override protected def expectedJsonBody = Map(
      "command" -> "openPayment",
      "paymentDetails" -> Map(
        "amount" -> payment.currencyAmount.amount,
        "currencyCode" -> payment.currencyAmount.currency
      ),
      "customerDetails" -> Map(
        "customerLoginID" -> customerLoginId
      ),
      "invoice" -> Map(
        "number" -> deal.invoiceId.get
      )
    )

    def returns(paymentToken: String): Unit = respondWith(validResponse(paymentToken))

    private def validResponse(paymentToken: String) = Map(
      "responseStatus" -> 0,
      "responseObject" -> Map(
        "paymentToken" -> paymentToken,
        "paymentId" -> randomStringWithLength(26)
      )
    )
  }

  case class AddPaymentMethodRequest(programId: String,
                                     programKey: String,
                                     creditCard: CreditCard,
                                     customer: Customer,
                                     paymentToken: String)
    extends ZoozRequest(programId, programKey) {

    override protected def expectedJsonBody: Map[String, Any] = Map(
      "command" -> "addPaymentMethod",
      "paymentToken" -> paymentToken,
      "email" -> customer.email.get,
      "paymentMethod" -> Map(
        "paymentMethodType" -> "CreditCard",
        "paymentMethodDetails" -> Map(
          "cardNumber" -> creditCard.number,
          "expirationDate" -> s"${creditCard.expiration.month}/${creditCard.expiration.year}",
          "cvvNumber" -> creditCard.csc.get,
          "cardHolderName" -> creditCard.holderName.get
        )
      ),
      "configuration" -> Map(
        "rememberPaymentMethod" -> false
      )
    )

    def returns(paymentMethodToken: String): Unit = respondWith(validResponse(paymentMethodToken))

    private def validResponse(paymentMethodToken: String) = Map(
      "responseStatus" -> 0,
      "responseObject" -> Map(
        "paymentMethodToken" -> paymentMethodToken,
        "paymentMethodType" -> "CreditCard",
        "paymentMethodStatus" -> 0,
        "expirationYear" -> creditCard.expiration.year,
        "expirationMonth" -> creditCard.expiration.month,
        "cardHolderName" -> creditCard.holderName.get,
        "subtype" -> "VISA",
        "lastFourDigits" -> creditCard.number.takeRight(4),
        "validDate" -> DateTime(year = creditCard.expiration.year, month = creditCard.expiration.month, day = 1).clicks,
        "paymentMethodLastSuccessfulUsedTimestamp" -> System.currentTimeMillis(),
        "paymentMethodLastUsedTimestamp" -> System.currentTimeMillis(),
        "processorName" -> "N/A",
        "binNumber" -> "422222",
        "binDetails" -> Map(
          "cardCountryCode" -> "DO",
          "cardVendor" -> "VISA",
          "cardLevel" -> "CLASSIC",
          "cardType" -> "CREDIT",
          "binNumber" -> "422222",
          "cardIssuer" -> "BANCO MULTIPLE PROMERICA DE LA REPUBLICA DOMINICANA, S. A."
        )
      )
    )
  }

  case class AuthorizationRequest(programId: String,
                                  programKey: String,
                                  payment: Payment,
                                  paymentToken: String,
                                  paymentMethodToken: String)
    extends ZoozRejectableRequest(programId, programKey) {

    override protected def expectedJsonBody = Map(
      "command" -> "authorizePayment",
      "paymentToken" -> paymentToken,
      "paymentMethod" -> Map(
        "paymentMethodType" -> "CreditCard",
        "paymentMethodToken" -> paymentMethodToken
      ),
      "paymentInstallments" -> Map(
        "numOfInstallments" -> payment.installments
      )
    )

    def returns(authorizationCode: String): Unit = respondWith(validResponse(authorizationCode))

    private def validResponse(authorizationCode: String) = Map(
      "responseStatus" -> 0,
      "responseObject" -> Map(
        "authorizationCode" -> authorizationCode,
        "responseType" -> "authorizeCompletion",
        "merchantId" -> randomStringWithLength(10),
        "processorName" -> "N/A",
        "processorResultCode" -> 1,
        "processorReferenceId" -> authorizationCode
      )
    )
  }

  case class CaptureRequest(programId: String,
                            programKey: String,
                            payment: Payment,
                            paymentToken: String)
    extends ZoozRejectableRequest(programId, programKey) {

    override protected def expectedJsonBody = Map(
      "command" -> "commitPayment",
      "paymentToken" -> paymentToken,
      "amount" -> payment.currencyAmount.amount
    )

    def returns(captureCode: String): Unit = respondWith(validResponse(captureCode))

    private def validResponse(captureCode: String) = Map(
      "responseStatus" -> 0,
      "responseObject" -> Map(
        "captureCode" -> captureCode,
        "actionID" -> randomStringWithLength(26),
        "processorName" -> "N/A"
      )
    )
  }

  case class VoidRequest(programId: String,
                         programKey: String,
                         paymentToken: String)
    extends ZoozRejectableRequest(programId, programKey) {

    override protected def expectedJsonBody = Map(
      "command" -> "voidPayment",
      "paymentToken" -> paymentToken
    )

    def returns(voidReferenceId: String): Unit = respondWith(validResponse(voidReferenceId))

    private def validResponse(voidReferenceId: String) = Map(
      "responseStatus" -> 0,
      "responseObject" -> Map(
        "voidReferenceId" -> voidReferenceId,
        "processorName" -> "N/A"
      )
    )
  }
}
