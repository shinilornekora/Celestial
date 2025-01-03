import android.Manifest
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.media.MediaRecorder
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.app.ActivityCompat
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

fun generateUniqueKey(): String {
    val timestamp = System.currentTimeMillis().toString()
    val digest = MessageDigest.getInstance("SHA-256")
    val hash = digest.digest(timestamp.toByteArray())
    return hash.joinToString("") { "%02x".format(it) }
}

class CameraHelper(
    private val context: Context,
    private val textureView: TextureView,
    private val imageSavedCallback: (File) -> Unit
) {
    private val cameraManager: CameraManager = context.getSystemService(CameraManager::class.java)
    private var cameraDevice: CameraDevice? = null
    private lateinit var cameraId: String
    private lateinit var imageReader: ImageReader
    private var cameraCaptureSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var isRecording = false
    private var currentStaticFileName = ""
    private val logTag = "[CameraHelper]"
    private var isFrontCamera = false

    private fun log(message: String) {
        Log.d(logTag, message)
    }

    fun startVideoRecording(outputFile: File) {
        if (isRecording) return

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO),
                1
            )
        }

        mediaRecorder?.apply {
            reset()
            release()
        }

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
            setOutputFile(outputFile.absolutePath)
            setVideoEncodingBitRate(10000000)
            setVideoFrameRate(30)
            setVideoSize(1920, 1080)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            prepare()
        }

        currentStaticFileName = outputFile.absolutePath

        cameraDevice?.let { camera ->
            val surfaceTexture = textureView.surfaceTexture ?: return
            surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
            val previewSurface = Surface(surfaceTexture)
            val recorderSurface = mediaRecorder!!.surface

            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                addTarget(previewSurface)
                addTarget(recorderSurface)
            }

            camera.createCaptureSession(
                listOf(previewSurface, recorderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                        mediaRecorder?.start()
                        isRecording = true

                        log("Успешно начата запись видео")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        log("Ошибка настройки сессии для записи видео.")
                    }
                },
                null
            )
        }
    }

    fun switchCamera() {
        isFrontCamera = !isFrontCamera
        releaseResources() // Освобождаем ресурсы текущей камеры
        openCamera() // Открываем новую камеру
    }

    fun stopVideoRecording() {
        if (!isRecording) {
            log("Съемка не ведется.")
            return
        }

        log("Пытаемся закончить запись")

        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
        isRecording = false
        startPreview()

        log("Запись завершена. Видео сохранено в: $currentStaticFileName")
    }

    fun openCamera() {
        val cameraPermissionStatus = ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA)

        if (cameraPermissionStatus != PackageManager.PERMISSION_GRANTED) {
            log("Не хватает разрешения на камеру.")
            return
        }

        cameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val characteristics = cameraManager.getCameraCharacteristics(id)
            val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (isFrontCamera) facing == CameraCharacteristics.LENS_FACING_FRONT
            else facing == CameraCharacteristics.LENS_FACING_BACK
        } ?: return log("Камера не найдена.")

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val streamConfigMap = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val previewSize = streamConfigMap?.getOutputSizes(SurfaceTexture::class.java)?.firstOrNull()
            ?: return log("Не удалось получить размеры потока.")

        imageReader = ImageReader.newInstance(previewSize.width, previewSize.height, ImageFormat.JPEG, 1)
        imageReader.setOnImageAvailableListener({ reader ->
            reader.acquireLatestImage()?.let { saveImage(it) }
        }, null)

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                startPreview()
                log("Камера открыта: ${if (isFrontCamera) "фронтальная" else "задняя"}")
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
                log("Ошибка в камере: $error")
            }
        }, null)
    }

    private fun startPreview() {
        val surfaceTexture = textureView.surfaceTexture ?: return log("Текстура не доступна.")
        surfaceTexture.setDefaultBufferSize(textureView.width, textureView.height)
        val previewSurface = Surface(surfaceTexture)

        cameraDevice?.let { camera ->
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                addTarget(previewSurface)
            }

            camera.createCaptureSession(
                listOf(previewSurface, imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        cameraCaptureSession = session
                        session.setRepeatingRequest(captureRequestBuilder.build(), null, null)
                        log("Превью начато")
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        log("Не удалось настроить превью.")
                    }
                },
                null
            )
        }
    }

    fun takePhoto(outputFile: File) {
        cameraDevice?.let { camera ->
            val captureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE).apply {
                addTarget(imageReader.surface)
                val rotation = (context as? androidx.fragment.app.FragmentActivity)?.windowManager?.defaultDisplay?.rotation
                set(CaptureRequest.JPEG_ORIENTATION, getOrientation(rotation ?: Surface.ROTATION_0))
            }

            camera.createCaptureSession(
                listOf(imageReader.surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.capture(captureRequestBuilder.build(), null, null)

                        triggerBlinkAnimation {
                            startPreview()
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        log("Захват изображения не удался.")
                    }
                },
                null
            )
        }
    }

    private fun saveImage(image: Image) {
        val currentTS = generateUniqueKey().take(10)
        val file = File(context.getExternalFilesDir(null), ("$currentTS.jpg"))
        val buffer = image.planes.first().buffer
        val bytes = ByteArray(buffer.remaining())

        buffer.get(bytes)
        FileOutputStream(file).use {
            it.write(bytes)
            image.close()
        }

        log("Сохранено изображение в: ${file.absolutePath}")
        imageSavedCallback(file)
    }

    private fun getOrientation(rotation: Int): Int = when (rotation) {
        Surface.ROTATION_0 -> 90
        Surface.ROTATION_90 -> 0
        Surface.ROTATION_180 -> 270
        Surface.ROTATION_270 -> 180
        else -> 90
    }

    private fun triggerBlinkAnimation(onAnimationEnd: () -> Unit) {
        ValueAnimator.ofFloat(1f, 0f, 1f).apply {
            duration = 300
            interpolator = AccelerateDecelerateInterpolator()

            addUpdateListener { animator ->
                val alphaValue = animator.animatedValue as Float
                textureView.alpha = alphaValue
            }

            addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    super.onAnimationEnd(animation)
                    onAnimationEnd()
                }
            })

            start()
        }
    }

    fun releaseResources() {
        cameraCaptureSession?.close()
        cameraDevice?.close()
        imageReader.close()
    }
}
