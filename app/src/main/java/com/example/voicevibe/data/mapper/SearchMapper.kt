package com.example.voicevibe.data.mapper

import com.example.voicevibe.data.model.SearchMaterialDto
import com.example.voicevibe.data.model.UnifiedSearchResponseDto
import com.example.voicevibe.domain.model.SearchMaterial
import com.example.voicevibe.domain.model.UnifiedSearchResponse

/**
 * Map SearchMaterialDto to domain SearchMaterial
 */
fun SearchMaterialDto.toDomain(): SearchMaterial {
    return SearchMaterial(
        id = this.id,
        title = this.title,
        description = this.description,
        sequence = this.sequence,
        unlocked = this.unlocked,
        completed = this.completed
    )
}

/**
 * Map UnifiedSearchResponseDto to domain UnifiedSearchResponse
 */
fun UnifiedSearchResponseDto.toDomain(): UnifiedSearchResponse {
    return UnifiedSearchResponse(
        users = this.users.map { it.toDomain() },
        groups = this.groups.map { it.toDomain() },
        materials = this.materials.map { it.toDomain() }
    )
}
