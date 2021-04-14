package com.nnt.mapapidemo

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.bumptech.glide.Glide


@BindingAdapter("loadImage")
fun loadImage(view: ImageView, url: String?){
    Glide.with(view)
        .load(url)
        .into(view)
}