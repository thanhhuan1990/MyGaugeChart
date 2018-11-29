package huynh.huan.gaugechart

import android.content.Context
import android.graphics.*
import android.util.AttributeSet

/**
 * Created by Huan.Huynh on 10/20/18.
 *
 * Subclass to draw GaugeChart with Pointer, Indicator, Mark, Center
 */
class GaugeView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0)
    : Gauge(context, attrs, defStyleAttr) {

    override fun getDegreeBetweenMark(): Float = degreeBetweenMark

    override fun indicatorWidth(): Float = indicatorWidth

    //==================================================================================================================
    // region Variables

    private var degreeBetweenMark = 6f

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

    private val pointerPaint        = Paint(Paint.ANTI_ALIAS_FLAG)
    private val markPaint           = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorPaint      = Paint(Paint.ANTI_ALIAS_FLAG)
    private val indicatorLightPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val circlePaint         = Paint(Paint.ANTI_ALIAS_FLAG)

    // endregion Variables
    //==================================================================================================================

    //==================================================================================================================
    // region Init

    init {
        initAttributeSet(context, attrs)
        init()
    }

    private fun init() {

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

        circlePaint.color       = a.getColor(R.styleable.GaugeView_gv_centerCircleColor, centerCircleColor)

        isWithMark              = a.getBoolean(R.styleable.GaugeView_gv_withMark, isWithMark)
        markWidth               = a.getFloat(R.styleable.GaugeView_gv_markWidth, markWidth)
        markPaint.color         = a.getColor(R.styleable.GaugeView_gv_markColor, -0x1)

        isWithPointer           = a.getBoolean(R.styleable.GaugeView_gv_withPointer, isWithPointer)
        pointerPaint.strokeWidth = 4f
        pointerPaint.color      = -0xd12c4d

        indicatorColor          = a.getColor(R.styleable.GaugeView_gv_indicatorColor, indicatorColor)
        indicatorWidth          = a.getDimension(R.styleable.GaugeView_gv_indicatorWidth, indicatorWidth)
        indicatorPaint.color    = a.getColor(R.styleable.GaugeView_gv_indicatorColor, centerCircleColor)

        a.recycle()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw Pointer
        drawPointer(canvas)

        // Draw Marks
        drawMarks(canvas)

        // Draw Indicator
        drawIndicator(canvas)

        // Draw center color
        canvas.drawCircle(width * .5f, centerY(), indicatorWidth, circlePaint)
    }

    // endregion Init
    //==================================================================================================================

    //==================================================================================================================
    // region Public Methods

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
        pointerPaint.color = color
        invalidate()
    }
    // endregion Public Methods
    //==================================================================================================================

    //==================================================================================================================
    // region Private Methods

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

    // endregion Private Methods
    //==================================================================================================================

}
