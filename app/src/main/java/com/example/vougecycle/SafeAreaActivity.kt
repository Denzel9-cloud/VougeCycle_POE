package com.example.vougecycle

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Applies system bar and display cutout spacing to every screen that extends this activity. */
open class SafeAreaActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Android 15+ draws app content edge to edge. We keep each screen's content
        // inside the safe area so it does not sit under the status or navigation bars.
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        val content = findViewById<View>(android.R.id.content) ?: return
        LanguageManager.translateViewTree(content)
        content.viewTreeObserver.addOnGlobalLayoutListener {
            if (content.isAttachedToWindow) LanguageManager.translateViewTree(content)
        }
        val initialLeft = content.paddingLeft
        val initialTop = content.paddingTop
        val initialRight = content.paddingRight
        val initialBottom = content.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
            val safeInsets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                initialLeft + safeInsets.left,
                initialTop + safeInsets.top,
                initialRight + safeInsets.right,
                initialBottom + safeInsets.bottom
            )
            windowInsets
        }
        ViewCompat.requestApplyInsets(content)
    }

    override fun onResume() {
        super.onResume()
        findViewById<View>(android.R.id.content)?.let(LanguageManager::translateViewTree)
        findViewById<android.widget.TextView>(R.id.btnLanguage)?.text =
            if (LanguageManager.isZulu(this)) "Ulimi: isiZulu" else "Language: English"
    }
}
