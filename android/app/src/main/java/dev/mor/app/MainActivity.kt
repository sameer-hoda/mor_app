package dev.mor.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.mor.app.data.AppVm
import dev.mor.app.data.ServerStore
import dev.mor.app.data.friendlyErr
import dev.mor.app.ui.AddSheet
import dev.mor.app.ui.CameraScreen
import dev.mor.app.ui.ChallengesScreen
import dev.mor.app.ui.Chandan
import dev.mor.app.ui.FoodItemScreen
import dev.mor.app.ui.HomeScreen
import dev.mor.app.ui.InboxScreen
import dev.mor.app.ui.InsightsScreen
import dev.mor.app.ui.LoggedScreen
import dev.mor.app.ui.MealDetailScreen
import dev.mor.app.ui.MorButton
import dev.mor.app.ui.MorField
import dev.mor.app.ui.MorTheme
import dev.mor.app.ui.MorToast
import dev.mor.app.ui.Muted
import dev.mor.app.ui.Neelu
import dev.mor.app.ui.PoseArt
import dev.mor.app.ui.ProfileScreen
import dev.mor.app.ui.RemindersScreen
import dev.mor.app.ui.ResultScreen
import dev.mor.app.ui.ScanningScreen
import dev.mor.app.ui.SearchScreen
import dev.mor.app.ui.StreakScreen
import dev.mor.app.ui.Teal
import dev.mor.app.ui.V2Onboarding
import dev.mor.app.ui.WeightScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder

class MainActivity : ComponentActivity() {
    private val vm: AppVm by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        vm.init(ServerStore(applicationContext))
        lifecycleScope.launch(Dispatchers.Default) { PoseArt.decodeAllAsync(applicationContext) }
        setContent {
            MorTheme {
                // Item 13: designed toast overlay driven straight from the
                // app state (keeps the error flag the snackbar host dropped).
                val snack by vm.snack.collectAsState()
                Box(Modifier.fillMaxSize()) {
                    AppNav(vm)
                    snack?.let { s ->
                        LaunchedEffect(s) {
                            kotlinx.coroutines.delay(2800)
                            vm.clearSnack()
                        }
                        Box(
                            Modifier.fillMaxSize().navigationBarsPadding()
                                .padding(bottom = 96.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            MorToast(s.msg, s.err) { vm.clearSnack() }
                        }
                    }
                }
            }
        }
    }
}

private fun dec(s: String): String = try {
    URLDecoder.decode(s, "UTF-8")
} catch (_: Exception) { s }

@Composable
fun AppNav(vm: AppVm) {
    val nav = rememberNavController()
    NavHost(nav, startDestination = "boot") {
        composable("boot") { BootScreen(nav, vm) }
        composable("v2") { V2Onboarding(nav, vm) }
        composable("home") { HomeScreen(nav, vm) }
        composable(
            "add?slot={slot}",
            arguments = listOf(navArgument("slot") { defaultValue = "" }),
        ) { back ->
            // Deep-link target: show home behind the sheet like the web does.
            HomeScreen(nav, vm)
            val arg = back.arguments?.getString("slot") ?: ""
            AddSheet(nav, vm, initialSlot = arg.ifEmpty { null }) { nav.popBackStack() }
        }
        composable(
            "camera?slot={slot}&gallery={gallery}",
            arguments = listOf(
                navArgument("slot") { defaultValue = "lunch" },
                navArgument("gallery") { defaultValue = 0 },
            ),
        ) { back ->
            CameraScreen(nav, vm,
                back.arguments?.getString("slot") ?: "lunch",
                (back.arguments?.getInt("gallery") ?: 0) == 1)
        }
        composable("scanning") { ScanningScreen(nav, vm) }
        composable("result/{scanId}", arguments = listOf(navArgument("scanId") { type = NavType.LongType })) {
            ResultScreen(nav, vm, it.arguments?.getLong("scanId") ?: 0L)
        }
        composable("logged") { LoggedScreen(nav, vm) }
        composable("meal/{id}", arguments = listOf(navArgument("id") { type = NavType.LongType })) {
            MealDetailScreen(nav, vm, it.arguments?.getLong("id") ?: 0L)
        }
        composable(
            "search?slot={slot}&q={q}&voice={voice}",
            arguments = listOf(
                navArgument("slot") { defaultValue = "dinner" },
                navArgument("q") { defaultValue = "" },
                navArgument("voice") { defaultValue = 0 },
            ),
        ) { back ->
            SearchScreen(nav, vm,
                back.arguments?.getString("slot") ?: "dinner",
                dec(back.arguments?.getString("q") ?: ""),
                (back.arguments?.getInt("voice") ?: 0) == 1)
        }
        composable(
            "food?name={name}&slot={slot}",
            arguments = listOf(
                navArgument("name") { defaultValue = "" },
                navArgument("slot") { defaultValue = "dinner" },
            ),
        ) { back ->
            FoodItemScreen(nav, vm,
                dec(back.arguments?.getString("name") ?: ""),
                back.arguments?.getString("slot") ?: "dinner")
        }
        composable("insights") { InsightsScreen(nav, vm) }
        composable("streak") { StreakScreen(nav, vm) }
        composable("challenges") { ChallengesScreen(nav, vm) }
        composable("weight") { WeightScreen(nav, vm) }
        composable("inbox") { InboxScreen(nav, vm) }
        composable("profile") { ProfileScreen(nav, vm) }
        composable("reminders") { RemindersScreen(nav, vm) }
    }
}

/** Boots every fresh or reset user into the one onboarding journey (v2). */
private fun bootRoute(p: dev.mor.app.data.Profile?): String =
    if (p?.onboarded == 1) "home" else "v2"

/** Loads profile, then routes to home or splash. Server URL editable on failure
 *  (needed for real phones: point at http://<mac-lan-ip>:4000). */
@Composable
fun BootScreen(nav: androidx.navigation.NavController, vm: AppVm) {
    var err by remember { mutableStateOf<String?>(null) }
    var serverText by remember { mutableStateOf(vm.store.baseUrl) }
    LaunchedEffect(serverText) { /* re-run only via key below */ }
    LaunchedEffect(Unit) {
        vm.refreshProfile { p ->
            if (p != null) {
                vm.refreshHome()
                nav.navigate(bootRoute(p)) {
                    popUpTo("boot") { inclusive = true }
                }
            } else err = "unreachable"
        }
    }
    Column(
        Modifier.fillMaxSize().background(Chandan).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        Neelu("wave", 150.dp)
        Spacer(Modifier.height(18.dp))
        if (err == null) {
            CircularProgressIndicator(color = Teal)
            Spacer(Modifier.height(12.dp))
            Muted("Waking Neelu…")
        } else {
            Muted("Can't reach the Mor server at ${vm.store.baseUrl}. " +
                "Check the address (AWS backend) or point it at a local backend instead.")
            Spacer(Modifier.height(12.dp))
            MorField(serverText, { serverText = it }, "http://…:4000")
            Spacer(Modifier.height(12.dp))
            MorButton("Retry") {
                if (serverText.isNotBlank()) {
                    vm.store.baseUrl = serverText
                    vm.rebuildApi()
                }
                err = null
                vm.refreshProfile { p ->
                    if (p != null) {
                        vm.refreshHome()
                        nav.navigate(bootRoute(p)) {
                            popUpTo("boot") { inclusive = true }
                        }
                    } else err = "unreachable"
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.width(220.dp)) {
            androidx.compose.foundation.text.BasicText("Mor v0.1",
                modifier = Modifier.align(Alignment.Center))
        }
    }
}
