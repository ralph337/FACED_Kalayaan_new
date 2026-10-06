package com.mswdo.kalayaan.faced.ui.faced

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.*
import com.mswdo.kalayaan.faced.data.repository.*
import com.mswdo.kalayaan.faced.util.DateUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

enum class SortMode { NAME_ASC, DATE_DESC, BARANGAY }

/** One family (grouped by head name + barangay) and every FACED form ever filed for them. */
data class FamilyGroup(
    val headName: String,
    val barangay: String,
    val filings: List<FacedRecordEntity>
) {
    val latest: FacedRecordEntity get() = filings.first() // filings are newest-first
    val filingCount: Int get() = filings.size
    val groupKey: String get() = "${headName.lowercase()}|${barangay.lowercase()}"
}

data class FacedListUiState(
    val query: String = "",
    val statusFilter: EvacuationStatus? = null,
    val barangayFilter: String? = null,
    val sortMode: SortMode = SortMode.DATE_DESC,
    val groups: List<FamilyGroup> = emptyList()
)

/**
 * Change #4: the FACED tab no longer lists one card per filing — every filing for the
 * same head-of-family + barangay is compiled into a single family group. Tapping a
 * family opens the list of all its filings (FacedFamilyViewModel/FamilyRecordsScreen),
 * and tapping a filing there opens the usual full FacedDetailScreen.
 */
class FacedListViewModel(private val repo: FacedRepository) : ViewModel() {
    private val filters = MutableStateFlow(FacedListUiState())

    val uiState: StateFlow<FacedListUiState> = combine(repo.observeAll(), filters) { all, f ->
        val grouped = all
            .groupBy { "${it.headName.trim().lowercase()}|${it.barangay.trim().lowercase()}" }
            .map { (_, filings) ->
                val sorted = filings.sortedByDescending { it.createdAt }
                FamilyGroup(headName = sorted.first().headName, barangay = sorted.first().barangay, filings = sorted)
            }

        var list = grouped
        if (f.query.isNotBlank()) {
            val q = f.query.trim().lowercase()
            list = list.filter { g ->
                g.headName.lowercase().contains(q) || g.barangay.lowercase().contains(q) ||
                    g.filings.any { it.serial.lowercase().contains(q) || it.stormName.lowercase().contains(q) || it.disasterEvent.lowercase().contains(q) }
            }
        }
        f.statusFilter?.let { st -> list = list.filter { it.latest.status == st } }
        f.barangayFilter?.let { b -> list = list.filter { it.barangay == b } }
        list = when (f.sortMode) {
            SortMode.NAME_ASC -> list.sortedBy { it.headName }
            SortMode.DATE_DESC -> list.sortedByDescending { it.latest.createdAt }
            SortMode.BARANGAY -> list.sortedBy { it.barangay }
        }
        f.copy(groups = list)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FacedListUiState())

    fun onQuery(q: String) { filters.value = filters.value.copy(query = q) }
    fun onStatusFilter(s: EvacuationStatus?) { filters.value = filters.value.copy(statusFilter = s) }
    fun onBarangayFilter(b: String?) { filters.value = filters.value.copy(barangayFilter = b) }
    fun onSort(mode: SortMode) { filters.value = filters.value.copy(sortMode = mode) }
}

/** Lists every filing for a single family (Change #4). */
class FacedFamilyViewModel(
    repo: FacedRepository,
    headName: String,
    barangay: String
) : ViewModel() {
    val filings: StateFlow<List<FacedRecordEntity>> =
        repo.observeForFamily(headName, barangay).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}

// ---------------- Filing / Edit wizard ----------------

data class MemberDraft(
    val name: String = "",
    val relation: String = "",
    val sex: String = "Male",
    val birthDate: String = DateUtils.today(),
    val education: String = "None",
    val occupation: String = "",
    val remarks: String = ""
)

data class FacedFormState(
    val serial: String = "",
    val headName: String = "",
    val barangay: String = "San Juan",
    val evacCenterName: String = "",
    val disasterEvent: String = "Typhoon / Severe Storm",
    val stormName: String = "",
    val civilStatus: String = "Single",
    val sex: String = "Male",
    val birthDate: String = DateUtils.today(),
    val birthPlace: String = "",
    val occupation: String = "",
    val religion: String = "",
    val monthlyIncome: String = "No Income",
    val idType: String = "PhilID",
    val idNumber: String = "",
    val contactNumber: String = "",
    val altContactNumber: String = "",
    val mothersMaidenName: String = "",
    val permanentAddress: String = "",
    val is4Ps: Boolean = false,
    val shelterDamage: ShelterDamage = ShelterDamage.NONE,
    val members: List<MemberDraft> = emptyList(),
    val vulnerableTags: List<VulnerableTagEntity> = emptyList(),
    val isEditing: Boolean = false,
    val isHeadNameLocked: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null,
    // Change #4: duplicate handling now offers a real choice instead of just blocking.
    val duplicateSerial: String? = null,
    val duplicateHeadName: String? = null,
    val duplicateFilingCount: Int = 0,
    val savedSerial: String? = null
)

