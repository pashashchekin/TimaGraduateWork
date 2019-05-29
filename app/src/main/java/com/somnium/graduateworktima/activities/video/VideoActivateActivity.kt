package com.somnium.graduateworktima.activities.video

import android.app.Dialog
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.somnium.graduateworktima.R
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.exoplayer2.source.MediaSource
import android.support.v4.content.ContextCompat
import android.view.View
import android.view.ViewGroup
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.somnium.graduateworktima.activities.photo.IMAGE_URL

const val VIDEO_URL = "video"

class VideoActivateActivity : AppCompatActivity() {

    private val STATE_RESUME_WINDOW = "resumeWindow"
    private val STATE_RESUME_POSITION = "resumePosition"

    private var mExoPlayerView: SimpleExoPlayerView? = null
    private var mVideoSource: MediaSource? = null

    private var mResumeWindow: Int = 0
    private var mResumePosition: Long = 0
    private var videoUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_audio)

        if (savedInstanceState != null) {
            mResumeWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            mResumePosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
        }
        videoUrl =  intent.extras.getString(VIDEO_URL)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_RESUME_WINDOW, mResumeWindow)
        outState.putLong(STATE_RESUME_POSITION, mResumePosition)
        super.onSaveInstanceState(outState)
    }


    private fun initExoPlayer() {
        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        val loadControl = DefaultLoadControl()
        val player = ExoPlayerFactory.newSimpleInstance(DefaultRenderersFactory(this), trackSelector, loadControl)
        mExoPlayerView!!.player = player

        val haveResumePosition = mResumeWindow != C.INDEX_UNSET
        if (haveResumePosition) {
            mExoPlayerView!!.player.seekTo(mResumeWindow, mResumePosition)
        }
        mExoPlayerView!!.player.prepare(mVideoSource)
        mExoPlayerView!!.player.playWhenReady = false
    }


    override fun onResume() {
        super.onResume()
        if (mExoPlayerView == null) {
            mExoPlayerView = findViewById(R.id.exoplayer)

            val userAgent = Util.getUserAgent(this@VideoActivateActivity, applicationContext.applicationInfo.packageName)
            val daUri = Uri.parse(videoUrl)
            mVideoSource = ExtractorMediaSource(daUri, DefaultDataSourceFactory(baseContext, userAgent), DefaultExtractorsFactory(), null, null)
        }
        initExoPlayer()
    }


    override fun onPause() {
        super.onPause()
        if (mExoPlayerView != null && mExoPlayerView!!.player != null) {
            mResumeWindow = mExoPlayerView!!.player.currentWindowIndex
            mResumePosition = Math.max(0, mExoPlayerView!!.player.contentPosition)
            mExoPlayerView!!.player.release()
        }
    }
}
