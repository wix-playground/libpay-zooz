package com.wix.pay.zooz

import com.wix.pay.testkit.LibPayTestSupport
import com.wix.pay.zooz.parsers.{JsonZoozAuthorizationParser, JsonZoozMerchantParser}
import com.wix.pay.{PaymentErrorException, PaymentRejectedException}
import org.json4s.ParserUtil.ParseException
import org.specs2.matcher.Matcher
import org.specs2.matcher.MustThrownMatchers._

import scala.util.{Random, Try}

trait ZoozTestSupport extends LibPayTestSupport {
  val programId = randomStringWithLength(10)
  val programKey = randomStringWithLength(12)
  val someMerchant = ZoozMerchant(programId, programKey)
  val someMerchantStr = JsonZoozMerchantParser.stringify(someMerchant)

  val customerLoginId = randomStringWithLength(14)
  val paymentToken = randomStringWithLength(26)
  val paymentMethodToken = randomStringWithLength(26)
  val authorizationCode = randomStringWithLength(18)
  val captureCode = randomStringWithLength(18)
  val voidReferenceId = randomStringWithLength(18)
  val errorMessage = "Some error message"

  def authorization(authorizationCode: String, paymentToken: String) =
    JsonZoozAuthorizationParser.stringify(ZoozAuthorization(authorizationCode, paymentToken))

  def succeedWith(value: String): Matcher[Try[String]] = beSuccessfulTry.withValue(value)
  def beSuccessfulAuthorizationWith(authorizationCode: String, paymentToken: String): Matcher[Try[String]] = succeedWith(authorization(authorizationCode, paymentToken))
  def beRejectedWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentRejectedException => e.message must contain(message) }
  def failWithMessage(message: String): Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.message must contain(message) }
  def beParseError: Matcher[Try[String]] = beFailedTry.like { case e: PaymentErrorException => e.cause must beAnInstanceOf[ParseException] }

  def randomStringWithLength(length: Int): String = Random.alphanumeric.take(length).mkString
}
