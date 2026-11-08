package dev.mor.app.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.LocalDate
import java.time.ZoneId

/** Step-count states. Never shows the backend's seeded sample count as real
 *  activity (items 6, 32): either live Health Connect data or an honest state. */
sealed interface StepsState {
    data object Loading : StepsState
    data class Live(val steps: Long) : StepsState
    /** Provider missing / too old / needs update. */
    data object Unavailable : StepsState
    /** Provider present but READ_STEPS not granted. */
    data object NeedsPermission : StepsState
    /** Read failed (transient). */
    data object Error : StepsState
}

val STEPS_PERMS = setOf(HealthPermission.getReadPermission(StepsRecord::class))

fun stepsAvailability(ctx: Context): StepsState {
    return when (HealthConnectClient.getSdkStatus(ctx)) {
        HealthConnectClient.SDK_AVAILABLE -> StepsState.Loading // caller resolves further
        else -> StepsState.Unavailable
    }
}

suspend fun readTodaySteps(ctx: Context): StepsState {
    if (HealthConnectClient.getSdkStatus(ctx) != HealthConnectClient.SDK_AVAILABLE) {
        return StepsState.Unavailable
    }
    return try {
        val client = HealthConnectClient.getOrCreate(ctx)
        val granted = client.permissionController.getGrantedPermissions()
        if (!granted.containsAll(STEPS_PERMS)) return StepsState.NeedsPermission
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now().atStartOfDay(zone).toInstant()
        val end = java.time.Instant.now()
        val resp = client.readRecords(
            ReadRecordsRequest(StepsRecord::class, TimeRangeFilter.between(start, end)),
        )
        StepsState.Live(resp.records.sumOf { it.count })
    } catch (_: Exception) {
        StepsState.Error
    }
}

/** Permission-request contract for Health Connect's system sheet. */
fun stepsPermissionContract(): androidx.activity.result.contract.ActivityResultContract<Set<String>, Set<String>> =
    PermissionController.createRequestPermissionResultContract()
