package uz.umarov.handgesture.ui.screen.capture

import android.Manifest
import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import uz.umarov.handgesture.helper.AppConstant
import uz.umarov.handgesture.helper.LocalStorageHelper
import uz.umarov.handgesture.helper.OrientationLiveData
import uz.umarov.handgesture.helper.PermissionHelper
import uz.umarov.handgesture.ui.screen.capture.view.CameraFragment
import uz.umarov.handgesture.view_model.capture.CameraModeViewModel
import uz.umarov.handgesture.view_model.capture.GestureDetectViewModel
import uz.umarov.handgesture.R
import uz.umarov.handgesture.databinding.ActivityCaptureBinding


class CaptureActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CaptureActivityy"
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Log.d(TAG, "Camera permission granted")
            initializeCameraAndGestureDetection()
        } else {
            Log.d(TAG, "Camera permission denied")
        }
    }

    private var _binding: ActivityCaptureBinding? = null

    private val binding get() = _binding!!
    private var cameraFragment: CameraFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _binding = DataBindingUtil.setContentView(
            this, R.layout.activity_capture
        )

        binding.lifecycleOwner = this

        setupGestureDetectViewModel()
        setupCameraModeViewModel()

        binding.captureActivity = this

        binding.screenRotation = OrientationLiveData(this).apply {
            observe(this@CaptureActivity) { orientation ->
                Log.d(TAG, "Screen orientation changed: $orientation")
            }
        }

        if (savedInstanceState == null) {
            checkCameraPermission()
        }

    }

    private fun checkCameraPermission() {
        if (!PermissionHelper.isCameraPermissionGranted(this)) {
            Log.d(TAG, "Requesting camera permission")
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            Log.d(TAG, "Camera permission already granted")
            initializeCameraAndGestureDetection()
        }
    }

    private fun initializeCameraAndGestureDetection() {
        if (binding.cameraModeViewModel?.availableCameraOrientations?.isNotEmpty() == true) {
            cameraFragment = CameraFragment()
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, cameraFragment!!).commit()
            Log.d(TAG, "Camera fragment initialized")
        }

        binding.cameraFragment = cameraFragment
    }

    private fun setupGestureDetectViewModel() {
        val gestureDetectViewModel = ViewModelProvider(this)[GestureDetectViewModel::class.java]
        binding.gestureDetectViewModel = gestureDetectViewModel

        Log.d(TAG, "GestureDetectViewModel initialized")

        val storedIsDrawHandTrackingLineValue = (LocalStorageHelper.readData(
            this, AppConstant.HAND_TRACKING_MODE_VALUE_KEY
        ) as Boolean?) ?: true
        gestureDetectViewModel.setAndSaveIsDrawHandTrackingLineValue(
            storedIsDrawHandTrackingLineValue
        )

        gestureDetectViewModel.handGestureOptions.observe(this) {
            Log.d(TAG, "Hand gesture options updated: $it")
        }

        gestureDetectViewModel.currentHandGesture.observe(this) { gesture ->
            Log.d(TAG, "Current hand gesture: $gesture")
        }

        gestureDetectViewModel.timerTrigger.observe(this) {
            Log.d(TAG, "Timer triggered")
            gestureDetectViewModel.setShouldRunHandTracking(true)
        }

    }

    private fun setupCameraModeViewModel() {
        val cameraModeViewModel = ViewModelProvider(this)[CameraModeViewModel::class.java]

        cameraModeViewModel.availableCameraOrientations = getAvailableCameraOrientations()

        binding.cameraModeViewModel = cameraModeViewModel

        val storedGridModeValue =
            (LocalStorageHelper.readData(this, AppConstant.GRID_MODE_VALUE_KEY) as Boolean?)
                ?: false
        cameraModeViewModel.setAndSaveGridMode(storedGridModeValue)

        if (cameraModeViewModel.availableCameraOrientations.isNotEmpty()) {
            val storedCameraOrientationValue = CameraSelector.LENS_FACING_FRONT
            cameraModeViewModel.setAndSaveCameraOrientation(storedCameraOrientationValue)
        }


        val storedCameraAspectRatioValue =
            (LocalStorageHelper.readData(this, AppConstant.ASPECT_RATIO_MODE_VALUE_KEY) as Int?)
                ?: 0
        cameraModeViewModel.setAndSaveAspectRatio(storedCameraAspectRatioValue)
    }

    private fun getAvailableCameraOrientations(): List<Int> {
        val availableCameraOrientations = mutableListOf<Int>()
        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        val cameraIds = cameraManager.cameraIdList.filter {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val capabilities = characteristics.get(
                CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES
            )
            capabilities?.contains(
                CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE
            ) ?: true
        }

        cameraIds.forEach { cameraId ->
            val cameraCharacteristics = cameraManager.getCameraCharacteristics(cameraId)
            val isCameraAvailable =
                cameraCharacteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) != CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY

            if (isCameraAvailable) {
                val orientationId = lensOrientationInt(
                    cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)!!
                )
                if (orientationId != -1) {
                    availableCameraOrientations.add(orientationId)
                }
            }
        }

        if (availableCameraOrientations.contains(CameraSelector.LENS_FACING_FRONT)) {
            availableCameraOrientations.remove(CameraSelector.LENS_FACING_BACK)
            availableCameraOrientations.add(0, CameraSelector.LENS_FACING_FRONT)
        }

        return availableCameraOrientations
    }

    private fun lensOrientationInt(value: Int) = when (value) {
        CameraCharacteristics.LENS_FACING_BACK -> CameraSelector.LENS_FACING_BACK
        CameraCharacteristics.LENS_FACING_FRONT -> CameraSelector.LENS_FACING_FRONT
        else -> -1
    }

    override fun onResume() {
        super.onResume()
        if (PermissionHelper.isCameraPermissionGranted(this)) {
            initializeCameraAndGestureDetection()
        }
    }


    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }


}
