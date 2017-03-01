package com.wix.pay.zooz

import com.wix.pay.PaymentGateway
import com.wix.pay.creditcard.CreditCard
import com.wix.pay.model.{Customer, Deal, Payment}

import scala.util.Try

class ZoozGateway extends PaymentGateway {
  override def authorize(merchantKey: String, creditCard: CreditCard, payment: Payment, customer: Option[Customer], deal: Option[Deal]): Try[String] = ???

  override def capture(merchantKey: String, authorizationKey: String, amount: Double): Try[String] = ???

  override def sale(merchantKey: String, creditCard: CreditCard, payment: Payment, customer: Option[Customer], deal: Option[Deal]): Try[String] = ???

  override def voidAuthorization(merchantKey: String, authorizationKey: String): Try[String] = ???
}
