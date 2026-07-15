package org.medtroniclabs.uhis.ui

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository

/**
 * Common base viewmodel which has support for fetching SS, SK and search trigger.
 */
open class BaseFilterViewModel(
    override var dispatcherIO: CoroutineDispatcher,
    open val metaRepository: MetaRepository,
) : BaseViewModel(dispatcherIO) {
    /**
     * Livedata storing shashthya shebikas
     */
    val shashthyaShebikasLiveData = MutableLiveData<List<ChipViewItemModel>>()

    /**
     * Livedata storing sub villages
     */
    val subVillagesLiveData = MutableLiveData<List<ChipViewItemModel>>()

    /**
     * Search query flow variable
     */
    private val searchQuery = MutableStateFlow<String?>(null)

    /**
     * Fetches shashtya shebikas
     */
    fun getShashtyaShebikas() =
        launch {
            shashthyaShebikasLiveData.postValue(
                metaRepository
                    .getShastyhaShebikas(SecuredPreference.getUserId())
                    .map {
                        val name = if (it.ssId.isNullOrBlank()) {
                            it.name
                        } else {
                            "${it.ssId} - ${it.name}"
                        }
                        ChipViewItemModel(it.id, name = name)
                    },
            )
        }

    /**
     * Shashthya shebikas selected, do some action
     */
    fun onShashtyaShebikaSelected(tags: List<ChipViewItemModel>) {
        getSubVillages(tags.mapNotNull { it.id })
    }

    /**
     * Fetches subvillage based on shashtya shebikas
     */
    private fun getSubVillages(ssIds: List<Long>) =
        launch {
            subVillagesLiveData.postValue(
                metaRepository
                    .getSubVillagesByShasthyaShebikaIds(ssIds)
                    .map {
                        ChipViewItemModel(
                            id = it.id,
                            name = it.name,
                        )
                    },
            )
        }

    /**
     * Search text got changed handle it
     */
    fun onTextChange(searchText: String?) {
        searchQuery.value = searchText
    }

    /**
     * Invoke this to get valid search query
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    protected fun observeSearch(
        debounceMillis: Long = 500L,
        minLength: Int = 3,
        onSearch: suspend (String) -> Unit,
    ): Job =
        searchQuery
            .map { it?.trim() }
            .filterNotNull()
            .distinctUntilChanged()
            .flatMapLatest { query ->
                when {
                    query.isEmpty() -> flowOf("") // reset immediately
                    query.length < minLength -> emptyFlow() // ignore 1-2 chars
                    else -> flow {
                        delay(debounceMillis)
                        emit(query)
                    }
                }
            }.onEach(onSearch)
            .launchIn(viewModelScope)
}
