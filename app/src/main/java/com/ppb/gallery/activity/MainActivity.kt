package com.ppb.gallery.activity

import android.Manifest
import android.app.ProgressDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ppb.gallery.R
import com.ppb.gallery.adapter.GalleryImageAdapter
import com.ppb.gallery.adapter.GalleryImageClickListener
import com.ppb.gallery.adapter.Image
import com.ppb.gallery.fragment.GalleryFullscreenFragment
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.util.Base64
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Button
import android.widget.ImageButton
import com.ppb.gallery.network.ApiConfig
import com.ppb.gallery.network.Default
import com.ppb.gallery.network.HttpHandler
import okhttp3.*
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import retrofit2.Response
import java.io.*


class MainActivity : AppCompatActivity(), GalleryImageClickListener {
    // gallery column count
    private val SPAN_COUNT = 3
//    private val CAMERA_REQUEST = 1888
    private val imageList = ArrayList<Image>()
    private lateinit var galleryAdapter: GalleryImageAdapter
    private lateinit var currentPhotoPath: String
    val REQUEST_IMAGE_CAPTURE = 1
    lateinit var ivTest : ImageView
    lateinit var btnUpload : Button
    lateinit var imagename:MultipartBody.Part
    private val imgList = ArrayList<String>()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ivTest = findViewById(R.id.ivTest) as ImageView
        btnUpload = findViewById(R.id.btnUpload) as Button
        btnUpload.setOnClickListener {
            uploadImage()
        }
        btnUpload.isEnabled = false

        // init adapter
        galleryAdapter = GalleryImageAdapter(imageList)
        galleryAdapter.listener = this
        // init recyclerview
        val recyclerView = findViewById(R.id.recyclerView) as RecyclerView

