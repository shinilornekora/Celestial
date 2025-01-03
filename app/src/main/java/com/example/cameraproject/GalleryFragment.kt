package com.example.cameraproject

import MediaAdapter
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.cameraproject.databinding.GalleryFragmentBinding
import java.io.File

class GalleryFragment : Fragment() {
    private var _binding: GalleryFragmentBinding? = null
    private val binding get() = _binding!!

    private lateinit var mediaAdapter: MediaAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = GalleryFragmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupGallery()
    }

    private fun setupGallery() {
        val mediaFiles = getMediaFiles()
        val walker = findNavController()

        if (mediaFiles.isEmpty()) {
            Toast.makeText(requireContext(), "Ничего не найдено!", Toast.LENGTH_SHORT).show()
        }

        mediaAdapter = MediaAdapter(mediaFiles,
            { file ->
                Toast.makeText(requireContext(), "Выбрано: ${file.name}", Toast.LENGTH_SHORT).show()
            },
            { file ->
                openFileWithIntent(file)
            }
        )

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = mediaAdapter
        }

        binding.photoButton.setOnClickListener {
            walker.navigate(R.id.action_gallery_to_photo)
        }

        binding.videoButton.setOnClickListener {
            walker.navigate(R.id.action_gallery_to_video)
        }
    }

    private fun openFileWithIntent(file: File) {
        val uri = Uri.fromFile(file)

        val mimeType = when (file.extension) {
            "jpg", "jpeg", "png" -> "image/*"
            "mp4" -> "video/*"
            else -> "*/*"
        }

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

        try {
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Нет доступных приложений для открытия файла", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getMediaFiles(): List<File> {
        val mediaDir = requireContext().getExternalFilesDir(null) ?: return emptyList()
        return mediaDir.listFiles()?.filter { it.isFile && (it.extension == "jpg" || it.extension == "mp4") } ?: emptyList()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
