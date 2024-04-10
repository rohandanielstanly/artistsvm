package com.vachanammusic.artists

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.vachanammusic.artists.databinding.FileUploadingBinding
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okio.Buffer
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder

class SongUploadingActivity : AppCompatActivity() {

    private val REQUEST_AUDIO_FILE = 1001
    private val UPLOAD_URL = "https://vachanammusic.com/songsUpload.php/"
    private lateinit var binding: FileUploadingBinding
    private var isUploading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = FileUploadingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        design()
        binding.audioEditEdt.isEnabled = false
        binding.imageEditEdt.isEnabled = false

//      fetchArtistAliases() // Call the function here
        fetchArtistAliasesForCurrentUser()

        binding.imgInfo.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://vachanammusic.com/artistsManual.pdf")
            startActivity(intent)
        }
        binding.uploadAudioBtn.background = getGradientDrawable(60, 2, -0x555556, Color.TRANSPARENT)
        binding.uploadImageBtn.background = getGradientDrawable(60, 2, -0x555556, Color.TRANSPARENT)
        binding.uploadToDatabaseBtn.background =
            getGradientDrawable(60, 2, -0x555556, Color.TRANSPARENT)

        val languages = arrayOf("Malayalam", "Tamil", "Hindi", "English", "Telugu", "Gujarati")
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.songLanguageSpinner.adapter = languageAdapter

        binding.songLanguageSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLanguage = languages[position]
                    binding.songLanguageTxt.text =
                        languageAbbreviations[selectedLanguage] ?: selectedLanguage
                    val textColor = Color.WHITE
                    val selectedView = binding.songLanguageSpinner.getChildAt(0) as? TextView
                    selectedView?.setTextColor(textColor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.uploadAudioBtn.setOnClickListener { uploadFileWithIntent("audio/*") }

        binding.uploadImageBtn.setOnClickListener { uploadFileWithIntent("image/*") }

        binding.uploadToDatabaseBtn.setOnClickListener {
            if (!isUploading) {
                // Set upload status to true
                isUploading = true

                val audioUrl = binding.audioEditEdt.text.toString()
                val imageUrl = binding.imageEditEdt.text.toString()
                val songTitle = binding.songTitleEdt.text.toString()
                val songDes = binding.songDesEdt.text.toString()
                val videoUrl = binding.videoUrlEdt.text.toString()
                val songLyrics = binding.songLyricsEdt.text.toString()
                val songViews = binding.songViewsEdt.text.toString()
                val artistId = binding.artistIdTxt.text.toString()
                val songLanguage = binding.songLanguageTxt.text.toString()

                if (audioUrl.isNotEmpty() && imageUrl.isNotEmpty() && songTitle.isNotEmpty() && songDes.isNotEmpty()) {
                    uploadToDatabase(
                        audioUrl,
                        imageUrl,
                        artistId,
                        songLanguage,
                        songTitle,
                        songDes,
                        videoUrl,
                        songLyrics,
                        songViews
                    )
                } else {
                    // Reset upload status to false
                    isUploading = false

                    Toast.makeText(
                        this@SongUploadingActivity,
                        "The fields are empty",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                Toast.makeText(
                    this@SongUploadingActivity,
                    "Upload in progress. Please wait.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private val languageAbbreviations = mapOf(
        "Malayalam" to "ml",
        "Tamil" to "ta",
        "Hindi" to "hi",
        "English" to "en",
        "Telugu" to "te",
        "Gujarati" to "gu"
    )

    private fun uploadFileWithIntent(type: String) {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = type
        startActivityForResult(intent, REQUEST_AUDIO_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_AUDIO_FILE && resultCode == Activity.RESULT_OK && data != null) {
            val selectedFileUri: Uri? = data.data
            selectedFileUri?.let {
                val filePath = getPathFromUri(selectedFileUri)
                filePath?.let { uploadFile(File(it), getMimeType(it)) }
                    ?: Toast.makeText(this, "Failed to get file path", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getPathFromUri(uri: Uri): String? {
        var filePath: String? = null
        try {
            val inputStream: InputStream? = contentResolver.openInputStream(uri)
            val extension: String? = getExtensionFromUri(uri)
            extension?.let {
                val tempFile: File? = createTempFileFromStream(inputStream, it)
                tempFile?.let { filePath = it.absolutePath }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return filePath
    }

    private fun getFileExtension(file: File): String {
        val fileName = file.name
        val dotIndex = fileName.lastIndexOf('.')
        return if (dotIndex != -1 && dotIndex < fileName.length - 1) {
            fileName.substring(dotIndex + 1)
        } else {
            ""
        }
    }

    private fun getExtensionFromUri(uri: Uri): String? {
        val mimeType = contentResolver.getType(uri)
        return when (mimeType) {
            "audio/mpeg" -> "mp3"
            "audio/mp4" -> "m4a"
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            else -> null
        }
    }

    private fun createTempFileFromStream(inputStream: InputStream?, extension: String): File? {
        var tempFile: File? = null
        inputStream?.let {
            try {
                tempFile = File.createTempFile("temp", ".$extension", cacheDir)
                val outputStream = FileOutputStream(tempFile)
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return tempFile
    }

    private fun getMimeType(filePath: String): String? {
        return try {
            val extension = MimeTypeMap.getFileExtensionFromUrl(filePath)
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun isImageUpload(mimeType: String?): Boolean {
        return mimeType != null && (mimeType == "image/png" || mimeType == "image/jpeg")
    }

    private fun uploadFile(file: File, mimeType: String?) {
        if (mimeType == null || !(mimeType == "audio/mpeg" || mimeType == "audio/x-m4a" || mimeType == "image/png" || mimeType == "image/jpeg")) {
            Toast.makeText(this, "Upload failed: Unsupported file type", Toast.LENGTH_SHORT).show()
            return
        }

        binding.progressBar.progress = 0
        binding.progressTextView.text = "0%"

        if (file == null) {
            Toast.makeText(this, "Upload failed: File is null", Toast.LENGTH_SHORT).show()
            return
        }

        if (mimeType == null) {
            Toast.makeText(this, "Upload failed: MIME type is null", Toast.LENGTH_SHORT).show()
            return
        }

        val originalFileName = getOriginalFileName(file)
        val fileExtension = getFileExtension(file)

        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                "$originalFileName.$fileExtension",
                createProgressRequestBody(
                    mimeType.toMediaTypeOrNull(),
                    file
                ) { bytesWritten, contentLength ->
                    val percentage = (100 * bytesWritten / contentLength).toInt()
                    runOnUiThread {
                        binding.progressBar.progress = percentage
                        binding.progressTextView.text = if (percentage == 100) {
                            "Completed!"
                        } else {
                            "$percentage%"
                        }
                    }
                })
            .build()

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val uploadedFilePath = "https://vachanammusic.com/$responseBody"
                    runOnUiThread {
                        if (isImageUpload(mimeType)) {
                            binding.imageEditEdt.text =
                                Editable.Factory.getInstance().newEditable(uploadedFilePath)
                        } else {
                            binding.audioEditEdt.text =
                                Editable.Factory.getInstance().newEditable(uploadedFilePath)
                        }
                        Toast.makeText(
                            this@SongUploadingActivity,
                            "Upload successful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@SongUploadingActivity,
                            "Upload failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                val errorMessage = "Upload failed: " + e.message
                runOnUiThread {
                    Toast.makeText(this@SongUploadingActivity, errorMessage, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        })
    }

    private fun createProgressRequestBody(
        contentType: MediaType?,
        file: File,
        listener: (Long, Long) -> Unit
    ): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return contentType
            }

            override fun contentLength(): Long {
                return file.length()
            }

            override fun writeTo(sink: BufferedSink) {
                file.source().use { source ->
                    val buffer = Buffer()
                    var remaining = contentLength()
                    var readCount: Long
                    while (source.read(buffer, 2048).also { readCount = it } != -1L) {
                        sink.write(buffer, readCount)
                        remaining -= readCount
                        listener(contentLength() - remaining, contentLength())
                    }
                }
            }
        }
    }

    private fun getOriginalFileName(file: File): String {
        var originalFileName = file.name
        if (originalFileName.contains(".")) {
            originalFileName = originalFileName.substring(0, originalFileName.lastIndexOf('.'))
        }
        return originalFileName
    }

    private fun uploadToDatabase(
        audioUrl: String,
        imageUrl: String,
        artistId: String,
        songLanguage: String,
        songTitle: String,
        songDes: String,
        videoUrl: String,
        songLyrics: String,
        songViews: String
    ) {
        val client = OkHttpClient()

        val requestBody = FormBody.Builder()
            .add("audioUrl", audioUrl)
            .add("imageUrl", imageUrl)
            .add("artistId", artistId)
            .add("songLanguage", songLanguage)
            .add("songTitle", songTitle)
            .add("songDes", songDes)
            .add("videoUrl", videoUrl)
            .add("songLyrics", songLyrics)
            .add("songViews", songViews)
            .build()

        val request = Request.Builder()
            .url("https://vachanammusic.com/songsCreation.php")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Toast.makeText(
                            this@SongUploadingActivity,
                            "Upload to database successful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@SongUploadingActivity,
                            "Upload to database failed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@SongUploadingActivity,
                        "Failed to upload to database: " + e.message,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }


    private fun fetchArtistAliasesForCurrentUser() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid
        userId?.let { uid ->
            val artistRef = FirebaseDatabase.getInstance().reference
                .child("artists")
                .child(uid)

            artistRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    val artistTitles = ArrayList<String>() // Change variable name
                    val artistIds = ArrayList<String>()
                    dataSnapshot.children.forEach { artistSnapshot ->
                        val artistTitle =
                            artistSnapshot.getValue(String::class.java) // Change variable name
                        val artistId = artistSnapshot.key
                        artistTitle?.let {
                            artistTitles.add(artistTitle) // Change variable name
                            if (artistId != null) {
                                artistIds.add(artistId)
                            }
                        }
                    }
                    runOnUiThread {
                        updateArtistSpinner(
                            artistTitles,
                            artistIds
                        )
                    } // Change function call
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    runOnUiThread {
                        Toast.makeText(
                            this@SongUploadingActivity,
                            "Error fetching artist aliases: " + databaseError.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        } ?: run {
            Toast.makeText(
                this@SongUploadingActivity,
                "User ID not found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }


    private fun fetchArtistId(artistAlias: String) {
        val client = OkHttpClient()
        val encodedAlias = URLEncoder.encode(artistAlias, "UTF-8")

        val url =
            "https://vachanammusic.com/artistsIdCheck.php?api=getArtistId&artist_alias=$encodedAlias"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val responseData = response.body?.string()
                // Log the response body
                Log.d("ResponseDebug", "Response body: $responseData")
                // Check if the response contains "Artist not found"
                if (responseData == "Artist not found") {
                    runOnUiThread {
                        binding.artistIdTxt.text = "Artist not found"
                    }
                } else {
                    // Set the artist ID text view
                    runOnUiThread {
                        binding.artistIdTxt.text = responseData ?: "Artist ID not found"
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    binding.artistIdTxt.text = "Failed to fetch artist ID"
                }
            }
        })
    }

    private fun updateArtistSpinner(artistNames: ArrayList<String>, artistIds: ArrayList<String>) {
        val artistAdapter = ArrayAdapter(
            this@SongUploadingActivity,
            android.R.layout.simple_spinner_item,
            artistNames
        )
        artistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.artistNameSpinner.adapter = artistAdapter

        binding.artistNameSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedArtistName = artistNames[position]
                    val textColor = Color.WHITE
                    val selectedView = binding.artistNameSpinner.getChildAt(0) as? TextView
                    selectedView?.setTextColor(textColor)
                    fetchArtistId(selectedArtistName)
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {}
            }
    }

    private fun getGradientDrawable(
        cornerRadius: Int,
        strokeWidth: Int,
        strokeColor: Int,
        backgroundColor: Int
    ): GradientDrawable {
        return GradientDrawable().apply {
            this.cornerRadius = cornerRadius.toFloat()
            this.setStroke(strokeWidth, strokeColor)
            this.setColor(backgroundColor)
        }
    }

    private fun design() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.parseColor("#000000")
    }
}

