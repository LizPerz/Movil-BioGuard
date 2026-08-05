package com.bioguard.movil.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.bioguard.movil.data.Resource
import com.bioguard.movil.data.repository.SensorRepository
import com.bioguard.movil.network.EventoMetabolicoResponse

class EventsPagingSource(
    private val sensorRepository: SensorRepository,
    private val pacienteId: String
) : PagingSource<Int, EventoMetabolicoResponse>() {

    override fun getRefreshKey(state: PagingState<Int, EventoMetabolicoResponse>): Int? {
        return state.anchorPosition?.let { anchor ->
            state.closestPageToPosition(anchor)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchor)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, EventoMetabolicoResponse> {
        val page = params.key ?: 1
        val pageSize = params.loadSize

        return try {
            val result = sensorRepository.getEventos(pacienteId, 100)
            if (result !is Resource.Success) {
                return LoadResult.Error(Exception("Error al cargar eventos"))
            }
            val events = result.data

            val fromIndex = (page - 1) * pageSize
            val toIndex = minOf(fromIndex + pageSize, events.size)

            val pageItems = if (fromIndex < events.size) {
                events.subList(fromIndex, toIndex)
            } else {
                emptyList()
            }

            LoadResult.Page(
                data = pageItems,
                prevKey = if (page == 1) null else page - 1,
                nextKey = if (toIndex >= events.size) null else page + 1
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
