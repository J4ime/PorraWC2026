package com.porrawc2026.app.util

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.porrawc2026.app.data.remote.LiveScorer

object GsonHolder {
    val gson: Gson = Gson()
    val scorerListType: java.lang.reflect.Type = object : TypeToken<List<LiveScorer>>() {}.type
}