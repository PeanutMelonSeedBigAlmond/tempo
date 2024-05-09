package moe.peanutmelonseedbigalmond.tempo.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import moe.peanutmelonseedbigalmond.tempo.ui.widget.data.StructedLrc

class LrcView : me.wcy.lrcview.LrcView {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(
        context,
        attrs,
        defStyle
    )

    private var touchListsner: LrcViewTouchEvent? = null

    fun loadStructedLyric(lrcs: List<StructedLrc>) {
        val cls = Class.forName("me.wcy.lrcview.LrcEntry")
        val constructor = cls.getDeclaredConstructor(Long::class.java, String::class.java)
            .also { it.isAccessible = true }
        val transformedLrc = lrcs.map {
            return@map constructor.newInstance(it.time, it.text)
        }

        if (lyricsAreSame(transformedLrc)) return

        reset()
        val superClass = this::class.java.superclass
        val onLrcLoaded = superClass.getDeclaredMethod("onLrcLoaded", List::class.java)
            .also { it.isAccessible = true }
        onLrcLoaded(this, transformedLrc)
    }

    fun setTouchListsner(listener: LrcViewTouchEvent) {
        this.touchListsner = listener
    }

    fun clearTouchListener() {
        touchListsner = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> touchListsner?.onTouchStart()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> touchListsner?.onTouchEnd()
        }
        return super.onTouchEvent(event)
    }

    fun reset() {
        val superClass = this::class.java.superclass
        val reset = superClass.getDeclaredMethod("reset").also { it.isAccessible = true }
        reset(this)
    }

    private fun lyricsAreSame(lrcs: List<Any>): Boolean {
        val cls = this::class.java.superclass
        val cachedLrcEntry =
            cls.getDeclaredField("mLrcEntryList").also { it.isAccessible = true }
                .get(this) as List<*>
        return cachedLrcEntry == lrcs
    }
}