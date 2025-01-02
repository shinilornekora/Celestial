package com.example.cameraproject

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Surface
import android.view.View
import android.view.ViewGroup
import android.view.TextureView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cameraproject.databinding.PhotoFragmentBinding
import java.io.File
import java.io.FileOutputStream

class PhotoFragment : Fragment() {
    private var _binding: PhotoFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var cameraId: String
    private lateinit var textureView: TextureView
    private lateinit var imageReader: ImageReader
    private lateinit var cameraCaptureSession: CameraCaptureSession

    private val REQUEST_CODE_PERMISSIONS = 101

    private fun log(message: String) {
        Log.d("[PHOTO]", message)
    }

    private fun checkPermissions() {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)

        if (cameraPermission != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
            log("No enough permissions to start.")
        } else {
            log("All is OK, starting the camera...")
            openCamera()
        }
    }

    private fun openCamera() {
        cameraManager = requireActivity().getSystemService(CameraManager::class.java)
        cameraId = cameraManager.cameraIdList[0]
        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSize = map?.getOutputSizes(SurfaceTexture::class.java)?.get(0)

        imageReader = ImageReader.newInstance(previewSize!!.width, previewSize.height, ImageFormat.JPEG, 1)

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {

            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview(camera)
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
            }
        }, null)
    }

    private fun startPreview(cameraDevice: CameraDevice) {
        textureView = binding.cameraContainer.textureView
        val surfaceTexture = textureView.surfaceTexture!!
        surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
        val surface = Surface(surfaceTexture)

        val captureRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)

        cameraDevice.createCaptureSession(listOf(surface, imageReader.surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                log("Configuration failed")
            }
        }, null)

        imageReader.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            saveImage(image)
            image.close()
        }, null)
    }

    private fun saveImage(image: Image?) {
        if (image == null) return

        val buffer = image.planes[0].buffer
        val bytes = ByteArray(buffer.remaining())
        buffer.get(bytes)

        val file = File(requireActivity().getExternalFilesDir(null), "photo.jpg")
        FileOutputStream(file).use { output ->
            output.write(bytes)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                log("Camera permission denied")
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        log("onCreateView callback")
        _binding = PhotoFragmentBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        log("onViewCreated callback")
        super.onViewCreated(view, savedInstanceState)

        val walker = findNavController()

        checkPermissions()

        binding.switchButton.setOnClickListener {
            log("Clicked on switcher")
            walker.navigate(R.id.action_photo_to_video)
        }

        binding.photoFooter.captureButton.setOnClickListener {
            log("Clicked on action button")
            takePhoto()
        }

        binding.photoFooter.galleryButton.setOnClickListener {
            log("Clicked on gallery button")
            walker.navigate(R.id.action_photo_to_gallery)
        }
    }

    private fun takePhoto() {
        if (cameraDevice == null) {
            log("Camera device is not available")
            return
        }

        try {
            val captureRequestBuilder = cameraDevice?.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            val imageReader = imageReader

            captureRequestBuilder?.addTarget(imageReader.surface)

            val rotation = requireActivity().windowManager.defaultDisplay.rotation
            captureRequestBuilder?.set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation))

            cameraDevice.createCaptureSession(listOf(imageReader.surface), object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    try {
                        session.capture(captureRequestBuilder!!.build(), object : CameraCaptureSession.CaptureCallback() {
                            override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {
                                super.onCaptureCompleted(session, request, result)
                                log("Image captured")
                            }
                        }, null)
                    } catch (e: CameraAccessException) {
                        log("Camera access exception: ${e.message}")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    log("Camera configuration failed")
                }
            }, null)
        } catch (e: CameraAccessException) {
            log("Camera access exception: ${e.message}")
        }
    }

    private fun getOrientation(rotation: Int): Int {
        return when (rotation) {
            Surface.ROTATION_0 -> 90
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> 90
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}