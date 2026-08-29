package org.falaeapp.falae.activity

import android.app.Activity
import android.os.Build
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

/** Keep board controls outside system bars and cutouts when edge-to-edge is enforced. */
internal fun Activity.protectContentFromSystemInsets() {
    if (Build.VERSION.SDK_INT < 35) return

    WindowCompat.setDecorFitsSystemWindows(window, false)
    val content = findViewById<View>(android.R.id.content)
    val left = content.paddingLeft
    val top = content.paddingTop
    val right = content.paddingRight
    val bottom = content.paddingBottom
    ViewCompat.setOnApplyWindowInsetsListener(content) { view, windowInsets ->
        val safeInsets = windowInsets.getInsets(
            WindowInsetsCompat.Type.systemBars() or
                WindowInsetsCompat.Type.displayCutout() or
                WindowInsetsCompat.Type.ime()
        )
        view.setPadding(
            left + safeInsets.left,
            top + safeInsets.top,
            right + safeInsets.right,
            bottom + safeInsets.bottom
        )
        // The content container owns the insets; DrawerLayout must not apply them twice.
        WindowInsetsCompat.CONSUMED
    }
    ViewCompat.requestApplyInsets(content)
}
