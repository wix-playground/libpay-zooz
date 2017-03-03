package com.wix.pay.zooz

import java.util.UUID

import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{Customer, Deal, Payment}
import org.json4s.JsonDSL._
import org.json4s._

class ZoozRequestBuilder(customerLoginIdGenerator: => String = UUID.randomUUID().toString,
                         defaultEmail: String = "example@example.org") {
  private val paymentMethodType = "CreditCard"

  def openPaymentRequest(payment: Payment, deal: Deal) = JObject(
    "command" -> "openPayment",
    "paymentDetails" -> JObject(
      "amount" -> payment.currencyAmount.amount,
      "currencyCode" -> payment.currencyAmount.currency
    ),
    "customerDetails" -> Map(
      "customerLoginID" -> customerLoginIdGenerator
    ),
    "invoice" -> Map(
      "number" -> deal.invoiceId.get
    )
  )

  def addPaymentMethodRequest(creditCard: CreditCard, customer: Option[Customer], paymentToken: String) = JObject(
    "command" -> "addPaymentMethod",
    "paymentToken" -> paymentToken,
    "email" -> email(customer),
    "paymentMethod" -> JObject(
      "paymentMethodType" -> paymentMethodType,
      "paymentMethodDetails" -> JObject(
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

  def authorizeRequest(payment: Payment, paymentToken: String, paymentMethodToken: String) = JObject(
    "command" -> "authorizePayment",
    "paymentToken" -> paymentToken,
    "paymentMethod" -> JObject(
      "paymentMethodType" -> paymentMethodType,
      "paymentMethodToken" -> paymentMethodToken
    ),
    "paymentInstallments" -> Map(
      "numOfInstallments" -> payment.installments
    )
  )

  def captureRequest(paymentToken: String, amount: Double) = JObject(
    "command" -> "commitPayment",
    "paymentToken" -> paymentToken,
    "amount" -> amount
  )

  def voidRequest(paymentToken: String) = JObject(
    "command" -> "voidPayment",
    "paymentToken" -> paymentToken
  )

  private def email(customer: Option[Customer]): String = customer.flatMap(_.email).filterEmpty.getOrElse(defaultEmail)

  private implicit class OptionStringExtensions(o: Option[String]) {
    def filterEmpty: Option[String] = o.filter(_.trim.nonEmpty)
  }
}