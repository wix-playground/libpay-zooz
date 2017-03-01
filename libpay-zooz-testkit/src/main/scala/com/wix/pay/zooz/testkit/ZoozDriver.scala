package com.wix.pay.zooz.testkit

import com.wix.hoopoe.http.testkit.EmbeddedHttpProbe

class ZoozDriver(port: Int) {
  private val probe = new EmbeddedHttpProbe(port, EmbeddedHttpProbe.NotFoundHandler)

  def reset(): Unit = probe.reset()

  def start(): Unit = probe.doStart()

  def stop(): Unit = probe.doStop()
}
