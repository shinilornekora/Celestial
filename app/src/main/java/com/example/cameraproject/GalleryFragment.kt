package com.example.cameraproject

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
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
        if (mediaFiles.isEmpty()) {
            Toast.makeText(requireContext(), "Ничего не найдено!", Toast.LENGTH_SHORT).show()
        }

        mediaAdapter = MediaAdapter(mediaFiles) { file ->
            Toast.makeText(requireContext(), "Выбрано: ${file.name}", Toast.LENGTH_SHORT).show()
        }

        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = mediaAdapter
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
