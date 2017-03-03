package com.wix.pay.zooz

import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.http.{ByteArrayContent, GenericUrl, HttpHeaders, HttpRequestFactory}
import com.wix.pay.creditcard.{CreditCard, CreditCardOptionalFields, YearMonth}
import org.json4s._
import org.json4s.native.JsonMethods._
import org.json4s.native.Serialization

object ZoozFakeClient extends App {
  // Replace with your credentials
  val programId = "yourProgramId"
  val programKey = "yourProgramKey"
  
  val partnerName = "yourPartnerName"
  val partnerPassword = "yourPartnerPassword"

  val paymentUrl = "https://sandbox.zooz.com/mobile/ZooZPaymentAPI"
  val partnerUrl = "https://sandbox-api.zooz.com/partners/merchants"

  val amount = 110.2
  val currency = "USD"
  val paymentMethodType = "CreditCard"

  implicit val formats = DefaultFormats

  val creditCard = CreditCard("4222222222222220", YearMonth(2020, 12), Some(CreditCardOptionalFields.withFields(
    csc = Some("101"), holderName = Some("John Smith"))
  ))

  val requestFactory: HttpRequestFactory = new NetHttpTransport().createRequestFactory()

  val paymentToken = openPayment()
  val paymentMethodToken = addPaymentMethod(paymentToken)
  val authorizationCode = authorize(paymentToken, paymentMethodToken)
  capture(paymentToken)
//  void(paymentToken)

//  createMerchant()

  def openPayment(): String = timed("openPayment") {
    val content = Map(
      "command" -> "openPayment",
      "paymentDetails" -> Map(
        "amount" -> amount,
        "currencyCode" -> currency
      ),
      "customerDetails" -> Map(
        "customerLoginID" -> "testBuyer"
      )
    )
    (postPayment(content) \ "responseObject" \ "paymentToken").extract[String]
  }

  def addPaymentMethod(paymentToken: String): String = timed("addPaymentMethod") {
    val content = Map(
      "command" -> "addPaymentMethod",
      "paymentToken" -> paymentToken,
      "email" -> "test@buyer.com",
      "paymentMethod" -> Map(
        "paymentMethodType" -> paymentMethodType,
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
    (postPayment(content) \ "responseObject" \ "paymentMethodToken").extract[String]
  }

  def authorize(paymentToken: String, paymentMethodToken: String): String = timed("authorize") {
    val content = Map(
      "command" -> "authorizePayment",
      "paymentToken" -> paymentToken,
      "paymentMethod" -> Map(
        "paymentMethodType" -> paymentMethodType,
        "paymentMethodToken" -> paymentMethodToken
      ),
      "paymentInstallments" -> Map(
        "numOfInstallments" -> 2
//        "firstInstallmentAmount" -> 50.6,
//        "remainingInstallmentsAmount" -> (amount - 50.6)
      )
    )
    (postPayment(content) \ "responseObject" \ "authorizationCode").extract[String]
  }

  def capture(paymentToken: String): String = timed("capture") {
    val content = Map(
      "command" -> "commitPayment",
      "paymentToken" -> paymentToken,
      "amount" -> amount
    )
    (postPayment(content) \ "responseObject" \ "captureCode").extract[String]
  }

  def void(paymentToken: String): Unit = timed("void") {
    val content = Map(
      "command" -> "voidPayment",
      "paymentToken" -> paymentToken
    )
    (postPayment(content) \ "responseObject" \ "voidReferenceId").extract[String]
  }

  def createMerchant(): String = timed("createMerchant") {
    val content = Map(
      "name" -> "Test Merchant",
      "email" -> "test@merchant.com",
      "address" -> Map(
        "countryCode" -> "IL"
      )
    )
    postPartner(content).toString
  }

  private def postPayment(content: Map[String, Any]) = {
    val response = post(paymentUrl, content, new HttpHeaders()
      .set("ZooZUniqueID", programId)
      .set("ZooZAppKey", programKey)
      .set("ZooZResponseType", "JSon")
    )
    if ((response \ "responseStatus").extract[Int] != 0) {
      throw new RuntimeException("Error!")
    }
    response
  }

  private def postPartner(content: Map[String, Any]) = {
    post(partnerUrl, content, new HttpHeaders()
      .set("Partner-Name", partnerName)
      .set("Partner-Password", partnerPassword)
      .set("ZooZResponseType", "JSon"))
  }

  private def post(url: String, content: Map[String, Any], headers: HttpHeaders) = {
    val response = requestFactory.buildPostRequest(
      new GenericUrl(url),
      new ByteArrayContent("application/json", Serialization.write(content).getBytes("UTF-8"))
    ).setHeaders(headers).execute()
    val str = response.parseAsString()
    println(s"[${response.getStatusCode}] Raw result: $str")
    parse(str)
  }

  private def timed[T](name: String)(f: => T): T = {
    val start = System.currentTimeMillis()
    val res = f
    val elapsed = System.currentTimeMillis() - start
    println(s"$name took $elapsed ms, result: $res")
    res
  }
}
