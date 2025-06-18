package com.example.mediaplayerdemo

import android.Manifest

import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.media.audiofx.BassBoost
import android.media.audiofx.PresetReverb
import android.media.audiofx.Equalizer
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.graphics.Paint
import android.view.View
import androidx.appcompat.app.AlertDialog

class MainActivity : AppCompatActivity(), TextureView.SurfaceTextureListener {

    private var mediaPlayer: MediaPlayer? = null
    private var videoMediaPlayer: MediaPlayer? = null
    private lateinit var textureView: TextureView
    private lateinit var videoControllerView: FrameLayout
    private lateinit var imageView: ImageView
    private var videoUri: Uri? = null
    private var audioUri: Uri? = null
    private var isSurfaceAvailable = false
    private val paint = Paint()

    // Audio effect objects
    private var bassBoost: BassBoost? = null
    private var reverb: PresetReverb? = null
    private var equalizer: Equalizer? = null

    // Video effect options
    private val videoEffectOptions = arrayOf("Grayscale", "Sepia", "Inverted", "High Contrast", "None")

    // Audio effect options
    private val audioEffectOptions = arrayOf("Bass Boost", "Reverb", "Equalizer (Pop)", "Equalizer (Rock)", "None")

    // Audio and video source options
    private val mediaSourceOptions = arrayOf("Default Sample", "Select from Files")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val playAudioButton: Button = findViewById(R.id.playAudioButton)
        val playAudioWithEffectsButton: Button = findViewById(R.id.playAudioWithEffectsButton)
        val playVideoButton: Button = findViewById(R.id.playVideoButton)
        val playVideoWithEffectsButton: Button = findViewById(R.id.playVideoWithEffectsButton)
        val recordVideoButton: Button = findViewById(R.id.recordVideoButton)
        val captureImageButton: Button = findViewById(R.id.captureImageButton)

        // Setup TextureView for video playback
        textureView = findViewById(R.id.textureView)
        textureView.surfaceTextureListener = this