class FacedFormViewModel(
    private val repo: FacedRepository,
    private val currentUserId: Long
) : ViewModel() {
    private val _state = MutableStateFlow(FacedFormState())
    val state: StateFlow<FacedFormState> = _state.asStateFlow()

    fun update(transform: (FacedFormState) -> FacedFormState) {
        val newState = transform(_state.value).copy(error = null)
        _state.value = recalculateAutoTags(newState)
    }

    fun addMember() { update { it.copy(members = it.members + MemberDraft()) } }
    fun removeMember(index: Int) { update { it.copy(members = it.members.filterIndexed { i, _ -> i != index }) } }
    fun updateMember(index: Int, member: MemberDraft) {
        update { s -> s.copy(members = s.members.mapIndexed { i, m -> if (i == index) member else m }) }
    }

    private fun recalculateAutoTags(s: FacedFormState): FacedFormState {
        val manualTags = s.vulnerableTags.filterNot { 
            it.vulnerabilityType == "Senior Citizen (60+)" || it.vulnerabilityType == "Child/Minor"
        }
        val autoTags = mutableListOf<VulnerableTagEntity>()

        // Check Head of Family
        if (s.headName.isNotBlank() && s.birthDate.isNotBlank()) {
            val age = DateUtils.ageFromBirthDate(s.birthDate)
            if (age >= 60) autoTags.add(VulnerableTagEntity(facedSerial = s.serial, personName = s.headName, vulnerabilityType = "Senior Citizen (60+)"))
            if (age < 18) autoTags.add(VulnerableTagEntity(facedSerial = s.serial, personName = s.headName, vulnerabilityType = "Child/Minor"))
        }

        // Check Members
        s.members.forEach { m ->
            if (m.name.isNotBlank() && m.birthDate.isNotBlank()) {
                val age = DateUtils.ageFromBirthDate(m.birthDate)
                if (age >= 60) autoTags.add(VulnerableTagEntity(facedSerial = s.serial, personName = m.name, vulnerabilityType = "Senior Citizen (60+)"))
                if (age < 18) autoTags.add(VulnerableTagEntity(facedSerial = s.serial, personName = m.name, vulnerabilityType = "Child/Minor"))
            }
        }

        return s.copy(vulnerableTags = manualTags + autoTags)
    }

    fun toggleVulnerable(person: String, type: String) {
        val s = _state.value
        val exists = s.vulnerableTags.any { it.personName == person && it.vulnerabilityType == type }
        val newTags = if (exists) s.vulnerableTags.filterNot { it.personName == person && it.vulnerabilityType == type }
        else s.vulnerableTags + VulnerableTagEntity(facedSerial = s.serial, personName = person, vulnerabilityType = type)
        _state.value = s.copy(vulnerableTags = newTags)
    }

    fun loadForEdit(full: FacedFullRecord) {
        val r = full.record
        _state.value = FacedFormState(
            serial = r.serial, headName = r.headName, barangay = r.barangay,
            evacCenterName = r.evacCenterName, disasterEvent = r.disasterEvent,
            stormName = r.stormName,
            civilStatus = r.civilStatus, sex = r.sex, birthDate = r.birthDate,
            birthPlace = r.birthPlace, occupation = r.occupation, religion = r.religion,
            monthlyIncome = r.monthlyIncome.toString(), idType = r.idType, idNumber = r.idNumber,
            contactNumber = r.contactNumber, altContactNumber = r.altContactNumber,
            mothersMaidenName = r.mothersMaidenName, permanentAddress = r.permanentAddress,
            is4Ps = r.is4Ps, shelterDamage = r.shelterDamage,
            members = full.members.map { MemberDraft(it.name, it.relation, it.sex, it.birthDate, it.education, it.occupation, it.remarks) },
            vulnerableTags = full.vulnerableTags,
            isEditing = true, isHeadNameLocked = true
        )
    }

    /** Pre-fills a brand-new filing for an already-known family (used from the family list's "+ New Filing"). */
    fun prefillForExistingFamily(headName: String, barangay: String, nextSerial: String) {
        _state.value = FacedFormState(serial = nextSerial, headName = headName, barangay = barangay, isHeadNameLocked = true)
    }

    fun initNewSerial(nextSerial: String) {
        _state.value = _state.value.copy(serial = nextSerial)
    }

    fun validateStep(step: Int): String? {
        val s = _state.value
        return when (step) {
            0 -> {
                if (s.barangay.isBlank()) "Please select a barangay."
                else if (s.evacCenterName.isBlank()) "Please select an evacuation center."
                else if (s.disasterEvent.isBlank()) "Please select a disaster event."
                else if (s.disasterEvent == "Typhoon / Severe Storm" && s.stormName.isBlank()) "Please enter the storm name."
                else null
            }
            1 -> {
                if (s.headName.isBlank()) "Head of family name is required."
                else if (s.contactNumber.isBlank()) "A primary contact number is required."
                else null
            }
            else -> null
        }
    }

    fun showError(err: String) { _state.value = _state.value.copy(error = err) }
    fun clearError() { _state.value = _state.value.copy(error = null) }

    private fun validate(s: FacedFormState): String? {
        return validateStep(0) ?: validateStep(1)
    }

    fun save() {
        val s = _state.value
        val err = validate(s)
        if (err != null) { _state.value = s.copy(error = err); return }

        viewModelScope.launch {
            _state.value = s.copy(isSaving = true)
            val entity = buildEntity(s)
            val members = buildMembers(s)
            when (val result = repo.trySave(entity, members, s.vulnerableTags, allowDuplicate = s.isEditing)) {
                is SaveFacedResult.DuplicateWarning -> {
                    _state.value = _state.value.copy(
                        isSaving = false,
                        duplicateSerial = result.existingSerial,
                        duplicateHeadName = result.existingHeadName,
                        duplicateFilingCount = result.filingCount
                    )
                }
                is SaveFacedResult.Success -> {
                    _state.value = _state.value.copy(isSaving = false, savedSerial = result.serial)
                }
            }
        }
    }

    /** Change #4, option A: overwrite the matched existing record instead of creating a new one. Head name stays locked. */
    fun updateExistingInstead() {
        val s = _state.value
        val existingSerial = s.duplicateSerial ?: return
        _state.value = s.copy(
            serial = existingSerial,
            headName = s.duplicateHeadName ?: s.headName,
            isEditing = true,
            isHeadNameLocked = true,
            duplicateSerial = null, duplicateHeadName = null, duplicateFilingCount = 0
        )
    }

    fun dismissDuplicateDialog() {
        _state.value = _state.value.copy(duplicateSerial = null, duplicateHeadName = null, duplicateFilingCount = 0)
    }

    private fun buildEntity(s: FacedFormState) = FacedRecordEntity(
        serial = s.serial, headName = s.headName.trim(), barangay = s.barangay,
        evacCenterName = s.evacCenterName, disasterEvent = s.disasterEvent,
        stormName = s.stormName.trim(),
        civilStatus = s.civilStatus, sex = s.sex, birthDate = s.birthDate,
        age = com.mswdo.kalayaan.faced.util.DateUtils.ageFromBirthDate(s.birthDate),
        birthPlace = s.birthPlace, occupation = s.occupation, religion = s.religion,
        monthlyIncome = s.monthlyIncome,
        idType = s.idType, idNumber = s.idNumber, contactNumber = s.contactNumber,
        altContactNumber = s.altContactNumber, mothersMaidenName = s.mothersMaidenName,
        permanentAddress = s.permanentAddress, is4Ps = s.is4Ps, shelterDamage = s.shelterDamage,
        presentMembersCount = 1 + s.members.size,
        filedByUserId = currentUserId
    )

    private fun buildMembers(s: FacedFormState) = s.members.filter { it.name.isNotBlank() }.map {
        val age = com.mswdo.kalayaan.faced.util.DateUtils.ageFromBirthDate(it.birthDate)
        HouseholdMemberEntity(
            facedSerial = s.serial, name = it.name.trim(), relation = it.relation,
            sex = it.sex, birthDate = it.birthDate, age = age,
            ageCategory = com.mswdo.kalayaan.faced.util.DateUtils.ageCategory(age),
            education = it.education, occupation = it.occupation, remarks = it.remarks
        )
    }
}
