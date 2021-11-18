package com.ppb.gallery.network

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class ApiConfig{

    companion object {
        const val ONLINE_URL = "https://gallery.ppb.iwanprakoso.com/"
    }

    private fun retrofit() : Retrofit{
        return Retrofit.Builder()
            .baseUrl(ONLINE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // Buat sebuah instance untuk call sebuah interface dari retrofit.
    fun instance() : ApiInterface {
        return retrofit().create(ApiInterface::class.java)

    }

}

interface ApiInterface{

    @Multipart
    @POST("imageUpload")
    fun upload(
        @Part imagename:MultipartBody.Part

    ) : Call<Default>

    @POST("upload")
    @FormUrlEncoded
    fun uploadBase64(
        @Field("image") base64 : String,
    ) : Call<Default>


}