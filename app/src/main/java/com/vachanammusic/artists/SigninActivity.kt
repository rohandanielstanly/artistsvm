package com.vachanammusic.artists

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.vachanammusic.artists.databinding.SigninBinding

class SigninActivity : AppCompatActivity() {
    private lateinit var binding: SigninBinding
    private var googleSignInClient: GoogleSignInClient? = null
    private var firebaseAuth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = SigninBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseAuth = FirebaseAuth.getInstance()
        binding.bt4.setOnClickListener { startGoogleSignIn() }


        design()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1099302317546-e0ovnjhqmapqacqbe2gcn3u0a7u5thmu.apps.googleusercontent.com")
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
        setupViews()
    }

    private fun design() {
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        window.statusBarColor = Color.TRANSPARENT

        window.navigationBarColor = Color.parseColor("#000000")
    }

    private fun setupViews() {
        binding.bt1.background = getGradientDrawable(100, 0, -0x1000000, -0xaffaa)
        binding.bt4.background = getGradientDrawable(100, 2, -0x555556, Color.TRANSPARENT)
        binding.button1.setTextColor(Color.WHITE)
    }


    private fun startGoogleSignIn() {
        val signInButton = binding.bt4

        // Animate the button click
        animateClick(signInButton)

        // Start the Google Sign-In process
        val signInIntent = googleSignInClient!!.signInIntent
        startActivityForResult(signInIntent, REQ_CD_GOOGLEAUTH)
    }

    public override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_CD_GOOGLEAUTH) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account?.idToken)
            } catch (e: ApiException) {
                Toast.makeText(applicationContext, "Sign-In failed ", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String?) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        firebaseAuth?.signInWithCredential(credential)
            ?.addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
//                    val user: FirebaseUser? = firebaseAuth?.currentUser
//                    val userName = user?.displayName ?: user?.email ?: "User"
//                    val welcomeMessage = "Hello $userName"
//                    Toast.makeText(applicationContext, welcomeMessage, Toast.LENGTH_SHORT).show()

//                    val intent = Intent(this@MainsigninActivity, MainActivity::class.java)
                    val intent = Intent(this@SigninActivity, VerificationActivity::class.java)

                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(
                        applicationContext,
                        "Authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
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

    private fun animateClick(view: View) {
        view.animate()
            .scaleX(0.9f)
            .scaleY(0.9f)
            .setDuration(100)
            .withEndAction {
                view.animate()
                    .scaleX(1.0f)
                    .scaleY(1.0f)
                    .setDuration(100)
                    .start()
            }
            .start()
    }

    companion object {
        private const val REQ_CD_GOOGLEAUTH = 101
    }
}
