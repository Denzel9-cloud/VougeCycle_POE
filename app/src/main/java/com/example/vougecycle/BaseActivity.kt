package com.example.vougecycle

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

open class BaseActivity : AuthenticatedActivity() {

    // Which tab is highlighted on this screen
    protected open val selectedTab: Tab = Tab.HOME

    enum class Tab { HOME, EXPLORE, SELL, INBOX, PROFILE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    /**
     * Call this from onCreate AFTER setContentView() to wire the bottom nav.
     * It looks for the ids defined in bottom_nav.xml and hooks up clicks.
     */
    protected fun setupBottomNav() {
        val navHome = findViewById<LinearLayout>(R.id.navHome) ?: return
        val navExplore = findViewById<LinearLayout>(R.id.navExplore)
        val navSell = findViewById<LinearLayout>(R.id.navSell)
        val navInbox = findViewById<LinearLayout>(R.id.navInbox)
        val navProfile = findViewById<LinearLayout>(R.id.navProfile)

        val tvHome = findViewById<TextView>(R.id.tvNavHome)
        val tvExplore = findViewById<TextView>(R.id.tvNavExplore)
        val tvSell = findViewById<TextView>(R.id.tvNavSell)
        val tvInbox = findViewById<TextView>(R.id.tvNavInbox)
        val tvProfile = findViewById<TextView>(R.id.tvNavProfile)

        // Reset all to grey
        listOf(tvHome, tvExplore, tvSell, tvInbox, tvProfile).forEach {
            it?.setTextColor(getColor(R.color.vc_grey_text))
        }

        // Highlight current tab
        when (selectedTab) {
            Tab.HOME -> tvHome?.setTextColor(getColor(R.color.vc_black))
            Tab.EXPLORE -> tvExplore?.setTextColor(getColor(R.color.vc_black))
            Tab.SELL -> tvSell?.setTextColor(getColor(R.color.vc_black))
            Tab.INBOX -> tvInbox?.setTextColor(getColor(R.color.vc_black))
            Tab.PROFILE -> tvProfile?.setTextColor(getColor(R.color.vc_black))
        }

        // Click handlers
        navHome?.setOnClickListener {
            if (selectedTab != Tab.HOME) {
                openMainTab(HomeActivity::class.java)
                finish()
            }
        }
        navExplore?.setOnClickListener {
            if (selectedTab != Tab.EXPLORE) {
                openMainTab(ExploreActivity::class.java)
                finish()
            }
        }
        navSell?.setOnClickListener {
            if (selectedTab != Tab.SELL) {
                openMainTab(SellActivity::class.java)
                finish()
            }
        }
        navInbox?.setOnClickListener {
            if (selectedTab != Tab.INBOX) {
                openMainTab(InboxActivity::class.java)
                finish()
            }
        }
        navProfile?.setOnClickListener {
            if (selectedTab != Tab.PROFILE) {
                openMainTab(ShopProfileActivity::class.java)
                finish()
            }
        }
    }

    private fun openMainTab(destination: Class<*>) {
        startActivity(Intent(this, destination).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
        finish()
    }
}
