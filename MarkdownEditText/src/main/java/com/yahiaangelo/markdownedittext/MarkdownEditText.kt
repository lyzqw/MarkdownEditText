package com.yahiaangelo.markdownedittext

import android.content.Context
import android.text.*
import android.text.style.ClickableSpan
import android.text.style.QuoteSpan
import android.text.style.StrikethroughSpan
import android.util.AttributeSet
import android.view.View
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.getSpans
import com.google.android.material.button.MaterialButton
import com.yahiaangelo.markdownedittext.model.EnhancedMovementMethod
import io.noties.markwon.*
import io.noties.markwon.core.spans.*
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tasklist.TaskListDrawable
import io.noties.markwon.ext.tasklist.TaskListItem
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.ext.tasklist.TaskListSpan
import org.commonmark.node.SoftLineBreak

class MarkdownEditText : AppCompatEditText {

    private var markwon: Markwon
    private var textWatcher: TextWatcher? = null
    private var markdownStylesBar: MarkdownStylesBar? = null
    private var isSelectionStyling = false
    var taskBoxColor: Int = ResourcesCompat.getColor(resources, R.color.primary, context.theme)
    var taskBoxBackgroundColor: Int = ResourcesCompat.getColor(resources, R.color.icon, context.theme)

    private val textWatchers: MutableList<TextWatcher> = emptyList<TextWatcher>().toMutableList()

