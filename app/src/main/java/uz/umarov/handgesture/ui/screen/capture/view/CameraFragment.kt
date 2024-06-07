package uz.umarov.handgesture.ui.screen.capture.view

import android.annotation.SuppressLint
import android.content.res.Configuration
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.mediapipe.tasks.vision.core.RunningMode
import uz.umarov.handgesture.R
import uz.umarov.handgesture.databinding.FragmentCameraBinding
import uz.umarov.handgesture.helper.AppConstant
import uz.umarov.handgesture.helper.Formatter
import uz.umarov.handgesture.helper.GestureRecognizerHelper
import uz.umarov.handgesture.helper.OrientationLiveData
import uz.umarov.handgesture.view_model.capture.CameraModeViewModel
import uz.umarov.handgesture.view_model.capture.GestureDetectViewModel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class CameraFragment : Fragment(),
    GestureRecognizerHelper.GestureRecognizerListener {

    companion object {
        private const val TAG = "CameraFragment"
    }

    private var _fragmentCameraBinding: FragmentCameraBinding? = null

    private val fragmentCameraBinding
        get() = _fragmentCameraBinding!!

    private var imageCapture: ImageCapture? = null

    private var gestureRecognizerHelper: GestureRecognizerHelper? = null
    private var preview: Preview? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private var cameraSelector: CameraSelector? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var aspectRatio: Int? = null

    private lateinit var backgroundExecutor: ExecutorService

    private lateinit var relativeOrientation: OrientationLiveData


    override fun onResume() {
        super.onResume()

        cameraProvider?.let {
            bindCameraUseCases(AppConstant.ANIMATION_DURATION_MILLIS.toLong())
        }

        backgroundExecutor.execute {
            if (gestureRecognizerHelper?.isClosed() == true) {
                gestureRecognizerHelper?.setupGestureRecognizer()
            }
        }
    }

    override fun onPause() {
        super.onPause()

        cameraProvider?.unbindAll()

        if (gestureRecognizerHelper != null) {
           backgroundExecutor.execute { gestureRecognizerHelper?.clearGestureRecognizer() }
        }
    }

    override fun onDestroyView() {
        _fragmentCameraBinding = null
        super.onDestroyView()

        backgroundExecutor.shutdown()
        backgroundExecutor.awaitTermination(
            Long.MAX_VALUE, TimeUnit.NANOSECONDS
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _fragmentCameraBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_camera, container, false)

        fragmentCameraBinding.lifecycleOwner = this

        val gestureDetectViewModel =
            ViewModelProvider(requireActivity())[GestureDetectViewModel::class.java]
        fragmentCameraBinding.gestureDetectViewModel = gestureDetectViewModel


        val cameraModeViewModel =
            ViewModelProvider(requireActivity())[CameraModeViewModel::class.java]

        fragmentCameraBinding.cameraModeViewModel = cameraModeViewModel

        cameraModeViewModel.cameraAspectRatio.observe(requireActivity()) {
            if (cameraProvider != null) {
                bindCameraUseCases()
            }
        }

        cameraModeViewModel.cameraOrientation.observe(requireActivity()) {
            if (cameraProvider != null) {
                bindCameraUseCases()
            }
        }

        gestureDetectViewModel.shouldRunHandTracking.observe(requireActivity()) {
            if (cameraProvider != null) {
                if (it)
                    bindImageAnalyzer()
                else {
                    unbindImageAnalyzer()
                }
            }
        }

        relativeOrientation = OrientationLiveData(requireContext()).apply {
            observe(viewLifecycleOwner) { orientation ->
                Log.d(TAG, "Orientation changed: $orientation")
                if (orientation == ORIENTATION_UNKNOWN) {
                    return@observe
                }

                val rotation = when (orientation) {
                    in 45 until 135 -> Surface.ROTATION_270
                    in 135 until 225 -> Surface.ROTATION_180
                    in 225 until 315 -> Surface.ROTATION_90
                    else -> Surface.ROTATION_0
                }
                preview?.targetRotation = rotation
                imageCapture?.targetRotation = rotation
            }
        }

        return fragmentCameraBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        backgroundExecutor = Executors.newSingleThreadExecutor()

         fragmentCameraBinding.viewFinder.post {
             setUpCamera()
        }

       backgroundExecutor.execute {
            gestureRecognizerHelper = GestureRecognizerHelper(
                context = requireContext(),
                runningMode = RunningMode.LIVE_STREAM,
                minHandDetectionConfidence = GestureRecognizerHelper.DEFAULT_HAND_DETECTION_CONFIDENCE,
                minHandTrackingConfidence = GestureRecognizerHelper.DEFAULT_HAND_TRACKING_CONFIDENCE,
                minHandPresenceConfidence = GestureRecognizerHelper.DEFAULT_HAND_PRESENCE_CONFIDENCE,
                currentDelegate = GestureRecognizerHelper.DELEGATE_CPU,
                gestureRecognizerListener = this
            )
        }
    }

    private fun setUpCamera() {
        val cameraProviderFuture =
            ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                bindCameraUseCases()
            }, ContextCompat.getMainExecutor(requireContext())
        )
    }

    @SuppressLint("UnsafeOptInUsageError", "RestrictedApi")
    private fun bindCameraUseCases(delay: Long = 0L) {

        if (activity == null) {
            return
        }

        val cameraModeViewModel =
            ViewModelProvider(requireActivity())[CameraModeViewModel::class.java]

        aspectRatio = cameraModeViewModel.cameraAspectRatio.value ?: AspectRatio.RATIO_4_3

       val cameraProvider = cameraProvider
            ?: throw IllegalStateException("Camera initialization failed.")

        val cameraOrientation =
            cameraModeViewModel.cameraOrientation.value ?: CameraSelector.LENS_FACING_BACK

        cameraSelector =
            CameraSelector.Builder().requireLensFacing(cameraOrientation).build()

        val targetRotation = Formatter.orientationToSurfaceRotation(relativeOrientation.value ?: 0)

        preview = Preview.Builder()
            .setTargetAspectRatio(aspectRatio!!)
            .setTargetRotation(targetRotation)
            .build()

         imageAnalyzer =
            ImageAnalysis.Builder().setTargetAspectRatio(aspectRatio!!)
                .setTargetRotation(fragmentCameraBinding.viewFinder.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                .build()
                .also {
                    it.setAnalyzer(backgroundExecutor) { image ->
                        recognizeHand(image)
                    }
                }


        imageCapture = ImageCapture.Builder()
            .setTargetAspectRatio(aspectRatio!!)
            .setTargetRotation(targetRotation)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()


        val handler = Handler(Looper.getMainLooper())
        handler.postDelayed(
            {
                cameraProvider.unbindAll()
                try {
                    cameraProvider.bindToLifecycle(
                        viewLifecycleOwner, cameraSelector!!, preview, imageAnalyzer, imageCapture
                    )

                   preview?.setSurfaceProvider(fragmentCameraBinding.viewFinder.surfaceProvider)
                } catch (exc: Exception) {
                    Log.e(TAG, "Use case binding failed", exc)
                } finally {
                    cameraModeViewModel.setShouldRefreshCamera(true)
                }
            },
            delay
        )
    }

    private fun bindImageAnalyzer() {
        if (imageAnalyzer == null) {
            return
        }

        unbindImageAnalyzer()

        cameraProvider?.bindToLifecycle(
            viewLifecycleOwner, cameraSelector!!, imageAnalyzer
        )
    }

    private fun unbindImageAnalyzer() {
        if (imageAnalyzer == null) {
            return
        }
        cameraProvider?.unbind(imageAnalyzer)
    }

    private fun recognizeHand(imageProxy: ImageProxy) {
        gestureRecognizerHelper?.recognizeLiveStream(
            imageProxy = imageProxy,
            isFontCamera =
            fragmentCameraBinding
                .cameraModeViewModel?.cameraOrientation?.value == (CameraSelector.LENS_FACING_FRONT
                ?: true),
            deviceRotation = relativeOrientation.value ?: 0
        )
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        imageAnalyzer?.targetRotation =
            fragmentCameraBinding.viewFinder.display.rotation
    }

    override fun onError(error: String, errorCode: Int) {
        TODO("Not yet implemented")
    }

      override fun onResults(
        resultBundle: GestureRecognizerHelper.ResultBundle
    ) {
        activity?.runOnUiThread {
            if (_fragmentCameraBinding != null) {

                if (fragmentCameraBinding.gestureDetectViewModel?.shouldRunHandTracking?.value == false) {
                    fragmentCameraBinding.overlay.clear()
                    return@runOnUiThread
                }

            }

            fragmentCameraBinding.overlay.setResults(
                resultBundle.results.first(),
                resultBundle.inputImageHeight,
                resultBundle.inputImageWidth,
                resultBundle.deviceRotation,
                RunningMode.LIVE_STREAM
            )

            fragmentCameraBinding.overlay.invalidate()
        }
    }
}
