package pl.art.mg4widget

import android.app.Activity
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.*

object Palette {
    val background = Color.rgb(4, 29, 48)
    val card = Color.rgb(10, 58, 82)
    val text = Color.rgb(239, 249, 255)
    val muted = Color.rgb(158, 194, 212)
    val accent = Color.rgb(116, 221, 243)
}
fun Activity.dp(n: Int) = (n * resources.displayMetrics.density).toInt()
fun Activity.page(): LinearLayout {
    val scroll = ScrollView(this).apply { setBackgroundColor(Palette.background); isFillViewport = true }
    val holder = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
    val width = minOf(resources.displayMetrics.widthPixels, dp(640))
    val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(24), dp(20), dp(24), dp(32)) }
    holder.addView(content, LinearLayout.LayoutParams(width, LinearLayout.LayoutParams.WRAP_CONTENT))
    scroll.addView(holder)
    scroll.setOnApplyWindowInsetsListener { v, insets ->
        val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.ime())
        v.setPadding(bars.left, bars.top, bars.right, bars.bottom); insets
    }
    setContentView(scroll)
    return content
}
fun Activity.text(parent: LinearLayout, value: String, size: Float = 15f, color: Int = Palette.text): TextView = TextView(this).apply {
    text = Language.text(value); textSize = size; setTextColor(color); setPadding(0, dp(5), 0, dp(5))
    parent.addView(this, LinearLayout.LayoutParams(-1, -2))
}
fun Activity.heading(parent: LinearLayout, label: String) = text(parent, label, 24f).apply { setTypeface(null, Typeface.BOLD); setPadding(0, dp(24), 0, dp(8)) }
fun Activity.card(parent: LinearLayout, compact: Boolean = false): LinearLayout = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL; setPadding(dp(if(compact) 12 else 20), dp(if(compact) 2 else 10), dp(if(compact) 12 else 20), dp(if(compact) 2 else 10))
    background = getDrawable(R.drawable.tile_background)
    parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(if(compact) 4 else 10); bottomMargin = dp(if(compact) 0 else 4) })
}
fun Activity.button(parent: LinearLayout, label: String, primary: Boolean = false, action: () -> Unit): Button = Button(this).apply {
    text = Language.text(label); isAllCaps = false; textSize = 15f; setTextColor(Palette.text)
    backgroundTintList = null
    background = getDrawable(if (primary) R.drawable.button_background else R.drawable.tile_background)
    setPadding(dp(12),dp(8),dp(12),dp(8))
    minHeight = dp(52)
    parent.addView(this, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
    setOnClickListener { action() }
}
fun Activity.input(parent: LinearLayout, hintText: String, type: Int, value: String = ""): EditText = EditText(this).apply {
    hint = Language.text(hintText); inputType = type; setText(value); setTextColor(Palette.text); setHintTextColor(Palette.muted)
    setSingleLine(); minHeight = dp(56); parent.addView(this, LinearLayout.LayoutParams(-1, -2))
}

fun Activity.toggle(parent: LinearLayout, label: String, checked: Boolean, action: (Boolean) -> Unit): Switch = Switch(this).apply {
    text = Language.text(label); textSize = 15f; setTextColor(Palette.text); isChecked = checked; minHeight = dp(40)
    setPadding(0, dp(2), 0, dp(2)); parent.addView(this, LinearLayout.LayoutParams(-1,-2))
    setOnCheckedChangeListener { _, value -> action(value) }
}
