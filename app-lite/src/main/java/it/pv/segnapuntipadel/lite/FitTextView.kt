package it.pv.segnapuntipadel.lite

import android.content.Context
import android.text.TextPaint
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ViewGroup
import android.widget.TextView

/**
 * TextView che riduce la dimensione del testo (partendo da android:textSize come massimo)
 * fino a farlo stare nello spazio disponibile. Le righe sono separate solo da '\n'.
 * Con altezza wrap_content viene vincolata solo la larghezza.
 */
class FitTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextView(context, attrs, defStyleAttr) {
    private var probe: TextPaint? = null
    private val maxTextSizePx: Float = textSize
    private val minTextSizePx: Float = minOf(
        maxTextSizePx,
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, MIN_TEXT_SP, resources.displayMetrics)
    )
    private val refitRunnable = Runnable { refit() }

    init {
        probe = TextPaint(paint)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w != oldw || h != oldh) {
            removeCallbacks(refitRunnable)
            post(refitRunnable)
        }
    }

    override fun onTextChanged(text: CharSequence?, start: Int, lengthBefore: Int, lengthAfter: Int) {
        super.onTextChanged(text, start, lengthBefore, lengthAfter)
        refit()
    }

    override fun onDetachedFromWindow() {
        removeCallbacks(refitRunnable)
        super.onDetachedFromWindow()
    }

    private fun refit() {
        val measurePaint = probe ?: return
        val availableWidth = width - compoundPaddingLeft - compoundPaddingRight
        val availableHeight = height - compoundPaddingTop - compoundPaddingBottom
        if (availableWidth <= 0) return
        val constrainHeight = layoutParams?.height != ViewGroup.LayoutParams.WRAP_CONTENT
        if (constrainHeight && availableHeight <= 0) return

        measurePaint.set(paint)
        val lines = text.toString().split('\n')
        var low = minTextSizePx
        var high = maxTextSizePx
        if (fits(measurePaint, lines, high, availableWidth, availableHeight, constrainHeight)) {
            low = high
        } else {
            for (step in 0 until SEARCH_STEPS) {
                val mid = (low + high) / 2f
                if (fits(measurePaint, lines, mid, availableWidth, availableHeight, constrainHeight)) {
                    low = mid
                } else {
                    high = mid
                }
            }
        }
        if (Math.abs(textSize - low) > 0.5f) {
            setTextSize(TypedValue.COMPLEX_UNIT_PX, low)
        }
    }

    private fun fits(
        measurePaint: TextPaint,
        lines: List<String>,
        sizePx: Float,
        availableWidth: Int,
        availableHeight: Int,
        constrainHeight: Boolean
    ): Boolean {
        measurePaint.textSize = sizePx
        val maxWidth = availableWidth * WIDTH_SAFETY
        for (line in lines) {
            if (measurePaint.measureText(line) > maxWidth) return false
        }
        if (!constrainHeight) return true
        val lineHeight = measurePaint.getFontMetrics(null) * lineSpacingMultiplierCompat()
        return lineHeight * lines.size <= availableHeight
    }

    // getLineSpacingMultiplier() è disponibile solo da API 16: va bene per minSdk 16.
    private fun lineSpacingMultiplierCompat(): Float = maxOf(1f, lineSpacingMultiplier)

    private companion object {
        const val MIN_TEXT_SP = 10f
        const val SEARCH_STEPS = 12
        const val WIDTH_SAFETY = 0.96f
    }
}