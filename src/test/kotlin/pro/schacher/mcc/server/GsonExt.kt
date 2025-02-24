package pro.schacher.mcc.server

import com.google.gson.Gson


inline fun <reified T> String.toDto(): T {
    return Gson().fromJson(this, T::class.java)
}