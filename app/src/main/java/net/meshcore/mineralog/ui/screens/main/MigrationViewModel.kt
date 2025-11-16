package net.meshcore.mineralog.ui.screens.main

import androidx.lifecycle.ViewModel
import net.meshcore.mineralog.util.AppLogger
import androidx.lifecycle.viewModelScope
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.flow.StateFlow
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.flow.asStateFlow
import net.meshcore.mineralog.util.AppLogger
import kotlinx.coroutines.launch
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.data.migration.AutoReferenceCreator
import net.meshcore.mineralog.util.AppLogger
import net.meshcore.mineralog.data.migration.MigrationReport
import net.meshcore.mineralog.util.AppLogger

/**
 * ViewModel for managing automatic reference mineral migration.
 *
 * Handles:
 * - Checking if migration is needed
 * - Running migration in background
 * - Managing migration report dialog state
 */
class MigrationViewModel(
    private val autoReferenceCreator: AutoReferenceCreator
) : ViewModel() {

    private val _migrationReport = MutableStateFlow<MigrationReport?>(null)
    val migrationReport: StateFlow<MigrationReport?> = _migrationReport.asStateFlow()

    private val _showMigrationDialog = MutableStateFlow(false)
    val showMigrationDialog: StateFlow<Boolean> = _showMigrationDialog.asStateFlow()

    /**
     * Check if migration is needed and run it if necessary.
     * Called once at app startup.
     */
    fun checkAndRunMigration() {
        if (!autoReferenceCreator.isMigrationDone()) {
            viewModelScope.launch {
                try {
                    val report = autoReferenceCreator.run()

                    // Only show dialog if something was migrated or there were divergent minerals
                    if (report.referencesCreated > 0 ||
                        report.simpleSpecimensLinked > 0 ||
                        report.componentsLinked > 0 ||
                        report.divergentMinerals.isNotEmpty()
                    ) {
                        _migrationReport.value = report
                        _showMigrationDialog.value = true
                    }
                } catch (e: Exception) {
                    AppLogger.e("MigrationViewModel", "Migration failed", e)
                }
            }
        }
    }

    /**
     * Dismiss the migration report dialog.
     */
    fun dismissMigrationDialog() {
        _showMigrationDialog.value = false
    }

    /**
     * Get the current migration report.
     */
    fun getMigrationReport(): MigrationReport? = _migrationReport.value
}
