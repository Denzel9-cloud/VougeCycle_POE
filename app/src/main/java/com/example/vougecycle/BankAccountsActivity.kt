package com.example.vougecycle

import android.os.Bundle
import android.text.InputType
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.example.vougecycle.databinding.ActivityBankAccountsBinding
import kotlinx.coroutines.launch

class BankAccountsActivity : AuthenticatedActivity() {
    private lateinit var binding: ActivityBankAccountsBinding
    private var accounts: List<BankAccount> = emptyList()
    private var transactionsByAccount: Map<String, List<AccountTransaction>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBankAccountsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.ivBack.setOnClickListener { finish() }
        binding.btnAddAccount.setOnClickListener { showAddAccountDialog() }
    }

    override fun onResume() {
        super.onResume()
        if (::binding.isInitialized) loadAccounts()
    }

    private fun loadAccounts() {
        lifecycleScope.launch {
            try {
                accounts = MarketplaceRepository.bankAccounts()
                val history = mutableMapOf<String, List<AccountTransaction>>()
                for (account in accounts) {
                    history[account.id] = MarketplaceRepository.accountTransactions(account.id).take(5)
                }
                transactionsByAccount = history
                renderAccounts()
            } catch (e: Exception) {
                LanguageManager.makeToast(this@BankAccountsActivity, AppReliability.friendlyMessage(e, "load bank accounts"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun renderAccounts() {
        binding.accountList.removeAllViews()
        binding.tvEmpty.visibility = if (accounts.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
        binding.btnAddAccount.isEnabled = accounts.size < 3
        binding.btnAddAccount.text = if (accounts.size < 3) "Add bank account (${accounts.size}/3)" else "Maximum of 3 accounts reached"

        accounts.forEach { account ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(16, 14, 16, 14)
                background = getDrawable(R.drawable.bg_input_field)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
                    bottomMargin = dp(12)
                }
            }
            card.addView(label(account.displayName(), 18, true, R.color.vc_black))
            card.addView(label("Balance: R %,.2f".format(account.balance), 17, true, R.color.vc_green))
            card.addView(label("Opening balance: R %,.2f".format(account.openingBalance), 13, false, R.color.vc_grey_text))
            val accountActions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            val editButton = Button(this).apply {
                text = "Edit balance"
                isAllCaps = false
                setOnClickListener { showEditBalanceDialog(account) }
            }
            val deleteButton = Button(this).apply {
                text = "Delete"
                isAllCaps = false
                setTextColor(getColor(R.color.vc_red))
                setOnClickListener { confirmDeleteAccount(account) }
            }
            accountActions.addView(editButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            accountActions.addView(deleteButton, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            card.addView(accountActions)
            if (account.isDefault) card.addView(label("Default seller payout account", 13, true, R.color.vc_green))
            else {
                val defaultButton = Button(this).apply {
                    text = "Use for seller payouts"
                    isAllCaps = false
                    setOnClickListener { setDefault(account) }
                }
                card.addView(defaultButton)
            }
            val txns = transactionsByAccount[account.id].orEmpty()
            if (txns.isNotEmpty()) {
                card.addView(label("Recent transactions", 14, true, R.color.vc_black))
                txns.forEach { transaction ->
                    val sign = if (transaction.transactionType == "credit") "+" else "−"
                    card.addView(label("$sign R %,.2f  •  ${transaction.description}".format(transaction.amount), 13, false, R.color.vc_grey_text))
                }
            } else card.addView(label("No transactions yet", 13, false, R.color.vc_grey_text))
            binding.accountList.addView(card)
        }
    }

    private fun showAddAccountDialog() {
        if (accounts.size >= 3) return
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
        }
        val nickname = EditText(this).apply { hint = "Account name (e.g. Personal)"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS }
        val bankName = EditText(this).apply { hint = "Bank name"; inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_WORDS }
        val openingBalance = EditText(this).apply { hint = "Starting demo balance (R)"; inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL }
        form.addView(nickname)
        form.addView(bankName)
        form.addView(openingBalance)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add bank account")
            .setMessage("Enter a label and starting demo balance. Do not enter real account numbers.")
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val accountName = nickname.text.toString().trim()
                val bank = bankName.text.toString().trim()
                val amount = openingBalance.text.toString().trim().toDoubleOrNull()
                if (accountName.isBlank()) nickname.error = "Enter an account name"
                else if (bank.isBlank()) bankName.error = "Enter a bank name"
                else if (amount == null || amount < 0.0) openingBalance.error = "Enter a valid amount"
                else {
                    dialog.dismiss()
                    lifecycleScope.launch {
                        try {
                            MarketplaceRepository.addBankAccount(bank, accountName, amount)
                            loadAccounts()
                        } catch (e: Exception) {
                            LanguageManager.makeToast(this@BankAccountsActivity, AppReliability.friendlyMessage(e, "save bank account"), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun setDefault(account: BankAccount) {
        lifecycleScope.launch {
            try {
                MarketplaceRepository.setDefaultBankAccount(account.id)
                loadAccounts()
            } catch (e: Exception) {
                LanguageManager.makeToast(this@BankAccountsActivity, AppReliability.friendlyMessage(e, "update bank account"), Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showEditBalanceDialog(account: BankAccount) {
        val amountInput = EditText(this).apply {
            hint = "Current balance (R)"
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText("%.2f".format(account.balance))
            setSelection(text.length)
        }
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(8), dp(20), 0)
            addView(amountInput)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit account balance")
            .setMessage(account.displayName())
            .setView(form)
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Save", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val amount = amountInput.text.toString().trim().toDoubleOrNull()
                if (amount == null || amount < 0.0) {
                    amountInput.error = "Enter a valid amount"
                } else {
                    dialog.dismiss()
                    lifecycleScope.launch {
                        try {
                            MarketplaceRepository.updateBankAccountBalance(account.id, amount)
                            loadAccounts()
                        } catch (e: Exception) {
                            LanguageManager.makeToast(this@BankAccountsActivity, AppReliability.friendlyMessage(e, "update bank account"), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteAccount(account: BankAccount) {
        AlertDialog.Builder(this)
            .setTitle("Delete bank account?")
            .setMessage("${account.displayName()} will be removed from your active accounts. Its past transactions will be preserved.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    try {
                        MarketplaceRepository.deleteBankAccount(account.id)
                        loadAccounts()
                    } catch (e: Exception) {
                        LanguageManager.makeToast(this@BankAccountsActivity, AppReliability.friendlyMessage(e, "delete bank account"), Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    private fun label(text: String, size: Int, bold: Boolean, color: Int) = TextView(this).apply {
        this.text = LanguageManager.translate(this@BankAccountsActivity, text)
        textSize = size.toFloat()
        setTextColor(getColor(color))
        if (bold) setTypeface(typeface, android.graphics.Typeface.BOLD)
        val lp = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp.bottomMargin = dp(6)
        layoutParams = lp
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
