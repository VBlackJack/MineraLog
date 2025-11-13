package net.meshcore.mineralog.ui.screens.home

/**
 * Quick Win #7: Sort options for mineral list
 */
enum class SortOption(val displayName: String, val description: String) {
    NAME_ASC("Name (A-Z)", "Sort alphabetically by name"),
    NAME_DESC("Name (Z-A)", "Sort reverse alphabetically"),
    DATE_NEWEST("Date Added (Newest)", "Show recently added first"),
    DATE_OLDEST("Date Added (Oldest)", "Show oldest items first"),
    GROUP("Group", "Sort by mineral group")
}
