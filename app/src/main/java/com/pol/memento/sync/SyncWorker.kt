package com.pol.memento.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.pol.memento.data.GitSettingsRepository

class SyncWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("SyncWorker", "Inizio sincronizzazione Git in background...")
        
        val gitSettings = GitSettingsRepository(context)
        val syncEngine = MarkdownSyncEngine(context, gitSettings)

        // Se le credenziali non ci sono o il repo non è configurato, esci
        if (gitSettings.getRepoUrl().isNullOrBlank() || gitSettings.getPat().isNullOrBlank()) {
            Log.w("SyncWorker", "Nessuna credenziale Git salvata. Sincronizzazione saltata.")
            return Result.success()
        }

        val result = syncEngine.syncAll()

        return if (result.isSuccess) {
            Log.d("SyncWorker", "Sincronizzazione (Pull/Push) completata con successo! Inizio Deserializzazione...")
            
            // Aggiorna DB locale
            val database = com.pol.memento.data.AppDatabase.getDatabase(context)
            syncEngine.syncDatabaseWithFiles(database.noteDao(), database.folderDao())
            
            Log.d("SyncWorker", "Deserializzazione completata.")
            Result.success()
        } else {
            val error = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
            Log.e("SyncWorker", "Sincronizzazione fallita: $error")
            Result.retry() // Riprova più tardi con il backoff
        }
    }
}
