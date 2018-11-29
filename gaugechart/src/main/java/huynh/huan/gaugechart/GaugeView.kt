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
 * Created by Huan.Huynh on 10/20/18.
 */
class GaugeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : View(context, attrs, defStyleAttr) {

    enum class GaugeMode constructor(
        val minDegree: Int,
        val maxDegree: Int
    ) {
        TOP(180, 360),
        BOTTOM(0, 180)
    }

    //==================================================================================================================
    // region Variables

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
    private var degree = startDegree.toFloat()

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
    private var degreeBetweenMark = 6f

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

    private var animator: ValueAnimator? = null
    private val realPercentAnimator: ValueAnimator

    /**
     * to contain all drawing that doesn't change
     */
    private var backgroundBitmap: Bitmap? = null
    private val backgroundBitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private var padding = 0

    private var attachedToWindow = false

    /**
     * @return offset Percent, between [0,1].
     */
    private val offsetPercent: Float
        get() = (currentPercent - getDegreeOfPercent(0f)) / (getDegreeOfPercent(100f) - getDegreeOfPercent(0f))

    private val viewSize: Int
        get() = Math.max(width, height)

    //==================================================================================================================
    // region Mark
    var isWithMark = true
        set(value) {
            field = value
            invalidate()
        }
    private val markPath = Path()
    var markWidth = 1f
        set(value) {
            field = value
            markPaint.strokeWidth = markWidth.px
            invalidate()
        }
    // endregion Mark
    //==================================================================================================================

    //==================================================================================================================
    // region Pointer

    var isWithPointer = true
        set(value) {
            field = value
            invalidate()
        }
    private val pointerPath = Path()

    private val indicatorPath = Path()
    var indicatorWidth: Float = 0.toFloat()
        set(value) {
            field = value
            indicatorPaint.strokeWidth = indicatorWidth
            invalidate()
            requestLayout()
        }
    // endregion Pointer
    //==================================================================================================================

    //==================================================================================================================
    // region Indicator
    private var indicatorColor = -0xde690d
    private var centerCircleColor: Int = indicatorColor
    // endregion Indicator
    //==================================================================================================================

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

    private val firstPaint          = Paint(Paint.ANTI_ALIAS_FLAG)
    private val secondPaint         = Paint(Paint.ANTI_ALIAS_FLAG)
    private val pointerPaint        = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePaint         = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markPaint           = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circleBackPaint     = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorLightPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // endregion Variables
    //==================================================================================================================

    //==================================================================================================================
    // region Init

    init {
        animator = ValueAnimator.ofFloat(0f, 1f)
        realPercentAnimator = ValueAnimator.ofFloat(0f, 1f)

        initAttributeSet(context, attrs)
        init()
    }

    private fun init() {
        firstPaint.style        = Paint.Style.STROKE
        secondPaint.style       = Paint.Style.STROKE

        markPaint.style         = Paint.Style.STROKE
        markPaint.strokeWidth   = markWidth.px

        indicatorLightPaint.style = Paint.Style.STROKE
        indicatorPath.fillType = Path.FillType.EVEN_ODD

        pointerPaint.style = Paint.Style.FILL_AND_STROKE
        pointerPath.fillType = Path.FillType.EVEN_ODD
    }

