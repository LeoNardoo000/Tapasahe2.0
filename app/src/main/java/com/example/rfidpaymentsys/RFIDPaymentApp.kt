    package com.example.rfidpaymentsys

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

/**
 * Custom Application class.
 *
 * Signs the device in to Firebase anonymously as early as possible in the
 * app's lifecycle — before MainActivity/MainViewModel are created and
 * start talking to the Realtime Database. This gives every session a
 * non-null `auth` object, which lets us write Realtime Database rules
 * that require `auth != null` instead of leaving the database open to
 * anyone with the project URL.
 *
 * We don't need to know WHICH user is using the app at this point — role
 * checking (Admin/Operator/Client) still happens the same way it does now,
 * via your existing login screens and the Users node. This anonymous
 * sign-in only proves "this is a request from our app," not who the
 * human is.
 */
class RFIDPaymentApp : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseApp.initializeApp(this)

        val auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnSuccessListener {
                    Log.d("RFIDPaymentApp", "Anonymous Firebase auth established: ${it.user?.uid}")
                }
                .addOnFailureListener { e ->
                    // If this fails (e.g. no internet on first launch), the app can
                    // still run offline via Room/SQLite. Firebase reads/writes will
                    // simply fail with a permission error until connectivity returns
                    // and this succeeds — the SDK does not automatically retry this
                    // call on its own, so we retry on the next app open.
                    Log.e("RFIDPaymentApp", "Anonymous auth failed: ${e.message}", e)
                }
        } else {
            Log.d("RFIDPaymentApp", "Already signed in anonymously: ${auth.currentUser?.uid}")
        }
    }
}
