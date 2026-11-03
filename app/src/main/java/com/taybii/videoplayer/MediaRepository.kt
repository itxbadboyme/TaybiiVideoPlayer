package com.taybii.videoplayer

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore

data class MediaEntry(val id: Long, val uri: Uri, val name: String, val durationMs: Long, val bytes: Long, val type: String)
object MediaRepository {
    fun load(c: Context, type: String): List<MediaEntry> {
        val video = type == "video"
        val source = if (video) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val id = MediaStore.MediaColumns._ID
        val name = MediaStore.MediaColumns.DISPLAY_NAME
        val duration = if(video) MediaStore.Video.Media.DURATION else MediaStore.Audio.Media.DURATION
        val projection = arrayOf(id,name,duration,MediaStore.MediaColumns.SIZE)
        val results= mutableListOf<MediaEntry>()
        try {
            c.contentResolver.query(source,projection,null,null,"${MediaStore.MediaColumns.DATE_ADDED} DESC")?.use { cur ->
                val iid=cur.getColumnIndexOrThrow(id)
                val n=cur.getColumnIndexOrThrow(name)
                val d=cur.getColumnIndexOrThrow(duration)
                val b=cur.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE)
                while(cur.moveToNext()) {
                    val value=cur.getLong(iid)
                    results.add(MediaEntry(value, ContentUris.withAppendedId(source,value),cur.getString(n)?:"Untitled",cur.getLong(d),cur.getLong(b),type))
                }
            }
        } catch(_:SecurityException) { }
        return results
    }
}
