package com.opplayer.app.player

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.os.SystemClock
import android.provider.Settings
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.roundToInt

enum class PlayerGestureKind { VOLUME, BRIGHTNESS }

private const val SWIPE_TRAVEL_RATIO = 0.7f
private const val MIN_BRIGHTNESS = 0.01f
private const val SYSTEM_BRIGHTNESS_DEFAULT = -1f
private const val DIRECTION_SLOP_PX = 24f
private const val VERTICAL_BIAS = 1.3f
private const val SEEK_CHAIN_WINDOW_MS = 900L

@SuppressLint("ClickableViewAccessibility")
class PlayerGestures(
    private val context: Context,
    private val activity: Activity?,
    private val isEnabled: () -> Boolean,
    private val onTap: () -> Unit,
    private val onSeek: (forward: Boolean) -> Unit,
    private val onIndicator: (PlayerGestureKind, Float) -> Unit,
    private val onSeekDragStart: () -> Unit = {},
    private val onSeekDrag: (fraction: Float) -> Unit = {},
    private val onSeekDragEnd: (commit: Boolean) -> Unit = {},

    private val elapsedRealtimeMs: () -> Long = { SystemClock.elapsedRealtime() }
) : View.OnTouchListener {

    private val audioManager = context.getSystemService(AudioManager::class.java)

    private val maxVolume =
        (audioManager?.getStreamMaxVolume(AudioManager.STREAM_MUSIC) ?: 15).coerceAtLeast(1)

    private var viewWidth = 0f
    private var viewHeight = 0f
    private var directionDecided = false
    private var isVerticalDrag = false
    private var isSeekDrag = false
    private var activeKind = PlayerGestureKind.VOLUME
    private var startValue = 0f
    private var lastSeekAt = 0L

    private var brightnessTouched = false

    private val detector = GestureDetector(
        context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                if (isEnabled() && withinSeekChain()) {
                    triggerSeek(e.x)
                    return true
                }
                return false
            }

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (withinSeekChain()) return true
                onTap()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                if (!isEnabled()) {
                    onTap()
                    return true
                }
                triggerSeek(e.x)
                return true
            }

            override fun onScroll(
                e1: MotionEvent?,
                e2: MotionEvent,
                distanceX: Float,
                distanceY: Float
            ): Boolean {
                if (!isEnabled() || e1 == null) return false

                val dx = e2.x - e1.x
                val dy = e2.y - e1.y

                if (!directionDecided) {
                    if (abs(dx) < DIRECTION_SLOP_PX && abs(dy) < DIRECTION_SLOP_PX) return false

                    directionDecided = true
                    isVerticalDrag = abs(dy) > abs(dx) * VERTICAL_BIAS

                    if (isVerticalDrag) {
                        activeKind = if (e1.x > viewWidth / 2f) {
                            PlayerGestureKind.VOLUME
                        } else {
                            PlayerGestureKind.BRIGHTNESS
                        }

                        startValue = when (activeKind) {
                            PlayerGestureKind.VOLUME -> volumeFraction()
                            PlayerGestureKind.BRIGHTNESS -> brightnessFraction()
                        }
                    } else {
                        isSeekDrag = true
                        onSeekDragStart()
                    }
                }

                if (isSeekDrag) {
                    onSeekDrag(dx / viewWidth.coerceAtLeast(1f))
                    return true
                }

                if (!isVerticalDrag) return false

                val travel = (viewHeight * SWIPE_TRAVEL_RATIO).coerceAtLeast(1f)
                val value = (startValue - dy / travel).coerceIn(0f, 1f)

                when (activeKind) {
                    PlayerGestureKind.VOLUME -> applyVolume(value)
                    PlayerGestureKind.BRIGHTNESS -> applyBrightness(value)
                }

                onIndicator(activeKind, value)
                return true
            }
        }
    )

    override fun onTouch(view: View, event: MotionEvent): Boolean {
        viewWidth = view.width.toFloat()
        viewHeight = view.height.toFloat()

        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            directionDecided = false
            isVerticalDrag = false
            isSeekDrag = false
        }

        detector.onTouchEvent(event)

        if (event.actionMasked == MotionEvent.ACTION_UP ||
            event.actionMasked == MotionEvent.ACTION_CANCEL
        ) {
            if (isSeekDrag) {
                onSeekDragEnd(event.actionMasked == MotionEvent.ACTION_UP)
            }

            directionDecided = false
            isVerticalDrag = false
            isSeekDrag = false
        }

        return true
    }

    fun release() {
        if (!brightnessTouched) return

        val window = activity?.window ?: return
        val attributes = window.attributes
        attributes.screenBrightness = SYSTEM_BRIGHTNESS_DEFAULT
        window.attributes = attributes
        brightnessTouched = false
    }

    private fun withinSeekChain(): Boolean =
        elapsedRealtimeMs() - lastSeekAt < SEEK_CHAIN_WINDOW_MS

    private fun triggerSeek(x: Float) {
        lastSeekAt = elapsedRealtimeMs()
        onSeek(x > viewWidth / 2f)
    }

    private fun volumeFraction(): Float {
        val current = audioManager?.getStreamVolume(AudioManager.STREAM_MUSIC) ?: 0
        return (current.toFloat() / maxVolume).coerceIn(0f, 1f)
    }

    private fun applyVolume(fraction: Float) {
        val target = (fraction.coerceIn(0f, 1f) * maxVolume).roundToInt()
        runCatching { audioManager?.setStreamVolume(AudioManager.STREAM_MUSIC, target, 0) }
    }

    private fun brightnessFraction(): Float {
        val current = activity?.window?.attributes?.screenBrightness ?: SYSTEM_BRIGHTNESS_DEFAULT
        if (current >= 0f) return current.coerceIn(MIN_BRIGHTNESS, 1f)

        val system = runCatching {
            Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_BRIGHTNESS
            ) / 255f
        }.getOrDefault(0.5f)

        return system.coerceIn(MIN_BRIGHTNESS, 1f)
    }

    private fun applyBrightness(fraction: Float) {
        val window = activity?.window ?: return
        val attributes = window.attributes
        attributes.screenBrightness = fraction.coerceIn(MIN_BRIGHTNESS, 1f)
        window.attributes = attributes
        brightnessTouched = true
    }
}
