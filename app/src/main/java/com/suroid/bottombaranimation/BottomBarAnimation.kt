@file:Suppress("unused")

package com.suroid.bottombaranimation

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.util.LayoutDirection
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.LinearLayout

/**
 * Created by 1HE on 10/23/2018.
 */

internal typealias IBottomNavigationListener = (model: BottomBarAnimation.Model) -> Unit

@Suppress("MemberVisibilityCanBePrivate")
class BottomBarAnimation : FrameLayout {

    var models = ArrayList<Model>()
    var cells = ArrayList<BottomBarAnimationCell>()

    private var selectedId = -1

    private var mOnClickedListener: IBottomNavigationListener = {}
    private var mOnShowListener: IBottomNavigationListener = {}

    private var heightCell = 0
    private var isAnimating = false

    private var defaultIconColor = Color.parseColor("#757575")
    private var selectedIconColor = Color.parseColor("#2196f3")
    private var backgroundBottomColor = Color.parseColor("#ffffff")
    private var shadowColor = -0x454546
    private var countTextColor = Color.parseColor("#ffffff")
    private var countBackgroundColor = Color.parseColor("#ff0000")
    private var countTypeface: Typeface? = null
    private var rippleColor = Color.parseColor("#757575")

    private lateinit var ll_cells: LinearLayout
    private lateinit var bezierView: BezierView

    init {
        heightCell = dip(context, 72)
    }

    constructor(context: Context) : super(context) {
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        setAttributeFromXml(context, attrs)
        initializeViews()
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        setAttributeFromXml(context, attrs)
        initializeViews()
    }

    private fun setAttributeFromXml(context: Context, attrs: AttributeSet) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.BottomNavigationAnimation, 0, 0)
        try {
            a?.apply {
                defaultIconColor = getColor(R.styleable.BottomNavigationAnimation_sur_defaultIconColor, defaultIconColor)
                selectedIconColor = getColor(R.styleable.BottomNavigationAnimation_sur_selectedIconColor, selectedIconColor)
                backgroundBottomColor = getColor(R.styleable.BottomNavigationAnimation_sur_backgroundBottomColor, backgroundBottomColor)
                countTextColor = getColor(R.styleable.BottomNavigationAnimation_sur_countTextColor, countTextColor)
                countBackgroundColor = getColor(R.styleable.BottomNavigationAnimation_sur_countBackgroundColor, countBackgroundColor)
                val typeface = getString(R.styleable.BottomNavigationAnimation_sur_countTypeface)
                rippleColor = getColor(R.styleable.BottomNavigationAnimation_sur_rippleColor, rippleColor)
                shadowColor = getColor(R.styleable.BottomNavigationAnimation_sur_shadowColor, shadowColor)

                if (typeface != null && !typeface.isEmpty())
                    countTypeface = Typeface.createFromAsset(context.assets, typeface)
            }
        } finally {
            a?.recycle()
        }
    }

    private fun initializeViews() {
        ll_cells = LinearLayout(context)
        ll_cells.apply {
            val params = FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightCell)
            params.gravity = Gravity.BOTTOM
            layoutParams = params
            orientation = LinearLayout.HORIZONTAL
            clipChildren = false
            clipToPadding = false
        }

        bezierView = BezierView(context)
        bezierView.apply {
            layoutParams = FrameLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, heightCell)
            color = backgroundBottomColor
            shadowColor = this@BottomBarAnimation.shadowColor
        }

        addView(bezierView)
        addView(ll_cells)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (selectedId == -1) {
            bezierView.bezierX = if (Build.VERSION.SDK_INT >= 17 && layoutDirection == LayoutDirection.RTL) measuredWidth + dipf(context, 72) else -dipf(context, 72)
        }
        if (selectedId != -1) {
            show(selectedId, false)
        }
    }

    fun add(model: Model) {
        val cell = BottomBarAnimationCell(context)
        cell.apply {
            val params = LinearLayout.LayoutParams(0, heightCell, 1f)
            layoutParams = params
            icon = model.icon
            count = model.count
            circleColor = this@BottomBarAnimation.backgroundBottomColor
            countTextColor = this@BottomBarAnimation.countTextColor
            countBackgroundColor = this@BottomBarAnimation.countBackgroundColor
            countTypeface = this@BottomBarAnimation.countTypeface
            rippleColor = this@BottomBarAnimation.rippleColor
            defaultIconColor = this@BottomBarAnimation.defaultIconColor
            selectedIconColor = this@BottomBarAnimation.selectedIconColor
            setOnClickListener {
                if (cell.isEnabledCell || isAnimating)
                    return@setOnClickListener
                show(model.id)
                mOnClickedListener(model)
            }
            disableCell()
            ll_cells.addView(this)
        }

        cells.add(cell)
        models.add(model)
    }

    fun show(id: Int, enableAnimation: Boolean = true) {
        for (i in models.indices) {
            val model = models[i]
            val cell = cells[i]
            if (model.id == id) {
                anim(cell, id, enableAnimation)
                cell.enableCell()
                mOnShowListener(model)
            } else {
                cell.disableCell()
            }
        }
        selectedId = id
    }

    private fun anim(cell: BottomBarAnimationCell, id: Int, enableAnimation: Boolean = true) {
        isAnimating = true

        val pos = getModelPosition(id)
        val nowPos = getModelPosition(selectedId)

        val nPos = if (nowPos < 0) 0 else nowPos
        val dif = Math.abs(pos - nPos)
        val d = (dif) * 100L + 150L

        val animDuration = if (enableAnimation) d else 1L
        val animInterpolator = FastOutSlowInInterpolator()

        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.apply {
            duration = animDuration
            interpolator = animInterpolator
            val beforeX = bezierView.bezierX
            addUpdateListener {
                val f = it.animatedFraction
                val newX = cell.x + (cell.measuredWidth / 2)
                if (newX > beforeX)
                    bezierView.bezierX = f * (newX - beforeX) + beforeX
                else
                    bezierView.bezierX = beforeX - f * (beforeX - newX)
                if (f == 1f)
                    isAnimating = false
            }
            start()
        }

        if (Math.abs(pos - nowPos) > 1) {
            val progressAnim = ValueAnimator.ofFloat(0f, 1f)
            progressAnim.apply {
                duration = animDuration
                interpolator = animInterpolator
                addUpdateListener {
                    val f = it.animatedFraction
                    bezierView.progress = f * 2f
                }
                start()
            }
        }

        cell.isFromLeft = pos > nowPos
        cells.forEach {
            it.duration = d
        }
    }

    fun isShowing(id: Int): Boolean {
        return selectedId == id
    }

    fun getModelById(id: Int): Model? {
        models.forEach {
            if (it.id == id)
                return it
        }
        return null
    }

    fun getCellById(id: Int): BottomBarAnimationCell? {
        return cells[getModelPosition(id)]
    }

    fun getModelPosition(id: Int): Int {
        for (i in models.indices) {
            val item = models[i]
            if (item.id == id)
                return i
        }
        return -1
    }

    fun setCount(id: Int, count: String) {
        val model = getModelById(id) ?: return
        val pos = getModelPosition(id)
        model.count = count
        cells[pos].count = count
    }

    fun setOnShowListener(listener: IBottomNavigationListener) {
        mOnShowListener = listener
    }

    fun setOnClickMenuListener(listener: IBottomNavigationListener) {
        mOnClickedListener = listener
    }

    class Model(var id: Int, var icon: Int) {

        var count: String = BottomBarAnimationCell.EMPTY_VALUE

    }
}