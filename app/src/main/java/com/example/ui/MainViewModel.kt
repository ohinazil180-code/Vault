package com.example.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.backup.BackupManager
import com.example.backup.BackupValidationResult
import com.example.data.model.ActivityLogEntity
import com.example.data.model.SecureNoteEntity
import com.example.data.model.SecureSecretEntity
import com.example.data.model.VaultConfigEntity
import com.example.data.model.VaultObjectEntity
import com.example.data.repository.VaultIntegrityReport
import com.example.data.repository.VaultRepository
import com.example.security.VaultKeyManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.ai.AiRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VaultRepository(application)
    val aiRepository = AiRepository(application)

    val config: StateFlow<VaultConfigEntity?> = repository.configFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val isInitialized: StateFlow<Boolean> = repository.configFlow
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val activeObjects: StateFlow<List<VaultObjectEntity>> = repository.activeObjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recycleBinObjects: StateFlow<List<VaultObjectEntity>> = repository.recycleBinObjectsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notes: StateFlow<List<SecureNoteEntity>> = repository.notesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val secrets: StateFlow<List<SecureSecretEntity>> = repository.secretsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activityLogs: StateFlow<List<ActivityLogEntity>> = repository.activityLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val totalStorageBytes: StateFlow<Long?> = repository.totalStorageBytesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    val duplicates: StateFlow<List<VaultObjectEntity>> = repository.duplicatesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _generatedRecoveryCode = MutableStateFlow<String?>(null)
    val generatedRecoveryCode: StateFlow<String?> = _generatedRecoveryCode.asStateFlow()

    private val _viewingDecryptedFile = MutableStateFlow<File?>(null)
    val viewingDecryptedFile: StateFlow<File?> = _viewingDecryptedFile.asStateFlow()

    private val _viewingObject = MutableStateFlow<VaultObjectEntity?>(null)
    val viewingObject: StateFlow<VaultObjectEntity?> = _viewingObject.asStateFlow()

    private val _integrityReport = MutableStateFlow<VaultIntegrityReport?>(null)
    val integrityReport: StateFlow<VaultIntegrityReport?> = _integrityReport.asStateFlow()

    private val _backupStatusMessage = MutableStateFlow<String?>(null)
    val backupStatusMessage: StateFlow<String?> = _backupStatusMessage.asStateFlow()

    private var autoLockJob: Job? = null
    private var lastActivityTime = System.currentTimeMillis()

    init {
        startAutoLockMonitor()
    }

    fun recordUserInteraction() {
        lastActivityTime = System.currentTimeMillis()
    }

    fun handleBackgroundTimeout(elapsedMs: Long) {
        if (_isUnlocked.value) {
            val timeoutSeconds = config.value?.autoLockTimeoutSeconds ?: 60
            if (timeoutSeconds > 0 && elapsedMs >= timeoutSeconds * 1000L) {
                lockVault()
            }
        }
    }

    private fun startAutoLockMonitor() {
        autoLockJob?.cancel()
        autoLockJob = viewModelScope.launch {
            while (true) {
                delay(5000)
                if (_isUnlocked.value) {
                    val timeoutSeconds = config.value?.autoLockTimeoutSeconds ?: 60
                    if (timeoutSeconds > 0) {
                        val elapsedSeconds = (System.currentTimeMillis() - lastActivityTime) / 1000
                        if (elapsedSeconds >= timeoutSeconds) {
                            lockVault()
                        }
                    }
                }
            }
        }
    }

    fun setupNewVault(pin: String, onComplete: () -> Unit) {
        viewModelScope.launch {
            val recoveryCode = repository.initializeVault(pin)
            _generatedRecoveryCode.value = recoveryCode
            _isUnlocked.value = true
            _authError.value = null
            recordUserInteraction()
            onComplete()
        }
    }

    fun unlockWithPin(pin: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val success = repository.unlockWithPin(pin)
            if (success) {
                _isUnlocked.value = true
                recordUserInteraction()
                onSuccess()
            } else {
                _authError.value = "Authentication failed: Invalid PIN"
            }
        }
    }

    fun unlockWithRecoveryCode(code: String, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _authError.value = null
            val success = repository.unlockWithRecoveryCode(code)
            if (success) {
                _isUnlocked.value = true
                recordUserInteraction()
                onSuccess()
            } else {
                _authError.value = "Recovery failed: Invalid recovery credential"
            }
        }
    }

    fun unlockWithBiometrics(onSuccess: () -> Unit) {
        // When BiometricPrompt succeeds, we obtain session unlock
        _isUnlocked.value = true
        _authError.value = null
        recordUserInteraction()
        onSuccess()
    }

    fun lockVault() {
        repository.lockVault()
        _isUnlocked.value = false
        _viewingDecryptedFile.value = null
        _viewingObject.value = null
    }

    fun clearAuthError() {
        _authError.value = null
    }

    fun clearGeneratedRecoveryCode() {
        _generatedRecoveryCode.value = null
    }

    // --- Files ---
    fun importFiles(uris: List<Uri>, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val count = repository.importFiles(uris)
            recordUserInteraction()
            onResult(count)
        }
    }

    fun toggleFavorite(obj: VaultObjectEntity) {
        viewModelScope.launch {
            repository.toggleFavorite(obj.id, obj.isFavorite)
            recordUserInteraction()
        }
    }

    fun renameObject(id: String, newName: String) {
        viewModelScope.launch {
            repository.renameObject(id, newName)
            recordUserInteraction()
        }
    }

    fun moveToTrash(id: String) {
        viewModelScope.launch {
            repository.moveToRecycleBin(id)
            recordUserInteraction()
        }
    }

    fun restoreFromTrash(id: String) {
        viewModelScope.launch {
            repository.restoreFromRecycleBin(id)
            recordUserInteraction()
        }
    }

    fun permanentDelete(id: String) {
        viewModelScope.launch {
            repository.permanentDelete(id)
            recordUserInteraction()
        }
    }

    fun emptyTrash() {
        viewModelScope.launch {
            repository.emptyRecycleBin()
            recordUserInteraction()
        }
    }

    fun openObjectForViewing(obj: VaultObjectEntity) {
        viewModelScope.launch {
            try {
                val file = repository.decryptObjectForViewing(obj)
                _viewingDecryptedFile.value = file
                _viewingObject.value = obj
                recordUserInteraction()
            } catch (e: Exception) {
                _authError.value = "Decryption failed: ${e.message}"
            }
        }
    }

    fun closeViewer() {
        _viewingDecryptedFile.value = null
        _viewingObject.value = null
    }

    // --- Notes ---
    fun saveNote(id: String?, title: String, content: String, category: String, isPinned: Boolean, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.saveNote(id, title, content, category, isPinned)
            recordUserInteraction()
            onDone()
        }
    }

    suspend fun decryptNote(encryptedContentBase64: String): String {
        return repository.decryptNoteContent(encryptedContentBase64)
    }

    fun deleteNote(id: String) {
        viewModelScope.launch {
            repository.deleteNote(id)
            recordUserInteraction()
        }
    }

    // --- Secrets ---
    fun saveSecret(
        id: String?,
        title: String,
        username: String,
        passwordPlain: String,
        websiteUrl: String,
        category: String,
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            repository.saveSecret(id, title, username, passwordPlain, websiteUrl, category, notes)
            recordUserInteraction()
            onDone()
        }
    }

    suspend fun decryptSecretPassword(encryptedPasswordBase64: String): String {
        return repository.decryptSecretPassword(encryptedPasswordBase64)
    }

    fun deleteSecret(id: String) {
        viewModelScope.launch {
            repository.deleteSecret(id)
            recordUserInteraction()
        }
    }

    // --- Settings & Audit ---
    fun changePin(newPin: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.changePin(newPin)
            recordUserInteraction()
            onResult(success)
        }
    }

    fun setAutoLockTimeout(seconds: Int) {
        viewModelScope.launch {
            repository.updateAutoLock(seconds)
            recordUserInteraction()
        }
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateBiometric(enabled)
            recordUserInteraction()
        }
    }

    fun setSecureScreenEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateSecureScreen(enabled)
            recordUserInteraction()
        }
    }

    fun runIntegrityCheck() {
        viewModelScope.launch {
            val report = repository.runIntegrityCheck()
            _integrityReport.value = report
            recordUserInteraction()
        }
    }

    fun clearActivityLogs() {
        viewModelScope.launch {
            repository.clearActivityLogs()
            recordUserInteraction()
        }
    }

    // --- Backup & Restore ---
    fun createBackup(password: String, exportUri: Uri?, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                BackupManager.createBackup(getApplication(), password, exportUri)
                onDone(true, "Encrypted backup archive created successfully")
            } catch (e: Exception) {
                onDone(false, "Backup creation failed: ${e.message}")
            }
        }
    }

    fun restoreBackup(backupUri: Uri, password: String, onDone: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val validation = BackupManager.inspectBackup(getApplication(), backupUri, password)
                if (!validation.isValid) {
                    onDone(false, validation.errorMessage ?: "Backup verification failed")
                    return@launch
                }
                val success = BackupManager.restoreBackup(getApplication(), backupUri, password)
                if (success) {
                    onDone(true, "Successfully restored ${validation.objectCount} files, ${validation.noteCount} notes, and ${validation.secretCount} secrets.")
                } else {
                    onDone(false, "Restore operation failed")
                }
            } catch (e: Exception) {
                onDone(false, "Restore error: ${e.message}")
            }
        }
    }
}
