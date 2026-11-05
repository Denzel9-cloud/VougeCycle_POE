package com.example.vougecycle

import android.app.Activity
import android.text.InputType
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView

object UiSafety {
    fun preventAutomaticKeyboard(activity: Activity) {
        activity.window.setSoftInputMode(
            WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN or
                WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )
    }

    fun bindPasswordVisibility(field: EditText, button: ImageView) {
        var visible = false
        button.setImageResource(R.drawable.ic_visibility_off)
        button.setOnClickListener {
            visible = !visible
            val cursor = field.selectionStart
            field.inputType = InputType.TYPE_CLASS_TEXT or if (visible) {
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            } else {
                InputType.TYPE_TEXT_VARIATION_PASSWORD
            }
            button.setImageResource(if (visible) R.drawable.ic_visibility else R.drawable.ic_visibility_off)
            button.contentDescription = if (visible) "Hide password" else "Show password"
            field.setSelection(cursor.coerceAtLeast(0).coerceAtMost(field.length()))
        }
    }
}
