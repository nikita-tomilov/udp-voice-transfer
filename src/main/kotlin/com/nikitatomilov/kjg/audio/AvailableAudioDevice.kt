package com.nikitatomilov.kjg.audio

import javax.sound.sampled.Line
import javax.sound.sampled.Mixer

data class AvailableAudioDevice(
  val mixer: Mixer,
  val line: Line,
  val deviceType: AvailableAudioDeviceType
) {
  override fun toString(): String {
    return mixer.mixerInfo.description
  }
}

enum class AvailableAudioDeviceType {
  INPUT, OUTPUT
}