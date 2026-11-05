package com.example.vougecycle

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView

/** Shows translated labels while keeping the original English values for app logic and Supabase. */
class LocalizedArrayAdapter(
    private val appContext: Context,
    private val values: List<String>
) : ArrayAdapter<String>(appContext, android.R.layout.simple_spinner_item, values) {

    init {
        setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)
        (view as? TextView)?.let {
            it.text = LanguageManager.translate(appContext, values[position])
        }
        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)
        (view as? TextView)?.let {
            it.text = LanguageManager.translate(appContext, values[position])
        }
        return view
    }
}
