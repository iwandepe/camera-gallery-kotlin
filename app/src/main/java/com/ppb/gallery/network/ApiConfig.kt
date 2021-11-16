package com.ppb.gallery.network

import okhttp3.MultipartBody
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

class ApiConfig{

    companion object {

        // base url dari end point.
        const val BASE_URL = "http://192.168.0.1:8000/"
        const val ONLINE_URL = "https://gallery.ppb.iwanprakoso.com/"
        const val IMAGE_URL = BASE_URL + "image/"

    }

    // init retrofit
    private fun retrofit() : Retrofit{
        return Retrofit.Builder()
            .baseUrl(ONLINE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    // buat sebuah instance untuk call sebuah interface dari retrofit.
    fun instance() : ApiInterface {

        return retrofit().create(ApiInterface::class.java)

    }


}

// interface dari retrofit
interface ApiInterface{

    @Multipart
    @POST("imageUpload") // end point dari upload
    fun upload(
        @Part imagename:MultipartBody.Part

    ) : Call<Default> // memanggil response model 'Default'

    @POST("upload")
    @FormUrlEncoded
    fun uploadBase64(
        @Field("image") base64 : String,
    ) : Call<Default>

//    @GET("gallery.php") // end point untuk menampilkan semua data
//    fun gallery() : Call<Gallery> // memanggil response model 'Gallery'
//
//    @GET("delete.php") // end point untuk menghapus data
//    fun delete(
//
//        @Query("imageid") imageid:String?
//
//    ) : Call<Default> // memanggil response model 'Default'

}