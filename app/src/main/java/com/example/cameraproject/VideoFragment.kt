package com.example.cameraproject

import CameraHelper
import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cameraproject.databinding.VideoFragmentBinding
import java.io.File

class VideoFragment : Fragment() {
    private var _binding: VideoFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var cameraHelper: CameraHelper
    private var isRecordingVideo = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = VideoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCameraHelper()
        checkPermissions()
        setupClickListeners()
    }

    private fun setupCameraHelper() {
        cameraHelper = CameraHelper(
            requireContext(),
            binding.cameraContainer.textureView
        ) { savedFile ->
            Log.d("VIDEO",  "сохранил видео в: ${savedFile.absolutePath}")
        }
    }

    private fun checkPermissions() {
        val context = requireContext()

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 101)
        } else {
            cameraHelper.openCamera()
        }
    }

    private fun setupClickListeners() {
        val navController = findNavController()

        binding.switchButton.setOnClickListener {
            navController.navigate(R.id.action_video_to_photo)
        }

        binding.videoFooter.captureButton.setOnClickListener {
            if (isRecordingVideo) {
                stopVideoRecording()
            } else {
                startVideoRecording()
            }
        }

        binding.videoFooter.galleryButton.setOnClickListener {
            navController.navigate(R.id.action_video_to_gallery)
        }
    }

    private fun startVideoRecording() {
        val videoFile = File(requireContext().getExternalFilesDir(null), "video.mp4")
        cameraHelper.startVideoRecording(videoFile)
        isRecordingVideo = true
        binding.videoFooter.captureButton.text = "Stop"
    }

    private fun stopVideoRecording() {
        cameraHelper.stopVideoRecording()
        isRecordingVideo = false
        binding.videoFooter.captureButton.text = "Record"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
