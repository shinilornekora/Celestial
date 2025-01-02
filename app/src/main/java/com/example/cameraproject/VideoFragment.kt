package com.example.cameraproject

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.cameraproject.databinding.VideoFragmentBinding

class VideoFragment: Fragment() {
    private var _binding: VideoFragmentBinding? = null;
    private val page get() = _binding!!

    private fun log(message: String) {
        Log.d("[VIDEO]", message);
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        log("onCreateView callback")
        _binding = VideoFragmentBinding.inflate(inflater, container, false)

        return page.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        log("onViewCreated callback")
        super.onViewCreated(view, savedInstanceState)

        val walker = findNavController();

        page.switchButton.setOnClickListener {
            log("Clicked on switcher")
            walker.navigate(R.id.action_video_to_photo)
        }

        page.videoFooter.captureButton.setOnClickListener {
            log("Clicked on action button")
        }

        page.videoFooter.galleryButton.setOnClickListener {
            log("Clicked on gallery button")
            walker.navigate(R.id.action_video_to_gallery)
        }
    }
}