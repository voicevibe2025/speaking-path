package com.example.voicevibe.domain.model

/**
 * Search result types
 */
enum class SearchResultType {
    USER,
    GROUP,
    MATERIAL
}

/**
 * Search filter/tab
 */
enum class SearchFilter {
    ALL,
    USERS,
    GROUPS,
    MATERIALS
}

/**
 * Simplified material (topic) for search results
 */
data class SearchMaterial(
    val id: String,
    val title: String,
    val description: String,
    val sequence: Int,
    val unlocked: Boolean,
    val completed: Boolean
)

/**
 * Unified search result item
 */
sealed class SearchResultItem {
    abstract val id: String
    abstract val type: SearchResultType
    
    data class UserResult(
        override val id: String,
        override val type: SearchResultType = SearchResultType.USER,
        val user: UserProfile
    ) : SearchResultItem()
    
    data class GroupResult(
        override val id: String,
        override val type: SearchResultType = SearchResultType.GROUP,
        val group: Group
    ) : SearchResultItem()
    
    data class MaterialResult(
        override val id: String,
        override val type: SearchResultType = SearchResultType.MATERIAL,
        val material: SearchMaterial
    ) : SearchResultItem()
}

/**
 * Unified search response
 */
data class UnifiedSearchResponse(
    val users: List<UserProfile>,
    val groups: List<Group>,
    val materials: List<SearchMaterial>
)
