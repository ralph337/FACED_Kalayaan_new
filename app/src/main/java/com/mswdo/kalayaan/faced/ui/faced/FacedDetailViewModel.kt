package com.mswdo.kalayaan.faced.ui.faced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.*
import com.mswdo.kalayaan.faced.data.repository.FacedFullRecord
import com.mswdo.kalayaan.faced.data.repository.FacedRepository
import com.mswdo.kalayaan.faced.data.repository.InventoryRepository
import com.mswdo.kalayaan.faced.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AssistanceFormState(
    val date: String = DateUtils.today(),
    val receivingMember: String = "",
    val disaster: String = "Typhoon / Severe Storm",
    val inventoryItemId: Long = 0,
    val itemName: String = "",
    val unit: String = "",
    val qty: String = "1",
    val cost: String = "",
    val provider: String = "MSWDO Kalayaan",
    val authorizingOfficer: String = "",
    val error: String? = null,
    val insufficientStock: Boolean = false
)

class FacedDetailViewModel(
    private val serial: String,
    private val facedRepo: FacedRepository,
    private val inventoryRepo: InventoryRepository
) : ViewModel() {

    val fullRecord: StateFlow<FacedFullRecord?> =
        facedRepo.observeFullRecord(serial).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val inventory: StateFlow<List<InventoryItemEntity>> =
        inventoryRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _assistanceForm = MutableStateFlow(AssistanceFormState())
    val assistanceForm: StateFlow<AssistanceFormState> = _assistanceForm.asStateFlow()

    fun updateAssistanceForm(transform: (AssistanceFormState) -> AssistanceFormState) {
        _assistanceForm.value = transform(_assistanceForm.value).copy(error = null, insufficientStock = false)
    }

    fun toggleStatus(current: EvacuationStatus, presentCount: Int? = null) {
        viewModelScope.launch {
            val newStatus = if (current == EvacuationStatus.IN_CENTER) EvacuationStatus.LEFT_CENTER else EvacuationStatus.IN_CENTER
            facedRepo.updateStatus(serial, newStatus, presentCount)
        }
    }

    fun checkOutAndReturnItems(headName: String, returnedItems: List<String>) {
        viewModelScope.launch {
            if (returnedItems.isNotEmpty()) {
                facedRepo.returnNonConsumableItems(serial, headName, returnedItems)
            }
            facedRepo.updateStatus(serial, EvacuationStatus.LEFT_CENTER, null)
        }
    }

    fun submitAssistance(defaultReceiver: String, onDone: () -> Unit) {
        val f = _assistanceForm.value
        val actualReceiver = f.receivingMember.ifBlank { defaultReceiver }
        val qty = f.qty.toIntOrNull() ?: 0
        if (actualReceiver.isBlank() || f.itemName.isBlank() || qty <= 0 || f.inventoryItemId == 0L) {
            _assistanceForm.value = f.copy(error = "Please complete all required fields.")
            return
        }
        viewModelScope.launch {
            val record = AssistanceRecordEntity(
                facedSerial = serial, date = f.date.ifBlank { com.mswdo.kalayaan.faced.util.DateUtils.today() },
                receivingMember = actualReceiver, disaster = f.disaster, itemName = f.itemName,
                unit = f.unit, qty = qty, estimatedCost = f.cost.toDoubleOrNull() ?: 0.0,
                provider = f.provider, authorizingOfficer = f.authorizingOfficer.ifBlank { "Pending Verification" }
            )
            val ok = facedRepo.recordAssistance(record, f.inventoryItemId)
            if (!ok) {
                _assistanceForm.value = f.copy(insufficientStock = true)
            } else {
                _assistanceForm.value = AssistanceFormState()
                onDone()
            }
        }
    }
}
