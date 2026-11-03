package com.taybii.videoplayer

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.*

object Ui {
    val bg = Color.rgb(16,17,20)
    val panel = Color.rgb(34,38,46)
    val text = Color.WHITE
    val sub = Color.rgb(159,166,180)
    val blue = Color.rgb(66,133,255)
    fun dp(c: Context, v: Int) = (v*c.resources.displayMetrics.density).toInt()
    fun shape(color: Int, radius: Int, c: Context) = GradientDrawable().apply { setColor(color); cornerRadius=dp(c,radius).toFloat() }
    fun label(c: Context, value: String, size: Float=16f, color: Int=text, bold: Boolean=false) = TextView(c).apply {
        this.text=value; textSize=size; setTextColor(color)
        if (bold) setTypeface(null,Typeface.BOLD)
        gravity=Gravity.CENTER_VERTICAL
    }
    fun button(c: Context, value: String, click: ()->Unit) = TextView(c).apply {
        text=value; textSize=15f; setTextColor(Color.WHITE); gravity=Gravity.CENTER
        background=shape(panel,12,c); setPadding(dp(c,14),dp(c,13),dp(c,14),dp(c,13))
        setOnClickListener { click() }
    }
    fun vertical(c:Context) = LinearLayout(c).apply { orientation=LinearLayout.VERTICAL }
    fun horizontal(c:Context) = LinearLayout(c).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER_VERTICAL }
}
