package com.bioguard.movil.data

import com.bioguard.movil.domain.model.DomainError

sealed class Resource<out T> {
    data class Success<T>(val data: T) : Resource<T>()
    data class Error(
        val message: String,
        val code: Int? = null,
        val domainError: DomainError? = null
    ) : Resource<Nothing>()
    data object Loading : Resource<Nothing>()
}

