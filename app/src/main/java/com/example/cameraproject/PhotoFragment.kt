package com.example.cameraproject

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cameraproject.databinding.PhotoFragmentBinding

class PhotoFragment : Fragment() {
    private var _binding: PhotoFragmentBinding? = null
    private val binding get() = _binding!!

    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_CODE_PERMISSIONS = 101

    private fun log(message: String) {
        Log.d("[PHOTO]", message)
    }

    private fun checkPermissions() {
        val cameraPermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
        val storagePermission = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (cameraPermission != PackageManager.PERMISSION_GRANTED || storagePermission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                requireActivity(),
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSIONS
            )
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            startActivityForResult(intent, REQUEST_IMAGE_CAPTURE)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                log("Camera permission denied")
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == Activity.RESULT_OK) {
            val imageBitmap = data?.extras?.get("data") as Bitmap
            binding.cameraContainer.cameraPreview.setImageBitmap(imageBitmap)
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

        binding.switchButton.setOnClickListener {
            log("Clicked on switcher")
            walker.navigate(R.id.action_photo_to_video)
        }

        binding.photoFooter.captureButton.setOnClickListener {
            log("Clicked on action button")
            checkPermissions()
        }

        binding.photoFooter.galleryButton.setOnClickListener {
            log("Clicked on gallery button")
            walker.navigate(R.id.action_photo_to_gallery)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}