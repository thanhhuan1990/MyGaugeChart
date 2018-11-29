package com.github.huanhuynh.gaugechart

import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.SeekBar
import com.crashlytics.android.Crashlytics
import com.skydoves.colorpickerpreference.ColorPickerDialog
import io.fabric.sdk.android.Fabric
import kotlinx.android.synthetic.main.layout_activity_main.*

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
            percent.id          -> {
                indicator_main?.percentTo(seekBar.progress)
                tv_progress?.text = seekBar.progress.toString()
            }
            sb_mark_width.id    -> {
                indicator_main?.markWidth = seekBar.progress / 2f
                tv_mark_width?.text = (seekBar.progress / 2f).toString()
            }
            indicator_width.id  -> {
                indicator_main?.indicatorWidth = seekBar.progress.toFloat()
                tv_indicator_width?.text = seekBar.progress.toString()
            }
            start_degree.id  -> {
                if(seekBar.progress <= end_degree.progress - 90) {
                    indicator_main?.startDegree = seekBar.progress
                    indicator_main?.percentTo(percent.progress)
                    tv_start_degree?.text = seekBar.progress.toString()
                } else {
                    seekBar.progress = end_degree.progress - 90
                }
            }
            end_degree.id  -> {
                if(seekBar.progress >= start_degree.progress + 90) {
                    indicator_main?.endDegree = seekBar.progress
                    indicator_main?.percentTo(percent.progress)
                    tv_end_degree?.text = seekBar.progress.toString()
                } else {
                    seekBar.progress = start_degree.progress + 90
                }
            }
            gauge_width.id  -> {
                indicator_main?.gaugeWidth = seekBar.progress.toFloat()
                tv_gauge_width?.text = seekBar.progress.toString()
            }
        }

    }

    private fun selectColor(view: View) {
        val builder = ColorPickerDialog.Builder(view.context, R.style.ColorPickerDialog)
        builder.setPositiveButton("OK") { colorEnvelope ->
            view.setBackgroundColor(colorEnvelope.color)

            when (view.id) {
                btnFirstStart.id            -> indicator_main?.setFirstStartColor(colorEnvelope.color)
                btnFirstEnd.id              -> indicator_main?.setFirstEndColor(colorEnvelope.color)
                btnSecondStart.id           -> indicator_main?.setSecondStartColor(colorEnvelope.color)
                btnSecondEnd.id             -> indicator_main?.setSecondEndColor(colorEnvelope.color)
                btnIndicatorColor.id        -> {
                    indicator_main?.setCenterCircleColor(colorEnvelope.color)
                    indicator_main?.setIndicatorColor(colorEnvelope.color)
                }
                btnMarkColor.id             -> indicator_main?.setMarkColor(colorEnvelope.color)
                btnBackgroundColor.id       -> indicator_main?.setBackgroundCircleColor(colorEnvelope.color)
            }
        }
        builder.show()
    }
}
