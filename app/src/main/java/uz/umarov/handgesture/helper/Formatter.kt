package uz.umarov.handgesture.helper

import android.view.Surface


object Formatter {

    fun orientationToSurfaceRotation(orientation: Int): Int {
        return when (orientation) {
            in 45 until 135 -> Surface.ROTATION_270
            in 135 until 225 -> Surface.ROTATION_180
            in 225 until 315 -> Surface.ROTATION_90
            else -> Surface.ROTATION_0
        }
    }
}