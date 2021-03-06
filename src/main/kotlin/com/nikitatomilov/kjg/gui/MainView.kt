package com.nikitatomilov.kjg.gui

import com.nikitatomilov.kjg.audio.Audio
import com.nikitatomilov.kjg.audio.AvailableAudioDeviceType
import com.nikitatomilov.kjg.audio.CallParams
import com.nikitatomilov.kjg.util.MessageBoxes
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

    s.onAccept = { startConversation(it) }
    s.openWindow()
  }

  private fun startConversation(params: CallParams) {
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

    val outcomingAddress = InetSocketAddress(params.targetHost, params.targetPort)
    val outcomingSocket = DatagramSocket()

    val incomingAddress = InetSocketAddress("0.0.0.0", params.listenPort)
    val incomingSocket = DatagramSocket(incomingAddress)

    val ex = Executors.newFixedThreadPool(8)
    a.start(
        mic,
        spk,
        params.sampleRate,
        params.bitsPerSample,
        params.packetSize,
        ex,
        {
          val dp = DatagramPacket(it, it.size, outcomingAddress)
          outcomingSocket.send(dp)
        },
        {
          val db = ByteArray(params.packetSize)
          val dp = DatagramPacket(db, db.size)
          incomingSocket.receive(dp)
          db
        },
        { Platform.runLater { lblStats.text = it } })
  }

  companion object : KLogging()
}

class AudioSettings : View() {

  lateinit var onAccept: (CallParams) -> Unit
  lateinit var hostPort: TextField
  lateinit var listenPort: TextField
  lateinit var samplingFreq: TextField
  lateinit var bitsPerSample: TextField
  lateinit var chunkSize: TextField

  override val root = vbox {
    hbox {
      label("Send to")
      hostPort = textfield("127.0.0.1:50150")
    }
    hbox {
      label("Listen at")
      listenPort = textfield("50150")
    }
    hbox {
      label("Sampling freq")
      samplingFreq = textfield("16000")
    }
    hbox {
      label("Bits per sample")
      bitsPerSample = textfield("16")
    }
    hbox {
      label("Chunk size")
      chunkSize = textfield("128")
    }
    button("ok") {
      action {
        try {
          val h = hostPort.text.split(":")[0]
          val p = hostPort.text.split(":")[1].toInt()
          val l = listenPort.text.toInt()
          val sf = samplingFreq.text.toFloat()
          val bps = bitsPerSample.text.toInt()
          val cs = chunkSize.text.toInt()
          val params = CallParams(h, p, l, sf, bps, cs)
          onAccept(params)
          find<AudioSettings>().close()
        } catch (e: Exception) {
          MessageBoxes.showAlert(e.toString(), "Error")
        }
      }
    }
  }

  companion object : KLogging()
}