    constructor(context: Context) : super(context, null) {
        markwon = markwonBuilder(context)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(
        context,
        attrs,
        R.attr.editTextStyle
    ) {
        markwon = markwonBuilder(context)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        markwon = markwonBuilder(context)
    }


    private fun markwonBuilder(context: Context): Markwon {
        movementMethod = EnhancedMovementMethod().getsInstance()
        return Markwon.builder(context)
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TaskListPlugin.create(taskBoxColor, taskBoxColor, taskBoxBackgroundColor))
            .usePlugin(object : AbstractMarkwonPlugin() {
                override fun configureVisitor(builder: MarkwonVisitor.Builder) {
                    super.configureVisitor(builder)
                    builder.on(
                        SoftLineBreak::class.java
                    ) { visitor, _ -> visitor.forceNewLine() }
                }

                override fun configureSpansFactory(builder: MarkwonSpansFactory.Builder) {
                    val origin = builder.getFactory(TaskListItem::class.java) ?: return

                    builder.setFactory(TaskListItem::class.java,object : SpanFactory {

                        override fun getSpans(
                            configuration: MarkwonConfiguration,
                            props: RenderProps
                        ): Any? {
                            val span = origin.getSpans(configuration, props)

                            if (span !is TaskListSpan){
                                return null
                            }else{
                                val taskClick = object : ClickableSpan(){
                                    override fun onClick(widget: View) {
                                        span.isDone = !span.isDone
                                        text?.setSpan(span, text?.getSpanStart(span)!!, text?.getSpanEnd(span)!!, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
                                    }

                                    override fun updateDrawState(ds: TextPaint) {
                                    }
                                }
                                return arrayOf(span, taskClick)
                            }
                        }

                    })

                }
            })
            .build()
    }

    fun setStylesBar(stylesBar: MarkdownStylesBar) {
        stylesBar.markdownEditText = this
        this.markdownStylesBar = stylesBar
    }

    fun triggerStyle(textStyle: TextStyle, stop: Boolean) {
        if (stop) {
            clearTextWatchers()
        } else {

            if (isSelectionStyling){
                styliseText(textStyle, selectionStart, selectionEnd)
                isSelectionStyling = false
            }else{
                textWatcher = object : TextWatcher {
                    override fun afterTextChanged(s: Editable?) {}

                    override fun beforeTextChanged(
                        s: CharSequence?,
                        start: Int,
                        count: Int,
                        after: Int
                    ) {}

                    override fun onTextChanged(
                        s: CharSequence?,
                        start: Int,
                        before: Int,
                        count: Int
                    ) {

                        if (before < count) {
                            styliseText(textStyle, start)
                        }
                    }

                }
                addTextWatcher(textWatcher!!)
            }

        }


    }

    fun triggerUnOrderedListStyle(stop: Boolean) {
        if (stop) {
           clearTextWatchers()
        }else{
            if (text!!.isNotEmpty()) {
                if ( text!!.length > 1 &&text!!.getGivenSpansAt(span = arrayOf(
                        TextStyle.ORDERED_LIST,
                        TextStyle.TASKS_LIST,
                    ),text!!.length - 2, text!!.length).isEmpty()){
                    if (text.toString().substring(text!!.length - 2, text!!.length) != "\n") {
                        text!!.append("\n ")
                    } else {
                        text!!.append(" ")
                    }
                }else{
                    text!!.append("\n ")
                }

            } else {
                text!!.append(" ")
            }

            text!!.setSpan(
                BulletListItemSpan(markwon.configuration().theme(), 0),
                text!!.length - 1,
                text!!.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            addTextWatcher(object : TextWatcher{
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    if (before < count){
                        val string = text.toString()
                        if (string.isNotEmpty() && string[string.length - 1] == '\n') {
                            text!!.append(" ")
                            text!!.setSpan(
                                BulletListItemSpan(markwon.configuration().theme(), 0),
                                text!!.length - 1,
                                text!!.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                    }

                }

            })

        }


    }

    fun triggerOrderedListStyle(stop: Boolean) {
        if (stop) {
            clearTextWatchers()
        } else {
            var currentNum = 1
            if (text!!.isNotEmpty()) {
                if ( text!!.length > 1 && text!!.getGivenSpansAt(span = arrayOf(
                        TextStyle.TASKS_LIST,
                        TextStyle.UNORDERED_LIST,
                    ),text!!.length - 2, text!!.length).isEmpty()){
                    if (text.toString().substring(text!!.length - 2, text!!.length) != "\n") {
                        text!!.append("\n  ")
                    } else {
                        text!!.append("  ")
                    }
                }else{
                    text!!.append("\n  ")
                }

            } else {
                text!!.append("  ")
            }

            text!!.setSpan(
                OrderedListItemSpan(markwon.configuration().theme(), "${currentNum}-"),
                text!!.length - 2,
                text!!.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            currentNum++

            addTextWatcher(object : TextWatcher{
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if (before < count){
                        val string = text.toString()
                        if (string.isNotEmpty() && string[string.length - 1] == '\n') {
                            text!!.append("  ")
                            text!!.setSpan(
                                OrderedListItemSpan(markwon.configuration().theme(), "${currentNum}-"),
                                text!!.length - 2,
                                text!!.length,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            currentNum++
                        }
                    }
                }

            })
        }
    }

    fun triggerTasksListStyle(stop: Boolean){

        if (stop) {
            clearTextWatchers()
        }else{
            if (text!!.isNotEmpty()) {
                if ( text!!.length > 1 && text!!.getGivenSpansAt(span = arrayOf(
                        TextStyle.ORDERED_LIST,
                        TextStyle.UNORDERED_LIST,
                    ),text!!.length - 2, text!!.length).isEmpty()){
                    if (text.toString().substring(text!!.length - 2, text!!.length) != "\n") {
                        text!!.append("\n ")
                    } else {
                        text!!.append(" ")
                    }
                }else{
                    text!!.append("\n ")
                }

            } else {
                text!!.append(" ")
            }

            setTaskSpan(text!!.length - 1,
                text!!.length, false)

            addTextWatcher(object : TextWatcher{
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {

                    if (before < count){
                        val string = text.toString()
                        if (string.isNotEmpty() && string[string.length - 1] == '\n') {
                            text!!.append(" ")
                            setTaskSpan(text!!.length - 1,
                                text!!.length, false)
                        }
                    }

                }

            })

        }
    }

    private fun setTaskSpan(start: Int, end: Int, isDone: Boolean){
        val taskSpan = TaskListSpan(markwon.configuration().theme(), TaskListDrawable(taskBoxColor, taskBoxColor, taskBoxBackgroundColor), isDone)
        text!!.setSpan(
           taskSpan ,
            start,
            end,
            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        text?.setSpan(object : ClickableSpan(){
            override fun onClick(widget: View) {
                taskSpan.isDone = !taskSpan.isDone
                text!!.setSpan(
                    taskSpan ,
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }

            override fun updateDrawState(ds: TextPaint) {

            }
        }, start, end,Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
    }


    private fun styliseText(
        textStyle: TextStyle,
        start: Int
    ) {
        when (textStyle) {
            TextStyle.BOLD -> {
                text!!.setSpan(
                    StrongEmphasisSpan(),
                    start,
                    start + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            TextStyle.ITALIC -> {
                text!!.setSpan(
                    EmphasisSpan(),
                    start,
                    start + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            TextStyle.STRIKE -> {
                text!!.setSpan(
                    StrikethroughSpan(),
                    start,
                    start + 1,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            }
            else -> {
            }
        }


    }

    private fun styliseText(
        textStyle: TextStyle,
        start: Int,
        end: Int
    ) {
        when (textStyle) {
            TextStyle.BOLD -> {
                text!!.setSpan(
                    StrongEmphasisSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            TextStyle.ITALIC -> {
                text!!.setSpan(
                    EmphasisSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            TextStyle.STRIKE -> {
                text!!.setSpan(
                    StrikethroughSpan(),
                    start,
                    end,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )

            }
            else -> {
            }
        }


    }

    enum class TextStyle {
        BOLD,
        ITALIC,
        STRIKE,
        QUOTE,
        UNORDERED_LIST,
        ORDERED_LIST,
        TASKS_LIST
    }

    fun getMD(): String {
        var mdText = text
        val startList = emptyList<Int>().toMutableList()
        val endList = emptyList<Int>().toMutableList()
        var i = 0

        for ((index, span) in text!!.getGivenSpans(
            span = arrayOf(
                TextStyle.BOLD,
                TextStyle.ITALIC,
                TextStyle.STRIKE,
                TextStyle.UNORDERED_LIST,
                TextStyle.ORDERED_LIST,
                TextStyle.TASKS_LIST
            )
        ).withIndex()) {
            val start = text!!.getSpanStart(span)
            val end = text!!.getSpanEnd(span)
            startList.add(index, start)
            endList.add(index, end)
        }

        for ((index, start) in startList.sorted().withIndex()) {
            val end = endList.sorted()[index]
            val spannedText = end.let { text!!.substring(start, it) }
            val span = end.let { text!!.getSpans(start, it, Any::class.java) }
            if (span != null) {
                for (selectedSpan in span.distinctBy { it.javaClass }) {
                    if (selectedSpan is BulletListItemSpan){
                        val mdString = "* $spannedText"
                        mdText = SpannableStringBuilder(
                            mdText!!.replaceRange(
                                start + i,
                                end + i,
                                mdString
                            )
                        )
                        i += 2
                    }else if(selectedSpan is TaskListSpan){
                            val mdString = if(selectedSpan.isDone) "* [x] $spannedText" else "* [ ] $spannedText"
                            mdText = SpannableStringBuilder(
                                mdText!!.replaceRange(
                                    start + i,
                                    end + i,
                                    mdString
                                )
                            )
                            i += 6
                    }
                    else{
                        if (spannedText.length > 1) {
                            when (selectedSpan) {
                                is StrongEmphasisSpan -> {
                                    val mdString = "**$spannedText**"
                                    mdText = SpannableStringBuilder(
                                        mdText!!.replaceRange(
                                            start + i,
                                            end + i,
                                            mdString
                                        )
                                    )
                                    i += 4
                                }
                                is EmphasisSpan -> {
                                    val mdString = "_${spannedText}_"
                                    mdText = SpannableStringBuilder(
                                        mdText!!.replaceRange(
                                            start + i,
                                            end + i,
                                            mdString
                                        )
                                    )
                                    i += 2
                                }
                                is StrikethroughSpan -> {
                                    val mdString = "~~$spannedText~~"
                                    mdText = SpannableStringBuilder(
                                        mdText!!.replaceRange(
                                            start + i,
                                            end + i,
                                            mdString
                                        )
                                    )
                                    i += 4
                                }
                                is OrderedListItemSpan -> {
                                    val mdString = "${selectedSpan.number} "
                                    mdText = SpannableStringBuilder(
                                        mdText!!.replaceRange(
                                            start + i,
                                            start + i + 2,
                                            mdString
                                        )
                                    )
                                    i += 1
                                }

                            }
                        }

                    }

                }
            }

        }
        return mdText.toString()
    }


    private fun Editable.getGivenSpans(vararg span: TextStyle): MutableList<Any> {
        val spanList = emptyArray<Any>().toMutableList()
        for (selectedSpan in span) {
            when (selectedSpan) {
                TextStyle.BOLD -> {
                    this.getSpans<StrongEmphasisSpan>().forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.ITALIC -> {
                    this.getSpans<EmphasisSpan>().forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.STRIKE -> {
                    this.getSpans<StrikethroughSpan>().forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.QUOTE -> {
                    this.getSpans<QuoteSpan>().forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.UNORDERED_LIST -> {
                    this.getSpans<BulletListItemSpan>().forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.ORDERED_LIST -> {
                    this.getSpans<OrderedListItemSpan>().forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.TASKS_LIST -> {
                    this.getSpans<TaskListSpan>().forEach {
                        spanList.add(it)
                    }
                }
            }
        }
        return spanList
    }

    private fun Editable.getGivenSpansAt(
        vararg span: TextStyle,
        start: Int,
        end: Int
    ): MutableList<Any> {
        val spanList = emptyArray<Any>().toMutableList()
        for (selectedSpan in span) {
            when (selectedSpan) {
                TextStyle.BOLD -> {
                    this.getSpans<StrongEmphasisSpan>(start, end).forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.ITALIC -> {
                    this.getSpans<EmphasisSpan>(start, end).forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.STRIKE -> {
                    this.getSpans<StrikethroughSpan>(start, end).forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.QUOTE -> {
                    this.getSpans<QuoteSpan>(start, end).forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.UNORDERED_LIST -> {
                    this.getSpans<BulletListItemSpan>(start, end).forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.ORDERED_LIST -> {
                    this.getSpans<OrderedListItemSpan>(start, end).forEach {
                        spanList.add(it)
                    }
                }
                TextStyle.TASKS_LIST -> {
                    this.getSpans<TaskListSpan>(start, end).forEach {
                        spanList.add(it)
                    }
                }
            }
        }
        return spanList
    }

    private var selectedButtonId: Int? = null

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (selStart == selEnd && markdownStylesBar != null && selStart > 0) {
            val selectedSpans = text!!.getGivenSpansAt(
                span = arrayOf(
                    TextStyle.BOLD,
                    TextStyle.ITALIC,
                    TextStyle.STRIKE
                ),
                start = selStart - 1, end = selStart
            )
            if (selectedSpans.size > 0) {
                for (span in selectedSpans.distinctBy { it.javaClass }) {
                    when (span) {
                        is StrongEmphasisSpan -> {
                            val boldButton =
                                markdownStylesBar!!.getViewWithId(R.id.style_button_bold) as MaterialButton
                            if (!boldButton.isChecked) {
                                boldButton.isChecked = true
                            }
                            selectedButtonId = boldButton.id
                        }
                        is EmphasisSpan -> {
                            val italicButton =
                                markdownStylesBar!!.getViewWithId(R.id.style_button_italic) as MaterialButton
                            if (!italicButton.isChecked) {
                                italicButton.isChecked = true
                            }
                            selectedButtonId = italicButton.id
                        }
                        is StrikethroughSpan -> {
                            val strikeThroughButton =
                                markdownStylesBar!!.getViewWithId(R.id.style_button_strike) as MaterialButton
                            if (!strikeThroughButton.isChecked) {
                                strikeThroughButton.isChecked = true
                            }
                            selectedButtonId = strikeThroughButton.id
                        }

                    }
                }
            } else {
                if (selectedButtonId != null) {
                    val button =
                        markdownStylesBar!!.getViewWithId(
                            selectedButtonId!!
                        ) as MaterialButton
                    if (button.isChecked) {
                        button.isChecked = false
                    }
                }
            }
        }else if(selStart != selEnd && markdownStylesBar != null){
            isSelectionStyling = true
        }


    }

    private fun addTextWatcher(textWatcher: TextWatcher) {
        textWatchers.add(textWatcher)
        addTextChangedListener(textWatcher)
    }

    private fun clearTextWatchers() {
        for (textWatcher in textWatchers) {
            removeTextChangedListener(textWatcher)
        }
    }

    private fun getCurrentCursorLine(): Int {
        return if (selectionStart != -1) layout.getLineForOffset(selectionStart) else -1
    }

    private fun getLineCharPosition(line: Int): Int {
        var chars = 1
        return if (line == 0) {
            0
        } else {
            for (i in 0 until line) {
                chars += text!!.lines()[i].length
            }
            chars
        }
    }

    //Renders md text in editText
    fun renderMD() {
        this.text = SpannableStringBuilder(markwon.toMarkdown(text.toString()))
    }

    //Renders given md string
    fun renderMD(md: String) {
        this.text = SpannableStringBuilder(markwon.toMarkdown(md))
    }
}
