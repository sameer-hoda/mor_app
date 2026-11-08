package dev.mor.app.data

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.GsonBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/** Persists the backend URL. Default is the AWS backend; the emulator shortcut
 *  http://10.0.2.2:4000 still works if you run a local backend. Change it in
 *  Profile → Backend URL. */
const val DEFAULT_SERVER_URL = "http://15.135.140.52:4000"

class ServerStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("mor", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("server_url", DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
        set(v) { prefs.edit().putString("server_url", v.trim().trimEnd('/')).apply() }

    val apiBase: String get() = baseUrl + "/"
}

fun buildApi(baseUrl: String): MorApi {
    val gson = GsonBuilder().setLenient().create()
    val http = OkHttpClient.Builder()
        .addInterceptor { chain ->
            chain.proceed(chain.request().newBuilder()
                .header("x-user-id", "1").build())
        }
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    return Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(http)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()
        .create(MorApi::class.java)
}

data class Snack(val msg: String, val err: Boolean = false)

/** Shared app state: mirrors web store.jsx (profile/home/snackbar). */
class AppVm : ViewModel() {
    lateinit var store: ServerStore
    private var api: MorApi? = null

    private val _profile = MutableStateFlow<Profile?>(null)
    val profile: StateFlow<Profile?> = _profile.asStateFlow()

    private val _home = MutableStateFlow<Home?>(null)
    val home: StateFlow<Home?> = _home.asStateFlow()

    private val _snack = MutableStateFlow<Snack?>(null)
    val snack: StateFlow<Snack?> = _snack.asStateFlow()

    /** Last scan result (mirrors window.__lastScan) and last log (window.__lastLog). */
    var lastScan: ScanResult? = null
    var lastLog: MealLog? = null
    var pendingSlot: String = "lunch"

    fun init(store: ServerStore) {
        this.store = store
        rebuildApi()
    }

    fun rebuildApi() {
        api = buildApi(store.apiBase)
    }

    fun api(): MorApi = api ?: buildApi(store.apiBase).also { api = it }

    fun showSnack(msg: String, err: Boolean = false) {
        _snack.value = Snack(msg, err)
    }

    fun clearSnack() { _snack.value = null }

    fun refreshHome() {
        viewModelScope.launch {
            try { _home.value = api().home() } catch (_: Exception) { }
        }
    }

    fun refreshProfile(onDone: (Profile?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val p = api().profile()
                _profile.value = p
                onDone(p)
            } catch (_: Exception) {
                onDone(null)
            }
        }
    }

    fun saveProfile(patch: Map<String, Any?>, onDone: (Profile?) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val p = api().saveProfile(body = patch)
                _profile.value = p
                onDone(p)
            } catch (e: Exception) {
                showSnack("Save failed: ${e.message}", true)
                onDone(null)
            }
        }
    }
}

fun friendlyErr(e: Exception): String =
    (e.message ?: "network error").replace(Regex("^[^:]+Exception:?\\s*"), "")

val SLOT_ORDER = listOf("breakfast", "lunch", "snack", "dinner")

/** Which meal the user most likely means right now. Rule (explicit): among the
 *  enabled meal times, pick the one nearest to local now by absolute distance;
 *  exact ties prefer the upcoming slot. Used by the logger default AND the Home
 *  nudge so the two always agree. */
fun relevantSlot(
    mealTimes: Map<String, String>?,
    nowMinutes: Int = java.time.LocalTime.now().hour * 60 + java.time.LocalTime.now().minute,
): String {
    val times = SLOT_ORDER.mapNotNull { s ->
        val t = mealTimes?.get(s) ?: return@mapNotNull null
        val p = t.split(":")
        val mins = (p.getOrNull(0)?.toIntOrNull() ?: return@mapNotNull null) * 60 +
            (p.getOrNull(1)?.toIntOrNull() ?: 0)
        s to mins
    }
    if (times.isEmpty()) return "dinner"
    return times.minWith(
        compareBy<Pair<String, Int>> { kotlin.math.abs(it.second - nowMinutes) }
            .thenBy { if (it.second >= nowMinutes) 0 else 1 }
            .thenBy { SLOT_ORDER.indexOf(it.first) },
    ).first
}
