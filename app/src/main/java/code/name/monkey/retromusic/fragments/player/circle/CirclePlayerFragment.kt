/*
 * Copyright (c) 2020 Hemanth Savarala.
 *
 * Licensed under the GNU General Public License v3
 *
 * This is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by
 *  the Free Software Foundation either version 3 of the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

package code.name.monkey.retromusic.fragments.player.circle

import android.content.Context
import android.graphics.Color
import android.graphics.PorterDuff
import android.media.AudioManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import code.name.monkey.appthemehelper.ThemeStore
import code.name.monkey.appthemehelper.util.ATHUtil
import code.name.monkey.appthemehelper.util.ColorUtil
import code.name.monkey.appthemehelper.util.TintHelper
import code.name.monkey.appthemehelper.util.ToolbarContentTintHelper
import code.name.monkey.retromusic.R
import code.name.monkey.retromusic.extensions.hide
import code.name.monkey.retromusic.extensions.show
import code.name.monkey.retromusic.fragments.base.AbsPlayerFragment
import code.name.monkey.retromusic.helper.MusicPlayerRemote
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper
import code.name.monkey.retromusic.helper.MusicProgressViewUpdateHelper.Callback
import code.name.monkey.retromusic.helper.PlayPauseButtonOnClickHandler
import code.name.monkey.retromusic.util.MusicUtil
import code.name.monkey.retromusic.util.PreferenceUtil
import code.name.monkey.retromusic.util.ViewUtil
import code.name.monkey.retromusic.views.SeekArc
import code.name.monkey.retromusic.views.SeekArc.OnSeekArcChangeListener
import code.name.monkey.retromusic.volume.AudioVolumeObserver
import code.name.monkey.retromusic.volume.OnAudioVolumeChangedListener
import kotlinx.android.synthetic.main.fragment_circle_player.*

/**
 * Created by hemanths on 2020-01-06.
 */

class CirclePlayerFragment : AbsPlayerFragment(), Callback, OnAudioVolumeChangedListener,
    OnSeekArcChangeListener {

    private lateinit var progressViewUpdateHelper: MusicProgressViewUpdateHelper
    private var audioVolumeObserver: AudioVolumeObserver? = null

    private val audioManager: AudioManager?
        get() = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        progressViewUpdateHelper = MusicProgressViewUpdateHelper(this)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_circle_player, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
        title.isSelected = true
    }

    private fun setUpPlayerToolbar() {
        playerToolbar.apply {
            inflateMenu(R.menu.menu_player)
            setNavigationOnClickListener { requireActivity().onBackPressed() }
            setOnMenuItemClickListener(this@CirclePlayerFragment)
            ToolbarContentTintHelper.colorizeToolbar(
                this,
                ATHUtil.resolveColor(requireContext(), R.attr.colorControlNormal),
                requireActivity()
            )
        }
    }

    private fun setupViews() {
        setUpProgressSlider()
        ViewUtil.setProgressDrawable(
            progressSlider,
            ThemeStore.accentColor(requireContext()),
            false
        )
        volumeSeekBar.progressColor = ThemeStore.accentColor(requireContext())
        volumeSeekBar.arcColor = ColorUtil
            .withAlpha(ThemeStore.accentColor(requireContext()), 0.25f)
        setUpPlayPauseFab()
        setUpPrevNext()
        setUpPlayerToolbar()
    }

    private fun setUpPrevNext() {
        updatePrevNextColor()
        nextButton.setOnClickListener { MusicPlayerRemote.playNextSong() }
        previousButton.setOnClickListener { MusicPlayerRemote.back() }
    }

    private fun updatePrevNextColor() {
        val accentColor = ThemeStore.accentColor(requireContext())
        nextButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
        previousButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN)
    }

    private fun setUpPlayPauseFab() {
        TintHelper.setTintAuto(playPauseButton, ThemeStore.accentColor(requireContext()), false)
        playPauseButton.setOnClickListener(PlayPauseButtonOnClickHandler())
    }

    override fun onResume() {
        super.onResume()
        progressViewUpdateHelper.start()
        if (audioVolumeObserver == null) {
            audioVolumeObserver = AudioVolumeObserver(requireActivity())
        }
        audioVolumeObserver!!.register(AudioManager.STREAM_MUSIC, this)

        val audioManager = audioManager
        if (audioManager != null) {
            volumeSeekBar.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            volumeSeekBar.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        }
        volumeSeekBar.setOnSeekArcChangeListener(this)
    }

    override fun onPause() {
        super.onPause()
        progressViewUpdateHelper.stop()
    }

    override fun playerToolbar(): Toolbar? {
        return playerToolbar
    }

    override fun onShow() {
    }

    override fun onHide() {
    }

    override fun onBackPressed(): Boolean = false

    override fun toolbarIconColor(): Int =
        ATHUtil.resolveColor(requireContext(), android.R.attr.colorControlNormal)

    override val paletteColor: Int
        get() = Color.BLACK

    override fun onColorChanged(color: Int) {
    }

    override fun onFavoriteToggled() {
    }

    override fun onPlayStateChanged() {
        updatePlayPauseDrawableState()
    }

    override fun onPlayingMetaChanged() {
        super.onPlayingMetaChanged()
        updateSong()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        updateSong()
        updatePlayPauseDrawableState()
    }

    private fun updateSong() {
        val song = MusicPlayerRemote.currentSong
        title.text = song.title
        text.text = song.artistName

        if (PreferenceUtil.getInstance(requireContext()).isSongInfo) {
            songInfo.text = getSongInfo(song)
            songInfo.show()
        } else {
            songInfo.hide()
        }
    }

    private fun updatePlayPauseDrawableState() {
        when {
            MusicPlayerRemote.isPlaying -> playPauseButton.setImageResource(R.drawable.ic_pause_white_24dp)
            else -> playPauseButton.setImageResource(R.drawable.ic_play_arrow_white_24dp)
        }
    }

    override fun onAudioVolumeChanged(currentVolume: Float, maxVolume: Float) {
        if (volumeSeekBar == null) {
            return
        }

        volumeSeekBar.max = maxVolume.toInt()
        volumeSeekBar.progress = currentVolume.toInt()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (audioVolumeObserver != null) {
            audioVolumeObserver!!.unregister()
        }
    }

    override fun onProgressChanged(seekArc: SeekArc?, progress: Int, fromUser: Boolean) {
        val audioManager = audioManager
        audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, progress, 0)
    }

    override fun onStartTrackingTouch(seekArc: SeekArc?) {
    }

    override fun onStopTrackingTouch(seekArc: SeekArc?) {
    }

    override fun onUpdateProgressViews(progress: Int, total: Int) {
        progressSlider.valueTo = total.toFloat()
        progressSlider.value = progress.toFloat()
        songTotalTime.text = MusicUtil.getReadableDurationString(total.toLong())
        songCurrentProgress.text = MusicUtil.getReadableDurationString(progress.toLong())
    }

    fun setUpProgressSlider() {
        progressSlider.addOnChangeListener { _, value, fromUser ->
            if (fromUser) {
                MusicPlayerRemote.seekTo(value.toInt())
                onUpdateProgressViews(
                    MusicPlayerRemote.songProgressMillis,
                    MusicPlayerRemote.songDurationMillis
                )
            }
        }
    }
}