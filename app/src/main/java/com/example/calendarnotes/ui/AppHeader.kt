package com.example.calendarnotes.ui

import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.calendarnotes.R

object AppHeader {
    fun setupBackHeader(
        activity: AppCompatActivity,
        title: String,
        overflowVisible: Boolean = false,
        onOverflowClick: ((View) -> Unit)? = null
    ) {
        activity.findViewById<TextView>(R.id.tvHeaderTitle).text = title
        activity.findViewById<View>(R.id.ivHeaderChevron).visibility = View.GONE
        activity.findViewById<View>(R.id.btnHeaderTitle).isClickable = false

        val btnNav = activity.findViewById<ImageButton>(R.id.btnHeaderNav)
        btnNav.setImageResource(R.drawable.ic_arrow_back)
        btnNav.contentDescription = activity.getString(R.string.navigate_back)
        btnNav.setOnClickListener { activity.finish() }

        val btnOverflow = activity.findViewById<ImageButton>(R.id.btnHeaderOverflow)
        if (overflowVisible && onOverflowClick != null) {
            btnOverflow.visibility = View.VISIBLE
            btnOverflow.setOnClickListener { onOverflowClick(it) }
        } else {
            btnOverflow.visibility = View.INVISIBLE
            btnOverflow.setOnClickListener(null)
        }
    }

    fun setTitle(activity: AppCompatActivity, title: String) {
        activity.findViewById<TextView>(R.id.tvHeaderTitle).text = title
    }
}
