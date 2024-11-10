package tech.noxasch.app_widget

import android.app.Activity
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import androidx.annotation.NonNull
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import android.graphics.BitmapFactory
import android.util.Base64


class AppWidgetMethodCallHandler(private val context: Context, )
    : MethodChannel.MethodCallHandler {

    private var channel: MethodChannel? = null
    private var activity: Activity? = null

    fun open(binaryMessenger: BinaryMessenger) {
        channel = MethodChannel(binaryMessenger, AppWidgetPlugin.CHANNEL)
        channel!!.setMethodCallHandler(this)
    }

    fun setActivity(_activity: Activity?) {
        activity = _activity
    }

    fun close() {
        if (channel == null) return

        channel!!.setMethodCallHandler(null)
        channel = null
        activity = null
    }

    fun handleConfigureIntent(intent: Intent): Boolean {
        val widgetId = intent.extras!!.getInt("widgetId")
        val layoutId = intent.extras!!.getInt("layoutId")
        val layoutName = intent.extras!!.getString("layoutName")
        channel!!.invokeMethod(AppWidgetPlugin.ON_CONFIGURE_WIDGET_CALLBACK,
                mapOf(
                        "widgetId" to widgetId,
                        "layoutId" to layoutId,
                        "layoutName" to layoutName
                )
        )
        return true
    }


    fun handleClickIntent(intent: Intent): Boolean {
        val payload = intent.extras?.getString(AppWidgetPlugin.EXTRA_PAYLOAD)

        channel!!.invokeMethod(AppWidgetPlugin.ON_ClICK_WIDGET_CALLBACK,  mapOf(
            "payload" to payload
        ))
        return true
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        when (call.method) {
            "cancelConfigureWidget" -> cancelConfigureWidget(result)
            "configureWidget" -> configureWidget(call, result)
            "getWidgetIds" -> getWidgetIds(call, result)
            "reloadWidgets" -> reloadWidgets(call, result)
            "updateWidget" -> updateWidget(call, result)
            "widgetExist" -> widgetExist(call, result)
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun getWidgetIds(call: MethodCall, result: MethodChannel.Result) {
        val androidPackageName = call.argument<String>("androidPackageName")
            ?: context.packageName
        val widgetProviderName = call.argument<String>("androidProviderName") ?: return result.error(
            "-1",
            "widgetProviderName is required!",
            null
        )

        return try {
            val widgetProviderClass = Class.forName("$androidPackageName.$widgetProviderName")
            val widgetProvider = ComponentName(context, widgetProviderClass)
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = widgetManager.getAppWidgetIds(widgetProvider)

            result.success(widgetIds)
        } catch (exception: Exception) {
            result.error("-2", exception.message, exception)
        }
    }

    private fun cancelConfigureWidget(result: MethodChannel.Result) {
        return try {
            activity!!.setResult(Activity.RESULT_CANCELED)
            result.success(true)
        } catch (exception: Exception) {
            result.error("-2", exception.message, exception)
        }
    }



    /// This should be called when configuring individual widgets
    private fun configureWidget(call: MethodCall, result: MethodChannel.Result) {
        try {
            if (activity == null) {
                result.error("-2", "Not attached to any activity!", null)
                return
            }

            val androidPackageName = call.argument<String>("androidPackageName") ?: context.packageName
            val widgetId = call.argument<Int>("widgetId")
                ?: return result.error("-1", "widgetId is required!", null)
            val layoutId = call.argument<Int>("layoutId")
                ?: return result.error("-1", "layoutId is required!", null)
            val payload = call.argument<String>("payload")
            val url = call.argument<String>("url")
            val base64Image = call.argument<String>("base64Image")
            val targetPackageName = call.argument<String>("targetPackageName")
                ?: return result.error("-3", "targetPackageName is required", null)

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val textViewsMap = call.argument<Map<String, String>>("textViews")

            // Create an intent to open the specified app when the widget is clicked
            val targetIntent = context.packageManager.getLaunchIntentForPackage(targetPackageName)
            if (targetIntent == null) {
                result.error("-3", "Target app not found: $targetPackageName", null)
                return
            }
            targetIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

            // Create a PendingIntent using the target Intent
            val pendingIntent = PendingIntent.getActivity(context, widgetId, targetIntent, PendingIntent.FLAG_UPDATE_CURRENT)

            // Set up RemoteViews and configure the widget
            val views = RemoteViews(context.packageName, layoutId).apply {
                // Set up text views
                textViewsMap?.forEach { (key, value) ->
                    val textViewId = context.resources.getIdentifier(key, "id", context.packageName)
                    if (textViewId == 0) throw Exception("TextView ID $key does not exist!")
                    setTextViewText(textViewId, value)
                    setOnClickPendingIntent(textViewId, pendingIntent) // Set click intent
                }

                // Set up the ImageView with the base64 image, if provided
                if (!base64Image.isNullOrEmpty()) {
                    val decodedBytes = Base64.decode(base64Image, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    val imageViewId = context.resources.getIdentifier("widget_image", "id", context.packageName)
                    if (bitmap != null && imageViewId != 0) {
                        setImageViewBitmap(imageViewId, bitmap)
                    } else {
                        throw Exception("Failed to decode or set image in widget.")
                    }
                    setOnClickPendingIntent(imageViewId, pendingIntent) // Set click intent for the image
                }
            }

            // Update the widget
            appWidgetManager.updateAppWidget(widgetId, views)

            // Confirm widget update
            val resultValue = Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
            activity?.setResult(Activity.RESULT_OK, resultValue)
            activity?.finish()
            result.success(true)
        } catch (exception: Exception) {
            result.error("-2", exception.message, exception)
        }
    }



    private fun widgetExist(call: MethodCall, result: MethodChannel.Result) {
        val widgetId = call.argument<Int>("widgetId") ?: return result.success(false)
        return try {
            val widgetManager = AppWidgetManager.getInstance(context)
            widgetManager.getAppWidgetInfo(widgetId) ?: return result.success(false)

            result.success(true)
        } catch (exception: Exception) {
            result.error("-2", exception.message, exception)
        }
    }

    // This should only be called after the widget has been configure for the first time
    private fun updateWidget(call: MethodCall, result: MethodChannel.Result) {
        return try {
            val androidPackageName = call.argument<String>("androidPackageName")
                ?: context.packageName
            val widgetId = call.argument<Int>("widgetId")
                ?: return result.error("-1", "widgetId is required!", null)
            val layoutId = call.argument<Int>("layoutId")
                    ?: return result.error("-1", "layoutId is required!", null)

            val payload = call.argument<String>("payload")
            val url = call.argument<String>("url")
            val activityClass = Class.forName("${context.packageName}.MainActivity")
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val pendingIntent = createPendingClickIntent(activityClass, widgetId, payload, url)
            val textViewsMap = call.argument<Map<String, String>>("textViews")

            if (textViewsMap != null) {
                val views = RemoteViews(context.packageName, layoutId)

                for ((key, value) in textViewsMap) {
                    val textViewId: Int =
                        context.resources.getIdentifier(key, "id", context.packageName)
                    if (textViewId == 0) throw Exception("Id $key does not exist!")

                    // only work if widget is blank - so we have to clear it first
                    views.setTextViewText(textViewId, "")
                    views.setTextViewText(textViewId, value)
                    views.setOnClickPendingIntent(textViewId, pendingIntent)
                }
                appWidgetManager.partiallyUpdateAppWidget(widgetId, views)
            }

            result.success(true)
        } catch (exception: Exception) {
            result.error("-2", exception.message, exception)
        }
    }

    /// Create click intent on a widget
    ///
    /// when clicked the intent will received by the broadcast AppWidgetBroadcastReceiver
    /// the receiver will expose the click event to dart callback
    ///
    /// by default will use widgetId as requestCode to make sure the intent doesn't replace existing
    /// widget intent.
    /// The callback will return widgetId, itemId (if supplied) and stringUid (if supplied)
    /// This parameters can be use on app side to easily fetch the data from database or API
    /// without storing in sharedPrefs.
    ///
    ///
    private fun createPendingClickIntent(
        activityClass: Class<*>,
        widgetId: Int,
        payload: String?,
        url: String?
    ): PendingIntent {
        val clickIntent = Intent(context, activityClass)
        clickIntent.action = AppWidgetPlugin.CLICK_WIDGET_ACTION
        clickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        clickIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        clickIntent.putExtra(AppWidgetPlugin.EXTRA_PAYLOAD, payload)
        if (url != null) clickIntent.data = (Uri.parse(url))

        var pendingIntentFlag = PendingIntent.FLAG_UPDATE_CURRENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            pendingIntentFlag =  pendingIntentFlag or PendingIntent.FLAG_IMMUTABLE
        }

        return PendingIntent.getActivity(context, widgetId, clickIntent, pendingIntentFlag)
    }

    /// force reload the widget and this will trigger onUpdate in broadcast receiver
    private fun reloadWidgets(call: MethodCall, result: MethodChannel.Result) {
        val androidPackageName = call.argument<String>("androidPackageName")
            ?:  context.packageName
        val widgetProviderName = call.argument<String>("androidProviderName")
            ?: return result.error(
                "-1",
                "widgetProviderName is required!",
                null
            )

        return try {
            val widgetClass = Class.forName("$androidPackageName.$widgetProviderName")
            val widgetProvider = ComponentName(context, widgetClass)
            val widgetManager = AppWidgetManager.getInstance(context)
            val widgetIds = widgetManager.getAppWidgetIds(widgetProvider)

            val reloadIntent = Intent()
            reloadIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            reloadIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds)
            context.sendBroadcast(reloadIntent)
            result.success(true)
        } catch (exception: Exception) {
            result.error("-2", exception.message, exception)
        }
    }

}
