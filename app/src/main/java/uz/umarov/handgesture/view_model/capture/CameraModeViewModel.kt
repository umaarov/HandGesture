package uz.umarov.handgesture.view_model.capture

import android.app.Application
import android.widget.Toast
import androidx.camera.core.AspectRatio
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import uz.umarov.handgesture.R
import uz.umarov.handgesture.helper.AppConstant
import uz.umarov.handgesture.helper.LocalStorageHelper

class CameraModeViewModel(application: Application) : AndroidViewModel(application) {

    var availableCameraOrientations = listOf<Int>()

    private val _cameraAspectRatio = MutableLiveData<Int>()
    private val _isOpenGrid = MutableLiveData<Boolean>()
    private val _cameraOrientation = MutableLiveData<Int>()

    private var shouldRefreshCamera = true

    val cameraAspectRatio: LiveData<Int> = _cameraAspectRatio
    val isOpenGrid : LiveData<Boolean> = _isOpenGrid
    val cameraOrientation: LiveData<Int> = _cameraOrientation

    fun switchAndSaveAspectRatio(){
        if(!shouldRefreshCamera){
            return
        }
        shouldRefreshCamera = false

        if(_cameraAspectRatio.value == AspectRatio.RATIO_4_3){
            setAndSaveAspectRatio(AspectRatio.RATIO_16_9)
        }else {
            setAndSaveAspectRatio(AspectRatio.RATIO_4_3)
        }
    }

    fun setAndSaveAspectRatio(newValue: Int){

        _cameraAspectRatio.value = newValue

        LocalStorageHelper.writeData(
            getApplication(),
            AppConstant.ASPECT_RATIO_MODE_VALUE_KEY,
            newValue
        )
    }


    fun setAndSaveGridMode(newValue: Boolean){

        _isOpenGrid.value = newValue

        LocalStorageHelper.writeData(
            getApplication(),
            AppConstant.GRID_MODE_VALUE_KEY,
            newValue
        )
    }

    fun switchAndSaveCameraOrientation(){
        if(availableCameraOrientations.isEmpty()){
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.no_camera_available_warning),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if(availableCameraOrientations.size == 1){
            Toast.makeText(
                getApplication(),
                getApplication<Application>().getString(R.string.one_camera_available_warning),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        if(!shouldRefreshCamera){
            return
        }
        shouldRefreshCamera = false

        val index = availableCameraOrientations.indexOf(cameraOrientation.value)
        val size = availableCameraOrientations.size

        val newIndex = (index + 1) % size

        setAndSaveCameraOrientation(availableCameraOrientations[newIndex])
    }

    fun setAndSaveCameraOrientation(newValue: Int){

        _cameraOrientation.value = newValue

        LocalStorageHelper.writeData(
            getApplication(),
            AppConstant.CAMERA_ORIENTATION_VALUE_KEY,
            newValue
        )
    }

    fun setShouldRefreshCamera(value : Boolean){
        shouldRefreshCamera = value
    }
}