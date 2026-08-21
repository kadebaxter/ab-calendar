package com.example.calendarnotes.data.models

enum class PersonStatus(
    val key: String,
    val label: String,
    val colorHex: String,
    val showStar: Boolean
) {
    NOT_IN_CONTACT("not_in_contact", "Not in contact", "#757575", false),
    JUST_MET("just_met", "Just met / figuring it out", "#FFC107", false),
    FRIEND("friend", "Friend", "#4A90A8", false),
    DATING("dating", "Dating", "#4CAF50", false),
    EXCLUSIVE("exclusive", "Exclusive", "#4CAF50", true),
    ENGAGED("engaged", "Engaged", "#81D4FA", true),
    FAMILY("family", "Family", "#2196F3", false),
    MARRIED("married", "Married", "#2196F3", true);

    companion object {
        fun fromKey(key: String?): PersonStatus {
            return entries.firstOrNull { it.key == key } ?: NOT_IN_CONTACT
        }
    }
}
