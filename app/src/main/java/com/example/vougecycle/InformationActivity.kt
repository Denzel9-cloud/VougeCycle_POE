package com.example.vougecycle

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import com.example.vougecycle.databinding.ActivityInformationBinding

class InformationActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityInformationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInformationBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener { finish() }

        val isPrivacyPage = intent.getStringExtra(EXTRA_PAGE) == PAGE_PRIVACY
        if (isPrivacyPage) showPrivacyContent() else showShopContent()
        LanguageManager.translateViewTree(binding.root)
    }

    private fun showPrivacyContent() {
        binding.tvHeading.text = LanguageManager.translate(this, "Privacy & Consent")
        addParagraph("VogueCycle respects your privacy. This page explains how information is used when you browse, buy, sell, or communicate through the marketplace.")
        addSection("Information we use", "VogueCycle uses account and profile details, contact details you add, listing information and photos, messages, order and delivery details, reviews, and your app preferences to provide the marketplace.")
        addSection("How information is used", "Information helps create and manage your account, display your profile and listings, support buyer and seller conversations, process orders and delivery, maintain platform safety, and respond to support requests.")
        addSection("What other users can see", "Your username, public profile information, listings, and reviews may be visible to other users. Contact details saved in Contact Details are private to your account. Order and delivery information is shared only with the people involved when needed to complete an order.")
        addSection("Your choices", "You can review and update your profile and contact details in Settings. Before posting an item, choose the photos and description you want shoppers to see. Avoid including sensitive personal information in public listings or messages.")
        addSection("Consent", "VogueCycle uses personal information for purposes needed to provide the marketplace and, where required, with your consent or another lawful justification. You decide what profile and listing information to make public.")
        addSection("Data care", "Account information is stored with the services used to operate VogueCycle and is protected by account access controls. Information may be retained while your account is active and when needed to maintain order records, protect users, or meet applicable requirements.")
        addSection("Your privacy rights", "You may ask to access or correct your personal information, request deletion where applicable, or object to certain processing. You may also raise a privacy complaint with the Information Regulator of South Africa.")
        addSection("Questions or updates", "Keep your contact details current so you can receive important account and order communications. For an account or privacy request, contact VogueCycle through its official support channel.")
    }

    private fun showShopContent() {
        binding.tvHeading.text = LanguageManager.translate(this, "Shop VougeCycle Pty Ltd")
        addParagraph("Buy vintage · Sell vintage · Keep fashion in motion")
        addSection("About VogueCycle", "VogueCycle is a marketplace for buying and selling pre-loved and vintage clothing. It brings shoppers and independent sellers together to give great fashion pieces another life.")
        addSection("Find your next favourite", "Browse clothing listings, compare item details, and message sellers when you need more information. Check the listed condition, sizing, price, and delivery details before placing an order.")
        addSection("Sell on VogueCycle", "Create a shop by listing items with clear photos, accurate descriptions, honest condition details, and a fair price. Respond to buyer questions and keep order and delivery information up to date.")
        addSection("Our community", "We want fashion to stay in motion through thoughtful reuse and respectful trading. Please describe items accurately, communicate clearly, protect personal information, and treat other members with respect.")
        addSection("Marketplace experience", "VogueCycle provides tools for browsing, listing, messaging, orders, delivery updates, and reviews. Buyers and sellers should use the information in each listing and order to make informed decisions.")
    }

    private fun addSection(title: String, body: String) {
        val heading = TextView(this).apply {
            text = LanguageManager.translate(this@InformationActivity, title)
            textSize = 17f
            setTextColor(getColor(R.color.vc_black))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
            setPadding(0, dp(18), 0, dp(5))
        }
        binding.content.addView(heading, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        addParagraph(body)
    }

    private fun addParagraph(text: String) {
        val paragraph = TextView(this).apply {
            this.text = LanguageManager.translate(this@InformationActivity, text)
            textSize = 15f
            setTextColor(getColor(R.color.vc_grey_text))
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        binding.content.addView(paragraph, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val PAGE_PRIVACY = "privacy"
        const val PAGE_SHOP = "shop"
        private const val EXTRA_PAGE = "information_page"

        fun intentFor(context: Context, page: String) = Intent(context, InformationActivity::class.java)
            .putExtra(EXTRA_PAGE, page)
    }
}
