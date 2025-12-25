package com.itheamc.aiassistant

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.itheamc.aiassistant.platform.PlatformLlmInference

class LlmContextInitializer : ContentProvider() {
    override fun onCreate(): Boolean {
        val ctx = context?.applicationContext ?: return false
        PlatformLlmInference.setContext(ctx)
        return true
    }


    override fun query(
        uri: Uri,
        p1: Array<out String>?,
        p2: String?,
        p3: Array<out String>?,
        p4: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, p1: ContentValues?): Uri? = null
    override fun delete(uri: Uri, p1: String?, p2: Array<out String>?): Int = 0
    override fun update(uri: Uri, p1: ContentValues?, p2: String?, p3: Array<out String>?): Int = 0
}