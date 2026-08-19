package com.pol.memento.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.pol.memento.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GitSyncScreen(viewModel: MainViewModel, onNavigateBack: () -> Unit) {
    val gitRepo = viewModel.gitSettingsRepo
    
    var repoUrl by remember { mutableStateOf(gitRepo.getRepoUrl() ?: "") }
    var username by remember { mutableStateOf(gitRepo.getUsername() ?: "") }
    var pat by remember { mutableStateOf(gitRepo.getPat() ?: "") }
    
    val isConfigured = gitRepo.getRepoUrl() != null && gitRepo.getPat() != null
    
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Sincronizzazione Git", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Cloud Sync Privato ☁️",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Usa Git per fare il backup e sincronizzare le tue note tra telefono e PC. I tuoi dati vengono inviati solo al repository che inserisci qui, offrendoti il controllo e la privacy totale."
                    )
                }
            }

            OutlinedTextField(
                value = repoUrl,
                onValueChange = { repoUrl = it },
                label = { Text("URL Repository (es. https://github.com/user/repo.git)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text("Username") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            OutlinedTextField(
                value = pat,
                onValueChange = { pat = it },
                label = { Text("Personal Access Token (PAT)") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Text(
                text = "Il token verrà criptato con cifratura AES-256 e salvato nel KeyStore Android sicuro.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            var isCloning by remember { mutableStateOf(false) }

            Button(
                onClick = {
                    if (repoUrl.isBlank() || pat.isBlank()) {
                        coroutineScope.launch { snackbarHostState.showSnackbar("Inserisci l'URL del repo e il Token") }
                    } else {
                        isCloning = true
                        coroutineScope.launch {
                            val result = viewModel.syncEngine.cloneRepo(repoUrl.trim(), username.trim(), pat.trim())
                            isCloning = false
                            if (result.isSuccess) {
                                gitRepo.saveGitCredentials(repoUrl.trim(), username.trim(), pat.trim())
                                snackbarHostState.showSnackbar("Repo clonato e credenziali salvate con successo!")
                            } else {
                                snackbarHostState.showSnackbar(result.exceptionOrNull()?.message ?: "Errore durante la clonazione")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCloning
            ) {
                if (isCloning) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clonazione in corso...")
                } else {
                    Text(if (isConfigured) "Aggiorna e Clona" else "Salva e Clona")
                }
            }

            if (isConfigured) {
                Button(
                    onClick = {
                        gitRepo.clearCredentials()
                        repoUrl = ""
                        username = ""
                        pat = ""
                        coroutineScope.launch { snackbarHostState.showSnackbar("Sincronizzazione disconnessa") }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Disconnetti")
                }
            }
        }
    }
}
