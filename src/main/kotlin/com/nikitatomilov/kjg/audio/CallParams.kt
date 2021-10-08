package com.nikitatomilov.kjg.audio

data class CallParams(
  val targetHost: String,
  val targetPort: Int,
  val listenPort: Int,
  val sampleRate: Float,
  val bitsPerSample: Int,
  val packetSize: Int
)