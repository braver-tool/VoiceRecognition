package com.speech.call

import android.annotation.SuppressLint
import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.tool.speech.GoogleVoiceTypingDisabledException
import com.tool.speech.Speech
import com.tool.speech.Speech.stopDueToDelay
import com.tool.speech.SpeechDelegate
import com.tool.speech.SpeechRecognitionNotAvailable
import java.util.*

class AppService : Service(), SpeechDelegate, stopDueToDelay {
    private var isFunctionStarted = false
    override fun onBind(intent: Intent): IBinder? {
        // TODO: Return the communication channel to the service.
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        if (!isFunctionStarted) {
            isFunctionStarted = true
            startVoiceRecognizeFunction(true)
            setupForegroundService(this)
            startForeGroundService(this, intent)
        }
        return START_STICKY
    }

    override fun onSpecifiedCommandPronounced(event: String) {
        startVoiceRecognizeFunction(false)
    }

    override fun onStartOfSpeech() {}
    override fun onSpeechRmsChanged(value: Float) {}
    override fun onSpeechPartialResults(results: List<String>) {
        for (partial in results) {
            Log.d("##@@$TAG", "---------->onSpeechPartialResults------->$partial")
        }
    }

    override fun onSpeechResult(result: String) {
        if (!result.isEmpty()) {
            Log.d("##@@$TAG", "---------->onSpeechResult------->$result")
            broadcastUpdate(result)
        }
    }

    /**
     * Method used to start speech recognize
     */
    private fun startVoiceRecognizeFunction(isInitiate: Boolean) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                (Objects.requireNonNull(getSystemService(AUDIO_SERVICE)) as AudioManager).setStreamMute(AudioManager.STREAM_SYSTEM, true)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (isInitiate) {
            delegate = this
            Speech.getInstance().setListener(this)
        }
        if (Speech.getInstance().isListening) {
            Speech.getInstance().stopListening()
        } else {
            System.setProperty("rx.unsafe-disable", "True")
            try {
                Speech.getInstance().stopTextToSpeech()
                Speech.getInstance().startListening(null, this)
            } catch (exc: SpeechRecognitionNotAvailable) {
                Log.d(TAG, "---------->" + exc.message)
            } catch (exc: GoogleVoiceTypingDisabledException) {
                Log.d(TAG, "---------->" + exc.message)
            }
        }
        muteBeepSoundOfRecorder(this, true)
    }

    /**
     * @param result - result from speech recognizer
     */
    private fun broadcastUpdate(result: String) {
        val intent = Intent(ACTION_SPEECH_RECOGNIZER_RESULT)
        intent.putExtra(RESPONSE_STATUS, result)
        sendBroadcast(intent)
    }

    private fun startForeGroundService(context: Context, intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    private fun setupForegroundService(context: Context) {
        startForeground(NOTIFICATION_ID, getForegroundNotification(context))
    }

    companion object {
        private val TAG = AppService::class.java.simpleName
        const val ACTION_SPEECH_RECOGNIZER_RESULT = BuildConfig.APPLICATION_ID + "ACTION_SPEECH_RECOGNIZER_RESULT"
        const val ACTION_SPEECH_RECOGNIZER_DESTROY = BuildConfig.APPLICATION_ID + "ACTION_SPEECH_RECOGNIZER_DESTROY"
        const val RESPONSE_STATUS = "response_status"
        const val IS_STOP_SERVICE = "is_stop_service"
        private const val SPEECH_CALL_CHANNEL_ID = "speech_call_channel_id"
        private const val SPEECH_CALL_CHANNEL_NAME = "speech_call_channel_name"
        const val NOTIFICATION_ID = 3646
        var delegate: SpeechDelegate? = null

        /**
         * Function to remove the beep sound of voice recognizer.
         */
        private fun muteBeepSoundOfRecorder(context: Context, isMute: Boolean) {
            val audioManager = context.getSystemService(AUDIO_SERVICE) as AudioManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, if (isMute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, if (isMute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_NOTIFICATION, if (isMute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_RING, if (isMute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
                audioManager.adjustStreamVolume(AudioManager.STREAM_SYSTEM, if (isMute) AudioManager.ADJUST_MUTE else AudioManager.ADJUST_UNMUTE, 0)
            } else {
                audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, isMute)
                audioManager.setStreamMute(AudioManager.STREAM_ALARM, isMute)
                audioManager.setStreamMute(AudioManager.STREAM_MUSIC, isMute)
                audioManager.setStreamMute(AudioManager.STREAM_RING, isMute)
                audioManager.setStreamMute(AudioManager.STREAM_SYSTEM, isMute)
            }
        }

        @JvmStatic
        fun stopOurService(context: Context) {
            val intent = Intent(context, AppService::class.java)
            context.stopService(intent)
            muteBeepSoundOfRecorder(context, false)
            Speech.getInstance().stopListening()
            Speech.getInstance().shutdown()
        }

        /**
         * PushNotification for Initiate Speech-Call Listener
         */
        fun getForegroundNotification(context: Context): Notification {
            val reqCode = Random().nextInt(3333)
            // service destroyed Intent
            val serviceDestroyIntent = Intent(context, LocalNotificationReceiver::class.java)
            val bundle = Bundle()
            bundle.putBoolean(IS_STOP_SERVICE, true)
            serviceDestroyIntent.putExtras(bundle)
            val callRejectIntent = PendingIntent.getBroadcast(context, reqCode + 1, serviceDestroyIntent, PendingIntent.FLAG_UPDATE_CURRENT)
            val notificationManager: NotificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            // Custom Remote Views
            val expandedView = RemoteViews(context.packageName, R.layout.notification_remote_view)
            expandedView.setOnClickPendingIntent(R.id.stopServiceImageView, callRejectIntent)
            // Notification Builder
            val notificationBuilder: NotificationCompat.Builder = NotificationCompat.Builder(context, SPEECH_CALL_CHANNEL_ID)
            notificationBuilder.setSmallIcon(notificationIcon)
            notificationBuilder.setCustomContentView(expandedView)
            notificationBuilder.setAutoCancel(true)
            notificationBuilder.setCategory(NotificationCompat.CATEGORY_MESSAGE)
            notificationBuilder.setContentIntent(callRejectIntent)
            notificationBuilder.priority = NotificationCompat.PRIORITY_LOW
            notificationBuilder.setChannelId(SPEECH_CALL_CHANNEL_ID)
            // Use a full-screen intent only for the highest-priority alerts where you
            // have an associated activity that you would like to launch after the user
            // interacts with the notification. Also, if your app targets Android 10
            // or higher, you need to request the USE_FULL_SCREEN_INTENT permission in
            // order for the platform to invoke this notification.
            //notificationBuilder.setFullScreenIntent(dismissIntent, true);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @SuppressLint("WrongConstant") val channel = NotificationChannel(SPEECH_CALL_CHANNEL_ID, SPEECH_CALL_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW)
                channel.lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                notificationManager.createNotificationChannel(channel)
            }
            return notificationBuilder.build()
        }

        /**
         * Method is return the notification icon based on build version
         *
         * @return icon
         */
        val notificationIcon: Int
            get() {
                val useWhiteIcon = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
                return if (useWhiteIcon) R.mipmap.ic_launcher else R.mipmap.ic_launcher_round
            }
    }
}