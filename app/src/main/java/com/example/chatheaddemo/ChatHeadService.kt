package com.example.chatheaddemo

import android.animation.ValueAnimator
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.*
import androidx.core.animation.doOnEnd
import androidx.core.app.NotificationCompat

class ChatHeadService : Service() {

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "chat_head_channel"
        private const val NOTIFICATION_ID = 1
    }

    private var widthScreen = 0
    private var heightScreen = 0
    private lateinit var windowManager: WindowManager
    private lateinit var chatHeadView: View
    private lateinit var chatView: View
    override fun onCreate() {
        super.onCreate()
        genScreenSize()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        chatHeadView = LayoutInflater.from(this).inflate(R.layout.chat_head_layout, null)
        chatView = LayoutInflater.from(this).inflate(R.layout.chat_head_layout_chat, null)
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 0

        val layoutParamsChat = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            1000,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        layoutParamsChat.gravity = Gravity.TOP or Gravity.START
        layoutParamsChat.x = 0
        layoutParamsChat.y = 500

        chatHeadView.setOnTouchListener(ChatHeadTouchListener(windowManager, layoutParams))
        chatHeadView.setOnClickListener {
            if (chatHeadView.tag != "true") {
                chatHeadView.tag = "true"
                val startX = layoutParams.x
                val startY = layoutParams.y
                val endX = widthScreen
                val endY = layoutParamsChat.y - 150
                val animator = ValueAnimator.ofFloat(0f, 1f)
                animator.duration = 500

                animator.addUpdateListener { valueAnimator ->
                    val fraction = valueAnimator.animatedFraction

                    val currentX = (startX + (endX - startX) * fraction).toInt()
                    val currentY = (startY + (endY - startY) * fraction).toInt()

                    layoutParams.x = currentX
                    layoutParams.y = currentY

                    windowManager.updateViewLayout(chatHeadView, layoutParams)
                }
                animator.doOnEnd {
                    windowManager.addView(chatView, layoutParamsChat)
                }
                animator.start()
            } else {
                chatHeadView.tag = "false"
                windowManager.removeView(chatView)
            }
        }
        windowManager.addView(chatHeadView, layoutParams)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        stopSelf()
        windowManager.removeView(chatHeadView)
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent =
            PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                "Chat Head",
                NotificationManager.IMPORTANCE_NONE
            )
            channel.lightColor = Color.BLUE
            channel.setSound(null,null)
            channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setContentTitle("Chat Head")
            .setContentText("Running in background")
            .setSmallIcon(R.drawable.ic_chat_head_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)

        return notificationBuilder.build()
    }

    inner class ChatHeadTouchListener(
        private val windowManager: WindowManager,
        private val layoutParams: WindowManager.LayoutParams
    ) : View.OnTouchListener {

        private var initialX: Int = 0
        private var initialY: Int = 0
        private var initialTouchX: Float = 0.toFloat()
        private var initialTouchY: Float = 0.toFloat()
        private var isMove = false
        override fun onTouch(view: View, event: MotionEvent): Boolean {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    return true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isMove)
                        view.performClick()
                    isMove = false
                }
                MotionEvent.ACTION_MOVE -> {
                    isMove = true
                    try {
                        windowManager.removeView(chatView)
                        chatHeadView.tag = "false"
                    }catch (_:java.lang.Exception){}

                    val deltaX = event.rawX - initialTouchX
                    val deltaY = event.rawY - initialTouchY
                    layoutParams.x = (initialX + deltaX).toInt()
                    layoutParams.y = (initialY + deltaY).toInt()
                    windowManager.updateViewLayout(view, layoutParams)
                    return true
                }
            }
            return false
        }
    }

    private fun genScreenSize() {
        try {
            val displayMetrics = Resources.getSystem().displayMetrics
            heightScreen = displayMetrics.heightPixels
            widthScreen = displayMetrics.widthPixels
        } catch (e: java.lang.Exception) {
        }
    }
}
