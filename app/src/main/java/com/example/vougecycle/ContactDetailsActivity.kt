package com.example.vougecycle

import android.os.Bundle
import android.text.InputType
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivityContactDetailsBinding
import kotlinx.coroutines.launch

class ContactDetailsActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityContactDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContactDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener { finish() }
        binding.btnAddContact.setOnClickListener { showAddContactDialog() }
        LanguageManager.translateViewTree(binding.root)
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadContacts()
    }

    private fun loadContacts() {
        lifecycleScope.launch {
            try {
                val contacts = ContactDetailsRepository.list()
                binding.contactList.removeAllViews()
                contacts.forEach { contact ->
                    val card = LinearLayout(this@ContactDetailsActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        setPadding(dp(14), dp(12), dp(14), dp(8))
                        setBackgroundResource(R.drawable.bg_input_field)
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply { bottomMargin = dp(10) }
                    }
                    card.addView(TextView(this@ContactDetailsActivity).apply {
                        text = LanguageManager.translate(this@ContactDetailsActivity,
                            if (contact.contactType == "email") "Email address" else "Phone number")
                        textSize = 13f
                        setTextColor(getColor(R.color.vc_grey_text))
                    })
                    val row = LinearLayout(this@ContactDetailsActivity).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = android.view.Gravity.CENTER_VERTICAL
                    }
                    row.addView(TextView(this@ContactDetailsActivity).apply {
                        text = contact.contactValue
                        textSize = 16f
                        setTextColor(getColor(R.color.vc_black))
                    }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
                    row.addView(Button(this@ContactDetailsActivity).apply {
                        text = LanguageManager.translate(this@ContactDetailsActivity, "Remove")
                        isAllCaps = false
                        setTextColor(getColor(R.color.vc_red))
                        setOnClickListener { confirmRemove(contact) }
                    })
                    card.addView(row)
                    binding.contactList.addView(card)
                }
                binding.tvEmpty.visibility = if (contacts.isEmpty()) View.VISIBLE else View.GONE
            } catch (e: Exception) {
                LanguageManager.makeToast(this@ContactDetailsActivity,
                    AppReliability.friendlyMessage(e, "load contact details"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAddContactDialog() {
        val typePicker = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@ContactDetailsActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf(
                    LanguageManager.translate(this@ContactDetailsActivity, "Phone number"),
                    LanguageManager.translate(this@ContactDetailsActivity, "Email address")
                )
            )
        }
        val valueInput = EditText(this).apply {
            hint = LanguageManager.translate(this@ContactDetailsActivity, "Enter a phone number or email address")
            inputType = InputType.TYPE_CLASS_TEXT
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(typePicker)
            addView(valueInput)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(LanguageManager.translate(this, "Add contact detail"))
            .setView(form)
            .setNegativeButton(LanguageManager.translate(this, "Cancel"), null)
            .setPositiveButton(LanguageManager.translate(this, "Save"), null)
            .create()
        typePicker.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                valueInput.inputType = if (position == 0) {
                    InputType.TYPE_CLASS_PHONE
                } else {
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val value = valueInput.text.toString().trim()
                val isEmail = typePicker.selectedItemPosition == 1
                val isValid = if (isEmail) Patterns.EMAIL_ADDRESS.matcher(value).matches() else {
                    value.count { it.isDigit() } >= 7 && value.matches(Regex("^[+0-9() .-]{7,24}$"))
                }
                if (!isValid) {
                    valueInput.error = LanguageManager.translate(this, "Enter a valid phone number or email address")
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    try {
                        ContactDetailsRepository.add(if (isEmail) "email" else "phone", value)
                        dialog.dismiss()
                        loadContacts()
                    } catch (e: Exception) {
                        LanguageManager.makeToast(this@ContactDetailsActivity,
                            AppReliability.friendlyMessage(e, "save contact details"), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmRemove(contact: ContactDetail) {
        AlertDialog.Builder(this)
            .setTitle(LanguageManager.translate(this, "Remove contact detail?"))
            .setMessage(contact.contactValue)
            .setNegativeButton(LanguageManager.translate(this, "Cancel"), null)
            .setPositiveButton(LanguageManager.translate(this, "Remove")) { _, _ ->
                lifecycleScope.launch {
                    try {
                        ContactDetailsRepository.delete(contact.id)
                        loadContacts()
                    } catch (e: Exception) {
                        LanguageManager.makeToast(this@ContactDetailsActivity,
                            AppReliability.friendlyMessage(e, "remove contact detail"), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
