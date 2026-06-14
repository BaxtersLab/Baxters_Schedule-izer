package com.baxter.schedulaizer.ui

import android.content.Context
import android.view.ContextThemeWrapper

fun Context.tryGetMainActivity(): MainActivity? {
    return when (this) {
        is MainActivity -> this
        is ContextThemeWrapper -> baseContext as? MainActivity
        else -> null
    }
}
