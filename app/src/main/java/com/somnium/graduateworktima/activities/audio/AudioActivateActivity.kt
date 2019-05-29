package com.somnium.graduateworktima.activities.audio

import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import com.somnium.graduateworktima.R
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter

const val AUDIO_URL = "audio"

class AudioActivateActivity : AppCompatActivity() {

    private val STATE_RESUME_WINDOW = "resumeWindow"
    private val STATE_RESUME_POSITION = "resumePosition"

    private var mExoPlayerView: SimpleExoPlayerView? = null
    private var mVideoSource: MediaSource? = null

    private var mResumeWindow: Int = 0
    private var mResumePosition: Long = 0
    private var audioUrl: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fullscreen_audio)

        if (savedInstanceState != null) {
            mResumeWindow = savedInstanceState.getInt(STATE_RESUME_WINDOW)
            mResumePosition = savedInstanceState.getLong(STATE_RESUME_POSITION)
        }
        audioUrl =  intent.extras.getString(AUDIO_URL)
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
        mExoPlayerView!!.player.playWhenReady = true
    }


    override fun onResume() {
        super.onResume()
        if (mExoPlayerView == null) {
            mExoPlayerView = findViewById(R.id.exoplayer)
            val userAgent = Util.getUserAgent(this@AudioActivateActivity, applicationContext.applicationInfo.packageName)
            val daUri = Uri.parse(audioUrl)
            mVideoSource = ExtractorMediaSource(daUri, DefaultDataSourceFactory(baseContext, userAgent), DefaultExtractorsFactory(), null, null) as MediaSource?
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
