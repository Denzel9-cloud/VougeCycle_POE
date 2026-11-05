package com.example.vougecycle

import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivityNotificationsBinding
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import io.github.jan.supabase.realtime.channel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

class NotificationsActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityNotificationsBinding
    private var realtimeChannel: RealtimeChannel? = null
    private var realtimeJob: Job? = null
    private var loading = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener { finish() }
        binding.btnRetry.setOnClickListener { loadNotifications() }
        binding.tvMarkAllRead.setOnClickListener {
            lifecycleScope.launch {
                try {
                    NotificationRepository.markAllRead()
                    loadNotifications()
                } catch (_: Exception) {
                    LanguageManager.makeToast(this@NotificationsActivity, "Could not mark notifications as read. Please try again.", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        }
        LanguageManager.translateViewTree(binding.root)
    }

    override fun onStart() {
        super.onStart()
        loadNotifications()
        startRealtimeUpdates()
    }

    override fun onStop() {
        realtimeJob?.cancel()
        realtimeJob = null
        realtimeChannel?.let { channel ->
            lifecycleScope.launch {
                try { Supabase.client.realtime.removeChannel(channel) } catch (_: Exception) { }
            }
        }
        realtimeChannel = null
        super.onStop()
    }

    private fun startRealtimeUpdates() {
        val userId = Supabase.client.auth.currentUserOrNull()?.id ?: return
        lifecycleScope.launch {
            try {
                val channel = Supabase.client.channel("notifications-$userId")
                realtimeChannel = channel
                val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
                    table = "notifications"
                }
                realtimeJob = changes.onEach {
                    loadNotifications()
                }.launchIn(lifecycleScope)
                channel.subscribe()
                binding.tvLiveStatus.text = LanguageManager.translate(this@NotificationsActivity, "Live updates are on")
            } catch (_: Exception) {
                binding.tvLiveStatus.text = LanguageManager.translate(this@NotificationsActivity, "Live updates unavailable. Check your connection.")
            }
        }
    }

    private fun loadNotifications() {
        if (loading || !::binding.isInitialized) return
        loading = true
        binding.progressLoading.visibility = View.VISIBLE
        binding.btnRetry.visibility = View.GONE
        lifecycleScope.launch {
            try {
                val items = NotificationRepository.list()
                binding.notificationList.removeAllViews()
                items.forEach { item ->
                    val card = LinearLayout(this@NotificationsActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(16), dp(14), dp(16), dp(14))
                        setBackgroundResource(R.drawable.bg_input_field)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = dp(10) }
                        if (!item.isRead) setBackgroundColor(getColor(R.color.vc_off_white))
                        setOnClickListener {
                            if (!item.isRead) lifecycleScope.launch {
                                try {
                                    NotificationRepository.markRead(item.id)
                                    loadNotifications()
                                } catch (_: Exception) { }
                            }
                        }
                    }
                    card.addView(TextView(this@NotificationsActivity).apply {
                        text = LanguageManager.translate(this@NotificationsActivity, item.title)
                        textSize = 16f
                        setTextColor(getColor(R.color.vc_black))
                        setTypeface(typeface, Typeface.BOLD)
                    })
                    card.addView(TextView(this@NotificationsActivity).apply {
                        text = item.body
                        textSize = 14f
                        setTextColor(getColor(R.color.vc_grey_text))
                        setPadding(0, dp(5), 0, dp(5))
                    })
                    card.addView(TextView(this@NotificationsActivity).apply {
                        text = item.createdAt.replace('T', ' ').take(16)
                        textSize = 12f
                        setTextColor(getColor(R.color.vc_grey_text))
                    })
                    binding.notificationList.addView(card)
                }
                binding.tvEmpty.visibility = if (items.isEmpty()) View.VISIBLE else View.GONE
                binding.tvMarkAllRead.visibility = if (items.any { !it.isRead }) View.VISIBLE else View.GONE
                binding.tvLiveStatus.text = LanguageManager.translate(this@NotificationsActivity, "Live updates are on")
            } catch (e: Exception) {
                binding.tvEmpty.text = LanguageManager.translate(this@NotificationsActivity, AppReliability.friendlyMessage(e, "load notifications"))
                binding.tvEmpty.visibility = View.VISIBLE
                binding.btnRetry.visibility = View.VISIBLE
                binding.tvLiveStatus.text = LanguageManager.translate(this@NotificationsActivity, "Live updates unavailable. Check your connection.")
            } finally {
                loading = false
                binding.progressLoading.visibility = View.GONE
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
