// GestureDetectViewModel.kt
package uz.umarov.handgesture.view_model.capture

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import uz.umarov.handgesture.helper.AppConstant
import uz.umarov.handgesture.helper.LocalStorageHelper
import uz.umarov.handgesture.model.GestureDetectOption

class GestureDetectViewModel(application: Application) : AndroidViewModel(application) {

    private var _shouldRunHandTracking = MutableLiveData<Boolean>().apply {
        this.value = true
    }

    private val _currentHandGesture = MutableLiveData<GestureDetectOption>()
    private val _isDrawHandTrackingLine = MutableLiveData<Boolean>().apply {
        value = false
    }

    private val _timerTrigger = MutableLiveData<Boolean>()

    val shouldRunHandTracking: LiveData<Boolean> = _shouldRunHandTracking

    val handGestureOptions = MutableLiveData<List<GestureDetectOption>>()
    val currentHandGesture: LiveData<GestureDetectOption> = _currentHandGesture
    val isDrawHandTrackingLine: LiveData<Boolean> = _isDrawHandTrackingLine
    val timerTrigger: LiveData<Boolean> = _timerTrigger



    fun setAndSaveIsDrawHandTrackingLineValue(newValue: Boolean) {

        _isDrawHandTrackingLine.value = newValue

        LocalStorageHelper.writeData(
            getApplication(),
            AppConstant.HAND_TRACKING_MODE_VALUE_KEY,
            newValue
        )
    }

    fun setShouldRunHandTracking(value: Boolean) {
        _shouldRunHandTracking.value = value
    }


}
