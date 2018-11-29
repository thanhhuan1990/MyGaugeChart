package huynh.huan.gaugechart

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.animation.DecelerateInterpolator
import kotlin.math.absoluteValue

/**
 * Created by Huan.Huynh on 11/29/18.
 *
 * Main class for draw GaugeChart
 */
abstract class Gauge @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    /**
     * Return degree between 2 marks if using mark.
     */
    abstract fun getDegreeBetweenMark() : Float

    /**
     * Return width of Indicator if any
     */
    abstract fun indicatorWidth() : Float

    //==================================================================================================================
    // region Variables

    private var attachedToWindow = false

    private val circleBackPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val firstPaint          = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondPaint         = Paint(Paint.ANTI_ALIAS_FLAG)

    private var backgroundBitmap: Bitmap? = null
    private val backgroundBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    var padding = 0

    var startDegree = 180
        set(value) {
            try {
                field = value
                checkStartAndEndDegree()
                degree = lastPercent.degreeAtPercent
                if (!attachedToWindow)
                    return
                updateBackgroundBitmap()
                invalidate()
                requestLayout()
            } catch (e: IllegalArgumentException) {
                Log.w("Gauge", e.message)
            }
        }
    var endDegree = 360
        set(value) {
            try {
                field = value
                checkStartAndEndDegree()
                degree = lastPercent.degreeAtPercent
                if (!attachedToWindow)
                    return
                updateBackgroundBitmap()
                invalidate()
                requestLayout()
            } catch (e: IllegalArgumentException) {
                Log.w("Gauge", e.message)
            }
        }

    /**
     * to rotate indicator
     * @return current degreeAtPercent where indicator must be.
     */
    var degree = startDegree.toFloat()

    /**
     * get the max range, default = 100
     *
     * @return max percent.
     * @see .getMinPercent
     */
    private var maxPercent = 100
        set(value) {
            field = value

            if (maxPercent <= minPercent) {
                field = minPercent
                throw IllegalArgumentException("maxPercent must be larger than minPercent !!")
            }
            if (!attachedToWindow)
                return
            updateBackgroundBitmap()
            setPercentAt(lastPercent)
        }

    /**
     * get the min range, default = 0
     */
    private var minPercent = 0
        set(value) {
            field = value
            if (minPercent >= maxPercent) {
                field = maxPercent
                throw IllegalArgumentException("minPercent must be smaller than maxPercent !!")
            }
            if (!attachedToWindow)
                return
            updateBackgroundBitmap()
            setPercentAt(lastPercent)
        }

    /**
     * @return the last percent which you set by [.percentTo],
     * @see .getCurrentPercent
     */
    private var lastPercent = 0f
    /**
     * what is percent now in **int**
     */
    private var currentIntPercent = 0
    /**
     * what is percent now in **float**
     *
     * @return current percent now.
     */
    private var currentPercent = 0f

    /**
     * @return offset Percent, between [0,1].
     */
    private val offsetPercent: Float
        get() = (currentPercent - 0f.getDegreeOfPercent) / (100f.getDegreeOfPercent - 0f.getDegreeOfPercent)


    private var animator: ValueAnimator? = null
    private val realPercentAnimator: ValueAnimator

    //==================================================================================================================
    // region Gauge
    private var firstStartColor = -0xd7909f
    private var firstEndColor = -0xd12c4d
    private var secondStartColor = -0x1e76ab
    private var secondEndColor = -0x78b2d5
    private val gaugeRect = RectF()
    var gaugeWidth: Float = 30f
        set(value) {
            field = value
            updateGaugeRect()
            invalidate()
        }
    // endregion Gauge
    //==================================================================================================================

    // endregion Variables
    //==================================================================================================================

    //==================================================================================================================
    // endregion Init

    init {
        initAttributeSet(context, attrs)

        animator = ValueAnimator.ofFloat(0f, 1f)
        realPercentAnimator = ValueAnimator.ofFloat(0f, 1f)

        firstPaint.style        = Paint.Style.STROKE
        secondPaint.style       = Paint.Style.STROKE
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        initDraw(canvas)

        // Draw View
        drawView(canvas)
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

        updateGaugeRect()

        updateBackgroundBitmap()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        val defaultSize = 250f.px.toInt()

        val widthMode = View.MeasureSpec.getMode(widthMeasureSpec)
        val heightMode = View.MeasureSpec.getMode(heightMeasureSpec)

        val size: Int

        size = if (widthMode == View.MeasureSpec.EXACTLY)
            measuredWidth
        else if (heightMode == View.MeasureSpec.EXACTLY)
            measuredHeight
        else if (widthMode == View.MeasureSpec.UNSPECIFIED && heightMode == View.MeasureSpec.UNSPECIFIED)
            defaultSize
        else if (widthMode == View.MeasureSpec.AT_MOST && heightMode == View.MeasureSpec.AT_MOST)
            Math.min(defaultSize, Math.min(measuredWidth, measuredHeight))
        else {
            if (widthMode == View.MeasureSpec.AT_MOST)
                Math.min(defaultSize, measuredWidth)
            else
                Math.min(defaultSize, measuredHeight)
        }

        var newH = size

        when {
            startDegree >=GaugeMode.TOP.minDegree && endDegree <=GaugeMode.TOP.maxDegree -> {
                newH = padding + newH.div(2) + indicatorWidth().toInt()
            }
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree -> {
                newH = padding + newH.div(2) + indicatorWidth().toInt()
            }
            startDegree % 360 != 90 && endDegree % 360 != 90 -> {
                val radius = size * 0.5 - padding

                newH = when {
                    startDegree % 360 <= 90 && (endDegree % 360 >= 90 || endDegree % 360 == 0) -> size

                    (startDegree % 360 - 90).absoluteValue < (endDegree % 360 - 90).absoluteValue -> {
                        size.div(2) + (radius * Math.cos(Math.toRadians(startDegree % 360 - 90.0)).absoluteValue).toInt() + padding * 2
                    }

                    else -> size.div(2) + (radius * Math.cos(Math.toRadians(endDegree % 360 - 90.0)).absoluteValue).toInt() + padding * 2
                }

                if(newH > size) newH = size
            }
        }
        setMeasuredDimension(size, newH)
    }

    private fun initDraw(canvas: Canvas) {
        canvas.translate(0f, 0f)

        backgroundBitmap?.apply { canvas.drawBitmap(this, 0f, 0f, backgroundBitmapPaint) }

        // check onPercentChangeEvent.
        val newPercent = currentPercent.toInt()
        if (newPercent != currentIntPercent) {
            val isPercentUp = newPercent > currentIntPercent
            val update = if (isPercentUp) 1 else -1
            // this loop to pass on all percent values,
            // to safe handle by call gauge.getCorrectIntPercent().
            while (currentIntPercent != newPercent) {
                currentIntPercent += update
            }
        }
        currentIntPercent = newPercent

        degree = currentPercent.degreeAtPercent

        firstPaint.strokeWidth = gaugeWidth
        secondPaint.strokeWidth = gaugeWidth
    }

    private fun initAttributeSet(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.Gauge, 0, 0)

        startDegree             = a.getInt(R.styleable.Gauge_gv_startDegree, startDegree)
        endDegree               = a.getInt(R.styleable.Gauge_gv_endDegree, endDegree)

        checkStartAndEndDegree()

        maxPercent              = a.getInt(R.styleable.Gauge_gv_maxPercent, maxPercent)
        minPercent              = a.getInt(R.styleable.Gauge_gv_minPercent, minPercent)

        lastPercent             = minPercent.toFloat()
        currentPercent          = minPercent.toFloat()
        degree                  = minPercent.toFloat().degreeAtPercent

        checkStartAndEndDegree()

        circleBackPaint.color   = a.getColor(R.styleable.Gauge_gv_backgroundCircleColor, -0x1)
        firstStartColor         = a.getColor(R.styleable.Gauge_gv_firstStartColor, firstStartColor)
        firstEndColor           = a.getColor(R.styleable.Gauge_gv_firstEndColor, firstEndColor)
        secondStartColor        = a.getColor(R.styleable.Gauge_gv_secondStartColor, secondStartColor)
        secondEndColor          = a.getColor(R.styleable.Gauge_gv_secondEndColor, secondEndColor)

        gaugeWidth              = a.getDimension(R.styleable.Gauge_gv_gaugeWidth, gaugeWidth)

        a.recycle()
    }


    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        attachedToWindow = true
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        attachedToWindow = false
    }

    override fun onSaveInstanceState(): Parcelable? {
        super.onSaveInstanceState()
        val bundle = Bundle()
        bundle.putParcelable("superState", super.onSaveInstanceState())
        bundle.putFloat("percent", lastPercent)
        return bundle
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var lastState = state
        val bundle = lastState as? Bundle?
        bundle?.apply {
            lastPercent = bundle.getFloat("percent")
            lastState = bundle.getParcelable("superState")
            super.onRestoreInstanceState(lastState)
            setPercentAt(lastPercent)
        }
    }

    override fun setPadding(left: Int, top: Int, right: Int, bottom: Int) {
        updatePadding(left, top, right, bottom)
        super.setPadding(padding, padding, padding, padding)
    }

    override fun setPaddingRelative(start: Int, top: Int, end: Int, bottom: Int) {
        updatePadding(start, top, end, bottom)
        super.setPaddingRelative(padding, padding, padding, padding)
    }

    // endregion Init
    //==================================================================================================================

    //==================================================================================================================
    // region Public Methods
    /**
     *Background Color,
     * Set it Color.TRANSPARENT to remove circle background.
     * @param backgroundColor new Background Color.
     */
    fun setBackgroundCircleColor(backgroundColor: Int) {
        circleBackPaint.color = backgroundColor
        if (!isAttachedToWindow) return
        updateBackgroundBitmap()
        invalidate()
    }

    /**
     * Change FirstSwipe's start color
     * @param color
     */
    fun setFirstStartColor(color: Int) {
        this.firstStartColor    = color
        invalidate()
    }

    /**
     * Change FirstSwipe's end color
     * @param color
     */
    fun setFirstEndColor(color: Int) {
        this.firstEndColor    = color
        invalidate()
    }

    /**
     * Change SecondSwipe's start color
     * @param color
     */
    fun setSecondStartColor(color: Int) {
        this.secondStartColor    = color
        invalidate()
    }

    /**
     * Change SecondSwipe's end color
     * @param color
     */
    fun setSecondEndColor(color: Int) {
        this.secondEndColor    = color
        invalidate()
    }

    /**
     * Update percent [0..100], default duration is 500ms
     * @param value
     * @param moveDuration
     */
    fun percentTo(value: Int, moveDuration: Long = 500) {
        var percent = (when {
            value > maxPercent -> maxPercent
            value < minPercent -> minPercent
            else -> value.toFloat().getDegreeOfPercent.toInt()
        }).toFloat()
        if (percent != 0f.getDegreeOfPercent && percent != 100f.getDegreeOfPercent && percent != (endDegree - startDegree) * .5f) { // 0, 50, 100
            var i = 0
            while (i <= endDegree) {
                if (i <= percent && percent <= i + getDegreeBetweenMark()) {
                    percent = i + getDegreeBetweenMark() * .5f
                    break
                }
                i += getDegreeBetweenMark().toInt()
            }
        }
        if (percent == this.lastPercent)
            return
        this.lastPercent = percent

        cancelPercentMove()
        animator = ValueAnimator.ofFloat(currentPercent, percent)
        animator?.apply {
            interpolator = DecelerateInterpolator()
            duration = moveDuration
            addUpdateListener {
                currentPercent = (animatedValue as? Float) ?: 0f
                postInvalidate()
            }
            start()
        }

        currentPercent = percent
    }

    // endregion Public Methods
    //==================================================================================================================

    //==================================================================================================================
    // region Private Methods

    private fun drawView(canvas: Canvas) {
        firstPaint.shader = firstSweep()
        secondPaint.shader = secondSweep()
        if (degree != startDegree.toFloat() && degree != endDegree.toFloat()) {
            canvas.drawArc(gaugeRect,
                startDegree.toFloat(),
                degree + getDegreeBetweenMark() / 2 - startDegree,
                false,
                firstPaint)
            canvas.drawArc(gaugeRect,
                degree + getDegreeBetweenMark() / 2,
                endDegree - (degree + getDegreeBetweenMark() / 2),
                false,
                secondPaint)
        } else {
            canvas.drawArc(gaugeRect,
                startDegree.toFloat(),
                degree - startDegree,
                false,
                firstPaint)
            canvas.drawArc(gaugeRect,
                degree,
                endDegree - degree,
                false,
                secondPaint)
        }
    }

    private fun firstSweep(): SweepGradient {
        val sweepGradient: SweepGradient
        val py = when {
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree -> height.toFloat()
            startDegree >=GaugeMode.TOP.minDegree && endDegree <=GaugeMode.TOP.maxDegree -> height.toFloat()
            else -> width * .5f
        }
        val position = offsetPercent * (degree - startDegree) / 360f
        if (position != 0f) {
            sweepGradient = SweepGradient(width * .5f, centerY(), intArrayOf(firstStartColor, firstEndColor, firstEndColor), floatArrayOf(0f, position * .9f, 1f))
            val matrix = Matrix()
            matrix.postRotate(startDegree.toFloat(), width * .5f, py)
            sweepGradient.setLocalMatrix(matrix)
        } else {
            sweepGradient = SweepGradient(width * .5f, centerY(), intArrayOf(firstStartColor, firstEndColor), floatArrayOf(0f, 1f))
        }

        return sweepGradient
    }

    private fun secondSweep(): SweepGradient {
        val py = when {
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree -> height.toFloat()
            startDegree >=GaugeMode.TOP.minDegree && endDegree <=GaugeMode.TOP.maxDegree -> height.toFloat()
            else -> width * .5f
        }
        val sweepGradient: SweepGradient
        val position = offsetPercent * (endDegree - degree) / 360f
        if (position != 1f) {
            sweepGradient = SweepGradient(width * .5f, centerY(), intArrayOf(secondStartColor, secondEndColor, secondEndColor), floatArrayOf(0f, position * .9f, 1f))
            val matrix = Matrix()
            matrix.postRotate(startDegree.toFloat(), width * .5f, py)
            sweepGradient.setLocalMatrix(matrix)
        } else {
            sweepGradient = SweepGradient(width * .5f, centerY(), intArrayOf(secondStartColor, secondEndColor), floatArrayOf(0f, 1f))
        }
        val matrix = Matrix()
        matrix.postRotate(degree + getDegreeBetweenMark() / 2, width * .5f, py)
        sweepGradient.setLocalMatrix(matrix)

        return sweepGradient
    }

    private fun updatePadding(left: Int, top: Int, right: Int, bottom: Int) {
        padding = Math.max(Math.max(left, right), Math.max(top, bottom))
    }

    /**
     * move Percent value to new Percent without animation.
     *
     * @param value current percent to move.
     */
    private fun setPercentAt(value: Float) {
        val percent = when {
            value > maxPercent.toFloat().getDegreeOfPercent -> maxPercent.toFloat().getDegreeOfPercent
            value < minPercent.toFloat().getDegreeOfPercent -> minPercent.toFloat().getDegreeOfPercent
            else -> value
        }

        this.lastPercent = percent
        this.currentPercent = percent
        invalidate()
    }

    /**
     * create canvas to draw [.backgroundBitmap].
     * @return [.backgroundBitmap]'s canvas.
     */
    private fun updateBackgroundBitmap() {
        if (width == 0 || height == 0)
            return
        backgroundBitmap = Bitmap.createBitmap(width, width, Bitmap.Config.ARGB_8888)
        backgroundBitmap?.apply {
            val canvas = Canvas(this)
            canvas.drawCircle(width * .5f, centerY(), width * .5f, circleBackPaint)
            // to fix preview mode issue
            canvas.clipRect(0, 0, width, width)
        }
    }

    /**
     * Verify inputted Start, End degrees
     */
    private fun checkStartAndEndDegree() {
        if (startDegree < 0)
            throw IllegalArgumentException("StartDegree can\'t be Negative")
        if (endDegree < 0)
            throw IllegalArgumentException("EndDegree can\'t be Negative")
        if (startDegree >= endDegree)
            throw IllegalArgumentException("EndDegree must be bigger than StartDegree !")
        if (endDegree - startDegree > 360)
            throw IllegalArgumentException("(EndDegree - StartDegree) must be smaller than 360 !")
    }

    /**
     * Get center height of view depends on View's type: Top / Bottom / Normal.
     */
    fun centerY() : Float {
        return when {
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree
            -> padding.toFloat() + indicatorWidth()

            else    -> width * .5f
        }
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun cancelPercentMove() {
        animator?.cancel()
        realPercentAnimator.cancel()
    }

    /**
     * Update Gauge's rect follow Height of view.
     */
    private fun updateGaugeRect() {
        val risk = gaugeWidth * .5f + padding
        val top = when {
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree
            -> (padding.toFloat() + width * 0.5f) * -1 + risk + indicatorWidth()
            else    -> risk
        }
        val bottom = when {
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree
            -> (padding.toFloat() + indicatorWidth() + width * 0.5f) - risk
            else    -> width - risk
        }
        gaugeRect.set(risk, top, width - risk, bottom)
    }
    /**
     * @return current Degree at that Percent.
     */
    private val Float.degreeAtPercent: Float
        get() {
            return this + startDegree
        }

    /**
     * @return Percent value at current percent.
     */
    private val Float.getDegreeOfPercent: Float
        get() {
            return this * (endDegree - startDegree).toFloat() * .01f
        }

    /**
     * convert dp to **pixel**.
     *
     * @return Dimension in pixel.
     */
    val Float.px: Float
        get() {
            return this * context.resources.displayMetrics.density
        }
    // endregion Private Methods
    //==================================================================================================================
}