package ir.metrix.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.messaging.FirebaseMessaging
import ir.metrix.analytics.MetrixAnalytics


class MainActivity : ComponentActivity() {

    private lateinit var gso: GoogleSignInOptions
    private lateinit var gsc: GoogleSignInClient
    private lateinit var googleBtn: ImageView

    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        googleBtn = findViewById(R.id.google_btn)

        gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).requestEmail().build()
        gsc = GoogleSignIn.getClient(this, gso)

        val acct = GoogleSignIn.getLastSignedInAccount(this)
        if (acct != null) {
            navigateToSecondActivity()
        }

        googleBtn.setOnClickListener { signIn() }

        MetrixAnalytics.setUserIdListener { metrixUserId ->
            Log.d(TAG, "metrixId: $metrixUserId")
        }

    }

    private fun signIn() {
        val signInIntent = gsc.signInIntent
        print("hi")
        resultLauncher.launch(signInIntent)
    }

    private var resultLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data

            val task = GoogleSignIn.getSignedInAccountFromIntent(data)

            try {
                task.getResult(ApiException::class.java)

                val acct = GoogleSignIn.getLastSignedInAccount(this) ?: throw Exception()
                MetrixAnalytics.User.setUserCustomId(acct.email)
                MetrixAnalytics.User.setFirstName(acct.givenName)
                MetrixAnalytics.User.setLastName(acct.familyName)
                MetrixAnalytics.User.setEmail(acct.email)

                FirebaseMessaging.getInstance().token.addOnCompleteListener(
                    OnCompleteListener { task ->
                        if (!task.isSuccessful) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.exception)
                            return@OnCompleteListener
                        }
                        val token = task.result
                        Log.d(TAG, "token: $token")
                        MetrixAnalytics.User.setFcmToken(token)
                    }
                )

                navigateToSecondActivity()
            } catch (e: ApiException) {
                Toast.makeText(applicationContext, e.message, Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun navigateToSecondActivity() {
        finish()
        val intent = Intent(this@MainActivity, SecondActivity::class.java)
        startActivity(intent)
    }
}