    private fun initAttributeSet(context: Context, attrs: AttributeSet?) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.GaugeView, 0, 0)

        degreeBetweenMark       = a.getFloat(R.styleable.GaugeView_gv_degreeBetweenMark, this.degreeBetweenMark)

        startDegree             = a.getInt(R.styleable.GaugeView_gv_startDegree, startDegree)
        endDegree               = a.getInt(R.styleable.GaugeView_gv_endDegree, endDegree)

        maxPercent              = a.getInt(R.styleable.GaugeView_gv_maxPercent, maxPercent)
        minPercent              = a.getInt(R.styleable.GaugeView_gv_minPercent, minPercent)

        lastPercent             = minPercent.toFloat()
        currentPercent          = minPercent.toFloat()
        degree                  = minPercent.toFloat().degreeAtPercent

        checkStartAndEndDegree()

        circleBackPaint.color   = a.getColor(R.styleable.GaugeView_gv_backgroundCircleColor, -0x1)
        circlePaint.color       = a.getColor(R.styleable.GaugeView_gv_centerCircleColor, centerCircleColor)

        firstStartColor         = a.getColor(R.styleable.GaugeView_gv_firstStartColor, firstStartColor)
        firstEndColor           = a.getColor(R.styleable.GaugeView_gv_firstEndColor, firstEndColor)
        secondStartColor        = a.getColor(R.styleable.GaugeView_gv_secondStartColor, secondStartColor)
        secondEndColor          = a.getColor(R.styleable.GaugeView_gv_secondEndColor, secondEndColor)

        isWithMark              = a.getBoolean(R.styleable.GaugeView_gv_withMark, isWithMark)
        markWidth               = a.getFloat(R.styleable.GaugeView_gv_markWidth, markWidth)
        markPaint.color         = a.getColor(R.styleable.GaugeView_gv_markColor, -0x1)

        isWithPointer           = a.getBoolean(R.styleable.GaugeView_gv_withPointer, isWithPointer)
        pointerPaint.strokeWidth = 4f
        pointerPaint.color = firstEndColor

        indicatorColor          = a.getColor(R.styleable.GaugeView_gv_indicatorColor, indicatorColor)
        indicatorWidth          = a.getDimension(R.styleable.GaugeView_gv_indicatorWidth, indicatorWidth)
        indicatorPaint.color    = a.getColor(R.styleable.GaugeView_gv_indicatorColor, centerCircleColor)

        gaugeWidth              = a.getDimension(R.styleable.GaugeView_gv_gaugeWidth, gaugeWidth)

        a.recycle()
    }

    override fun onSizeChanged(w: Int, h: Int, oldW: Int, oldH: Int) {
        super.onSizeChanged(w, h, oldW, oldH)
        setPadding(paddingLeft, paddingTop, paddingRight, paddingBottom)

        updateGaugeRect()

        updateBackgroundBitmap()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        initDraw(canvas)

        // Draw View
        drawView(canvas)

        // Draw Pointer
        drawPointer(canvas)

        // Draw Marks
        drawMarks(canvas)

        // Draw Indicator
        drawIndicator(canvas)

        // Draw center color
        canvas.drawCircle(width * .5f, centerY(), indicatorWidth, circlePaint)

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
                newH = padding + newH.div(2) + indicatorWidth.toInt()
            }
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree -> {
                newH = padding + newH.div(2) + indicatorWidth.toInt()
            }
            startDegree % 360 != 90 && endDegree % 360 != 90 -> {
                val radius = size * 0.5 - padding

                newH = when {
                    startDegree % 360 < 90 || endDegree % 360 > 90 -> size

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
        val bundle = lastState as Bundle?
        lastPercent = bundle!!.getFloat("percent")
        lastState = bundle.getParcelable("superState")
        super.onRestoreInstanceState(lastState)
        setPercentAt(lastPercent)
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
     * Update percent [0..100], default duration is 500ms
     * @param value
     * @param moveDuration
     */
    fun percentTo(value: Int, moveDuration: Long = 500) {
        var percent = (when {
            value > maxPercent -> maxPercent
            value < minPercent -> minPercent
            else -> getDegreeOfPercent(value.toFloat()).toInt()
        }).toFloat()
        if (percent != getDegreeOfPercent(0f) && percent != getDegreeOfPercent(100f) && percent != (endDegree - startDegree) * .5f) { // 0, 50, 100
            var i = 0
            while (i <= endDegree) {
                if (i <= percent && percent <= i + degreeBetweenMark) {
                    percent = i + degreeBetweenMark * .5f
                    break
                }
                i += degreeBetweenMark.toInt()
            }
        }
        if (percent == this.lastPercent)
            return
        this.lastPercent = percent

        cancelPercentMove()
        animator = ValueAnimator.ofFloat(currentPercent, percent)
        animator!!.interpolator = DecelerateInterpolator()
        animator!!.duration = moveDuration
        animator!!.addUpdateListener {
            currentPercent = animator!!.animatedValue as Float
            postInvalidate()
        }
        animator!!.start()

        currentPercent = percent
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
        pointerPaint.color = firstEndColor
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
     * Change Mark's color
     * @param color
     */
    fun setMarkColor(color: Int) {
        markPaint.color = color
        invalidate()
    }

    /**
     * Change color of Center point
     * @param color
     */
    fun setCenterCircleColor(color: Int) {
        centerCircleColor = color
        circlePaint.color = color
        invalidate()
    }

    /**
     * Change color of Indicator
     * @param color
     */
    fun setIndicatorColor(color: Int) {
        indicatorPaint.color = color
        invalidate()
    }

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
    // endregion Public Methods
    //==================================================================================================================

    //==================================================================================================================
    // region Private Methods

    private fun initDraw(canvas: Canvas) {
        canvas.translate(0f, 0f)

        if (backgroundBitmap != null)
            canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, backgroundBitmapPaint)

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

    private fun centerY() : Float {
        return when {
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree
                    -> padding.toFloat() + indicatorWidth

            else    -> width * .5f
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
        val sweepGradient = SweepGradient(width * .5f, centerY(), intArrayOf(secondStartColor, secondEndColor, secondStartColor), floatArrayOf(0f, .1f, 1f))
        val matrix = Matrix()
        matrix.postRotate(degree + degreeBetweenMark / 2, width * .5f, py)
        sweepGradient.setLocalMatrix(matrix)

        return sweepGradient
    }

    private fun drawView(canvas: Canvas) {
        firstPaint.shader = firstSweep()
        secondPaint.shader = secondSweep()
        if (degree != startDegree.toFloat() && degree != endDegree.toFloat()) {
            canvas.drawArc(gaugeRect,
                startDegree.toFloat(),
                degree + degreeBetweenMark / 2 - startDegree,
                false,
                firstPaint)
            canvas.drawArc(gaugeRect,
                degree + degreeBetweenMark / 2,
                endDegree - (degree + degreeBetweenMark / 2),
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

    private fun drawPointer(canvas: Canvas) {
        if (isWithPointer && degree != startDegree.toFloat() && degree != endDegree.toFloat()) {

            pointerPath.reset()

            val pointerA = PointF()
            val pointerB = PointF()
            val pointerC = PointF()
            if(startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree) {

                pointerPath.moveTo(width * .5f, height - (padding + gaugeWidth + 30))
                canvas.save()
                canvas.rotate(degree - 90, width * .5f, padding.toFloat() + indicatorWidth)

                pointerA.x = width * .5f
                pointerA.y = height - (padding + gaugeWidth + 30)

                pointerB.x = width * .5f - (width * 0.5f - padding - gaugeWidth * 4) * Math.tan(Math.toRadians((degreeBetweenMark * 0.5f).toDouble())).toFloat()
                pointerB.y = height - (padding + gaugeWidth - markWidth)

                pointerC.x = width * .5f + (width * 0.5f - padding - gaugeWidth * 4) * Math.tan(Math.toRadians((degreeBetweenMark * 0.5f).toDouble())).toFloat()
                pointerC.y = height - (padding + gaugeWidth - markWidth)

            } else {

                pointerPath.moveTo(width * .5f, padding + gaugeWidth + 30)
                canvas.save()
                canvas.rotate(90 + degree, width * .5f, width * .5f)

                pointerA.x = width * .5f
                pointerA.y = padding + gaugeWidth + 30

                pointerB.x = width * .5f - (width * 0.5f - padding - gaugeWidth * 4) * Math.tan(Math.toRadians((degreeBetweenMark * 0.5f).toDouble())).toFloat()
                pointerB.y = padding + gaugeWidth - markWidth

                pointerC.x = width * .5f + (width * 0.5f - padding - gaugeWidth * 4) * Math.tan(Math.toRadians((degreeBetweenMark * 0.5f).toDouble())).toFloat()
                pointerC.y = padding + gaugeWidth - markWidth
            }

            pointerPath.lineTo(pointerA.x, pointerA.y)
            pointerPath.lineTo(pointerB.x, pointerB.y)
            pointerPath.lineTo(pointerC.x, pointerC.y)
            pointerPath.close()

            canvas.drawPath(pointerPath, pointerPaint)
            canvas.restore()
        }
    }

    private fun drawMarks(canvas: Canvas) {
        if (!isWithMark) return

        markPath.reset()

        if(startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree) {

            markPath.moveTo((width - padding.toFloat()), padding.toFloat() + indicatorWidth)
            markPath.lineTo((width - padding - gaugeWidth), padding.toFloat() + indicatorWidth)

            canvas.save()
            canvas.rotate(startDegree.toFloat(), width * .5f, padding.toFloat() + indicatorWidth)
            var i = startDegree.toFloat()
            while (i <= endDegree) {
                canvas.drawPath(markPath, markPaint)
                canvas.rotate(degreeBetweenMark, width * .5f, padding.toFloat() + indicatorWidth)
                i += degreeBetweenMark
            }
        } else {

            markPath.moveTo(width * .5f, padding.toFloat())
            markPath.lineTo(width * .5f, padding + gaugeWidth)

            canvas.save()
            canvas.rotate(90f + startDegree, width * .5f, width * .5f)
            var i = startDegree.toFloat()
            while (i <= endDegree) {
                canvas.drawPath(markPath, markPaint)
                canvas.rotate(degreeBetweenMark, width * .5f, width * .5f)
                i += degreeBetweenMark
            }
        }
        canvas.restore()
    }

    private fun drawIndicator(canvas: Canvas) {
        canvas.save()

        indicatorPath.reset()

        val indicatorA = PointF()
        indicatorA.x = width * .5f
        if(startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree) {
            indicatorPath.moveTo(width * .5f, height - padding.toFloat() - gaugeWidth - indicatorWidth - viewSize * .1f)
            canvas.rotate(degree - 90, width * .5f, centerY())
            indicatorA.y = height - padding.toFloat() - gaugeWidth - indicatorWidth - viewSize * .1f
        } else {
            indicatorPath.moveTo(width * .5f, padding.toFloat() + gaugeWidth + viewSize * .1f)
            canvas.rotate(degree + 90, width * .5f, centerY())
            indicatorA.y = padding.toFloat() + gaugeWidth + viewSize * .1f
        }

        val indicatorB = PointF()
        indicatorB.x = width * .5f - indicatorWidth / 2
        indicatorB.y = centerY()

        val indicatorC = PointF()
        indicatorC.x = width * .5f + indicatorWidth / 2
        indicatorC.y = centerY()

        indicatorPath.lineTo(indicatorA.x, indicatorA.y)
        indicatorPath.lineTo(indicatorB.x, indicatorB.y)
        indicatorPath.lineTo(indicatorC.x, indicatorC.y)
        indicatorPath.close()

        canvas.drawPath(indicatorPath, indicatorPaint)
        canvas.restore()
    }

    /**
     * Update Gauge's rect follow Height of view.
     */
    private fun updateGaugeRect() {
        val risk = gaugeWidth * .5f + padding
        val top = when {
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree
                    -> (padding.toFloat() + width * 0.5f) * -1 + risk+ indicatorWidth
            else    -> risk
        }
        val bottom = when {
            startDegree >=GaugeMode.BOTTOM.minDegree && endDegree <=GaugeMode.BOTTOM.maxDegree
                    -> (padding.toFloat() + indicatorWidth + width * 0.5f) - risk
            else    -> width - risk
        }
        gaugeRect.set(risk, top, width - risk, bottom)
    }

    /**
     * notice that padding or size have changed.
     */
    private fun updatePadding(left: Int, top: Int, right: Int, bottom: Int) {
        padding = Math.max(Math.max(left, right), Math.max(top, bottom))
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
     * @return current Degree at that Percent.
     */
    private val Float.degreeAtPercent: Float
        get() {
            return this + startDegree
        }

    /**
     * @param percent between [0, 100].
     * @return Percent value at current percent.
     */
    private fun getDegreeOfPercent(percent: Float): Float {
        return percent * (endDegree - startDegree).toFloat() * .01f
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private fun cancelPercentMove() {
        animator!!.cancel()
        realPercentAnimator.cancel()
    }

    /**
     * move Percent value to new Percent without animation.
     *
     * @param value current percent to move.
     */
    private fun setPercentAt(value: Float) {
        val percent = when {
            value > getDegreeOfPercent(maxPercent.toFloat()) -> getDegreeOfPercent(maxPercent.toFloat())
            value < getDegreeOfPercent(minPercent.toFloat()) -> getDegreeOfPercent(minPercent.toFloat())
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
        val canvas = Canvas(backgroundBitmap!!)
        canvas.drawCircle(width * .5f, centerY(), width * .5f, circleBackPaint)

        // to fix preview mode issue
        canvas.clipRect(0, 0, width, width)
    }

    /**
     * convert dp to **pixel**.
     *
     * @return Dimension in pixel.
     */
    private val Float.px: Float
        get() {
            return this * context.resources.displayMetrics.density
        }

    // endregion Private Methods
    //==================================================================================================================

}
