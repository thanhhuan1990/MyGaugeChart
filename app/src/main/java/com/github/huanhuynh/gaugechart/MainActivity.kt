package com.github.huanhuynh.gaugechart

import android.annotation.SuppressLint
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import com.crashlytics.android.Crashlytics
import com.flask.colorpicker.ColorPickerView
import com.flask.colorpicker.builder.ColorPickerDialogBuilder
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.layout_activity_main.*

/**
 * Demo app module's MainActivity
 */
class MainActivity : AppCompatActivity(), SeekBar.OnSeekBarChangeListener {

    @SuppressLint("CheckResult")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Fabric.with(this, Crashlytics())
        setContentView(R.layout.layout_activity_main)

        start_degree?.setOnSeekBarChangeListener(this)
        end_degree?.setOnSeekBarChangeListener(this)
        percent?.setOnSeekBarChangeListener(this)
        sb_mark_width?.setOnSeekBarChangeListener(this)
        indicator_width?.setOnSeekBarChangeListener(this)
        gauge_width?.setOnSeekBarChangeListener(this)
        gauge_width?.progress = indicator_main?.gaugeWidth?.toInt() ?: 0

        btnFirstStart?.setOnClickListener { selectColor(it) }
        btnFirstEnd?.setOnClickListener { selectColor(it) }
        btnSecondStart?.setOnClickListener { selectColor(it) }
        btnSecondEnd?.setOnClickListener { selectColor(it) }
        btnIndicatorColor?.setOnClickListener { selectColor(it) }
        btnMarkColor?.setOnClickListener { selectColor(it) }
        btnBackgroundColor?.setOnClickListener { selectColor(it) }

        cb_indicator?.setOnCheckedChangeListener { _, isChecked -> indicator_main?.isWithPointer = isChecked }
        cb_mark?.setOnCheckedChangeListener { _, isChecked -> indicator_main?.isWithMark = isChecked }
    }

    override fun onStartTrackingTouch(p0: SeekBar?) {}

    override fun onStopTrackingTouch(p0: SeekBar?) {}

    override fun onProgressChanged(seekBar: SeekBar?, p1: Int, p2: Boolean) {

        when(seekBar?.id){
            percent.id          -> setPercent(seekBar.progress)
            sb_mark_width.id    -> setMarkWidth(seekBar.progress)
            indicator_width.id  -> setIndicatorWidth(seekBar.progress)
            start_degree.id     -> setStartDegree(seekBar.progress)
            end_degree.id       -> setEndDegree(seekBar.progress)
            gauge_width.id      -> setGaugeWidth(seekBar.progress)
        }

    }

    private fun selectColor(view: View) {

        ColorPickerDialogBuilder
            .with(this)
            .setTitle("Choose color")
            .initialColor((view.background as? ColorDrawable)?.color ?: -0x1)
            .wheelType(ColorPickerView.WHEEL_TYPE.FLOWER)
            .density(12)
            .setPositiveButton("OK") { _, color, _ ->
                view.setBackgroundColor(color)
                setColor(view, color)
            }
            .setNegativeButton("Cancel") { _, _ ->  }
            .build()
            .show()
    }

    private fun setColor(view: View, color: Int) {
        when (view.id) {
            btnFirstStart.id            -> indicator_main?.setFirstStartColor(color)
            btnFirstEnd.id              -> indicator_main?.setFirstEndColor(color)
            btnSecondStart.id           -> indicator_main?.setSecondStartColor(color)
            btnSecondEnd.id             -> indicator_main?.setSecondEndColor(color)
            btnIndicatorColor.id        -> {
                indicator_main?.setCenterCircleColor(color)
                indicator_main?.setIndicatorColor(color)
            }
            btnMarkColor.id             -> indicator_main?.setMarkColor(color)
            btnBackgroundColor.id       -> indicator_main?.setBackgroundCircleColor(color)
        }
    }

    private fun setPercent(progress: Int) {
        indicator_main?.percentTo(progress)
        tv_progress?.text = progress.toString()
    }

    private fun setMarkWidth(value: Int) {
        indicator_main?.markWidth = value / 2f
        tv_mark_width?.text = (value / 2f).toString()
    }

    private fun setIndicatorWidth(value: Int) {
        indicator_main?.indicatorWidth = value.toFloat()
        tv_indicator_width?.text = value.toString()
    }

    private fun setStartDegree(value: Int) {
        if(value <= end_degree.progress - 90) {
            indicator_main?.startDegree = value
            indicator_main?.percentTo(percent.progress)
            tv_start_degree?.text = value.toString()
        } else {
            start_degree?.progress = end_degree.progress - 90
        }
    }

    private fun setEndDegree(value: Int) {
        if(value >= start_degree.progress + 90) {
            indicator_main?.endDegree = value
            indicator_main?.percentTo(percent.progress)
            tv_end_degree?.text = value.toString()
        } else {
            end_degree.progress = start_degree.progress + 90
        }
    }

    private fun setGaugeWidth(value: Int) {
        indicator_main?.gaugeWidth = value.toFloat()
        tv_gauge_width?.text = value.toString()
    }
}
