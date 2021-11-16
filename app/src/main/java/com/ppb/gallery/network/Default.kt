package com.ppb.gallery.network
import com.google.gson.annotations.SerializedName

data class Default (

//    @SerializedName("message")
//    var message:String?

    @SerializedName("success")
    var success:Boolean?,

    @SerializedName("url")
    var url:String?

)
