package com.ppb.gallery.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.Target
import com.ppb.gallery.R
import kotlinx.android.synthetic.main.item_gallery_image.view.*

class GalleryImageAdapter(private val itemList: List<Image>) : RecyclerView.Adapter<GalleryImageAdapter.ViewHolder>() {
    private var context: Context? = null
    var listener: GalleryImageClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryImageAdapter.ViewHolder {
        context = parent.context
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_image, parent,
            false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return itemList.size
    }

    override fun onBindViewHolder(holder: GalleryImageAdapter.ViewHolder, position: Int) {
        holder.bind()
    }
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind() {
            val image = itemList.get(adapterPosition)
            val height = itemView.ivGalleryImage.height
            val width =  itemView.ivGalleryImage.width
            // load image
            Glide.with(context!!)
                .load(image.imageUrl)
                .override(width, height)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(itemView.ivGalleryImage)
            // adding click or tap handler for our image layout
            itemView.container.setOnClickListener {
                listener?.onClick(adapterPosition)
            }
        }
    }
}