package com.example.safehome.ui.monitor

import android.content.Context
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Shader
import android.widget.TextView
import com.example.safehome.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.github.mikephil.charting.utils.MPPointF

object ChartHelper {

    private val dateFormat = SimpleDateFormat("dd/MM", Locale.getDefault())
    private val fullDateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    class ChartMarkerView(context: Context, private val unit: String, private val timestamps: List<Long> = emptyList()) : com.github.mikephil.charting.components.MarkerView(context, R.layout.layout_chart_marker) {

        init {
            android.util.Log.d("ChartMarker", "ChartMarkerView created: unit=$unit, timestamps.size=${timestamps.size}")
            android.util.Log.d("ChartMarker", "Marker dimensions before measure: width=$width, height=$height")

            // PHASE B: Kiểm tra findViewById trong constructor
            val tvValue = findViewById<TextView>(R.id.tvValue)
            val tvTime = findViewById<TextView>(R.id.tvTime)
            val tvDate = findViewById<TextView>(R.id.tvDate)

            android.util.Log.d("ChartMarker", "PHASE B - findViewById check:")
            android.util.Log.d("ChartMarker", "tvDate = $tvDate")
            android.util.Log.d("ChartMarker", "tvTime = $tvTime")
            android.util.Log.d("ChartMarker", "tvValue = $tvValue")
            android.util.Log.d("ChartMarker", "tvDate == null? ${tvDate == null}")
            android.util.Log.d("ChartMarker", "tvTime == null? ${tvTime == null}")
            android.util.Log.d("ChartMarker", "tvValue == null? ${tvValue == null}")

            // Force measure and layout in init
            measure(
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED),
                android.view.View.MeasureSpec.makeMeasureSpec(0, android.view.View.MeasureSpec.UNSPECIFIED)
            )
            layout(0, 0, measuredWidth, measuredHeight)
            android.util.Log.d("ChartMarker", "After force measure/layout: width=$measuredWidth, height=$measuredHeight")
        }

        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            android.util.Log.d("ChartMarker", "Marker onMeasure: width=$width, height=$height")
        }

        override fun refreshContent(e: Entry, highlight: com.github.mikephil.charting.highlight.Highlight) {
            android.util.Log.d("ChartMarker", "refreshContent called: x=${e.x}, y=${e.y}, unit=$unit, timestamps.size=${timestamps.size}")
            android.util.Log.d("ChartMarker", "Marker dimensions at refresh: width=$width, height=$height")

            val tvValue = findViewById<TextView>(R.id.tvValue)
            val tvTime = findViewById<TextView>(R.id.tvTime)
            val tvDate = findViewById<TextView>(R.id.tvDate)

            android.util.Log.d("ChartMarker", "TextViews found: tvValue=${tvValue != null}, tvTime=${tvTime != null}, tvDate=${tvDate != null}")
            
            if (tvValue == null || tvTime == null || tvDate == null) {
                android.util.Log.e("ChartMarker", "TextViews are null! Cannot update content")
                return
            }
            
            tvValue.text = String.format(Locale.getDefault(), "%.1f%s", e.y, unit)

            // Format full date and time from timestamp
            // If timestamps are provided, use index-based lookup, otherwise use x as timestamp
            val timestamp = if (timestamps.isNotEmpty()) {
                val index = e.x.toInt()
                android.util.Log.d("ChartMarker", "Looking up timestamp for index=$index, timestamps.size=${timestamps.size}")
                if (index >= 0 && index < timestamps.size) {
                    android.util.Log.d("ChartMarker", "Found timestamp: ${timestamps[index]}")
                    timestamps[index]
                } else {
                    android.util.Log.w("ChartMarker", "Index out of bounds, using x as timestamp")
                    e.x.toLong()
                }
            } else {
                android.util.Log.d("ChartMarker", "No timestamps provided, using x as timestamp")
                e.x.toLong()
            }

            val date = Date(timestamp)
            tvDate.text = fullDateFormat.format(date)
            tvTime.text = timeFormat.format(date)

            // PHASE C: In log sau setText
            android.util.Log.d("ChartMarker", "PHASE C - After setText:")
            android.util.Log.d("ChartMarker", "Date = ${tvDate.text}")
            android.util.Log.d("ChartMarker", "Time = ${tvTime.text}")
            android.util.Log.d("ChartMarker", "Value = ${tvValue.text}")

            // PHASE D: Kiểm tra visibility
            android.util.Log.d("ChartMarker", "PHASE D - Visibility check:")
            android.util.Log.d("ChartMarker", "tvDate.visibility = ${tvDate.visibility} (0=VISIBLE, 4=INVISIBLE, 8=GONE)")
            android.util.Log.d("ChartMarker", "tvTime.visibility = ${tvTime.visibility}")
            android.util.Log.d("ChartMarker", "tvValue.visibility = ${tvValue.visibility}")

            // PHASE E: Kiểm tra alpha
            android.util.Log.d("ChartMarker", "PHASE E - Alpha check:")
            android.util.Log.d("ChartMarker", "tvDate.alpha = ${tvDate.alpha}")
            android.util.Log.d("ChartMarker", "tvTime.alpha = ${tvTime.alpha}")
            android.util.Log.d("ChartMarker", "tvValue.alpha = ${tvValue.alpha}")

            // PHASE F: Kiểm tra currentTextColor
            android.util.Log.d("ChartMarker", "PHASE F - TextColor check:")
            android.util.Log.d("ChartMarker", "tvDate.currentTextColor = 0x${Integer.toHexString(tvDate.currentTextColor)}")
            android.util.Log.d("ChartMarker", "tvTime.currentTextColor = 0x${Integer.toHexString(tvTime.currentTextColor)}")
            android.util.Log.d("ChartMarker", "tvValue.currentTextColor = 0x${Integer.toHexString(tvValue.currentTextColor)}")

            // Call super.refreshContent() for internal updates
            super.refreshContent(e, highlight)

            // Log marker dimensions sau refresh
            android.util.Log.d("ChartMarker", "Marker dimensions after refresh: width=$width, height=$height")
        }

        override fun getOffset(): com.github.mikephil.charting.utils.MPPointF {
            // Position tooltip above the point, centered horizontally
            android.util.Log.d("ChartMarker", "getOffset called: width=$width, height=$height")
            // Use smaller offset to keep marker within chart bounds for testing
            return com.github.mikephil.charting.utils.MPPointF(-(width / 2).toFloat(), -20f)
        }

        override fun draw(canvas: android.graphics.Canvas, posX: Float, posY: Float) {
            android.util.Log.d("ChartMarker", "draw called: posX=$posX, posY=$posY, width=$width, height=$height")
            super.draw(canvas, posX, posY)
        }

        override fun dispatchDraw(canvas: android.graphics.Canvas) {
            android.util.Log.d("MARKER", "dispatchDraw")
            super.dispatchDraw(canvas)
        }
    }

    // Simple marker for testing
    class SimpleMarker(context: Context) : com.github.mikephil.charting.components.MarkerView(context, android.R.layout.simple_list_item_1) {
        private val text = findViewById<TextView>(android.R.id.text1)

        override fun refreshContent(e: Entry, highlight: com.github.mikephil.charting.highlight.Highlight) {
            text.text = "${e.y}"
            super.refreshContent(e, highlight)
        }

        override fun getOffset() =
            com.github.mikephil.charting.utils.MPPointF(-(width / 2f), -height.toFloat())
    }

    fun setupLineChart(chart: LineChart, unit: String, color: Int) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)
            setDrawGridBackground(false)
            setHighlightPerDragEnabled(true) // Enable highlight on drag
            
            legend.isEnabled = false
            
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                setDrawGridLines(false)
                granularity = 1f
                textColor = Color.parseColor("#64748B")
                textSize = 11f
                setLabelCount(6, false) // Limit to ~6 labels max
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        val date = Date(value.toLong())
                        val dayStr = dateFormat.format(date)
                        android.util.Log.d("ChartXAxis", "X-axis label: $dayStr")
                        return dayStr
                    }
                }
            }
            
            axisLeft.apply {
                setDrawGridLines(true)
                gridColor = Color.parseColor("#E2E8F0")
                gridLineWidth = 0.5f
                textColor = Color.parseColor("#64748B")
                textSize = 11f
                setLabelCount(5, false)
                setDrawAxisLine(false)
                valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        return String.format(Locale.getDefault(), "%.1f%s", value, unit)
                    }
                }
            }
            
            axisRight.isEnabled = false
            
            // Configure highlight behavior
            setHighlightPerDragEnabled(true)
            isHighlightPerDragEnabled = true

            // CHECK 4: Log highlight settings
            android.util.Log.d("MARKER", "isHighlightPerTapEnabled = $isHighlightPerTapEnabled")
            android.util.Log.d("MARKER", "isHighlightPerDragEnabled = $isHighlightPerDragEnabled")
        }
    }

    fun setupMiniChart(chart: LineChart, color: Int) {
        chart.apply {
            description.isEnabled = false
            setTouchEnabled(false)
            isDragEnabled = false
            setScaleEnabled(false)
            setPinchZoom(false)
            setDrawGridBackground(false)
            
            legend.isEnabled = false
            
            xAxis.apply {
                setDrawLabels(false)
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.TRANSPARENT
            }
            
            axisLeft.apply {
                setDrawLabels(false)
                setDrawGridLines(false)
                setDrawAxisLine(false)
                textColor = Color.TRANSPARENT
                setLabelCount(3, false)
            }
            
            axisRight.isEnabled = false
        }
    }

    fun createLineData(entries: List<Entry>, color: Int, label: String, isMini: Boolean = false): LineData {
        val dataSet = LineDataSet(entries, label).apply {
            this.color = color
            setCircleColor(color)
            lineWidth = if (isMini) 1.5f else 2.5f
            circleRadius = if (isMini) 0f else 4f
            setDrawCircleHole(false)
            setDrawValues(false)
            mode = LineDataSet.Mode.CUBIC_BEZIER
            cubicIntensity = 0.2f
            
            if (!isMini) {
                setDrawFilled(true)
                highLightColor = Color.parseColor("#94A3B8") // Light gray for crosshair
                setDrawHighlightIndicators(true)
                setHighlightLineWidth(1f)
                // Disable horizontal highlight line, keep only vertical
                setDrawHorizontalHighlightIndicator(false)
                setDrawVerticalHighlightIndicator(true)
            }
        }
        
        return LineData(dataSet)
    }

    fun updateChart(chart: LineChart, entries: List<Entry>, color: Int, unit: String) {
        android.util.Log.d("ChartHelper", "=== START updateChart ===")
        android.util.Log.d("ChartHelper", "updateChart called with ${entries.size} entries, chart width=${chart.width}, chart height=${chart.height}")
        if (entries.isEmpty()) {
            android.util.Log.d("ChartHelper", "Entries empty, clearing chart")
            chart.clear()
            return
        }
        
        try {
            android.util.Log.d("ChartHelper", "Setting up line chart")
            setupLineChart(chart, unit, color)
            
            android.util.Log.d("ChartHelper", "Creating line data")
            val lineData = createLineData(entries, color, "")
            
            // Set gradient fill for the line
            val dataSet = lineData.getDataSetByIndex(0) as LineDataSet
            android.util.Log.d("ChartHelper", "Setting draw filled")
            dataSet.setDrawFilled(true)
            
            // Set fill color with transparency
            val fillColor = Color.argb(100, Color.red(color), Color.green(color), Color.blue(color))
            dataSet.setFillColor(fillColor)
            
            // Set highlight color for crosshair
            android.util.Log.d("ChartHelper", "Setting highlight color")
            dataSet.highLightColor = Color.parseColor("#94A3B8") // Light gray crosshair
            dataSet.setHighlightLineWidth(1f)
            dataSet.setDrawHorizontalHighlightIndicator(false)
            dataSet.setDrawVerticalHighlightIndicator(true)
            
            android.util.Log.d("ChartHelper", "Setting chart data")
            chart.data = lineData
            android.util.Log.d("ChartHelper", "Notify data changed")
            chart.notifyDataSetChanged()
            android.util.Log.d("ChartHelper", "Invalidate chart")
            chart.invalidate()
            
            android.util.Log.d("ChartHelper", "Animate chart")
            chart.animateX(1000)
            android.util.Log.d("ChartHelper", "Chart update complete")
        } catch (e: Exception) {
            android.util.Log.e("ChartHelper", "Error updating chart", e)
        }
    }

    fun updateMiniChart(chart: LineChart, entries: List<Entry>, color: Int) {
        if (entries.isEmpty()) {
            chart.clear()
            return
        }
        
        setupMiniChart(chart, color)
        val lineData = createLineData(entries, color, "", isMini = true)
        chart.data = lineData
        chart.notifyDataSetChanged()
        chart.invalidate()
        
        chart.animateX(800)
    }

    fun formatTimestamp(timestamp: String): Long {
        return try {
            java.time.Instant.parse(timestamp).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    fun getMinMax(values: List<Double>): Pair<Double, Double> {
        if (values.isEmpty()) return 0.0 to 0.0
        val min = values.minOrNull() ?: 0.0
        val max = values.maxOrNull() ?: 0.0
        return min to max
    }
}
