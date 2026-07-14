package com.example.livekitdemo.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.livekitdemo.R
import com.example.livekitdemo.databinding.ItemParticipantBinding
import com.example.livekitdemo.model.ParticipantModel
import io.livekit.android.renderer.SurfaceViewRenderer
import io.livekit.android.room.track.VideoTrack

class ParticipantAdapter(
    private val initRenderer: (SurfaceViewRenderer) -> Unit,
) : ListAdapter<ParticipantModel, ParticipantAdapter.ParticipantViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantViewHolder(binding, initRenderer)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    override fun onViewRecycled(holder: ParticipantViewHolder) {
        holder.unbind()
    }

    class ParticipantViewHolder(
        private val binding: ItemParticipantBinding,
        private val initRenderer: (SurfaceViewRenderer) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        private var boundTrack: VideoTrack? = null
        private var rendererInitialized = false

        fun bind(model: ParticipantModel) {
            binding.textName.text = if (model.isLocal) {
                itemView.context.getString(R.string.you_label)
            } else {
                model.displayName
            }
            binding.textMicOff.visibility = if (model.isMicEnabled) View.GONE else View.VISIBLE
            binding.textCameraOff.visibility = if (model.isCameraEnabled) View.GONE else View.VISIBLE

            if (!rendererInitialized) {
                initRenderer(binding.videoRenderer)
                rendererInitialized = true
            }

            val newTrack = model.videoTrack
            if (newTrack !== boundTrack) {
                boundTrack?.removeRenderer(binding.videoRenderer)
                newTrack?.addRenderer(binding.videoRenderer)
                boundTrack = newTrack
            }
        }

        fun unbind() {
            boundTrack?.removeRenderer(binding.videoRenderer)
            boundTrack = null
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<ParticipantModel>() {
            override fun areItemsTheSame(oldItem: ParticipantModel, newItem: ParticipantModel) =
                oldItem.sid == newItem.sid

            override fun areContentsTheSame(oldItem: ParticipantModel, newItem: ParticipantModel) =
                oldItem == newItem
        }
    }
}
