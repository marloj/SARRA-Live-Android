package com.example.sarra

import android.Manifest
import android.content.pm.PackageManager
import android.icu.util.TimeUnit
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Adapter
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.*
import kotlinx.android.synthetic.main.activity_main.*
import okhttp3.*
import okio.ByteString
import java.nio.ByteBuffer
import kotlin.concurrent.thread
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.recyclerview.widget.LinearLayoutManager
import org.json.JSONArray
import org.json.JSONObject
import javax.net.ssl.HostnameVerifier

class MainActivity : AppCompatActivity() {
    private var client: OkHttpClient = OkHttpClient.Builder().readTimeout(50, java.util.concurrent.TimeUnit.SECONDS).build()
    private val NORMAL_CLOSURE_STATUS = 1000
    private var record: AudioRecord? = null
    private var bufferSize = 4096
    private var buffer = ByteArray(2*bufferSize)
    private var status = false
    private var ws: WebSocket? = null
    private var MY_PERMISSIONS_RECORD_AUDIO = 1
    private lateinit var uttAdapter: UtteranceAdapter



    // websocket listener class
    private inner class EchoWebSocketListener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response?) {
            webSocket.send("{\"name\":\"default\"}")
        }

        override fun onMessage(webSocket: WebSocket?, text: String) {
            val result = JSONObject(text)
            if (!status && result.has("status") && result.getString("status") == "ready") {
                startRecording()
            }
            if (result.has("text")) {
                if(result.getBoolean("final")) {
                    //val arr = result.getString("text")
                    //output(arr.join(" ").replace("\"", ""), true)
                    output(result.getString("text"), true)
                } else {
                    output(result.getString("text"), false)
                }
            }
/*
            if (result.has("eof")) {
                uttAdapter.updateLastNonFinal("end of recognition", true)
            }
*/
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            // uttAdapter.updateLastNonFinal(code.toString(), true)
            // uttAdapter.updateLastNonFinal(reason, true)
            stopRecording()
            webSocket.close(NORMAL_CLOSURE_STATUS, null)
        }

        override fun onFailure(
            webSocket: WebSocket?,
            t: Throwable,
            response: Response?
        ) {
            //output("Error : " + t.message)
        }
    }

    // setup everything
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // ask for permission if needed
        if (checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Please grant permissions to record audio", Toast.LENGTH_LONG).show();

            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO), MY_PERMISSIONS_RECORD_AUDIO);
        } else {
            ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.RECORD_AUDIO), MY_PERMISSIONS_RECORD_AUDIO);
        }

        client =  OkHttpClient.Builder().readTimeout(50, java.util.concurrent.TimeUnit.SECONDS).build()
        speakButton.setOnClickListener { toggleRecording() }

        utteranceList.apply {
            uttAdapter = UtteranceAdapter()
            addItemDecoration(UtteranceDecoration(5))
            adapter = uttAdapter
            layoutManager = LinearLayoutManager(this@MainActivity)
        }
    }

    // start the recording process
    private fun startRecording() {
        if (checkSelfPermission(this,Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            record = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                2 * bufferSize
            )

            status = true
            thread(start = true) {
                record!!.startRecording()
                while (status) {
                    val cnt = record!!.read(buffer,0, 2 * bufferSize)
                    ws!!.send(ByteString.of(buffer, 0, cnt))
                }
            }

            speakButton.setImageResource(R.drawable.ic_round_stop_24)
        }
    }

    private fun stopRecording() {
        status = false
        if (record != null) {
            record!!.stop()
            record!!.release()
        }
        speakButton.setImageResource(R.drawable.ic_round_mic_24)
    }

    private fun toggleRecording() {

        if(!status) {


            // create websocket connection
            var request_builder: Request.Builder? = Request.Builder().url("wss://marhula.fei.tuke.sk/live");
            //val token: String = intent.getStringExtra("token").toString();
            //if (token.isNotEmpty()) {
            //    request_builder = request_builder?.addHeader("Bearer", token);
            //}
            val request = request_builder?.build()
            val listener = EchoWebSocketListener()
            ws = client.newWebSocket(request, listener)
            if(ws != null) {
                Toast.makeText(this, "connected", Toast.LENGTH_LONG).show()
            }

            ws!!.send("hi")
            //client.dispatcher().executorService().shutdown()
        } else {
            //ws!!.close(1000, "end of recognition")
            //stopRecording()
            ws!!.send(ByteString.encodeUtf8("<EOD>"))
        }
    }

    private fun output(txt: String, final: Boolean) {
        runOnUiThread {
            uttAdapter.updateLastNonFinal(txt, final)
            utteranceList.smoothScrollToPosition(uttAdapter.itemCount)
        }
    }
}