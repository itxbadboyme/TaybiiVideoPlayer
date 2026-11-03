package com.taybii.videoplayer

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Size
import android.view.Gravity
import android.view.View
import android.widget.*
import android.app.Activity

class MainActivity:Activity() {
    private lateinit var root:LinearLayout
    private lateinit var items:LinearLayout
    private lateinit var title:TextView
    private lateinit var videoBtn:TextView
    private lateinit var audioBtn:TextView
    private var tab="video"
    private var videoFiles=emptyList<MediaEntry>()
    private var audioFiles=emptyList<MediaEntry>()
    override fun onCreate(savedInstanceState:Bundle?) { super.onCreate(savedInstanceState); window.statusBarColor=Ui.bg; window.navigationBarColor=Ui.bg; build(); permissions() }
    override fun onResume() { super.onResume(); if(::items.isInitialized) reload() }
    private fun permissions() {
        val list=if (Build.VERSION.SDK_INT>=33) arrayOf(Manifest.permission.READ_MEDIA_VIDEO,Manifest.permission.READ_MEDIA_AUDIO)
            else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        val missing=list.filter { checkSelfPermission(it)!=PackageManager.PERMISSION_GRANTED }
        if(missing.isNotEmpty()) requestPermissions(missing.toTypedArray(),47) else reload()
    }
    override fun onRequestPermissionsResult(requestCode:Int, permissions:Array<out String>, grantResults:IntArray) { super.onRequestPermissionsResult(requestCode,permissions,grantResults); if(requestCode==47) reload() }
    private fun build() {
        root=Ui.vertical(this).apply { setBackgroundColor(Ui.bg); setPadding(Ui.dp(this@MainActivity,18),Ui.dp(this@MainActivity,12),Ui.dp(this@MainActivity,18),0) }
        setContentView(root)
        val header=Ui.horizontal(this)
        header.addView(Ui.label(this,"▶  Taybii",27f,Color.WHITE,true),LinearLayout.LayoutParams(0,Ui.dp(this,63),1f))
        header.addView(Ui.button(this,"Refresh") { reload() })
        root.addView(header)
        title=Ui.label(this,"All Videos",22f,Color.WHITE,true)
        root.addView(title, LinearLayout.LayoutParams(-1,Ui.dp(this,56)))
        val scroll=ScrollView(this)
        items=Ui.vertical(this)
        scroll.addView(items)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val nav=Ui.horizontal(this)
        videoBtn=Ui.button(this,"▣  VIDEO") { tab="video"; render() }
        audioBtn=Ui.button(this,"♫  AUDIO") { tab="audio"; render() }
        nav.addView(videoBtn,LinearLayout.LayoutParams(0,Ui.dp(this,66),1f))
        val space=View(this); nav.addView(space,LinearLayout.LayoutParams(Ui.dp(this,10),1))
        nav.addView(audioBtn,LinearLayout.LayoutParams(0,Ui.dp(this,66),1f))
        root.addView(nav)
    }
    private fun reload() { videoFiles=MediaRepository.load(this,"video");audioFiles=MediaRepository.load(this,"audio");render() }
    private fun render() {
        title.text=if(tab=="video") "All Videos" else "All Audio"
        videoBtn.background=Ui.shape(if(tab=="video") Ui.blue else Ui.panel,12,this)
        audioBtn.background=Ui.shape(if(tab=="audio") Ui.blue else Ui.panel,12,this)
        items.removeAllViews()
        val current=if(tab=="video")videoFiles else audioFiles
        if(current.isEmpty()) {
            items.addView(Ui.label(this,"No ${tab} files found. Grant media access in Android Settings and tap Refresh.",15f,Ui.sub))
            return
        }
        current.forEachIndexed { index, entry ->
            val row=Ui.horizontal(this).apply { setPadding(0,Ui.dp(this@MainActivity,8),0,Ui.dp(this@MainActivity,8)) }
            if(tab=="video") {
                val thumb=ImageView(this).apply { scaleType=ImageView.ScaleType.CENTER_CROP; background=Ui.shape(Ui.panel,8,this@MainActivity) }
                row.addView(thumb,LinearLayout.LayoutParams(Ui.dp(this,112),Ui.dp(this,73)))
                try { thumb.setImageBitmap(contentResolver.loadThumbnail(entry.uri,Size(220,146),null)) } catch(_:Exception) { }
            } else {
                val icon=Ui.label(this,"♫",30f,Ui.blue,true).apply { gravity=Gravity.CENTER; background=Ui.shape(Ui.panel,10,this@MainActivity) }
                row.addView(icon,LinearLayout.LayoutParams(Ui.dp(this,70),Ui.dp(this,70)))
            }
            val info=Ui.vertical(this).apply { setPadding(Ui.dp(this@MainActivity,14),0,0,0) }
            info.addView(Ui.label(this,entry.name,15f,Color.WHITE,true).apply{ maxLines=2; ellipsize=android.text.TextUtils.TruncateAt.END })
            info.addView(Ui.label(this,"${format(entry.durationMs)}  •  ${formatSize(entry.bytes)}",12f,Ui.sub))
            row.addView(info,LinearLayout.LayoutParams(0,-2,1f))
            row.setOnClickListener { val intent=Intent(this,PlayerActivity::class.java).apply { putExtra("kind",tab);putExtra("index",index) };startActivity(intent) }
            items.addView(row)
        }
    }
    companion object {
        fun format(ms:Long):String { val sec=ms/1000; return if(sec>=3600) "%d:%02d:%02d".format(sec/3600,(sec%3600)/60,sec%60) else "%02d:%02d".format(sec/60,sec%60) }
        fun formatSize(bytes:Long):String=if(bytes>=1024*1024*1024) "%.2f GB".format(bytes/(1024.0*1024*1024)) else "%.1f MB".format(bytes/(1024.0*1024))
    }
}