        recyclerView.layoutManager = GridLayoutManager(this, SPAN_COUNT)
        recyclerView.adapter = galleryAdapter
        // load images
        loadImages()

        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_IMAGE_CAPTURE)

        setSupportActionBar(toolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_add -> {
                Toast.makeText(applicationContext, "click on add image", Toast.LENGTH_LONG).show()

//                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
//                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)

                dispatchTakePictureIntent()

                return true
            }
            R.id.action_exit ->{
                Toast.makeText(applicationContext, "click on exit", Toast.LENGTH_LONG).show()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadImages() {
        imageList.clear()
        fetchData()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
//            val photo: Bitmap = data?.extras?.get("data") as Bitmap
//            ivTest.setImageBitmap(photo)

            setPic()
            btnUpload.isEnabled = true

            // TODO: Captured image quality is very low                     (v)
            // TODO: Capture image still use extra                          (?)
            // TODO: POST image captured to server                          (v)
            // TODO: Get url of newly posted image and update gallery       (~)
        }
    }

    override fun onClick(position: Int) {
        val bundle = Bundle()
        bundle.putSerializable("images", imageList)
        bundle.putInt("position", position)
        val fragmentTransaction = supportFragmentManager.beginTransaction()
        val galleryFragment = GalleryFullscreenFragment()
        galleryFragment.setArguments(bundle)
        galleryFragment.show(fragmentTransaction, "gallery")
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File = getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        ).apply {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = absolutePath
            Log.d("Main", "Photopath : "+currentPhotoPath)
        }
    }

    private fun dispatchTakePictureIntent() {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            // Ensure that there's a camera activity to handle the intent
            takePictureIntent.resolveActivity(packageManager)?.also {
                // Create the File where the photo should go
                val photoFile: File? = try {
                    createImageFile()
                } catch (ex: IOException) {
                    // Error occurred while creating the File
                    Log.e("main", "dispatchTakePictureIntent IOException: ")
                    null
                }
                // Continue only if the File was successfully created
                photoFile?.also {
                    val photoURI: Uri = FileProvider.getUriForFile(
                        this,
                        "com.example.android.fileprovider",
                        photoFile
                    )
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)
                    startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
                }
            }
        }
    }

    private fun galleryAddPic() {
        Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE).also { mediaScanIntent ->
            val f = File(currentPhotoPath)
            mediaScanIntent.data = Uri.fromFile(f)
            sendBroadcast(mediaScanIntent)
        }
    }

    private fun setPic() {
        // Get the dimensions of the View
        val targetW: Int = ivTest.getWidth()
        val targetH: Int = ivTest.getHeight()

        // Get the dimensions of the bitmap
        val bmOptions = BitmapFactory.Options()
        bmOptions.inJustDecodeBounds = true
        BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
        val photoW = bmOptions.outWidth
        val photoH = bmOptions.outHeight

        // Determine how much to scale down the image
        val scaleFactor = Math.max(1, Math.min(photoW / targetW, photoH / targetH))

        // Decode the image file into a Bitmap sized to fill the View
        bmOptions.inJustDecodeBounds = false
        bmOptions.inSampleSize = scaleFactor
        bmOptions.inPurgeable = true
        val bitmap = BitmapFactory.decodeFile(currentPhotoPath, bmOptions)
        ivTest.setImageBitmap(bitmap)
    }

    private fun uploadImage(){
//        Toast.makeText(applicationContext, "Upload image", Toast.LENGTH_SHORT).show()

//        val requestBody = RequestBody.create(MediaType.parse("multipart"), File(currentPhotoPath))
//        imagename = MultipartBody.Part.createFormData("imageName", File(currentPhotoPath)?.name, requestBody)
//        val call = ApiConfig().instance().upload(imagename)
        val base64 : String = "data:image/jpeg;base64," + imageToBase64(currentPhotoPath)

        val call = ApiConfig().instance().uploadBase64(
            base64
        )

        call.enqueue(object : retrofit2.Callback<Default>{

            override fun onFailure(call: retrofit2.Call<Default>?, t: Throwable?) {
                Toast.makeText(applicationContext,"Connection error",Toast.LENGTH_SHORT).show()
                Log.d("UploadFailure", t.toString())
            }

            override fun onResponse(call: retrofit2.Call<Default>?, response: Response<Default>?) {
                Toast.makeText(applicationContext, "Upload Success to " + response?.body()?.url, Toast.LENGTH_LONG).show()

                if(response?.isSuccessful == true){
                    imageList.clear()
                    fetchData()

                    ivTest.setImageDrawable(getDrawable(R.drawable.ic_launcher_background))
                    btnUpload.isEnabled = false
                }

                response?.body()?.toString()?.let { Log.i("ResponseBody", it) }

//                if(response?.body()?.message?.contains("Success",true)!!){
//                    this@UploadActivity.finish()
//                }

            }

        })

    }

    fun imageToBase64(fileName: String) : String{
        val inputStream: InputStream = FileInputStream(fileName)

        val bytes: ByteArray
        val buffer = ByteArray(8192)
        var bytesRead: Int
        val output = ByteArrayOutputStream()

        try {
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                output.write(buffer, 0, bytesRead)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

        bytes = output.toByteArray()

        return android.util.Base64.encodeToString(bytes, Base64.DEFAULT)


    }

    fun fetchData() {
        GetData().execute()
    }

    inner class GetData : AsyncTask<Void?, Void?, Void?>() {
        override fun onPreExecute() {
            super.onPreExecute()

            Log.i("Main", "onPreExecute")
        }

        override fun onPostExecute(result: Void?) {
            super.onPostExecute(result)

            Log.i("Main", "onPostExecute")
            galleryAdapter.notifyDataSetChanged()

        }

        protected override fun doInBackground(vararg p0: Void?): Void? {
            val httpHandler = HttpHandler()
            val url = "https://gallery.ppb.iwanprakoso.com/images"
            val jsonStr: String? = httpHandler.makeServiceCall(url)
            var imgUrl : String

            if (jsonStr != null) {
                try {
                    val response = JSONObject(jsonStr)
                    val status = response.getBoolean("success") as Boolean
                    val imgArray = response.getJSONArray("urls")
                    for (i in 0 until imgArray.length()) {
                        imgArray.getString(i).also { imgUrl = it }
                        imgUrl = "https://$imgUrl"
                        Log.i("IMGURL", imgUrl + " == " + i.toString())

                        imageList.add(Image(imgUrl, "Image $i"))
                    }
                } catch (e: JSONException) {
                    Log.e("Main", "JSON Parsing Error: " + e.message)
                }
            } else {
                Log.e("Main", "Couldn't get json from server.")
            }

            return null
        }
    }

}

//    ==== SOURCES !!!
//    https://stackoverflow.com/questions/20322528/uploading-images-to-server-android
//    https://handyopinion.com/upload-file-to-server-in-android-kotlin/
//    https://www.youtube.com/watch?v=dbSAIuyHInY
//    https://erthru.medium.com/upload-gambar-menggunakan-retrofit-2-76fc74a232a9