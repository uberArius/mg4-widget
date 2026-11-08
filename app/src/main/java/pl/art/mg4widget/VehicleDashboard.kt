package pl.art.mg4widget

import android.app.Activity
import android.content.ClipData
import android.view.DragEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Toast

/** Selected tiles first, in widget order; other readings remain visible and selectable. */
class VehicleDashboard(private val activity: Activity, private val store: Store, private val container: LinearLayout) {
    private val alert = activity.text(container, "", 13f, Palette.text)
    private val help = activity.text(container, "Dotknij kafelka, aby dodać/usunąć go z widżetu. Nowy trafia na górę. Przytrzymaj wybrany kafelek i przeciągnij, aby zmienić kolejność.", 12f, Palette.muted)
    private val grid = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
    private var lastState = ""
    private var dragging = false
    private var scrollStep = 0
    private fun scroll(): ScrollView? {
        var parent = container.parent
        while (parent != null) { if (parent is ScrollView) return parent; parent = parent.parent }
        return null
    }
    private val autoScroll = object : Runnable {
        override fun run() {
            if (dragging && scrollStep != 0) {
                scroll()?.scrollBy(0, scrollStep)
                grid.postDelayed(this, 32)
            }
        }
    }
    private fun edge(view: View, event: DragEvent) {
        val scroll = scroll() ?: return
        val point = IntArray(2); val top = IntArray(2)
        view.getLocationOnScreen(point); scroll.getLocationOnScreen(top)
        val y = point[1] + event.y - top[1]
        scrollStep = when { y < activity.dp(90) -> -activity.dp(12); y > scroll.height - activity.dp(90) -> activity.dp(12); else -> 0 }
        grid.removeCallbacks(autoScroll)
        if (scrollStep != 0) grid.post(autoScroll)
    }
    init {
        container.addView(grid)
        grid.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) { dragging = false; grid.removeCallbacks(autoScroll) }
        })
        update()
    }
    private fun save(fields: List<Field>) { store.selectedFields = fields; Widgets.updateAll(activity) }
    fun update() {
        if (dragging) return
        val selected = store.selectedFields
        val snapshot = store.snapshot
        val state = Language.code + "${store.frontTyreThreshold}/${store.rearTyreThreshold}" + snapshot.json() + selected.joinToString() + store.flag("icons", true) + store.flag("rhd") + store.flag("miles")
        if (state == lastState) return
        lastState = state
        val causes = Tiles.selectable.filter { snapshot.tileCausesWarning(it, store.frontTyreBar, store.rearTyreBar) }
        alert.text = if (causes.isEmpty()) "" else "Alarm postoju: " + causes.joinToString { it.label } + ". Sprawdź czerwone kafelki i czas odczytu."
        alert.text = Language.text(alert.text)
        alert.visibility = if (causes.isEmpty()) View.GONE else View.VISIBLE
        container.setBackgroundResource(if (causes.isNotEmpty()) R.drawable.widget_alert else R.drawable.widget_background)
        grid.removeAllViews()
        val ordered = selected + Tiles.selectable.filter { it !in selected }
        ordered.chunked(2).forEach { pair ->
            val row = LinearLayout(activity).apply { orientation = LinearLayout.HORIZONTAL }
            grid.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = activity.dp(5) })
            val height = pair.maxOf { when (it) {
                Field.TYRES, Field.DOORS, Field.WINDOWS -> 76
                Field.SCHEDULE -> 80
                Field.TEMPERATURES, Field.CHARGING -> 68
                else -> 58
            } }
            pair.forEachIndexed { index, field ->
                val box = LinearLayout(activity).apply { orientation = LinearLayout.VERTICAL }
                row.addView(box, LinearLayout.LayoutParams(0, -1, 1f).apply { if (index > 0) marginStart = activity.dp(5) })
                val chosen = field in selected
                activity.text(box, if (chosen) "✓ Na widżecie · ${selected.indexOf(field)+1}" else "○ Dodaj do widżetu", 11f, if (chosen) Palette.accent else Palette.muted)
                val tile = Widgets.tile(activity, store, field, height).apply(activity, box)
                box.addView(tile, LinearLayout.LayoutParams(-1, -1))
                box.isSelected = chosen
                box.contentDescription = field.label + if (chosen) ", wybrany, pozycja ${selected.indexOf(field)+1}. Dotknij, aby usunąć; przytrzymaj, aby przenieść." else ", niewybrany. Dotknij, aby dodać."
                box.setOnClickListener {
                    save(Tiles.toggled(store.selectedFields, field))
                    update()
                    if (!chosen) grid.post { scroll()?.smoothScrollTo(0, 0) }
                }
                box.setOnLongClickListener {
                    if (field !in store.selectedFields) {
                        Toast.makeText(activity, Language.text("Najpierw dotknij kafelka, aby go wybrać."), Toast.LENGTH_SHORT).show()
                        true
                    } else {
                        dragging = true
                        val started = box.startDragAndDrop(ClipData.newPlainText("tile",field.name),View.DragShadowBuilder(box),field,0)
                        if (!started) dragging = false
                        started
                    }
                }
                box.setOnDragListener { view, event ->
                    if (event.localState !is Field) false else {
                        when (event.action) {
                            DragEvent.ACTION_DRAG_ENTERED -> view.alpha = 0.55f
                            DragEvent.ACTION_DRAG_LOCATION -> edge(view,event)
                            DragEvent.ACTION_DRAG_EXITED -> view.alpha = 1f
                            DragEvent.ACTION_DROP -> save(Tiles.moved(store.selectedFields,event.localState as Field,field))
                            DragEvent.ACTION_DRAG_ENDED -> {
                                view.alpha = 1f; dragging = false; scrollStep = 0
                                grid.removeCallbacks(autoScroll); grid.post { update() }
                            }
                        }
                        true
                    }
                }
            }
        }
    }
}
