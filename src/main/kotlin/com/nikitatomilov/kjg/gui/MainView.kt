package com.nikitatomilov.kjg.gui

import com.nikitatomilov.kjg.audio.Audio
import com.nikitatomilov.kjg.audio.AvailableAudioDeviceType
import javafx.application.Platform
import javafx.event.ActionEvent
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.control.TextField
import javafx.scene.layout.AnchorPane
import javafx.stage.WindowEvent
import mu.KLogging
import tornadofx.*
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine

@Suppress("unused")
class MainView : View() {

  override val root: AnchorPane by fxml("/fxml/main.fxml")

  private val cmdHelloWorld: Button by fxid()

  private val lblStats: Label by fxid()

  fun startPressed(event: ActionEvent?) {
    this.currentWindow!!.addEventFilter(WindowEvent.WINDOW_CLOSE_REQUEST) { System.exit(0) }
    val s = find<AudioSettings>()

    var targetHost = ""
    var targetPort = 0
    var listenAtPort = 0

    s.onAccept = { startConversation(targetHost, targetPort, listenAtPort) }
    s.targetHostPortCallback = {
      try {
        val h = it.split(":")[0]
        val p = it.split(":")[1]
        targetHost = h
        targetPort = p.toInt()
        logger.info { "host $h port $p" }
      } catch (e: Exception) {

      }
    }
    s.listenAtPortCallback = {
      try {
        listenAtPort = it.toInt()
        logger.info { "listen at $it" }
      } catch (e: Exception) {

      }
    }
    s.openWindow()
  }

  private fun startConversation(targetHost: String, targetPort: Int, listenPort: Int) {
    cmdHelloWorld.text = "Started"
    cmdHelloWorld.isDisable = true

    val devices = Audio.enumerateDevices()
    val mics = devices.filter { it.deviceType == AvailableAudioDeviceType.INPUT }
    val spks = devices.filter { it.deviceType == AvailableAudioDeviceType.OUTPUT }

    logger.info { "Available Mics:" }
    mics.forEach { logger.info { " - $it" } }

    logger.info { "Available Speakers:" }
    spks.forEach { logger.info { " - $it" } }

    val a = Audio()
    val mic = mics.first().line as TargetDataLine
    val spk = spks.first().line as SourceDataLine

    val outcomingAddress = InetSocketAddress(targetHost, targetPort)
    val outcomingSocket = DatagramSocket()

    val incomingAddress = InetSocketAddress("0.0.0.0", listenPort)
    val incomingSocket = DatagramSocket(incomingAddress)

    val ex = Executors.newFixedThreadPool(8)
    a.start(
        mic,
        spk,
        Audio.RATE,
        Audio.SAMPLE_SIZE_BITS,
        ex,
        {
          val dp = DatagramPacket(it, it.size, outcomingAddress)
          outcomingSocket.send(dp)
        },
        {
          val db = ByteArray(Audio.CHUNK_SIZE)
          val dp = DatagramPacket(db, db.size)
          incomingSocket.receive(dp)
          db
        },
        { Platform.runLater { lblStats.text = it } })
  }

  companion object : KLogging()
}

class AudioSettings : View() {

  lateinit var targetHostPortCallback: (String) -> Unit

  lateinit var listenAtPortCallback: (String) -> Unit

  lateinit var onAccept: () -> Unit

  lateinit var t1: TextField

  lateinit var t2: TextField

  override val root = vbox {
    hbox {
      label("Send to")
      t1 = textfield("127.0.0.1:50150") {
        textProperty().addListener { _, _, newValue -> targetHostPortCallback(newValue) }
      }
    }
    hbox {
      label("Listen at")
      t2 = textfield("50150") {
        textProperty().addListener { _, _, newValue -> listenAtPortCallback(newValue) }
      }
    }
    button("ok") {
      action {
        targetHostPortCallback(t1.text)
        listenAtPortCallback(t2.text)
        onAccept()
        find<AudioSettings>().close()
      }
    }
  }

  companion object : KLogging()
}