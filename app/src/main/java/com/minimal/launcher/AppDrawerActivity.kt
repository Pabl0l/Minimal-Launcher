package com.minimal.launcher

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/** Cajon de apps: lista A-Z, buscador e indice lateral. Tambien sirve de selector. */
class AppDrawerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PICK = "pick"
        const val EXTRA_PKG = "pkg"
        const val EXTRA_CLS = "cls"
        const val EXTRA_LABEL = "label"
    }

    private lateinit var adapter: AppListAdapter
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

        adapter = AppListAdapter(
            items = emptyList(),
            onClick = { onAppSelected(it) }
        )
        recycler.adapter = adapter

        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {
                filter(s?.toString().orEmpty())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        sideIndex.onLetter = { letter ->
            val pos = adapter.indexForLetter(letter)
            if (pos >= 0) layoutManager.scrollToPositionWithOffset(pos, 0)
        }

        loadAppsAsync()
    }

    /** Carga las apps en un hilo aparte para no bloquear la UI. */
    private fun loadAppsAsync() {
        Thread {
            val apps = AppsRepository.getApps(this)
            runOnUiThread {
                allApps = apps
                adapter.submit(apps)
            }
        }.start()
    }

    private fun filter(query: String) {
        val q = query.trim().lowercase()
        val result = if (q.isEmpty()) allApps
        else allApps.filter { it.label.lowercase().contains(q) }
        adapter.submit(result)
    }

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
}
