package pl.art.mg4widget

import android.app.Activity
import android.content.ClipData
import android.os.Bundle
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView

/** Editing is local until Save; leaving the screen never writes the draft. */
class TileOrderActivity : Activity() {
    private lateinit var store: Store
    private lateinit var rows: LinearLayout
    private var draft = mutableListOf<Field>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = Store(this)
        draft = savedInstanceState?.getStringArrayList("order")?.mapNotNull { name ->
            Field.entries.firstOrNull { it.name == name }
        }?.toMutableList() ?: store.selectedFields.toMutableList()
        val page = page()
        heading(page, "Kolejność kafelków")
        text(page, "Przytrzymaj ☰ i upuść na wybranym kafelku albo użyj strzałek. Dwie kolumny: od lewej do prawej, następnie kolejny wiersz. Jedna kolumna: od góry do dołu.", 14f, Palette.muted)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        page.addView(actions)
        button(actions, "Anuluj") { finish() }.layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f)
        button(actions, "Zapisz", true) {
            // Preserve any newly enabled fields if settings changed while this draft was open.
            val current = store.selectedFields
            store.selectedFields = draft.filter { it in current } + current.filter { it !in draft }
            Widgets.updateAll(this)
            finish()
        }.layoutParams = LinearLayout.LayoutParams(0, dp(52), 1f).apply { marginStart = dp(8) }
        rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        page.addView(rows)
        renderRows()
        // Keep long lists reachable while dragging near the visible screen edges.
        val scroll = page.parent.parent as ScrollView
        scroll.setOnDragListener { _, event ->
            if (event.localState !is Field) false else {
                if (event.action == DragEvent.ACTION_DRAG_LOCATION) {
                    if (event.y < dp(80)) scroll.smoothScrollBy(0, -dp(32))
                    else if (event.y > scroll.height - dp(80)) scroll.smoothScrollBy(0, dp(32))
                }
                true
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putStringArrayList("order", ArrayList(draft.map { it.name }))
        super.onSaveInstanceState(outState)
    }

    private fun move(field: Field, target: Int) {
        val old = draft.indexOf(field)
        if (old < 0 || target !in draft.indices || old == target) return
        draft.removeAt(old)
        draft.add(target, field)
        renderRows()
        rows.announceForAccessibility("${field.label}, pozycja ${target + 1}")
    }

    private fun renderRows() {
        rows.removeAllViews()
        if (draft.isEmpty()) text(rows, "Najpierw włącz kafelki na poprzednim ekranie.")
        draft.forEachIndexed { index, field ->
            val row = card(rows, compact = true).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(4), dp(2), dp(4), dp(2))
            }
            fun control(label: String, description: String, enabled: Boolean = true, action: () -> Unit): Button =
                Button(this).apply {
                    text = label; textSize = 20f; setTextColor(Palette.accent)
                    background = null; minWidth = 0; minimumWidth = 0
                    setPadding(0, 0, 0, 0); isEnabled = enabled; alpha = if (enabled) 1f else 0.3f
                    contentDescription = description
                    row.addView(this, LinearLayout.LayoutParams(dp(48), dp(48)))
                    setOnClickListener { action() }
                }
            control("☰", "Przeciągnij: ${field.label}") {}.apply {
                setOnLongClickListener {
                    startDragAndDrop(ClipData.newPlainText("kafelek", field.name), View.DragShadowBuilder(this), field, 0)
                }
            }
            text(row, "${index + 1}. ${field.label}", 14f).layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
            control("↑", "${field.label}: przesuń w górę", index > 0) { move(field, index - 1) }
            control("↓", "${field.label}: przesuń w dół", index < draft.lastIndex) { move(field, index + 1) }
            row.setOnDragListener { view, event ->
                if (event.localState !is Field) false else {
                    when (event.action) {
                        DragEvent.ACTION_DRAG_ENTERED -> view.alpha = 0.6f
                        DragEvent.ACTION_DRAG_EXITED, DragEvent.ACTION_DRAG_ENDED -> view.alpha = 1f
                        DragEvent.ACTION_DROP -> {
                            view.alpha = 1f
                            move(event.localState as Field, draft.indexOf(field))
                        }
                    }
                    true
                }
            }
        }
    }
}
