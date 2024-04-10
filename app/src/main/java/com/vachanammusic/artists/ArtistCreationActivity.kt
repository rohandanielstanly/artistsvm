package com.vachanammusic.artists

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.vachanammusic.artists.databinding.ArtistCreationBinding
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.Buffer
import okio.BufferedSink
import okio.source
import org.json.JSONArray
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream


class ArtistCreationActivity : AppCompatActivity() {

    private val REQUEST_AUDIO_FILE = 1001
    private val UPLOAD_URL = "https://vachanammusic.com/artistsUpload.php/"
    private lateinit var binding: ArtistCreationBinding
    private var lastPressedButton: Button? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ArtistCreationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        design()

        binding.imgInfo.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://vachanammusic.com/artistsManual.pdf")
            startActivity(intent)
        }
        binding.profileBtn.background = getGradientDrawable(60, 2, -0x555556, Color.TRANSPARENT)
        binding.coverBtn.background = getGradientDrawable(60, 2, -0x555556, Color.TRANSPARENT)
        binding.upcomingBtn.background = getGradientDrawable(60, 2, -0x555556, Color.TRANSPARENT)

        binding.uploadToDatabaseBtn.background =
            getGradientDrawable(60, 2, -0x555556, Color.TRANSPARENT)

        val languages = arrayOf("Malayalam", "Tamil", "Hindi", "English", "Telegue", "Gujarati")
        val languageAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, languages)
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.songLanguageSpinner.adapter = languageAdapter

        binding.songLanguageSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedLanguage = languages[position]
                    binding.artistLanguageTxt.text =
                        languageAbbreviations[selectedLanguage] ?: selectedLanguage
                    // Rest of your code remains unchanged
                    val textColor = Color.WHITE
                    val selectedView = binding.songLanguageSpinner.getChildAt(0) as? TextView
                    selectedView?.setTextColor(textColor)
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }
        binding.profileBtn.setOnClickListener {
            lastPressedButton = binding.profileBtn
            uploadFileWithIntent("image/*")
        }
        binding.coverBtn.setOnClickListener {
            lastPressedButton = binding.coverBtn
            uploadFileWithIntent("image/*")
        }
        binding.upcomingBtn.setOnClickListener {
            lastPressedButton = binding.upcomingBtn
            uploadFileWithIntent("image/*")
        }

        binding.uploadToDatabaseBtn.setOnClickListener {
            val profileUrl = binding.profileEditEdt.text.toString()
            val coverUrl = binding.coverEditEdt.text.toString()
            val upcomingUrl = binding.upcomingEditEdt.text.toString()
            val artistTitle = binding.artistTitleEdt.text.toString()
            val artistDes = binding.artistDesEdt.text.toString()
            val artistLanguage = binding.artistLanguageTxt.text.toString()

            if (profileUrl.isNotEmpty() && coverUrl.isNotEmpty() && artistTitle.isNotEmpty() && artistDes.isNotEmpty()) {
                uploadToDatabase(
                    profileUrl,
                    coverUrl,
                    upcomingUrl,
                    artistLanguage,
                    artistTitle,
                    artistDes
                )
            } else {
                Toast.makeText(
                    this@ArtistCreationActivity,
                    "The fields are empty",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        fetchArtistAliases()
    }

    private val languageAbbreviations = mapOf(
        "Malayalam" to "aml",
        "Tamil" to "ata",
        "Hindi" to "ahi",
        "English" to "aen",
        "Telegue" to "ate",
        "Gujarati" to "agu"
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
                            val imageUrl = "https://vachanammusic.com/$responseBody"
                            when (lastPressedButton) {
                                binding.profileBtn -> {
                                    Glide.with(this@ArtistCreationActivity)
                                        .load(imageUrl)
                                        .apply(RequestOptions().centerCrop()) // Optional: You can apply additional options here
                                        .into(binding.profileImageView)
                                    binding.profileEditEdt.text = Editable.Factory.getInstance().newEditable(uploadedFilePath)
                                }

                                binding.coverBtn -> {
                                    Glide.with(this@ArtistCreationActivity)
                                        .load(imageUrl)
                                        .apply(RequestOptions().centerCrop()) // Optional: You can apply additional options here
                                        .into(binding.coverImageView)
                                    binding.coverEditEdt.text = Editable.Factory.getInstance().newEditable(uploadedFilePath)

                                }

                                binding.upcomingBtn -> {
                                    Glide.with(this@ArtistCreationActivity)
                                        .load(imageUrl)
                                        .apply(RequestOptions().centerCrop()) // Optional: You can apply additional options here
                                        .into(binding.upcomingImageView)
                                    binding.upcomingEditEdt.text = Editable.Factory.getInstance().newEditable(uploadedFilePath)

                                }
                            }
                        } else {
                            Toast.makeText(
                                this@ArtistCreationActivity,
                                "Upload failed: Unsupported file type",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        Toast.makeText(
                            this@ArtistCreationActivity,
                            "Upload successful",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@ArtistCreationActivity,
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
                    Toast.makeText(this@ArtistCreationActivity, errorMessage, Toast.LENGTH_SHORT)
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
        profileUrl: String,
        coverUrl: String,
        upcomingUrl: String,
        artistLanguage: String,
        artistTitle: String,
        artistDes: String
    ) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val userId = currentUser?.uid

        userId?.let { uid ->
            val artistRef = FirebaseDatabase.getInstance().reference
                .child("artists")
                .child(uid)

            // Check if the artist with the same name already exists
            artistRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    var artistExists = false
                    for (artistSnapshot in dataSnapshot.children) {
                        val artistName = artistSnapshot.getValue(String::class.java)
                        if (artistName == artistTitle) {
                            artistExists = true
                            break
                        }
                    }

                    if (artistExists) {
                        // Artist with the same name already exists
                        runOnUiThread {
                            Toast.makeText(
                                this@ArtistCreationActivity,
                                "Artist with the same name already exists in Firebase",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // Proceed with uploading data to the database
                        val client = OkHttpClient()

                        val requestBody = FormBody.Builder()
                            .add("profileUrl", profileUrl)
                            .add("coverUrl", coverUrl)
                            .add("upcomingUrl", upcomingUrl)
                            .add("artistLanguage", artistLanguage)
                            .add("artistTitle", artistTitle)
                            .add("artistDes", artistDes)
                            .build()

                        val request = Request.Builder()
                            .url("https://vachanammusic.com/artistsCreation.php")
                            .post(requestBody)
                            .build()

                        client.newCall(request).enqueue(object : Callback {
                            override fun onResponse(call: Call, response: Response) {
                                if (response.isSuccessful) {
                                    // Store the artist ID in Firebase Realtime Database
                                    val artistId = artistTitle
                                    if (artistId.isNotEmpty()) {
                                        val newArtistRef = artistRef.push() // Generate unique key for the artist
                                        newArtistRef.setValue(artistId)
                                            .addOnCompleteListener { task ->
                                                if (task.isSuccessful) {
                                                    runOnUiThread {
                                                        Toast.makeText(
                                                            this@ArtistCreationActivity,
                                                            "Artist added to Firebase",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                } else {
                                                    runOnUiThread {
                                                        Toast.makeText(
                                                            this@ArtistCreationActivity,
                                                            "Failed to add artist to Firebase",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            }
                                    } else {
                                        runOnUiThread {
                                            Toast.makeText(
                                                this@ArtistCreationActivity,
                                                "Artist ID is empty or null",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                } else {
                                    runOnUiThread {
                                        Toast.makeText(
                                            this@ArtistCreationActivity,
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
                                        this@ArtistCreationActivity,
                                        "Failed to upload to database: " + e.message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        })
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    runOnUiThread {
                        Toast.makeText(
                            this@ArtistCreationActivity,
                            "Error checking artist existence: " + databaseError.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })
        } ?: run {
            Toast.makeText(
                this@ArtistCreationActivity,
                "User ID not found",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun fetchArtistAliases() {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://vachanammusic.com/api.php?api=getArtists")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(
                        this@ArtistCreationActivity,
                        "Failed to fetch artist aliases",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    responseData?.let {
                        try {
                            val jsonArray = JSONArray(it)
                            val artistAliases = ArrayList<String>()
                            val artistIds = ArrayList<String>()
                            for (i in 0 until jsonArray.length()) {
                                val jsonObject = jsonArray.getJSONObject(i)
                                val artistAlias = jsonObject.getString("artist_alias")
                                val artistId = jsonObject.getString("artist_id")
                                artistAliases.add(artistAlias)
                                artistIds.add(artistId)
                            }
                            runOnUiThread { updateArtistSpinner(artistAliases, artistIds) }
                        } catch (e: Exception) {
                            e.printStackTrace()
                            runOnUiThread {
                                Toast.makeText(
                                    this@ArtistCreationActivity,
                                    "Failed to parse artist aliases: " + e.message,
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(
                            this@ArtistCreationActivity,
                            "Failed to fetch artist aliases: " + response.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        })
    }

    private fun updateArtistSpinner(
        artistAliases: ArrayList<String>,
        artistIds: ArrayList<String>
    ) {
        val artistAdapter = ArrayAdapter(
            this@ArtistCreationActivity,
            android.R.layout.simple_spinner_item,
            artistAliases
        )
        artistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.artistNameSpinner.adapter = artistAdapter

        binding.artistNameSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parentView: AdapterView<*>?,
                    selectedItemView: android.view.View?,
                    position: Int,
                    id: Long
                ) {
                    val selectedArtistId = artistIds[position]
                    binding.artistIdTxt.text = selectedArtistId
                    val textColor = Color.WHITE
                    val selectedView = binding.artistNameSpinner.getChildAt(0) as? TextView
                    selectedView?.setTextColor(textColor)
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
