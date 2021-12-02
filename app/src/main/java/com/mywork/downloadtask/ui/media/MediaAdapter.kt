package com.mywork.downloadtask.ui.media

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mywork.downloadtask.R
import com.mywork.downloadtask.data.remote.model.Media
import com.mywork.downloadtask.databinding.ItemMediaBinding
import javax.inject.Inject
//using list adapter instead of RecyclerView.Adapter to solve notifyDataSetChanged() and make asynchronous comparisons
class MediaAdapter @Inject constructor() : ListAdapter<Media , MediaAdapter.MediaVieHolder>(Media.itemCallback) {
    inner class MediaVieHolder(val binding:ItemMediaBinding) : RecyclerView.ViewHolder(binding.root){
        init {
            binding.btnDownload.setOnClickListener {
                clickListener.clickDownload(adapterPosition)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MediaVieHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val view:ItemMediaBinding = DataBindingUtil.inflate(layoutInflater , R.layout.item_media , parent , false)
        return MediaVieHolder(view)
    }

    override fun onBindViewHolder(holder: MediaVieHolder, position: Int) {
        val media: Media = getItem(position)
        holder.binding.media = media
        holder.binding.executePendingBindings()
    }

    private lateinit var clickListener: ClickListener

    fun setClickListener(clickListener: ClickListener){
        this.clickListener = clickListener
    }

    interface ClickListener {
        fun clickDownload(position: Int)
    }
}