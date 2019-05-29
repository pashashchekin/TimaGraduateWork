package com.somnium.graduateworktima

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.somnium.graduateworktima.activities.audio.AudioActivity
import com.somnium.graduateworktima.activities.documents.DocumentsActivity
import com.somnium.graduateworktima.activities.photo.PhotoActivity
import com.somnium.graduateworktima.activities.video.VideoActivity
import kotlinx.android.synthetic.main.bottom_navigation_view.*


abstract class BaseActivity(val navNumber: Int) : AppCompatActivity() {
    private val TAG = "BaseActivity"

    fun setupBottomNavigation() {
        bottom_navigation_view.setIconSize(30f, 30f)
        bottom_navigation_view.setTextVisibility(false)
        bottom_navigation_view.enableItemShiftingMode(false)
        bottom_navigation_view.enableShiftingMode(false)
        bottom_navigation_view.enableAnimation(false)

        bottom_navigation_view.setOnNavigationItemSelectedListener {
            val nextActivity =
                    when (it.itemId) {
                        R.id.nav_item_photo-> PhotoActivity::class.java
                        R.id.nav_item_video -> VideoActivity::class.java
                        R.id.nav_item_audio -> AudioActivity::class.java
                        R.id.nav_item_document -> DocumentsActivity::class.java
                        else -> {
                            Log.e(TAG, "unknown nav item clicked $it")
                            null
                        }
                    }
            if (nextActivity != null) {
                val intent = Intent(this, nextActivity)
                intent.flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                startActivity(intent)
                overridePendingTransition(0, 0)
                true
            } else {
                false
            }
        }
        bottom_navigation_view.menu.getItem(navNumber).isChecked = true
    }
}