package com.example.voicevibe.domain.model

/**
 * Unified search results containing users, groups, and materials
 */
data class UnifiedSearchResults(
    val users: List<UserProfile>,
    val groups: List<Group>,
    val materials: List<Material>
)

/**
 * Material (Speaking Journey Topic) search result
 */
data class Material(
    val id: String,
    val title: String,
    val description: String,
    val sequence: Int,
    val type: String = "topic"
)

/**
 * Search result type enum for categorization
 */
enum class SearchResultType {
    USER,
    GROUP,
    MATERIAL
}

/**
 * Generic search result wrapper for displaying in a unified list
 */
sealed class SearchResult {
    data class UserResult(val user: UserProfile) : SearchResult()
    data class GroupResult(val group: Group) : SearchResult()
    data class MaterialResult(val material: Material) : SearchResult()
}
