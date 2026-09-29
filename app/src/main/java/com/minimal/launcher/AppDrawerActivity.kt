package com.minimal.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Cajon de apps: lista A-Z, buscador e indice lateral. Tambien sirve de selector.
 *  Incluye búsqueda universal: apps + ajustes + web. */
class AppDrawerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PICK = "pick"
        const val EXTRA_PKG = "pkg"
        const val EXTRA_CLS = "cls"
        const val EXTRA_LABEL = "label"
    }

    private lateinit var appAdapter: AppListAdapter
    private lateinit var searchAdapter: SearchResultsAdapter
    private lateinit var layoutManager: LinearLayoutManager
    private var allApps: List<AppInfo> = emptyList()
    private var isPickMode = false

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drawer)
        isPickMode = intent.getBooleanExtra(EXTRA_PICK, false)

        val root = findViewById<GestureFrameLayout>(R.id.drawer_root)
        root.horizontalOnly = true
        root.onSwipe = { direction -> if (direction == "right") finish() }

        val recycler = findViewById<RecyclerView>(R.id.app_list)
        val search = findViewById<EditText>(R.id.search)
        val sideIndex = findViewById<SideIndexView>(R.id.side_index)

        layoutManager = LinearLayoutManager(this)
        recycler.layoutManager = layoutManager
        recycler.setHasFixedSize(true)

        // Adapter de apps (lista normal)
        appAdapter = AppListAdapter(
            items = emptyList(),
            onClick = { onAppSelected(it) },
            onLongClick = { showAppOptions(it) }
        )

        // Adapter de búsqueda universal
        searchAdapter = SearchResultsAdapter(
            items = emptyList(),
            onClick = { result -> UniversalSearch.launch(this, result) }
        )

        recycler.adapter = appAdapter

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                onSearchChanged(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sideIndex.onLetter = { letter ->
            val pos = appAdapter.indexForLetter(letter)
            if (pos >= 0) layoutManager.scrollToPositionWithOffset(pos, 0)
        }

        loadAppsAsync()
    }

    /** Carga las apps en un hilo IO usando Coroutines. */
    private fun loadAppsAsync() {
        lifecycleScope.launch {
            val apps = withContext(Dispatchers.IO) {
                AppsRepository.getApps(this@AppDrawerActivity)
            }
            allApps = apps
            appAdapter.submit(apps)
        }
    }

    /** Cambio en el buscador: si hay query, muestra búsqueda universal; si no, la lista de apps. */
    private fun onSearchChanged(query: String) {
        val q = query.trim()
        if (q.isEmpty() || isPickMode) {
            // Lista normal de apps
            recycler()?.adapter = appAdapter
            sideIndex()?.visibility = View.VISIBLE
            filterApps(q)
        } else {
            // Búsqueda universal (apps + ajustes + web)
            lifecycleScope.launch {
                val results = withContext(Dispatchers.IO) {
                    UniversalSearch.search(this@AppDrawerActivity, q)
                }
                searchAdapter.submit(results)
                recycler()?.adapter = searchAdapter
                sideIndex()?.visibility = View.GONE
            }
        }
    }

    private fun filterApps(query: String) {
        val q = query.trim().lowercase()
        val result = if (q.isEmpty()) allApps
        else allApps.filter { it.label.lowercase().contains(q) }
        appAdapter.submit(result)
    }

    private fun recycler(): RecyclerView? = findViewById(R.id.app_list)
    private fun sideIndex(): SideIndexView? = findViewById(R.id.side_index)

    private fun onAppSelected(app: AppInfo) {
        if (isPickMode) {
            val data = Intent()
                .putExtra(EXTRA_PKG, app.pkg)
                .putExtra(EXTRA_CLS, app.cls)
                .putExtra(EXTRA_LABEL, app.label)
            setResult(RESULT_OK, data)
            finish()
        } else {
            try {
                startActivity(AppsRepository.launchIntent(app.pkg, app.cls))
            } catch (e: Exception) {
                Toast.makeText(this, "No se pudo abrir la app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Long-press en una app del drawer: opciones de acción. */
    private fun showAppOptions(app: AppInfo) {
        val options = arrayOf(
            getString(R.string.app_open),
            getString(R.string.app_uninstall),
            getString(R.string.app_info)
        )
        AlertDialog.Builder(this, R.style.Theme_MinimalDialog)
            .setTitle(app.label)
            .setItems(options) { _, which ->
                when (options[which]) {
                    getString(R.string.app_open) -> {
                        try {
                            startActivity(AppsRepository.launchIntent(app.pkg, app.cls))
                        } catch (_: Exception) {
                            Toast.makeText(this, "No se pudo abrir", Toast.LENGTH_SHORT).show()
                        }
                    }
                    getString(R.string.app_uninstall) -> {
                        val uri = android.net.Uri.parse("package:${app.pkg}")
                        val intent = Intent(Intent.ACTION_DELETE, uri)
                        try {
                            startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(this, "No se pudo desinstalar", Toast.LENGTH_SHORT).show()
                        }
                    }
                    getString(R.string.app_info) -> {
                        val uri = android.net.Uri.parse("package:${app.pkg}")
                        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri)
                        try {
                            startActivity(intent)
                        } catch (_: Exception) {
                            Toast.makeText(this, "No se pudo abrir", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            .show()
    }
}
