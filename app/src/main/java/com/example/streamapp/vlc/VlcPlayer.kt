package com.example.streamapp.vlc

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import android.view.SurfaceView
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import java.io.File

class VlcPlayer(private val context: Context) {
    private lateinit var libVlc: LibVLC
    private lateinit var mediaPlayer: MediaPlayer

    var onError: ((String) -> Unit)? = null
    var onPlaying: (() -> Unit)? = null

    private var _isInitialized = false
    val isInitialized: Boolean get() = _isInitialized

    fun initialize() {
        if (_isInitialized) return

        try {
            val options = mutableListOf(
                "--no-drop-late-frames",
                "--no-skip-frames",
                "--rtsp-tcp",
                "--codec=avcodec",
                "--avcodec-hw=none",
                "--no-video-title-show",
                "--verbose=2",
                "--file-logging",
                "--logfile=/sdcard/vlc-log.txt"
            )
            libVlc = LibVLC(context, options)
            mediaPlayer = MediaPlayer(libVlc)

            mediaPlayer.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.EncounteredError -> {
                        Log.e("VlcPlayer", "Playback error")
                        onError?.invoke("Playback error occurred")
                    }
                    MediaPlayer.Event.EndReached -> {
                        Log.d("VlcPlayer", "End of stream reached")
                        onError?.invoke("Stream ended")
                    }
                    MediaPlayer.Event.Playing -> {
                        Log.d("VlcPlayer", "Playback started successfully")
                        onPlaying?.invoke()
                    }
                    MediaPlayer.Event.Buffering -> {
                        Log.d("VlcPlayer", "Buffering: ${event.buffering}")
                        if (event.buffering < 100) {
                            Log.d("VlcPlayer", "Buffering in progress: ${event.buffering}%")
                        } else {
                            Log.d("VlcPlayer", "Buffering complete, starting playback")
                        }
                    }
                }
            }

            _isInitialized = true
            Log.d("VlcPlayer", "LibVLC initialized successfully")

        } catch (e: Exception) {
            Log.e("VlcPlayer", "VLC init failed: ${e.message}", e)
            onError?.invoke("VLC init error: ${e.message}")
        }
    }

    fun attachSurface(surfaceView: SurfaceView) {
        try {
            if (!_isInitialized) {
                onError?.invoke("Surface attach failed: VLC not initialized")
                return
            }

            val vout = mediaPlayer.vlcVout
            vout.setVideoView(surfaceView)
            vout.attachViews()
            Log.d("VlcPlayer", "Surface attached")
        } catch (e: Exception) {
            Log.e("VlcPlayer", "Surface attach failed: ${e.message}")
            onError?.invoke("Surface attach failed: ${e.message}")
        }
    }

    fun playStream(url: String) {
        try {
            Log.d("VlcPlayer", "Trying to play stream: $url")
            if (url.isBlank()) {
                onError?.invoke("Invalid URL: URL cannot be empty")
                return
            }

            val media = Media(libVlc, Uri.parse(url))
            media.setHWDecoderEnabled(false, false)
            media.addOption(":network-caching=1000")
            media.addOption(":clock-jitter=0")
            media.addOption(":clock-synchro=0")
            media.addOption(":rtsp-tcp")

            mediaPlayer.media = media

            val started = mediaPlayer.play()
            Log.d("VlcPlayer", "Playback started: $started")

            media.release()
        } catch (e: Exception) {
            Log.e("VlcPlayer", "Error playing stream: ${e.message}", e)
            onError?.invoke("Stream error: ${e.message}")
        }
    }

    fun stopPlayback() {
        try {
            if (_isInitialized && mediaPlayer.isPlaying) {
                mediaPlayer.stop()
            }
        } catch (e: Exception) {
            Log.e("VlcPlayer", "Error stopping playback: ${e.message}")
        }
    }

    fun release() {
        try {
            if (_isInitialized) {
                mediaPlayer.stop()
                mediaPlayer.vlcVout.detachViews()
                mediaPlayer.release()
                libVlc.release()
                _isInitialized = false
            }
        } catch (e: Exception) {
            Log.e("VlcPlayer", "Release error: ${e.message}")
        }
    }

    private var isRecording = false
    private var recordingPath: String? = null

    fun startRecording(): Boolean {
        return try {
            if (!isInitialized || mediaPlayer.media == null) {
                onError?.invoke("Recording failed: Stream not playing")
                return false
            }

            val fileName = "vlc_recording_${System.currentTimeMillis()}.mp4"
            val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), fileName)
            recordingPath = file.absolutePath

            mediaPlayer.media!!.addOption(":sout=#file{dst=$recordingPath}")
            mediaPlayer.media!!.addOption(":sout-keep")

            isRecording = true
            Log.d("VlcPlayer", "Recording started to $recordingPath")
            true
        } catch (e: Exception) {
            Log.e("VlcPlayer", "Failed to start recording: ${e.message}")
            onError?.invoke("Failed to start recording")
            false
        }
    }

    fun stopRecording() {
        if (isRecording) {
            stopPlayback()
            mediaPlayer.play()
            isRecording = false
            Log.d("VlcPlayer", "Recording stopped. File saved to $recordingPath")
        }
    }

    fun getRecordingPath(): String? = recordingPath
}
