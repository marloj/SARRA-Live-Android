package com.example.sarra

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.AuthSession
import com.amplifyframework.auth.AuthUserAttributeKey
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.auth.cognito.AWSCognitoAuthSession
import com.amplifyframework.auth.options.AuthSignUpOptions
import com.amplifyframework.core.Amplify
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.OkHttpClient

class LoginActivity  : AppCompatActivity() {

    private var MY_PERMISSION_READ_PHONE_STATE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        Amplify.addPlugin(AWSCognitoAuthPlugin())

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_PHONE_STATE), MY_PERMISSION_READ_PHONE_STATE);
        } else {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_PHONE_STATE), MY_PERMISSION_READ_PHONE_STATE);
        }

        try {
            Amplify.configure(applicationContext)
            Log.i("MyAmplifyApp", "Initialized Amplify")
        } catch (error: AmplifyException) {
            Log.e("MyAmplifyApp", "Could not initialize Amplify", error)
        }

        button.setOnClickListener { attemptToSignUp() }
        button3.setOnClickListener { confirmCode() }
        button4.setOnClickListener { attemptToLogIn() }
        button2.setOnClickListener { gofree() }
    }

    private fun gofree() {
        val intent = Intent(this, MainActivity::class.java).apply {
            putExtra("token", "")
        }
        startActivity(intent);
    }

    private fun confirmCode() {
        if(editTextTextCode.text.isEmpty()) {
            Toast.makeText(this, "Please enter confirmation code", Toast.LENGTH_LONG).show();
            return;
        }
        Amplify.Auth.confirmSignUp(
            editTextTextEmailAddress.text.toString(), editTextTextCode.text.toString(),
            { result ->
                if (result.isSignUpComplete) {
                    Log.i("===================================","Sign Up complete, you should be added to premium users");
                    this@LoginActivity.runOnUiThread(Runnable {
                        attemptToLogIn();
                    })
                } else {
                    Log.i("=======================", "Signup not complete");
                }
            },
            {
                Log.i("================================", "Error while confirming the request.");
            }
        )
    }

    @SuppressLint("LongLogTag")
    private fun attemptToSignUp() {
        if(editTextTextEmailAddress.text.isEmpty() || editTextTextPassword.text.isEmpty()) {
            Log.i("================================", "Please fill in your credentials");
            return;
        }
        val options = AuthSignUpOptions.builder()
            .userAttribute(AuthUserAttributeKey.email(), editTextTextEmailAddress.text.toString())
            .build()
        Amplify.Auth.signUp(
            editTextTextEmailAddress.text.toString(),
            editTextTextPassword.text.toString(), options,
            {   Log.i("AuthQuickStart", "Sign up succeeded: $it");
                this@LoginActivity.runOnUiThread(Runnable {
                    confirm_mode();
                });
            },
            {
                Log.i("================================", "Something is wrong with the request $it");
                this@LoginActivity.runOnUiThread(Runnable {
                    default_mode();
                });
            }
        )
    }

    private fun default_mode() {
        button4.visibility = View.VISIBLE;
        button.visibility = View.VISIBLE;
        button3.visibility = View.INVISIBLE;
        editTextTextCode.visibility = View.INVISIBLE;
        editTextTextEmailAddress.isEnabled = true;
        editTextTextPassword.isEnabled = true;
    }

    private fun confirm_mode() {
        button4.visibility = View.INVISIBLE;
        button.visibility = View.INVISIBLE;
        button3.visibility = View.VISIBLE;
        editTextTextCode.visibility = View.VISIBLE;
        editTextTextEmailAddress.isEnabled = false;
        editTextTextPassword.isEnabled = false;
    }

    private fun attemptToLogIn() {
        if(editTextTextEmailAddress.text.isEmpty() || editTextTextEmailAddress.text.isEmpty()) {
            Log.i("================================", "Please fill in your credentials");
            return;
        }

        Amplify.Auth.signIn(editTextTextEmailAddress.text.toString(), editTextTextPassword.text.toString(),
            { result ->
                if (result.isSignInComplete) {
                    Log.i("================================","Logged in...");
                    Amplify.Auth.fetchAuthSession(
                        {
                            val authsession = it as AWSCognitoAuthSession;
                            val intent = Intent(this, MainActivity::class.java).apply {
                                putExtra("token", authsession.userPoolTokens.value?.accessToken.toString())
                            }
                            startActivity(intent);
                        },
                        { Log.e("AmplifyQuickstart", "Failed to fetch auth session") }
                    )
                } else {
                    Log.i("================================","LogIn not complete");
                }
            },
            {
                Log.i("================================", "Error loggin in...");
            }
        )
    }
}