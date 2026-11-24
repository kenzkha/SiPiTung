package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ActivityMode
import com.example.data.AvatarShopItem
import com.example.data.ChildProfile
import com.example.data.LearningSession
import com.example.data.MathTopic
import com.example.data.ParentReward
import com.example.data.PintarHitungRepository
import com.example.data.RewardCatalog
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class MainTab {
    BERANDA,
    BELAJAR,
    MAIN,
    ORANG_TUA
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class PintarHitungViewModel(
    private val repository: PintarHitungRepository,
    private val currentUserId: String,
    defaultDisplayName: String
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.BERANDA)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _selectedTopic = MutableStateFlow(MathTopic.PENGURANGAN)
    val selectedTopic: StateFlow<MathTopic> = _selectedTopic.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isMutating = MutableStateFlow(false)
    val isMutating: StateFlow<Boolean> = _isMutating.asStateFlow()

    val profileState: StateFlow<UiState<ChildProfile>> = repository.observeProfile(currentUserId)
        .map<ChildProfile?, UiState<ChildProfile>> { profile ->
            if (profile != null) {
                UiState.Success(profile)
            } else {
                UiState.Loading
            }
        }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing child profile", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat profil belajar."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val sessionsState: StateFlow<UiState<List<LearningSession>>> = repository.observeSessions(currentUserId)
        .map<List<LearningSession>, UiState<List<LearningSession>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing learning sessions", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat riwayat belajar."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    val rewardsState: StateFlow<UiState<List<ParentReward>>> = repository.observeRewards(currentUserId)
        .map<List<ParentReward>, UiState<List<ParentReward>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing rewards", rootError)
            emit(UiState.Error(rootError.localizedMessage ?: "Gagal memuat daftar hadiah."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = UiState.Loading
        )

    init {
        viewModelScope.launch {
            val firstName = defaultDisplayName.trim().split(" ").firstOrNull().orEmpty()
                .ifBlank { "Adik Pintar" }
            val result = repository.ensureProfileInitialized(currentUserId, firstName)
            result.onFailure { err ->
                _snackbarMessage.value = "Gagal menyiapkan profil cloud: ${err.localizedMessage ?: "coba lagi"}"
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun selectTopic(topic: MathTopic) {
        _selectedTopic.value = topic
    }

    fun openWorksheetWithTopic(topic: MathTopic) {
        _selectedTopic.value = topic
        _selectedTab.value = MainTab.BELAJAR
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun updateGrade(grade: Int) {
        val current = (profileState.value as? UiState.Success)?.data ?: return
        if (current.gradeInt == grade) return
        updateProfileSettings(current.childName, grade)
    }

    fun updateProfileSettings(childName: String, grade: Int) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.updateChildSettings(currentUserId, childName, grade)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Profil diperbarui: $childName • Kelas $grade SD"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal memperbarui profil."
            }
        }
    }

    fun recordSession(
        mode: ActivityMode,
        topic: MathTopic,
        grade: Int,
        totalQuestions: Int,
        correctAnswers: Int,
        starsEarned: Int,
        durationSeconds: Int
    ) {
        val currentProfile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            val result = repository.recordCompletedSession(
                userId = currentUserId,
                currentProfile = currentProfile,
                mode = mode,
                topic = topic,
                grade = grade,
                totalQuestions = totalQuestions,
                correctAnswers = correctAnswers,
                starsEarned = starsEarned,
                durationSeconds = durationSeconds
            )
            result.onSuccess { newlyUnlocked ->
                if (newlyUnlocked.isNotEmpty()) {
                    val badgeTitle = RewardCatalog.badges
                        .firstOrNull { it.id == newlyUnlocked.first() }?.title ?: "Lencana Baru"
                    _snackbarMessage.value = "Hebat! +$starsEarned Bintang & Lencana \"$badgeTitle\" terbuka!"
                } else {
                    _snackbarMessage.value = "Progres tersimpan real-time! +$starsEarned Bintang!"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menyimpan progres sesi."
            }
        }
    }

    fun purchaseOrEquipAvatar(item: AvatarShopItem) {
        val currentProfile = (profileState.value as? UiState.Success)?.data ?: return
        val owned = item.id in currentProfile.ownedAvatarItems
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.purchaseOrEquipAvatar(currentUserId, currentProfile, item)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = if (owned) {
                    "${item.title} dipakai sebagai lencana profil!"
                } else {
                    "Yeay! ${item.title} berhasil dibuka!"
                }
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menukar bintang."
            }
        }
    }

    fun createParentReward(title: String, starCost: Int, iconKey: String) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.createParentReward(currentUserId, title, starCost, iconKey)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Hadiah \"$title\" ($starCost bintang) ditambahkan!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menambahkan hadiah."
            }
        }
    }

    fun redeemParentReward(reward: ParentReward) {
        val currentProfile = (profileState.value as? UiState.Success)?.data ?: return
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.redeemParentReward(currentUserId, currentProfile, reward)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Hadiah \"${reward.title}\" ditukar! Minta orang tua menyetujui di tab Orang Tua."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Bintang belum cukup untuk hadiah ini."
            }
        }
    }

    fun fulfillParentReward(reward: ParentReward) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.fulfillParentReward(currentUserId, reward.id)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Hadiah \"${reward.title}\" ditandai sudah diberikan!"
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal memperbarui status hadiah."
            }
        }
    }

    fun deleteParentReward(reward: ParentReward) {
        viewModelScope.launch {
            _isMutating.value = true
            val result = repository.deleteParentReward(currentUserId, reward.id)
            _isMutating.value = false
            result.onSuccess {
                _snackbarMessage.value = "Hadiah \"${reward.title}\" dihapus."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Gagal menghapus hadiah."
            }
        }
    }

    private companion object {
        const val TAG = "PintarHitungVM"
        const val STOP_TIMEOUT_MILLIS = 5000L
    }
}
