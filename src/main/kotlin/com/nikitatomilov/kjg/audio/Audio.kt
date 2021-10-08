package com.nikitatomilov.kjg.audio

import mu.KLogging
import java.util.concurrent.ExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.SourceDataLine
import javax.sound.sampled.TargetDataLine

class Audio {
  private val started = AtomicBoolean(false)

  private val shouldStop = AtomicBoolean(false)

  private val bytesIncoming = AtomicInteger(0)
  private val packagesIncoming = AtomicInteger(0)
  private val bytesOutcoming = AtomicInteger(0)
  private val packagesOutcoming = AtomicInteger(0)

  fun start(
    micLine: TargetDataLine,
    spkLine: SourceDataLine,
    sampleRate: Float,
    sampleSizeBits: Int,
    packetSize: Int,
    ex: ExecutorService,
    micData: (ByteArray) -> Unit,
    spkData: () -> ByteArray,
    stats: (String) -> Unit
  ) {
    val format = buildFormat(sampleRate, sampleSizeBits)
    if (started.compareAndSet(false, true)) {
      micLine.open(format)
      micLine.start()

      spkLine.open(format)
      spkLine.start()

      ex.submit {
        while (!shouldStop.get()) {
          try {
            val data = ByteArray(packetSize)
            micLine.read(data, 0, data.size)
            micData(data)
            bytesOutcoming.addAndGet(data.size)
            packagesOutcoming.incrementAndGet()
          } catch (e: Exception) {
            logger.error { "Error in mic thread: $e" }
          }
        }
      }
      ex.submit {
        while (!shouldStop.get()) {
          try {
            val data = spkData()
            bytesIncoming.addAndGet(data.size)
            packagesIncoming.incrementAndGet()
            spkLine.write(data, 0, data.size)
          } catch (e: Exception) {
            logger.error { "Error in spk thread: $e" }
          }
        }
      }
      ex.submit {
        while (!shouldStop.get()) {
          val bi = bytesIncoming.getAndSet(0)
          val bo = bytesOutcoming.getAndSet(0)
          val pi = packagesIncoming.getAndSet(0)
          val po = packagesOutcoming.getAndSet(0)
          stats(
              """
            Incoming: $bi bytes/sec $pi packets/sec
            Outcoming: $bo bytes/sec $po packets/sec
          """.trimIndent())
          Thread.sleep(1000)
        }
      }
    } else {
      logger.error { "Already started" }
    }
  }

  companion object : KLogging() {
    private val ENCODING = AudioFormat.Encoding.PCM_SIGNED
    val RATE = 16000.0f
    val SAMPLE_SIZE_BITS = 16
    private val CHANNELS = 1
    private val IS_BIG_ENDIAN = true
    val CHUNK_SIZE = 128

    fun enumerateDevices(): List<AvailableAudioDevice> {
      val ans = ArrayList<AvailableAudioDevice>()

      val mixersInfo = AudioSystem.getMixerInfo()
      for (mixerInfo in mixersInfo) {
        val mixer = AudioSystem.getMixer(mixerInfo)

        // Check if is input device
        var lineInfos = mixer.targetLineInfo
            .filter { it.lineClass == TargetDataLine::class.java } //just in case
        for (lineInfo in lineInfos) {
          val line = mixer.getLine(lineInfo)
          ans.add(AvailableAudioDevice(mixer, line, AvailableAudioDeviceType.INPUT))
        }
        // Check if is output device
        lineInfos = mixer.sourceLineInfo
            .filter { it.lineClass == SourceDataLine::class.java } //just in case
        for (lineInfo in lineInfos) {
          val line = mixer.getLine(lineInfo)
          ans.add(AvailableAudioDevice(mixer, line, AvailableAudioDeviceType.OUTPUT))
        }
      }
      return ans
    }

    private fun buildFormat(sampleRate: Float, sampleSizeBits: Int): AudioFormat {
      return AudioFormat(
          ENCODING, sampleRate, sampleSizeBits, CHANNELS,
          sampleSizeBits / 8 * CHANNELS, sampleRate, IS_BIG_ENDIAN)
    }
  }
}