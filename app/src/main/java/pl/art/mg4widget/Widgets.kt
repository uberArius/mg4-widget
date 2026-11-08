package pl.art.mg4widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.SizeF
import android.view.Gravity
import android.view.View
import android.widget.RemoteViews

class FullWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) { ids.forEach { Widgets.update(context, it) } }
    override fun onAppWidgetOptionsChanged(context: Context, manager: AppWidgetManager, id: Int, options: Bundle) { Widgets.update(context, id) }
}
object Widgets {
    fun kind(context: Context, id: Int) = "morski"
    fun ids(context: Context): List<Int> = AppWidgetManager.getInstance(context).getAppWidgetIds(ComponentName(context, FullWidget::class.java)).toList()
    fun updateAll(context: Context) { ids(context).forEach { update(context, it) } }
    fun icon(field: Field) = when (field) {
        Field.SOC -> R.drawable.ic_battery
        Field.RANGE, Field.JOURNEY, Field.FUEL_RANGE -> R.drawable.ic_range
        Field.TEMPERATURES, Field.INSIDE -> R.drawable.ic_temp
        Field.OUTSIDE -> R.drawable.ic_sun
        Field.LOCKED -> R.drawable.ic_lock
        Field.CLIMATE -> R.drawable.ic_fan
        Field.MILEAGE -> R.drawable.ic_dial
        Field.AUX -> R.drawable.ic_aux
        Field.CHARGING, Field.POWER -> R.drawable.ic_plug
        Field.TYRES, Field.TARGET, Field.TYRE_FL, Field.TYRE_FR, Field.TYRE_RL, Field.TYRE_RR -> R.drawable.ic_target
        Field.SEATS, Field.SEAT_LEFT, Field.SEAT_RIGHT, Field.REAR_HEAT -> R.drawable.ic_heat
        else -> R.drawable.ic_generic
    }
    fun tile(context: Context, store: Store, field: Field, minimumHeight: Int): RemoteViews {
        val grid = field in listOf(Field.TYRES,Field.DOORS,Field.WINDOWS)
        val row = RemoteViews(context.packageName, if(grid) R.layout.widget_grid else R.layout.widget_field)
        row.setInt(R.id.field_root,"setMinimumHeight",(minimumHeight*context.resources.displayMetrics.density).toInt())
        val reading = store.snapshot.fields[field]
        row.setLocalizedText(R.id.field_label, if (field == Field.CHARGING) Tiles.chargingTitle(store.snapshot) else field.label)
        if(grid) {
            val members=Tiles.members(field,store.flag("rhd"))
            listOf(R.id.cell_0,R.id.cell_1,R.id.cell_2,R.id.cell_3).forEachIndexed { i, cell ->
                row.setLocalizedText(cell,Tiles.cell(store.snapshot,members[i]))
                row.setContentDescription(cell,listOf("Przód lewa", "Przód prawa", "Tył lewa", "Tył prawa")[i]+": "+store.snapshot.value(members[i]))
            }
        } else {
            val value=when(field) {
                Field.SOC -> Tiles.batteryText(store.snapshot)
                Field.CHARGING -> Tiles.chargingText(store.snapshot)
                Field.TEMPERATURES -> "Wew. ${store.snapshot.value(Field.INSIDE)}\nZew. ${store.snapshot.value(Field.OUTSIDE)}"
                Field.SEATS -> "L: ${Tiles.cell(store.snapshot,Field.SEAT_LEFT)}   P: ${Tiles.cell(store.snapshot,Field.SEAT_RIGHT)}"
                else -> store.snapshot.value(field,store.flag("miles"))
            }
            row.setLocalizedText(R.id.field_value,value)
            row.setTextViewTextSize(R.id.field_value,android.util.TypedValue.COMPLEX_UNIT_SP,if(field == Field.CHARGING) 12f else if(field in Tiles.groups) 14f else 18f)
        }
        row.setLocalizedText(R.id.field_time,Tiles.time(store.snapshot,field))
        row.setInt(R.id.field_root, "setBackgroundResource", if (field.isAlert(reading?.value) || store.snapshot.tileCausesWarning(field, store.frontTyreBar, store.rearTyreBar)) R.drawable.tile_alert else R.drawable.tile_background)
        row.setImageViewResource(R.id.field_icon, icon(field))
        val icons = store.flag("icons", true)
        row.setViewVisibility(R.id.field_icon, if (icons) View.VISIBLE else View.GONE)
        // Align grouped timestamps with the title after its 20 dp icon and 7 dp gap.
        if (grid) row.setViewPadding(R.id.field_time, if (icons) (27 * context.resources.displayMetrics.density).toInt() else 0, 0, 0, 0)
        row.setViewPadding(R.id.field_content, if (icons) (7 * context.resources.displayMetrics.density).toInt() else 0, 0, 0, 0)
        (if(grid) listOf(R.id.field_label,R.id.field_time) else listOf(R.id.field_label, R.id.field_value, R.id.field_time)).forEach {
            row.setInt(it, "setGravity", if (icons) Gravity.START else Gravity.CENTER)
        }
        return row
    }
    fun update(context: Context, id: Int) {
        val store = Store(context)
        val expired = Trial.isExpired(context)
        fun layout(size: SizeF): RemoteViews {
            val columns = if (size.width < 280f) 1 else 2
            val pairs=store.selectedFields.chunked(columns)
            fun height(field: Field)=when(field) {
                Field.TYRES,Field.DOORS,Field.WINDOWS -> 76
                Field.SCHEDULE -> 80
                Field.TEMPERATURES, Field.CHARGING -> 68
                else -> 58
            }
            var contentHeight=0f
            val buttons=store.flag("showRefresh",true)||store.flag("showHeat",true)
            val available=(size.height-20-36-(if(buttons) 48 else 0)-(if(expired) 100 else 34)).coerceAtLeast(24f)
            val view = RemoteViews(context.packageName, R.layout.widget_main)
            view.setLocalizedText(R.id.widget_refresh, "Odśwież")
            view.setContentDescription(R.id.widget_refresh, Language.text("Odśwież dane samochodu"))
            view.setLocalizedText(R.id.widget_heat, "Ogrzewanie")
            view.setLocalizedText(R.id.widget_empty, if (Language.polish) "Wybierz kafelki w aplikacji" else "Select tiles in the app")
            view.setInt(R.id.widget_root, "setBackgroundResource", if (store.snapshot.parkedWarning(store.frontTyreBar, store.rearTyreBar)) R.drawable.widget_alert else R.drawable.widget_background)
            val open = PendingIntent.getActivity(context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            view.setOnClickPendingIntent(R.id.widget_header, open)
            view.setOnClickPendingIntent(R.id.widget_empty, open)
            view.setOnClickPendingIntent(R.id.widget_status, open)
            view.setLocalizedText(R.id.widget_title, VehicleInfo.title(store.vehicle))
            view.setViewVisibility(R.id.widget_title, if (columns == 1) View.GONE else View.VISIBLE)
            view.setInt(R.id.widget_status, "setMaxLines", if (expired) 10 else 3)
            view.setLocalizedText(R.id.widget_status, if (expired) Trial.MESSAGE else if (store.demo) "Dane przykładowe · bez połączenia z autem" else store.message)
            val items = RemoteViews.RemoteCollectionItems.Builder().setHasStableIds(true).setViewTypeCount(1)
            pairs.forEachIndexed { index, pair ->
                val pairView = RemoteViews(context.packageName, R.layout.widget_pair)
                pairView.removeAllViews(R.id.field_left)
                pairView.removeAllViews(R.id.field_right)
                pairView.setViewVisibility(R.id.field_right, if (pair.size == 1) View.GONE else View.VISIBLE)
                pair.forEachIndexed { side, field ->
                    val row = tile(context, store, field, pair.maxOf { height(it) })
                    pairView.addView(if (side == 0) R.id.field_left else R.id.field_right, row)
                }
                // Measure the actual text/layout (including font scaling), not just nominal row minima.
                val measured=pairView.apply(context,null)
                measured.measure(View.MeasureSpec.makeMeasureSpec(((size.width-20).coerceAtLeast(1f)*context.resources.displayMetrics.density).toInt(),View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(0,View.MeasureSpec.UNSPECIFIED))
                contentHeight+=measured.measuredHeight/context.resources.displayMetrics.density+5
                items.addItem(index.toLong(), pairView)
            }
            view.setViewLayoutHeight(R.id.widget_fields,minOf(contentHeight,available),android.util.TypedValue.COMPLEX_UNIT_DIP)
            view.setRemoteAdapter(R.id.widget_fields, items.build())
            view.setViewVisibility(R.id.widget_empty, if (store.selectedFields.isEmpty()) View.VISIBLE else View.GONE)
            val refresh = store.flag("showRefresh", true)
            val heat = store.flag("showHeat", true)
            view.setViewVisibility(R.id.widget_refresh, if (refresh) View.VISIBLE else View.GONE)
            view.setViewLayoutMargin(R.id.widget_heat, RemoteViews.MARGIN_START, if (refresh) 5f else 0f, android.util.TypedValue.COMPLEX_UNIT_DIP)
            view.setViewVisibility(R.id.widget_heat, if (heat) View.VISIBLE else View.GONE)
            view.setViewVisibility(R.id.widget_actions, if (refresh || heat) View.VISIBLE else View.GONE)
            view.setOnClickPendingIntent(R.id.widget_refresh, PendingIntent.getBroadcast(context, id, Intent(context, RefreshReceiver::class.java).setAction("pl.art.mg4widget.REFRESH").setData(android.net.Uri.parse("mg4widget://refresh/$id")), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            view.setOnClickPendingIntent(R.id.widget_heat, if (expired) open else PendingIntent.getActivity(context, id, Intent(context, ClimateActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE))
            return view
        }
        val manager = AppWidgetManager.getInstance(context)
        val options = manager.getAppWidgetOptions(id)
        @Suppress("DEPRECATION")
        val sizes = options.getParcelableArrayList<SizeF>(AppWidgetManager.OPTION_APPWIDGET_SIZES)?.takeIf { it.isNotEmpty() }
            ?: arrayListOf(SizeF(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH,320).toFloat(), options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT,480).toFloat()),
                SizeF(options.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH,320).toFloat(), options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT,480).toFloat()))
        manager.updateAppWidget(id,RemoteViews(sizes.distinct().take(16).associateWith { layout(it) }))
    }
}
