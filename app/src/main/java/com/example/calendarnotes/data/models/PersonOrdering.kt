package com.example.calendarnotes.data.models

/**
 * Shared people ordering used across People tab, select-people, event lists, filters, etc.
 *
 * 1. Star statuses first, then everyone else, Not in contact last
 * 2. Within a tier by status rank (Married before Engaged before Exclusive, etc.)
 * 3. Then most recent linked event, then name
 */
object PersonOrdering {
    fun sorted(
        people: List<Person>,
        lastEventByPersonId: Map<Long, Long> = emptyMap(),
        preferStatusOrdinal: Boolean = false
    ): List<Person> {
        return people.sortedWith(
            compareBy<Person> { personTier(it) }
                .thenBy { statusRank(it.status) }
                .thenByDescending { lastEventByPersonId[it.id] ?: Long.MIN_VALUE }
                .thenBy {
                    if (preferStatusOrdinal) it.status.ordinal else 0
                }
                .thenBy { it.name.lowercase() }
        )
    }

    private fun personTier(person: Person): Int = when {
        person.status.showStar -> 0
        person.status == PersonStatus.NOT_IN_CONTACT -> 2
        else -> 1
    }

    private fun statusRank(status: PersonStatus): Int = when (status) {
        PersonStatus.MARRIED -> 0
        PersonStatus.ENGAGED -> 1
        PersonStatus.EXCLUSIVE -> 2
        PersonStatus.DATING -> 3
        PersonStatus.FAMILY -> 4
        PersonStatus.JUST_MET -> 5
        PersonStatus.FRIEND -> 6
        PersonStatus.NOT_IN_CONTACT -> 7
    }
}
