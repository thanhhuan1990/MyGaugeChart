# Simplifier
[![Codacy Badge](https://api.codacy.com/project/badge/Grade/113443c78da34886b77aaa1348ef3ce3)](https://www.codacy.com/app/thanhhuan1990/MyGaugeChart?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=thanhhuan1990/MyGaugeChart&amp;utm_campaign=Badge_Grade)

- Show GaugeChart
- Custom GaugeChart with simple methods

How to add dependency?
--------

```groovy
repositories {
    maven {
        url "https://jitpack.io"
    }
}
```
then add a library dependency
```groovy
 implementation 'com.github.thanhhuan1990:MyGaugeChart:$version'
```
## Usage
```
<huynh.huan.gaugechart.GaugeView
                android:id="@+id/indicator_main"
                android:layout_width="0dp"
                android:layout_height="0dp"
                android:layout_margin="16dp"
                app:layout_constraintEnd_toEndOf="parent"
                app:layout_constraintStart_toStartOf="parent"
                app:layout_constraintTop_toTopOf="parent"
                app:gv_backgroundCircleColor="@android:color/transparent"
                app:gv_centerCircleColor="@color/color_indicator"
                app:gv_degreeBetweenMark="6"
                app:gv_endDegree="360"
                app:gv_firstEndColor="@color/first_end_color_indicator"
                app:gv_firstStartColor="@color/first_start_color_indicator"
                app:gv_indicatorColor="@color/color_indicator"
                app:gv_indicatorWidth="3dp"
                app:gv_markColor="@android:color/white"
                app:gv_markWidth="5"
                app:gv_maxPercent="100"
                app:gv_minPercent="0"
                app:gv_secondEndColor="@color/second_end_color_indicator"
                app:gv_secondStartColor="@color/second_start_color_indicator"
                app:gv_startDegree="0"
                app:gv_withMark="true" />
```
Licence
--------

Apache License, Version 2.0


    Copyright (C) 2018, Huan.Huynh

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