        // Setup layer type for hardware acceleration with paint effects
        textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)

        videoControllerView = findViewById(R.id.videoControllerView)
        imageView = findViewById(R.id.imageView)

        // Audio buttons
        playAudioButton.setOnClickListener {
            stopVideoIfPlaying()
            showMediaSourceDialog(mediaType = "audio", withEffects = false)
        }

        playAudioWithEffectsButton.setOnClickListener {
            stopVideoIfPlaying()
            showMediaSourceDialog(mediaType = "audio", withEffects = true)
        }

        // Video buttons
        playVideoButton.setOnClickListener {
            stopAudioIfPlaying()
            showMediaSourceDialog(mediaType = "video", withEffects = false)
        }

        playVideoWithEffectsButton.setOnClickListener {
            stopAudioIfPlaying()
            showMediaSourceDialog(mediaType = "video", withEffects = true)
        }

        // Camera and image capture buttons
        recordVideoButton.setOnClickListener {
            if (checkCameraPermission()) {
                val intent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
                videoCaptureLauncher.launch(intent)
            } else {
                requestCameraPermission()
            }
        }

        captureImageButton.setOnClickListener {
            if (checkCameraPermission()) {
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                imageCaptureLauncher.launch(intent)
            } else {
                requestCameraPermission()
            }
        }
    }

    private fun showMediaSourceDialog(mediaType: String, withEffects: Boolean) {
        AlertDialog.Builder(this)
            .setTitle("Select $mediaType Source")
            .setItems(mediaSourceOptions) { _, which ->
                when (mediaSourceOptions[which]) {
                    "Default Sample" -> {
                        if (mediaType == "audio") {
                            handleAudioSelection(withEffects, isDefault = true)
                        } else {
                            handleVideoSelection(withEffects, isDefault = true)
                        }
                    }
                    "Select from Files" -> {
                        val intent = Intent(Intent.ACTION_PICK).apply {
                            type = "$mediaType/*"
                        }
                        if (mediaType == "audio") {
                            audioPickLauncher.launch(intent)
                        } else {
                            videoPickLauncher.launch(intent)
                        }
                    }
                }
            }
            .show()
    }

    private fun handleAudioSelection(withEffects: Boolean, isDefault: Boolean) {
        if (withEffects) {
            showAudioEffectsDialog(isDefault)
        } else {
            playAudio(effect = "None", isDefault = isDefault)
        }
    }

    private fun handleVideoSelection(withEffects: Boolean, isDefault: Boolean) {
        if (withEffects) {
            showVideoEffectsDialog(isDefault)
        } else {
            playVideo(effect = "None", isDefault = isDefault)
        }
    }

    private val audioPickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                audioUri = result.data?.data
                if (audioUri != null) {
                    showAudioEffectsDialog(isDefault = false)
                }
            }
        }

    private val videoPickLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                videoUri = result.data?.data
                if (videoUri != null) {
                    showVideoEffectsDialog(isDefault = false)
                }
            }
        }

    private fun showAudioEffectsDialog(isDefault: Boolean = true) {
        AlertDialog.Builder(this)
            .setTitle("Select Audio Effect")
            .setItems(audioEffectOptions) { _, which ->
                val selectedEffect = audioEffectOptions[which]
                playAudio(effect = selectedEffect, isDefault = isDefault)
            }
            .show()
    }

    private fun showVideoEffectsDialog(isDefault: Boolean = true) {
        AlertDialog.Builder(this)
            .setTitle("Select Video Effect")
            .setItems(videoEffectOptions) { _, which ->
                val selectedEffect = videoEffectOptions[which]
                playVideo(effect = selectedEffect, isDefault = isDefault)
            }
            .show()
    }

    private fun playAudio(effect: String, isDefault: Boolean = true) {
        // Reset and release any existing audio effects
        releaseAudioEffects()

        // Reset and recreate MediaPlayer
        mediaPlayer?.reset()

        // Determine audio source
        val audioSource = if (isDefault) {
            MediaPlayer.create(this, R.raw.sample_audio)
        } else {
            try {
                MediaPlayer().apply {
                    setDataSource(applicationContext, audioUri!!)
                    prepare()
                }
            } catch (e: Exception) {
                Toast.makeText(this, "Error playing selected audio", Toast.LENGTH_SHORT).show()
                return
            }
        }
        mediaPlayer = audioSource

        // Apply selected effect
        when (effect) {
            "Bass Boost" -> {
                bassBoost = BassBoost(0, mediaPlayer!!.audioSessionId).apply {
                    setStrength(1000)
                    enabled = true
                }
                Toast.makeText(this, "Playing Audio with Bass Boost", Toast.LENGTH_SHORT).show()
            }
            "Reverb" -> {
                reverb = PresetReverb(0, mediaPlayer!!.audioSessionId).apply {
                    preset = PresetReverb.PRESET_LARGEHALL
                    enabled = true
                }
                Toast.makeText(this, "Playing Audio with Reverb", Toast.LENGTH_SHORT).show()
            }
            "Equalizer (Pop)" -> {
                equalizer = Equalizer(0, mediaPlayer!!.audioSessionId).apply {
                    usePreset(1) // Pop preset
                    enabled = true
                }
                Toast.makeText(this, "Playing Audio with Pop EQ", Toast.LENGTH_SHORT).show()
            }
            "Equalizer (Rock)" -> {
                equalizer = Equalizer(0, mediaPlayer!!.audioSessionId).apply {
                    usePreset(3) // Rock preset
                    enabled = true
                }
                Toast.makeText(this, "Playing Audio with Rock EQ", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // No effect
                Toast.makeText(this, "Playing Audio (No Effects)", Toast.LENGTH_SHORT).show()
            }
        }

        mediaPlayer?.start()
    }

    private fun playVideo(effect: String = "None", isDefault: Boolean = true) {
        if (!isSurfaceAvailable) {
            Toast.makeText(this, "Surface not ready yet", Toast.LENGTH_SHORT).show()
            return
        }

        stopVideoIfPlaying()

        // Apply selected effect
        when (effect) {
            "Grayscale" -> {
                val grayscaleMatrix = ColorMatrix()
                grayscaleMatrix.setSaturation(0f) // 0 saturation means grayscale
                paint.colorFilter = ColorMatrixColorFilter(grayscaleMatrix)
                textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                Toast.makeText(this, "Playing Video with Grayscale Effect", Toast.LENGTH_SHORT).show()
            }
            "Sepia" -> {
                val sepiaMatrix = ColorMatrix().apply {
                    set(floatArrayOf(
                        0.393f, 0.769f, 0.189f, 0f, 0f,
                        0.349f, 0.686f, 0.168f, 0f, 0f,
                        0.272f, 0.534f, 0.131f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                paint.colorFilter = ColorMatrixColorFilter(sepiaMatrix)
                textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                Toast.makeText(this, "Playing Video with Sepia Effect", Toast.LENGTH_SHORT).show()
            }
            "Inverted" -> {
                val invertMatrix = ColorMatrix().apply {
                    set(floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    ))
                }
                paint.colorFilter = ColorMatrixColorFilter(invertMatrix)
                textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                Toast.makeText(this, "Playing Video with Inverted Colors", Toast.LENGTH_SHORT).show()
            }
            "High Contrast" -> {
                val contrastMatrix = ColorMatrix().apply {
                    setSaturation(2f) // Increase saturation
                    // Increase contrast
                    postConcat(ColorMatrix().apply {
                        set(floatArrayOf(
                            1.5f, 0f, 0f, 0f, -50f,
                            0f, 1.5f, 0f, 0f, -50f,
                            0f, 0f, 1.5f, 0f, -50f,
                            0f, 0f, 0f, 1f, 0f
                        ))
                    })
                }
                paint.colorFilter = ColorMatrixColorFilter(contrastMatrix)
                textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                Toast.makeText(this, "Playing Video with High Contrast", Toast.LENGTH_SHORT).show()
            }
            else -> {
                // No effect
                paint.colorFilter = null
                textureView.setLayerType(View.LAYER_TYPE_HARDWARE, paint)
                Toast.makeText(this, "Playing Video (No Effects)", Toast.LENGTH_SHORT).show()
            }
        }

        // Determine video source
        if (isDefault) {
            val videoPath = "android.resource://" + packageName + "/" + R.raw.sample_video
            videoUri = Uri.parse(videoPath)
        }

        videoMediaPlayer = MediaPlayer().apply {
            setDataSource(applicationContext, videoUri!!)
            val surface = Surface(textureView.surfaceTexture)
            setSurface(surface)

            // Add completion listener to handle when video finishes
            setOnCompletionListener { mp ->
                // Video playback completed
                Toast.makeText(this@MainActivity, "Video Playback Completed", Toast.LENGTH_SHORT).show()
            }

            // Add error listener to handle playback issues
            setOnErrorListener { mp, what, extra ->
                Toast.makeText(applicationContext, "Error playing video: $what", Toast.LENGTH_SHORT).show()
                true // Return true to indicate the error is handled
            }

            setOnPreparedListener { mp ->
                mp.start()

                // Add media controller
                val mediaController = MediaController(this@MainActivity)
                mediaController.setAnchorView(videoControllerView)
                mediaController.setMediaPlayer(object : MediaController.MediaPlayerControl {
                    override fun start() = mp.start()
                    override fun pause() = mp.pause()
                    override fun getDuration() = mp.duration
                    override fun getCurrentPosition() = mp.currentPosition
                    override fun seekTo(pos: Int) = mp.seekTo(pos)
                    override fun isPlaying() = mp.isPlaying
                    override fun getBufferPercentage() = 0
                    override fun canPause() = true
                    override fun canSeekBackward() = true
                    override fun canSeekForward() = true
                    override fun getAudioSessionId() = mp.audioSessionId
                })
                mediaController.show()
            }
            prepareAsync()
        }
    }

    private fun stopVideoIfPlaying() {
        videoMediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.reset()
            it.release()
        }
        videoMediaPlayer = null
    }

    private fun stopAudioIfPlaying() {
        releaseAudioEffects()

        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.reset()
            it.release()
            mediaPlayer = null
        }
    }

    private fun releaseAudioEffects() {
        bassBoost?.release()
        bassBoost = null

        reverb?.release()
        reverb = null

        equalizer?.release()
        equalizer = null
    }

    // Permission and camera-related methods
    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 100)
    }

    // TextureView.SurfaceTextureListener methods
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        isSurfaceAvailable = true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        // Handle size change if needed
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        isSurfaceAvailable = false
        stopVideoIfPlaying()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        // Handle texture updates if needed
    }

    // Capture launchers
    private val videoCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                videoUri = result.data?.data
                if (videoUri != null) {
                    showVideoEffectsDialog(isDefault = false)
                }
            }
        }

    private val imageCaptureLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageBitmap = result.data?.extras?.get("data") as? android.graphics.Bitmap
                if (imageBitmap != null) {
                    imageView.setImageBitmap(imageBitmap)
                }
            }
        }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioIfPlaying()
        stopVideoIfPlaying()
    }
}