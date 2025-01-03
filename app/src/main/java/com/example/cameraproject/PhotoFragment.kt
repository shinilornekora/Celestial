package com.example.cameraproject

import CameraHelper
import android.Manifest
import android.content.pm.PackageManager
import android.hardware.camera2.*
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cameraproject.databinding.PhotoFragmentBinding
import java.io.File

class PhotoFragment : Fragment() {
    private var _binding: PhotoFragmentBinding? = null
    private val binding get() = _binding!!
    private lateinit var cameraHelper: CameraHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = PhotoFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        cameraHelper = CameraHelper(requireContext(), binding.cameraContainer.textureView) { file ->
            Log.d("[PHOTO]", "Photo saved: ${file.absolutePath}")
        }

        checkPermissions()
        setupClickListeners()
    }

    private fun checkPermissions() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        } else {
            cameraHelper.openCamera()
        }
    }

    private fun setupClickListeners() {
        binding.photoFooter.captureButton.setOnClickListener {
            cameraHelper.takePhoto(File(requireContext().getExternalFilesDir(null), "photo.jpg"))
        }

        binding.photoFooter.galleryButton.setOnClickListener {
            findNavController().navigate(R.id.action_photo_to_gallery)
        }

        binding.switchButton.setOnClickListener {
            findNavController().navigate(R.id.action_photo_to_video)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
