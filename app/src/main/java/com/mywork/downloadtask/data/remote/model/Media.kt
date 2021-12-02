package com.mywork.downloadtask.data.remote.model

import androidx.recyclerview.widget.DiffUtil
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medias")
data class Media(
    @PrimaryKey val id: Int,
    val type: String,
    val url: String,
    val name: String,
    var isDownloaded: Boolean,
    var isDownloading: Boolean,
    var downloadedId: Long
) {

    companion object {
        var itemCallback: DiffUtil.ItemCallback<Media> =
            object :
                DiffUtil.ItemCallback<Media>() {
                override fun areItemsTheSame(
                    oldItem: Media,
                    newItem: Media
                ): Boolean {
                    return oldItem.id == newItem.id
                }

                override fun areContentsTheSame(
                    oldItem: Media,
                    newItem: Media
                ): Boolean {
                    return oldItem == newItem
                }
            }
    }
}