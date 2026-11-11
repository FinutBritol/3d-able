package com.example.dbtest.map

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

class Map3DController(
    private val mapView: SiteMapView,
    private val scope: CoroutineScope,
    private val onModeChanged: (enabled: Boolean) -> Unit
) {
    private var animJob: Job? = null
    private var targetPitch = FLAT_PITCH

    fun setTilt(progress: Int) {
        targetPitch = FLAT_PITCH - progress

        if (progress > 0 && !mapView.mode3D) {
            mapView.set3D(true)
            mapView.pitchDeg = FLAT_PITCH
            onModeChanged(true)
        }

        if (!mapView.mode3D) return
        animateToTarget()
    }

    fun cancel() {
        animJob?.cancel()
    }

    private fun animateToTarget() {
        animJob?.cancel()
        animJob = scope.launch {
            while (isActive && abs(targetPitch - mapView.pitchDeg) > SNAP_DEGREES) {
                mapView.pitchDeg += (targetPitch - mapView.pitchDeg) * EASE
                delay(FRAME_MS)
            }
            ensureActive()

            mapView.pitchDeg = targetPitch
            if (targetPitch >= FLAT_PITCH) {
                mapView.set3D(false)
                onModeChanged(false)
            }
        }
    }

    private companion object {
        const val FLAT_PITCH = 90f
        const val SNAP_DEGREES = 0.5f
        const val EASE = 0.25f
        const val FRAME_MS = 16L
    }
}