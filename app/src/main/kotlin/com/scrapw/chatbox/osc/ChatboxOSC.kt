package com.scrapw.chatbox.osc

import android.util.Log
import com.illposed.osc.OSCMessage
import com.illposed.osc.transport.udp.OSCPortOut
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetAddress
import java.net.UnknownHostException

class ChatboxOSC(
    ipAddress: String,
    var port: Int
) {

    private val oscScope = CoroutineScope(Dispatchers.IO)
    private val sendMutex = Mutex()
    private var realtimeMsgJob: Job? = null

    val TAG: String
        get() = "OSC@$ipAddress:$port"

    var addressResolvable = true
        private set

    var ipAddress = ipAddress
        set(value) {
            Log.d(TAG, "IP Address $field -> $value")

            field = value
            try {
                inetAddress = InetAddress.getByName(value)
                Log.d(TAG, "Resolve to $inetAddress.address")
                addressResolvable = true
            } catch (e: UnknownHostException) {
                Log.d(TAG, "Can't resolve $value")
                addressResolvable = false
            }
        }

    init {
        oscScope.launch {
            this@ChatboxOSC.ipAddress = ipAddress
        }
    }

    private lateinit var inetAddress: InetAddress

    var typing = false
        set(value) {
            field = value
            sendOscMessage("/chatbox/typing", listOf(value))
        }

    private fun sendOscMessage(address: String, arguments: List<Any?>, delay: Long = 0) {
        oscScope.launch {
            sendOscMessageNow(address, arguments, delay)
        }
    }

    private suspend fun sendOscMessageNow(
        address: String,
        arguments: List<Any?>,
        delay: Long = 0
    ) {
        delay(delay)
        sendMutex.withLock {
            val message = OSCMessage(address, arguments)
            val sender = OSCPortOut(inetAddress, port)
            try {
                sender.send(message)
                Log.d(TAG, "Message: ${message.address}  ${message.arguments}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed send Message: $message")
            }
            sender.close()
        }
    }

    fun sendMessage(text: String, sendImmediately: Boolean, triggerSFX: Boolean) {
        sendOscMessage("/chatbox/input", listOf(text, sendImmediately, triggerSFX))
    }

    fun sendRealtimeMessage(text: String, isFinal: Boolean = false) {
        realtimeMsgJob?.cancel()
        realtimeMsgJob = oscScope.launch {
            sendOscMessageNow("/chatbox/input", listOf(text, true, false))
            sendOscMessageNow(
                "/chatbox/typing",
                listOf(text.isNotEmpty() && !isFinal),
                50
            )

            // VRChat may ignore an input update that arrives too soon after the
            // previous partial transcript. Once recognition goes quiet, resend
            // the newest state after its update interval. A newer transcript
            // cancels this job, so stale text can never overwrite newer text.
            delay(1_100)
            sendOscMessageNow("/chatbox/input", listOf(text, true, false))
        }
    }
}
