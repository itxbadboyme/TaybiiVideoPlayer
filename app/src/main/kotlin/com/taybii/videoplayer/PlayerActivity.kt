package com.taybii.videoplayer

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Color
import android.media.AudioManager
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackSelectionParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlin.math.abs

class PlayerActivity:Activity() {
    private lateinit var player:ExoPlayer
    private lateinit var view:PlayerView
    private lateinit var container:FrameLayout
    private lateinit var toastText:TextView
    private lateinit var nameText:TextView
    private lateinit var controls:LinearLayout
    private lateinit var playButton:TextView
    private lateinit var seek:SeekBar
    private lateinit var time:TextView
    private lateinit var audio:AudioManager
    private var media=emptyList<MediaEntry>()
    private var current=0
    private var kind="video"
    private var repeatSetting=0
    private var eq:Equalizer?=null
    private var bass:BassBoost?=null
    private var downX=0f
    private var downY=0f
    private var lastY=0f
    private var moved=false
    private var adjusting=false
    private val handler=android.os.Handler(android.os.Looper.getMainLooper())
    private val tick=object:Runnable { override fun run() { updateProgress();handler.postDelayed(this,700) } }

    override fun onCreate(state:Bundle?) {
        super.onCreate(state)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.statusBarColor=Color.BLACK;window.navigationBarColor=Color.BLACK
        kind=intent.getStringExtra("kind")?:"video"
        media=MediaRepository.load(this,kind)
        current=intent.getIntExtra("index",0).coerceIn(0,(media.size-1).coerceAtLeast(0))
        if(media.isEmpty()) { finish();return }
        if(kind=="video") requestedOrientation=ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        audio=getSystemService(Context.AUDIO_SERVICE) as AudioManager
        player=ExoPlayer.Builder(this).build()
        player.addListener(object:Player.Listener {
            override fun onMediaItemTransition(item:MediaItem?,reason:Int) { current=player.currentMediaItemIndex.coerceAtLeast(0);refreshTitle() }
            override fun onPlaybackStateChanged(state:Int) { updateProgress() }
            override fun onIsPlayingChanged(isPlaying:Boolean) { if(::playButton.isInitialized) playButton.text=if(isPlaying) "Ⅱ" else "▶" }
        })
        build()
        media.forEach { player.addMediaItem(MediaItem.fromUri(it.uri)) }
        player.prepare();player.seekToDefaultPosition(current);player.playWhenReady=true
        handler.post(tick)
    }
    private fun build() {
        container=FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        setContentView(container)
        view=PlayerView(this).apply { player=this@PlayerActivity.player;useController=false;setBackgroundColor(Color.BLACK) }
        container.addView(view,FrameLayout.LayoutParams(-1,-1))
        val top=Ui.horizontal(this).apply { setPadding(Ui.dp(this@PlayerActivity,16),Ui.dp(this@PlayerActivity,8),Ui.dp(this@PlayerActivity,16),0); setBackgroundColor(0x66000000) }
        top.addView(Ui.button(this,"‹") { finish() })
        nameText=Ui.label(this,"",16f,Color.WHITE,true).apply{maxLines=1;ellipsize=android.text.TextUtils.TruncateAt.END;setPadding(Ui.dp(this@PlayerActivity,12),0,Ui.dp(this@PlayerActivity,12),0)}
        top.addView(nameText,LinearLayout.LayoutParams(0,Ui.dp(this,52),1f))
        top.addView(Ui.button(this,"⚙  Settings") { showSettings() })
        container.addView(top,FrameLayout.LayoutParams(-1,-2,Gravity.TOP))
        controls=Ui.vertical(this).apply { setPadding(Ui.dp(this@PlayerActivity,16),Ui.dp(this@PlayerActivity,8),Ui.dp(this@PlayerActivity,16),Ui.dp(this@PlayerActivity,14));setBackgroundColor(0x99000000) }
        val buttons=Ui.horizontal(this).apply{gravity=Gravity.CENTER}
        buttons.addView(Ui.button(this,"◀◀") { player.seekToPreviousMediaItem() })
        playButton=Ui.button(this,"Ⅱ") { if(player.isPlaying)player.pause() else player.play() }
        buttons.addView(playButton,LinearLayout.LayoutParams(Ui.dp(this,100),-2))
        buttons.addView(Ui.button(this,"▶▶") { next() })
        controls.addView(buttons)
        seek=SeekBar(this).apply { max=1000;progressTintList=android.content.res.ColorStateList.valueOf(Ui.blue);thumbTintList=android.content.res.ColorStateList.valueOf(Ui.blue) }
        seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{
            override fun onProgressChanged(s:SeekBar?,progress:Int,fromUser:Boolean) { if(fromUser && player.duration>0) time.text="${MainActivity.format(player.duration*progress/1000)} / ${MainActivity.format(player.duration)}" }
            override fun onStartTrackingTouch(s:SeekBar?) {}
            override fun onStopTrackingTouch(s:SeekBar?) { if(player.duration>0)player.seekTo(player.duration*seek.progress/1000) }
        })
        controls.addView(seek)
        time=Ui.label(this,"00:00 / 00:00",13f,Color.WHITE)
        controls.addView(time)
        container.addView(controls,FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM))
        toastText=Ui.label(this,"",20f,Color.WHITE,true).apply { gravity=Gravity.CENTER;setPadding(24,12,24,12);background=Ui.shape(0xB3000000,14,this@PlayerActivity);visibility=View.GONE }
        container.addView(toastText,FrameLayout.LayoutParams(-2,-2,Gravity.CENTER))
        val gesture=View(this)
        container.addView(gesture,FrameLayout.LayoutParams(-1,-1))
        // Video-surface gestures stay behind the top buttons and playback controls.
        container.removeView(gesture);container.addView(gesture,1)
        gesture.setOnTouchListener { _,event -> onGesture(event) }
        refreshTitle()
    }
    private fun onGesture(e:MotionEvent):Boolean {
        if(kind!="video") { if(e.action==MotionEvent.ACTION_UP)toggleControls();return true }
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> { downX=e.x;downY=e.y;lastY=e.y; moved=false;adjusting=false;return true }
            MotionEvent.ACTION_MOVE -> {
                val dx=abs(e.x-downX);val dy=abs(e.y-downY)
                if(!adjusting && dy>Ui.dp(this,24) && dy>dx)adjusting=true
                if(adjusting){
                    val delta=(lastY-e.y)/resources.displayMetrics.heightPixels.toFloat()
                    if(downX<container.width/2) {
                        val currentBrightness=window.attributes.screenBrightness.takeIf{it>=0}?:0.5f
                        val target=(currentBrightness+delta*2.4f).coerceIn(0.02f,1f)
                        window.attributes=window.attributes.apply{screenBrightness=target}
                        overlay("☀  ${(target*100).toInt()}%")
                    } else {
                        val max=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                        val now=audio.getStreamVolume(AudioManager.STREAM_MUSIC)
                        val next=(now+delta*max*2.4f).toInt().coerceIn(0,max)
                        if(next!=now)audio.setStreamVolume(AudioManager.STREAM_MUSIC,next,0)
                        overlay("♫  ${(next*100/max.coerceAtLeast(1))}%")
                    }
                    moved=true;lastY=e.y
                }
                return true
            }
            MotionEvent.ACTION_CANCEL,MotionEvent.ACTION_UP -> { if(!moved)toggleControls();handler.postDelayed({toastText.visibility=View.GONE},800);return true }
        }
        return true
    }
    private fun overlay(s:String) { toastText.text=s;toastText.visibility=View.VISIBLE }
    private fun toggleControls() { controls.visibility=if(controls.visibility==View.VISIBLE)View.GONE else View.VISIBLE }
    private fun next() { if(player.hasNextMediaItem())player.seekToNextMediaItem() else if(repeatSetting==2)player.seekToDefaultPosition(0) else Toast.makeText(this,"Last item",Toast.LENGTH_SHORT).show() }
    private fun refreshTitle() { if(::nameText.isInitialized && media.isNotEmpty()) nameText.text=media.getOrNull(current)?.name?:"Taybii Player" }
    private fun updateProgress() {
        if(!::seek.isInitialized || !::player.isInitialized) return
        val d=player.duration.coerceAtLeast(0);val pos=player.currentPosition.coerceAtLeast(0)
        if(!seek.isPressed)seek.progress=if(d>0)(pos*1000/d).toInt().coerceIn(0,1000) else 0
        time.text="${MainActivity.format(pos)} / ${MainActivity.format(d)}"
    }
    private fun showSettings() {
        val names=arrayOf("Audio track","Equalizer","Bass boost","Favorite","Repeat","Next")
        AlertDialog.Builder(this).setTitle("Player Settings").setItems(names) { _,which ->
            when(which) { 0->tracks();1->equalizer();2->boost();3->favorite();4->repeat();5->next() }
        }.setNegativeButton("Close",null).show()
    }
    private fun tracks() {
        val options=mutableListOf<Pair<String,TrackSelectionOverride?>>()
        options.add("Auto (default)" to null)
        player.currentTracks.groups.forEach { group ->
            if(group.type==C.TRACK_TYPE_AUDIO) for(i in 0 until group.length) {
                if(group.isTrackSupported(i)) {
                    val fmt=group.getTrackFormat(i)
                    val language=fmt.language?:"Unknown language"
                    options.add("$language • ${fmt.channelCount.takeIf{it>0}?:"?"} ch" to TrackSelectionOverride(group.mediaTrackGroup,listOf(i)))
                }
            }
        }
        AlertDialog.Builder(this).setTitle("Audio tracks").setItems(options.map{it.first}.toTypedArray()) { _,index ->
            val builder=player.trackSelectionParameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_AUDIO)
            options[index].second?.let { builder.addOverride(it) }
            player.trackSelectionParameters=builder.build()
        }.show()
    }
    private fun getEq():Equalizer? {
        if(eq==null)try { eq=Equalizer(0,player.audioSessionId).apply { enabled=true } }catch(_:Exception) { Toast.makeText(this,"Equalizer unavailable on this device",Toast.LENGTH_LONG).show() }
        return eq
    }
    private fun equalizer() {
        val effect=getEq()?:return
        val presets=(0 until effect.numberOfPresets.toInt()).map { effect.getPresetName(it.toShort()).toString() }
        if(presets.isEmpty()) { Toast.makeText(this,"No presets supported",Toast.LENGTH_SHORT).show();return }
        AlertDialog.Builder(this).setTitle("Equalizer presets").setSingleChoiceItems(presets.toTypedArray(),effect.currentPreset.toInt().coerceAtLeast(0)) { dialog,index ->
            try{effect.usePreset(index.toShort())}catch(_:Exception){};dialog.dismiss()
        }.setNegativeButton("Close",null).show()
    }
    private fun boost() {
        if(bass==null)try { bass=BassBoost(0,player.audioSessionId).apply{enabled=true} }catch(_:Exception) { Toast.makeText(this,"Bass boost unavailable",Toast.LENGTH_LONG).show();return }
        val effect=bass?:return
        val slider=SeekBar(this).apply{max=1000;progress=effect.roundedStrength.toInt()}
        AlertDialog.Builder(this).setTitle("Bass boost").setView(slider).setPositiveButton("Apply") { _,_->try{effect.setStrength(slider.progress.toShort())}catch(_:Exception){} }.setNegativeButton("Cancel",null).show()
    }
    private fun favorite() {
        val entry=media.getOrNull(current)?:return
        val chosen=FavoritesRepository.toggle(this,entry.uri.toString())
        Toast.makeText(this,if(chosen)"Added to favorites" else "Removed from favorites",Toast.LENGTH_SHORT).show()
    }
    private fun repeat() {
        repeatSetting=(repeatSetting+1)%3
        player.repeatMode=when(repeatSetting) { 1->Player.REPEAT_MODE_ONE;2->Player.REPEAT_MODE_ALL;else->Player.REPEAT_MODE_OFF }
        Toast.makeText(this,when(repeatSetting){1->"Repeat one";2->"Repeat all";else->"Repeat off"},Toast.LENGTH_SHORT).show()
    }
    override fun onStop() { super.onStop(); if(::player.isInitialized)player.pause() }
    override fun onDestroy() { handler.removeCallbacksAndMessages(null); try{eq?.release();bass?.release()}catch(_:Exception){};if(::player.isInitialized)player.release();super.onDestroy() }
}
