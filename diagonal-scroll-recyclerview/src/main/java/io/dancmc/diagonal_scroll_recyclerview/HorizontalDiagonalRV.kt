package io.dancmc.diagonal_scroll_recyclerview

import android.content.Context
import android.graphics.Rect
import android.support.v7.widget.RecyclerView
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.*


open class HorizontalDiagonalRV : ScrollView {
    private lateinit var linearLayout: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private var downX = 0f
    private var downY = 0f
    private var furthestDistanceMovedPx = 0f

    constructor(context: Context) : super(context) {
        constructViews(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        constructViews(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        constructViews(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes) {
        constructViews(context)
    }


    private fun constructViews(context: Context) {

        val params = ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // init recyclerview
        recyclerView = LayoutInflater.from(context).inflate(R.layout.horizontal_rv, this, false) as RecyclerView
        recyclerView.layoutParams = params

        // init LinearLayout
        linearLayout = LinearLayout(context)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        linearLayout.layoutParams = params
        linearLayout.addView(recyclerView)

        this.addView(linearLayout)

    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        return true
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {


        if (this.childCount != 0) {
            super.onTouchEvent(event)
            recyclerView.onTouchEvent(event)

            when (event.action) {

                MotionEvent.ACTION_DOWN -> {
                    downX = event.rawX
                    downY = event.rawY
                }

                MotionEvent.ACTION_UP -> {
                    if (recyclerView.adapter != null && recyclerView.adapter.itemCount > 0 && isClick(furthestDistanceMovedPx)) {

                        // allows users to implement custom click behaviour/searching
                        if (recyclerView.adapter is CoordinatesClickListener) {
                            (recyclerView.adapter as CoordinatesClickListener).clickCoordinates(event.rawX, event.rawY)

                            // default implementation assumes a standard recyclerview with child layout rows, recursive search
                        } else {
                            try {
                                val v = findChildAtLocation(recyclerView, event.rawX.toInt(), event.rawY.toInt())
                                v?.performClick()
                            } catch (e: Exception) {
                                Log.d(TAG, "onTouch: " + e.message)
                            }
                        }
                    }
                    furthestDistanceMovedPx = 0f
                }

                MotionEvent.ACTION_MOVE -> {
                    val distanceFromStart = pxDistance(event.rawX, event.rawY, downX, downY)
                    if (distanceFromStart > furthestDistanceMovedPx) {
                        furthestDistanceMovedPx = distanceFromStart
                    }
                }
            }

        }
        return true
    }

    private fun findChildAtLocation(v: ViewGroup, x: Int, y: Int): View? {

        for (i in 0 until v.childCount) {
            val childCoords = IntArray(2)
            var child: View? = v.getChildAt(i)

            if (child != null) {
                child.getLocationOnScreen(childCoords)
                val childArea = Rect(childCoords[0], childCoords[1], childCoords[0] + child.width, childCoords[1] + child.height)

                if (childArea.contains(x, y)) {
                    if (child is ViewGroup) {
                        child = findChildAtLocation(child, x, y)
                    }
                    return child
                }
            }
        }
        return null
    }


    interface CoordinatesClickListener {
        fun clickCoordinates(x: Float, y: Float)
    }

    // View customisation

    fun setRecyclerViewAdapter(adapter: RecyclerView.Adapter<*>) {
        recyclerView.adapter = adapter

    }

    fun setRecyclerViewLayoutManager(manager: RecyclerView.LayoutManager) {
        recyclerView.layoutManager = manager
    }

    fun addToLinearLayout(v: View, index: Int) {
        linearLayout.addView(v, index)
    }

    fun replaceRecyclerView(newRecyclerView: RecyclerView) {
        linearLayout.removeView(this.recyclerView)
        this.recyclerView = newRecyclerView;
        linearLayout.addView(this.recyclerView)
    }

    fun replaceLinearLayout(newLinearLayout: LinearLayout, rvIncluded: Boolean = false) {

        // swap the current recyclerview if not included in new LL
        if (!rvIncluded) {
            this.linearLayout.removeView(recyclerView)
            newLinearLayout.addView(recyclerView)
        }

        this.removeView(this.linearLayout)
        this.addView(linearLayout)

    }

    fun getLinearLayout():LinearLayout{
        return this.linearLayout
    }

    fun getRecyclerView():RecyclerView{
        return recyclerView
    }

    // Misc utilities

    // https://stackoverflow.com/questions/9965695/how-to-distinguish-between-move-and-click-in-ontouchevent
    private fun pxToDp(px: Float): Float {
        return px / resources.displayMetrics.density
    }

    private fun pxDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2
        val dy = y1 - y2
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    private fun dpDistance(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val distanceInPx = pxDistance(x1, y1, x2, y2)
        return pxToDp(distanceInPx).toInt().toFloat()
    }

    private fun isClick(furthestDistanceMovedPx: Float): Boolean {
        return pxToDp(furthestDistanceMovedPx) < 15
    }

    companion object {

        private val TAG = "DiagonalRecyclerView"
    }

}
