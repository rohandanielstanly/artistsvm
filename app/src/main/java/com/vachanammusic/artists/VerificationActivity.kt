package com.vachanammusic.artists

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class VerificationActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextDescription: EditText
    private lateinit var editTextArtists: EditText
    private lateinit var editTextOrganization: EditText
    private lateinit var editWebsiteUrl: EditText
    private lateinit var editOthers: EditText
    private lateinit var btnSubmit: Button
    private lateinit var btnContact: Button
    private lateinit var imgInfo: ImageView

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var verificationInProgressText: TextView
    private lateinit var linear19: LinearLayout
    private lateinit var radioButtonTerms: RadioButton



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.verification)
        design()
        editTextName = findViewById(R.id.editTextName)
        editTextDescription = findViewById(R.id.editTextDescription)
        editTextArtists = findViewById(R.id.editTextArtists)
        editTextOrganization = findViewById(R.id.editTextOrganization)
        editWebsiteUrl = findViewById(R.id.editWebsiteUrl)
        editOthers = findViewById(R.id.editOthers)
        linear19 = findViewById(R.id.linear19)
        imgInfo = findViewById(R.id.imgInfo)

        radioButtonTerms = findViewById(R.id.radioButtonTerms)
        btnSubmit = findViewById(R.id.btnSubmit)
        btnContact = findViewById(R.id.btnContact)

        verificationInProgressText = findViewById(R.id.verificationInProgressText)

        // Initially hide EditText fields and show progress text


        // Fixing Firebase database reference
        database = FirebaseDatabase.getInstance().reference.child("submissions")
        btnSubmit.background = getGradientDrawable(100, 2, -0x555556, Color.TRANSPARENT)
        btnContact.background = getGradientDrawable(100, 2, -0x555556, Color.TRANSPARENT)

        btnSubmit.setOnClickListener {
            submitData()
        }

        btnContact.setOnClickListener {
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:vachanammusic@gmail.com")
            }
            startActivity(emailIntent)
        }

        imgInfo.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://vachanammusic.com/artistsManual.pdf")
            startActivity(intent)
        }

        radioButtonTerms.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showTermsAndConditionsDialog()
            }
        }


        auth = FirebaseAuth.getInstance()

        // Check if user is already logged in
        if (auth.currentUser == null) {
            // If user is not logged in, redirect to sign-up page
            val intent = Intent(this, SigninActivity::class.java)
            startActivity(intent)
            finish()
        } else {
            // If user is logged in, check approval status
            val userId = auth.currentUser!!.uid
            val userSubmissionsRef = database.child(userId)

            userSubmissionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        // If submission exists, check approval status
                        for (submissionSnapshot in snapshot.children) {
                            val submissionId = submissionSnapshot.key
                            if (submissionId != null) {
                                checkApproval(userId, submissionId)
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VerificationActivity, "Failed to check submission: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        }

        checkApprovalAndProceed()
    }

    private fun checkApprovalAndProceed() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            val userId = currentUser.uid

            val userSubmissionsRef = database.child("submissions").child(userId)

            userSubmissionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (submissionSnapshot in snapshot.children) {
                            val submissionId = submissionSnapshot.key
                            if (submissionId != null) {
                                // Check approval status
                                checkApproval(userId, submissionId)
                                return  // Exit loop after checking the first submission
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VerificationActivity, "Failed to check submission: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
//            Toast.makeText(this, "Current user is null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun submitData() {
        val name = editTextName.text.toString()
        val description = editTextDescription.text.toString()
        val artists = editTextArtists.text.toString()
        val organization = editTextOrganization.text.toString()
        val website = editWebsiteUrl.text.toString()
        val others = editOthers.text.toString()

        val currentUser = auth.currentUser


        if (!radioButtonTerms.isChecked) {
            Toast.makeText(this, "Please agree to the terms and conditions", Toast.LENGTH_SHORT).show()
            return // Exit the function without proceeding
        }

        if (currentUser != null) {
            val userId = currentUser.uid // Get the user ID (UID) of the current user

            // Check if submission already exists for the user
            val userSubmissionsRef = database.child("submissions").child(userId)
            userSubmissionsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        Toast.makeText(this@VerificationActivity, "You have already submitted a verification request.", Toast.LENGTH_SHORT).show()
                    } else {
                        // Create submission object
                        val submission = Submission(name, description, artists, organization,website,others, "no") // Initially set to "no"

                        // Save submission under the user's unique identifier
                        val submissionId = database.child("submissions").child(userId).push().key // Generate unique key for submission

                        if (submissionId != null) {
                            userSubmissionsRef.child(submissionId).setValue(submission)
                                .addOnSuccessListener {
                                    Toast.makeText(this@VerificationActivity, "Submission successful", Toast.LENGTH_SHORT).show()
                                    // Pass submissionId to checkApproval function
                                    checkApproval(userId, submissionId) // Pass userId and submissionId
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this@VerificationActivity, "Submission failed: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }

                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@VerificationActivity, "Failed to check submission: ${error.message}", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
//            Toast.makeText(this, "Current user is null", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkApproval(userId: String, submissionId: String) {
        val userSubmissionsRef = database.child("submissions").child(userId).child(submissionId)

        userSubmissionsRef.child("approved").addListenerForSingleValueEvent(object :
            ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val approved = snapshot.getValue(String::class.java)
                if (approved == "yes") {
                    // If approved, navigate to MainActivity
                    val intent = Intent(this@VerificationActivity, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {

                    linear19.visibility = View.GONE
                    verificationInProgressText.visibility = View.VISIBLE
                    btnContact.visibility = View.VISIBLE
                    Toast.makeText(this@VerificationActivity, "Verification in progress", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@VerificationActivity, "Failed to check approval: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
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

    private fun showTermsAndConditionsDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        dialogBuilder.setTitle("Terms and Conditions")
        dialogBuilder.setMessage(
                "\n" +
                "Welcome to the Artist Upload App for Vachanam Music!\n" +
                "\n" +
                "By downloading and using this app, you agree to the following terms and conditions:\n" +
                "\n" +
                "Purpose of the App: The Artist Upload App for Vachanam Music is designed for artists to upload Christian songs to the Vachanam Music platform. It is intended solely for the purpose of sharing Christian music content.\n" +
                "\n" +
                "Ad-Free Experience: Vachanam Music is committed to providing an ad-free experience for all users of this app. You will not encounter any advertisements while using the app.\n" +
                "\n" +
                "Christian Content Only: This app is dedicated to promoting Christian music. Users are only allowed to upload songs that align with Christian values and themes.\n" +
                "\n" +
                "User Responsibilities: As a user of this app, you are responsible for ensuring that the content you upload complies with our guidelines and does not violate any copyright laws.\n" +
                "\n" +
                "Artist and Song Creation: Users of this app have the ability to create artists and songs to be uploaded to Vachanam Music. By creating artists and songs, you agree to provide accurate and truthful information.\n" +
                "\n" +
                "Verification Process: There is a one-time verification process for users of this app. This process is necessary to ensure the authenticity of the content being uploaded and to maintain the integrity of the Vachanam Music platform.\n" +
                "\n" +
                "Data Privacy: We are committed to protecting your privacy. Any personal information collected during the verification process will be kept confidential and will not be shared with third parties without your consent.\n" +
                "\n" +
                "Updates and Changes: Vachanam Music reserves the right to update or modify these terms and conditions at any time. Any changes will be communicated to users through the app.\n" +
                "\n" +
                "By using the Artist Upload App for Vachanam Music, you acknowledge that you have read and understood these terms and conditions, and you agree to comply with them.\n" +
                "\n" +
                "If you have any questions or concerns about these terms and conditions, please contact us at vachanammusic@gmail.com.\n" +
                "\n")
        dialogBuilder.setPositiveButton("Agree") { dialog, _ ->
            // Set the terms and conditions checkbox as checked
            radioButtonTerms.isChecked = true
            dialog.dismiss()
        }
        dialogBuilder.setNegativeButton("Disagree") { dialog, _ ->
            // Uncheck the terms and conditions checkbox
            radioButtonTerms.isChecked = false
            dialog.dismiss()
        }
        val dialog = dialogBuilder.create()
        dialog.show()
    }


}
