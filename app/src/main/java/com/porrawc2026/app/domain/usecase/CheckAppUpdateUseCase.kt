package com.porrawc2026.app.domain.usecase

import android.content.Context
import com.porrawc2026.app.util.UpdateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

sealed class InstallResult {
    data class Success(val version: String) : InstallResult()
    data class Error(val message: String) : InstallResult()
}

@Singleton
class CheckAppUpdateUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun check(): UpdateManager.UpdateInfo? {
        return UpdateManager.checkForUpdate(context)
    }

    suspend fun install(context: Context): InstallResult {
        return runCatching {
            val info = UpdateManager.checkForUpdate(context)
            when {
                info == null -> InstallResult.Error("Error al comprobar actualizacion. Sin conexion?")
                !info.isNewer -> InstallResult.Error("Ya tienes la ultima version")
                else -> {
                    val ok = UpdateManager.downloadAndInstall(context, info.downloadUrl)
                    if (!ok) InstallResult.Error("Error al descargar la actualizacion")
                    else InstallResult.Success(
                        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "?"
                    )
                }
            }
        }.getOrElse { e ->
            InstallResult.Error("Error al actualizar: ${e.message}")
        }
    }
}
