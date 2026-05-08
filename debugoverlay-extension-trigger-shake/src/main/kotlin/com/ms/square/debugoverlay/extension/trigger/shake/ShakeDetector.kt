/*
 * Adapted from Square's Seismic library (https://github.com/square/seismic)
 * Original commit: 6651880b81a7f5a98ad8e1a7806574d4dabba4d4
 * Copyright 2012 Square, Inc.
 * Licensed under the Apache License, Version 2.0
 */
package com.ms.square.debugoverlay.extension.trigger.shake

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager

/**
 * Detects phone shaking. If more than 75% of the samples taken in the past 0.5s are
 * accelerating, the device is a) shaking, or b) free falling 1.84m (h = 1/2 * g * t^2 * 3/4).
 *
 * Adapted from Square's Seismic library to Kotlin. Marked internal — this module's only
 * surface is the AndroidX Startup initializer.
 */
internal class ShakeDetector(private val listener: Listener) : SensorEventListener {

  /** Listens for shakes. */
  internal fun interface Listener {
    /** Called on the main thread when the device is shaken. */
    fun hearShake()
  }

  /**
   * When the magnitude of total acceleration exceeds this
   * value, the phone is accelerating.
   */
  private var accelerationThreshold = DEFAULT_ACCELERATION_THRESHOLD

  private val queue = SampleQueue()
  private var sensorManager: SensorManager? = null
  private var accelerometer: Sensor? = null

  /**
   * Starts listening for shakes on devices with appropriate hardware.
   *
   * @return true if the device supports shake detection.
   */
  fun start(sensorManager: SensorManager): Boolean {
    // Already started?
    if (accelerometer != null) {
      return true
    }
    val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) ?: return false
    accelerometer = sensor
    this.sensorManager = sensorManager
    // Diverges from upstream Seismic, which uses SENSOR_DELAY_FASTEST. FASTEST requires the
    // android.permission.HIGH_SAMPLING_RATE_SENSORS permission on API 31+; GAME (~50Hz) is
    // plenty for shake detection (window is 0.5s).
    sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
    return true
  }

  fun stop() {
    val sensor = accelerometer ?: return
    queue.clear()
    sensorManager?.unregisterListener(this, sensor)
    sensorManager = null
    accelerometer = null
  }

  /** Sets the acceleration threshold sensitivity. */
  fun setSensitivity(accelerationThreshold: Int) {
    this.accelerationThreshold = accelerationThreshold
  }

  override fun onSensorChanged(event: SensorEvent) {
    val accelerating = isAccelerating(event)
    val timestamp = event.timestamp
    queue.add(timestamp, accelerating)
    if (queue.isShaking) {
      queue.clear()
      listener.hearShake()
    }
  }

  override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) = Unit

  private fun isAccelerating(event: SensorEvent): Boolean {
    val ax = event.values[0]
    val ay = event.values[1]
    val az = event.values[2]

    // Instead of comparing magnitude to ACCELERATION_THRESHOLD,
    // compare their squares. This is equivalent and doesn't need the
    // actual magnitude, which would be computed using (expensive) Math.sqrt().
    val magnitudeSquared = ax * ax + ay * ay + az * az
    return magnitudeSquared > accelerationThreshold * accelerationThreshold
  }

  companion object {
    /** Light = small movements detected. */
    const val SENSITIVITY_LIGHT: Int = 11

    /** Medium sensitivity. */
    const val SENSITIVITY_MEDIUM: Int = 13

    /** Hard = aggressive movements required. */
    const val SENSITIVITY_HARD: Int = 15

    private const val DEFAULT_ACCELERATION_THRESHOLD = SENSITIVITY_MEDIUM
  }

  /**
   * Sample queue — a fixed-size circular buffer of [Sample] entries that span at most
   * [MAX_WINDOW_SIZE] nanoseconds. Used to determine whether enough recent samples were
   * accelerating to count as a shake.
   *
   * Visibility is `internal` (not `private`) so the parity tests under `src/test` can
   * exercise it directly, mirroring upstream Seismic's `ShakeDetectorTest`.
   */
  internal class SampleQueue {
    private val pool = SamplePool()
    private var oldest: Sample? = null
    private var newest: Sample? = null
    private var sampleCount = 0
    private var acceleratingCount = 0

    fun add(timestamp: Long, accelerating: Boolean) {
      // Purge samples that precede the window.
      purge(timestamp - MAX_WINDOW_SIZE)
      val added = pool.acquire().apply {
        this.timestamp = timestamp
        this.accelerating = accelerating
        this.next = null
      }
      newest?.next = added
      newest = added
      if (oldest == null) oldest = added
      // Update running average.
      sampleCount++
      if (accelerating) acceleratingCount++
    }

    /** Removes all samples from this queue. */
    fun clear() {
      var sample = oldest
      while (sample != null) {
        val next = sample.next
        pool.release(sample)
        sample = next
      }
      oldest = null
      newest = null
      sampleCount = 0
      acceleratingCount = 0
    }

    /** Purges samples with timestamps older than cutoff. */
    private fun purge(cutoff: Long) {
      var head = oldest
      while (sampleCount >= MIN_QUEUE_SIZE && head != null && cutoff - head.timestamp > 0) {
        if (head.accelerating) acceleratingCount--
        sampleCount--
        val next = head.next
        if (next == null) newest = null
        oldest = next
        pool.release(head)
        head = next
      }
    }

    /**
     * Returns true if we have enough samples and at least 3/4 are accelerating.
     */
    val isShaking: Boolean
      get() {
        val tail = newest ?: return false
        val head = oldest ?: return false
        return tail.timestamp - head.timestamp >= MIN_WINDOW_SIZE &&
          acceleratingCount >= (sampleCount shr 1) + (sampleCount shr 2)
      }

    /** Snapshot of the queue contents from oldest to newest. Used by parity tests. */
    fun asList(): List<Sample> {
      val result = mutableListOf<Sample>()
      var current = oldest
      while (current != null) {
        result.add(current)
        current = current.next
      }
      return result
    }

    companion object {
      /** Window size in ns. Used to compute the average. */
      private const val MAX_WINDOW_SIZE = 500_000_000L // 0.5s in ns
      private const val MIN_WINDOW_SIZE = MAX_WINDOW_SIZE shr 1 // 0.25s in ns

      /**
       * Ensure the queue size never falls below this size, even if the device
       * fails to deliver this many events during the time window. The LG Ally
       * is one such device.
       */
      private const val MIN_QUEUE_SIZE = 4
    }
  }

  /** An accelerometer sample. Internal for parity test access. */
  internal class Sample {
    /** Time sample was taken. */
    var timestamp: Long = 0

    /** If acceleration > accelerationThreshold. */
    var accelerating: Boolean = false

    /** Next sample in the queue or pool. */
    var next: Sample? = null
  }

  /**
   * Small linked-list pool that recycles [Sample] instances to avoid per-event allocation.
   */
  private class SamplePool {
    private var head: Sample? = null

    fun acquire(): Sample {
      val pooled = head ?: return Sample()
      head = pooled.next
      return pooled
    }

    fun release(sample: Sample) {
      sample.next = head
      head = sample
    }
  }
}
