package com.reproductor.kiosk

import android.app.Activity
import android.app.AlertDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import android.os.Bundle
import android.text.InputFilter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

/**
 * Settings → Apps: which installed apps are on the home screen, in which order
 * (the first one is the featured button) and with which names.
 */
class AppsActivity : Activity() {
    private lateinit var prefs: Prefs
    private lateinit var list: LinearLayout

    /** Refreshes the list when an app is installed or uninstalled while this screen is open. */
    private val packagesChanged = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) = render()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(24), dp(24), dp(24), dp(24))
        }
        setContentView(ScrollView(this).apply { addView(list) })
    }

    override fun onResume() {
        super.onResume()
        // Back from an Android screen opened from here: restore the normal allow list and lock.
        Kiosk.lockIfEnabled(this)
        registerReceiver(packagesChanged, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addDataScheme("package")
        })
        render()
    }

    override fun onPause() {
        unregisterReceiver(packagesChanged)
        super.onPause()
    }

    private fun save(selection: AppSelection) {
        prefs.selection = selection
        if (Kiosk.isOwner(this)) Kiosk.applyPolicies(this)
        render()
    }

    private fun render() {
        list.removeAllViews()
        val selection = prefs.selection
        list.addView(header(getString(R.string.apps_on_home, AppSelection.MAX)))
        list.addView(note(R.string.apps_featured_hint))
        if (selection.apps.isEmpty()) list.addView(note(R.string.apps_none))
        selection.apps.forEachIndexed { index, app ->
            list.addView(row(app.packageName, app.label, listOf(
                action(R.string.app_up, enabled = index > 0) { save(selection.moveUp(app.packageName)) },
                action(R.string.app_down, enabled = index < selection.apps.lastIndex) { save(selection.moveDown(app.packageName)) },
                action(R.string.app_rename) { askName(app) },
                action(R.string.app_remove) { save(selection.remove(app.packageName)) },
            )))
        }

        storeSection()

        list.addView(header(getString(R.string.apps_installed)))
        if (!selection.canAdd) list.addView(note(R.string.apps_full))
        installedApps()
            .filter { !selection.contains(it.first) }
            .forEach { (packageName, label) ->
                val actions = mutableListOf<View>(
                    action(R.string.app_add, enabled = selection.canAdd) { save(selection.add(packageName, label)) },
                )
                if (AppSelection.canUninstall(packageName, this.packageName, isSystemApp(packageName))) {
                    actions += action(R.string.app_uninstall) { confirmUninstall(packageName, label) }
                }
                list.addView(row(packageName, label, actions))
            }

        list.addView(Button(this).apply {
            setText(R.string.close)
            isAllCaps = false
            setOnClickListener { finish() }
        })
    }

    /** The store button, the store choice when several are installed, or how to get one. */
    private fun storeSection() {
        val store = prefs.resolvedStore
        if (store == null) {
            list.addView(note(R.string.apps_no_store))
            list.addView(Button(this).apply {
                setText(R.string.apps_install_aurora)
                isAllCaps = false
                setOnClickListener { installAurora() }
            })
            return
        }
        val launch = Apps.launchIntent(this, store.packageName) ?: return
        list.addView(Button(this).apply {
            text = getString(R.string.apps_store, store.label)
            isAllCaps = false
            setOnClickListener { startActivity(launch) }
        })
        val installed = Stores.installed { Apps.launchIntent(this, it) != null }
        if (installed.size > 1) {
            list.addView(Button(this).apply {
                text = getString(R.string.apps_store_change, store.label)
                isAllCaps = false
                setOnClickListener { chooseStore(installed, store) }
            })
        }
    }

    /**
     * Opens Aurora's official page in the browser. The browser and Android Settings (for its
     * one-time "install unknown apps" switch) are allowed through the lock until this screen
     * shows again; the system installer is always allowed.
     */
    private fun installAurora() {
        val page = Intent(Intent.ACTION_VIEW, Uri.parse(Stores.AURORA_DOWNLOAD_PAGE))
        if (packageManager.resolveActivity(page, 0) == null) {
            Toast.makeText(this, R.string.apps_no_browser, Toast.LENGTH_LONG).show()
            return
        }
        Kiosk.openAllowingTarget(this, page, alsoAllow = listOf(Stores.ANDROID_SETTINGS))
    }

    private fun chooseStore(installed: List<Store>, current: Store) {
        AlertDialog.Builder(this)
            .setTitle(R.string.apps_store_title)
            .setSingleChoiceItems(installed.map { it.label }.toTypedArray(), installed.indexOf(current)) { dialog, which ->
                prefs.store = installed[which].packageName
                if (Kiosk.isOwner(this)) Kiosk.applyPolicies(this)
                dialog.dismiss()
                render()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    /** Launchable apps the user may offer on the home screen, sorted by name. */
    private fun installedApps(): List<Pair<String, String>> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return packageManager.queryIntentActivities(intent, 0)
            .map { info: ResolveInfo -> info.activityInfo.packageName to info.loadLabel(packageManager).toString() }
            .distinctBy { it.first }
            .filter { AppSelection.isOfferable(it.first, packageName) }
            .sortedBy { it.second.lowercase() }
    }

    private fun isSystemApp(packageName: String): Boolean {
        val flags = runCatching { packageManager.getApplicationInfo(packageName, 0).flags }.getOrDefault(ApplicationInfo.FLAG_SYSTEM)
        return flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
    }

    private fun confirmUninstall(packageName: String, label: String) {
        AlertDialog.Builder(this)
            .setMessage(getString(R.string.uninstall_confirm, label))
            .setPositiveButton(R.string.app_uninstall) { _, _ -> Uninstaller.uninstall(this, packageName, label) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun askName(app: HomeApp) {
        val field = EditText(this).apply {
            setText(app.label)
            setSelection(text.length)
            filters = arrayOf(InputFilter.LengthFilter(24))
            // In landscape the keyboard would otherwise cover the whole dialog.
            imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.app_rename_title)
            .setView(field)
            .setPositiveButton(R.string.ok) { _, _ -> save(prefs.selection.rename(app.packageName, field.text.toString())) }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun row(packageName: String, label: String, actions: List<View>): View {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))
        }
        val icon = ImageView(this).apply {
            runCatching { setImageDrawable(packageManager.getApplicationIcon(packageName)) }
        }
        row.addView(icon, LinearLayout.LayoutParams(dp(40), dp(40)))
        row.addView(TextView(this).apply {
            text = label
            setTextColor(0xFFE0E0E0.toInt())
            textSize = 18f
            setPadding(dp(16), 0, dp(8), 0)
        }, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        actions.forEach { row.addView(it) }
        return row
    }

    private fun action(text: Int, enabled: Boolean = true, onClick: () -> Unit) = Button(this).apply {
        setText(text)
        isAllCaps = false
        isEnabled = enabled
        minWidth = dp(56)
        setOnClickListener { onClick() }
    }

    private fun header(text: String) = TextView(this).apply {
        this.text = text
        setTextColor(0xFFE0E0E0.toInt())
        textSize = 22f
        setPadding(0, dp(16), 0, dp(4))
    }

    private fun note(text: Int) = TextView(this).apply {
        setText(text)
        setTextColor(0xFF9A9A9A.toInt())
        textSize = 15f
        setPadding(0, 0, 0, dp(8))
    }

    private fun dp(value: Int) =
        TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
}
