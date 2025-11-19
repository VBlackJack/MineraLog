package com.argumentor.core.i18n

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

interface StringProvider {
    fun getString(@StringRes resId: Int, vararg args: Any): String
}

@Singleton
class AndroidStringProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : StringProvider {
    override fun getString(@StringRes resId: Int, vararg args: Any): String =
        context.getString(resId, *args)
}
