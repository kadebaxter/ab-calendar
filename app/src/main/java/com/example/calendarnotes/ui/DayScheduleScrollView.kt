package com.example.calendarnotes.ui

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.ScrollView

/**
 * ScrollView that can optionally intercept touches for horizontal day-change swipes
 * before hour-row children consume the gesture.
 */
class DayScheduleScrollView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ScrollView(context, attrs, defStyleAttr) {

    var interceptTouch: ((MotionEvent) -> Boolean)? = null
    var handleTouch: ((MotionEvent) -> Boolean)? = null

    init {
        // Keep cards from painting over the date strip / all-day area above.
        clipChildren = true
        clipToPadding = true
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (interceptTouch?.invoke(ev) == true) {
            return true
        }
        return super.onInterceptTouchEvent(ev)
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean {
        if (handleTouch?.invoke(ev) == true) {
            return true
        }
        return super.onTouchEvent(ev)
    }
}
