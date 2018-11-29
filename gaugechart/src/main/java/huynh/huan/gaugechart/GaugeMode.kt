package huynh.huan.gaugechart

/**
 * Created by Huan.Huynh on 11/29/18.
 *
 * Copyright © 2018 teqnological. All rights reserved.
 */

enum class GaugeMode constructor(
    val minDegree: Int,
    val maxDegree: Int
) {
    TOP(180, 360),
    BOTTOM(0, 180)
}