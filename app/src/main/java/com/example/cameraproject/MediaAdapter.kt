import android.view.GestureDetector
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.cameraproject.R
import java.io.File

class MediaAdapter(
    private val mediaFiles: List<File>,
    private val onItemClick: (File) -> Unit,
    private val onDoubleClick: (File) -> Unit
) : RecyclerView.Adapter<MediaAdapter.MediaViewHolder>() {

    inner class MediaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val thumbnail: ImageView = view.findViewById(R.id.thumbnail)

        private val gestureDetector = GestureDetector(view.context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDoubleTap(e: MotionEvent): Boolean {
                onDoubleClick(mediaFiles[adapterPosition])
                return super.onDoubleTap(e)
            }
        })

        init {
            view.setOnClickListener {
                onItemClick(mediaFiles[adapterPosition])
            }

            view.setOnTouchListener { v, event ->
                gestureDetector.onTouchEvent(event)
                false
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return MediaViewHolder(view)
    }

    override fun onBindViewHolder(holder: MediaViewHolder, position: Int) {
        val file = mediaFiles[position]
        Glide.with(holder.thumbnail.context)
            .load(file)
            .into(holder.thumbnail)
    }

    override fun getItemCount(): Int = mediaFiles.size
}
