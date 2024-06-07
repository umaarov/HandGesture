package uz.umarov.handgesture.helper

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.view.OrientationEventListener
import android.view.Surface
import androidx.lifecycle.LiveData

class OrientationLiveData(
    context: Context
) : LiveData<Int>() {

    private val listener = object : OrientationEventListener(context.applicationContext) {
        override fun onOrientationChanged(orientation: Int) {
            val rotation = when {
                orientation <= 45 -> 0
                orientation <= 135 -> 90
                orientation <= 225 -> 180
                orientation <= 315 -> 270
                else -> 0
            }
            if (rotation != value) postValue(rotation)
        }
    }

    override fun onActive() {
        super.onActive()
        listener.enable()
    }

    override fun onInactive() {
        super.onInactive()
        listener.disable()
    }
}