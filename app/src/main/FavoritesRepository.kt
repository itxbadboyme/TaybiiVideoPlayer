package com.taybii.videoplayer
import android.content.Context
object FavoritesRepository {
    private const val PREF="taybii_favorites"
    fun contains(c:Context,id:String)=c.getSharedPreferences(PREF,0).getBoolean(id,false)
    fun toggle(c:Context,id:String):Boolean {
        val next=!contains(c,id)
        c.getSharedPreferences(PREF,0).edit().putBoolean(id,next).apply()
        return next
    }
}
