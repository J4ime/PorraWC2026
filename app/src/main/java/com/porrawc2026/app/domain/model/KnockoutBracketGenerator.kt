package com.porrawc2026.app.domain.model

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.data.remote.MatchScheduleProvider

data class GroupStanding(
    val teamName: String,
    val points: Int,
    val goalDifference: Int,
    val goalsFor: Int,
    val position: Int
)

data class ThirdPlaceTeam(
    val teamName: String,
    val group: String,
    val points: Int,
    val goalDifference: Int,
    val goalsFor: Int,
    val rank: Int
)

object KnockoutBracketGenerator {

    private val groups = listOf("A","B","C","D","E","F","G","H","I","J","K","L")

    data class DieciseisavoSlot(
        val matchId: Int,
        val homeType: SlotTeam,  // e.g., 1st of group X
        val awayType: SlotTeam   // e.g., 2nd of group Y or 3rd from specific groups
    )

    sealed class SlotTeam {
        data class GroupPosition(val position: Int, val group: String) : SlotTeam()
        data class ThirdParty(val eligibleGroups: List<String>) : SlotTeam()
    }

    private val dieciseisavoSlots = listOf(
        DieciseisavoSlot(73, SlotTeam.GroupPosition(2, "A"), SlotTeam.GroupPosition(2, "B")),
        DieciseisavoSlot(74, SlotTeam.GroupPosition(1, "E"), SlotTeam.ThirdParty(listOf("A","B","C","D","F"))),
        DieciseisavoSlot(75, SlotTeam.GroupPosition(1, "F"), SlotTeam.GroupPosition(2, "C")),
        DieciseisavoSlot(76, SlotTeam.GroupPosition(1, "C"), SlotTeam.GroupPosition(2, "F")),
        DieciseisavoSlot(77, SlotTeam.GroupPosition(1, "I"), SlotTeam.ThirdParty(listOf("C","D","F","G","H"))),
        DieciseisavoSlot(78, SlotTeam.GroupPosition(2, "E"), SlotTeam.GroupPosition(2, "I")),
        DieciseisavoSlot(79, SlotTeam.GroupPosition(1, "A"), SlotTeam.ThirdParty(listOf("C","E","F","H","I"))),
        DieciseisavoSlot(80, SlotTeam.GroupPosition(1, "L"), SlotTeam.ThirdParty(listOf("E","H","I","J","K"))),
        DieciseisavoSlot(81, SlotTeam.GroupPosition(1, "D"), SlotTeam.ThirdParty(listOf("B","E","F","I","J"))),
        DieciseisavoSlot(82, SlotTeam.GroupPosition(1, "G"), SlotTeam.ThirdParty(listOf("A","E","H","I","J"))),
        DieciseisavoSlot(83, SlotTeam.GroupPosition(2, "K"), SlotTeam.GroupPosition(2, "L")),
        DieciseisavoSlot(84, SlotTeam.GroupPosition(1, "H"), SlotTeam.GroupPosition(2, "J")),
        DieciseisavoSlot(85, SlotTeam.GroupPosition(1, "B"), SlotTeam.ThirdParty(listOf("E","F","G","I","J"))),
        DieciseisavoSlot(86, SlotTeam.GroupPosition(1, "J"), SlotTeam.GroupPosition(2, "H")),
        DieciseisavoSlot(87, SlotTeam.GroupPosition(1, "K"), SlotTeam.ThirdParty(listOf("D","E","I","J","L"))),
        DieciseisavoSlot(88, SlotTeam.GroupPosition(2, "D"), SlotTeam.GroupPosition(2, "G"))
    )

    fun generateDieciseisavos(
        allMatches: List<MatchEntity>,
        allTeams: List<com.porrawc2026.app.data.local.entity.TeamEntity>
    ): List<MatchEntity> {
        val standings = computeAllGroupStandings(allMatches, allTeams)
        val topTwo = mutableMapOf<String, MutableMap<Int, String>>() // group -> (position -> teamName)
        val thirdPlaced = mutableListOf<ThirdPlaceTeam>()

        for ((group, entries) in standings) {
            for ((i, entry) in entries.withIndex()) {
                val pos = i + 1
                if (pos <= 2) {
                    topTwo.getOrPut(group) { mutableMapOf() }[pos] = entry.teamName
                } else if (pos == 3) {
                    thirdPlaced.add(
                        ThirdPlaceTeam(
                            teamName = entry.teamName,
                            group = group,
                            points = entry.points,
                            goalDifference = entry.goalDifference,
                            goalsFor = entry.goalsFor,
                            rank = 0
                        )
                    )
                }
            }
        }

        val rankedThird = thirdPlaced.sortedWith(
            compareByDescending<ThirdPlaceTeam> { it.points }
                .thenByDescending { it.goalDifference }
                .thenByDescending { it.goalsFor }
        ).mapIndexed { idx, t -> t.copy(rank = idx + 1) }

        val qualifiedThird = rankedThird.take(8)
        val assignedThird = assignThirdPlaced(qualifiedThird)

        val schedule = MatchScheduleProvider.getDieciseisavosSchedule()

        return dieciseisavoSlots.map { slot ->
            val homeTeam = resolveTeam(slot.homeType, topTwo, assignedThird)
            val awayTeam = resolveTeam(slot.awayType, topTwo, assignedThird)
            val sched = schedule[slot.matchId]
            val dateStr = sched?.date ?: ""
            val tv = sched?.tv ?: "DAZN"

            MatchEntity(
                id = slot.matchId,
                groupName = "Dieciseisavos",
                matchday = "Dieciseisavos",
                dateTime = dateStr,
                homeTeam = homeTeam,
                awayTeam = awayTeam,
                isKnockout = true,
                knockoutRound = "Dieciseisavos",
                matchNumber = slot.matchId,
                tvChannel = tv
            )
        }
    }

    private fun computeAllGroupStandings(
        matches: List<MatchEntity>,
        teams: List<com.porrawc2026.app.data.local.entity.TeamEntity>
    ): Map<String, List<GroupStanding>> {
        val groupsMap = mutableMapOf<String, MutableMap<String, GroupStanding>>()

        for (team in teams) {
            val g = team.groupLetter.uppercase()
            val standings = groupsMap.getOrPut(g) { mutableMapOf() }
            standings[team.name] = GroupStanding(teamName = team.name, points = 0, goalDifference = 0, goalsFor = 0, position = 0)
        }

        val groupMatches = matches.filter { !it.isKnockout }
        for (match in groupMatches) {
            val homeGoals = match.homeGoals ?: continue
            val awayGoals = match.awayGoals ?: continue
            val group = match.groupName.removePrefix("Grupo ").trim().uppercase()
            val standings = groupsMap[group] ?: continue

            val home = standings[match.homeTeam] ?: continue
            val away = standings[match.awayTeam] ?: continue

            standings[match.homeTeam] = home.copy(
                points = home.points + when {
                    homeGoals > awayGoals -> 3
                    homeGoals == awayGoals -> 1
                    else -> 0
                },
                goalDifference = home.goalDifference + (homeGoals - awayGoals),
                goalsFor = home.goalsFor + homeGoals
            )
            standings[match.awayTeam] = away.copy(
                points = away.points + when {
                    awayGoals > homeGoals -> 3
                    awayGoals == homeGoals -> 1
                    else -> 0
                },
                goalDifference = away.goalDifference + (awayGoals - homeGoals),
                goalsFor = away.goalsFor + awayGoals
            )
        }

        return groupsMap.mapValues { (_, teamMap) ->
            teamMap.values.sortedWith(
                compareByDescending<GroupStanding> { it.points }
                    .thenByDescending { it.goalDifference }
                    .thenByDescending { it.goalsFor }
            ).mapIndexed { idx, entry -> entry.copy(position = idx + 1) }
        }
    }

    private fun resolveTeam(
        slot: SlotTeam,
        topTwo: Map<String, Map<Int, String>>,
        assignedThird: Map<String, String>
    ): String {
        return when (slot) {
            is SlotTeam.GroupPosition -> {
                topTwo[slot.group]?.get(slot.position)
                    ?: "${ordinal(slot.position)} ${slot.group}"
            }
            is SlotTeam.ThirdParty -> {
                for (group in slot.eligibleGroups) {
                    val team = assignedThird[group]
                    if (team != null) return team
                }
                "3º Grupo ${slot.eligibleGroups.first()}"
            }
        }
    }

    private fun assignThirdPlaced(thirdTeams: List<ThirdPlaceTeam>): Map<String, String> {
        // Greedy assignment: for each third-placed slot in order,
        // assign the best-ranked eligible third-placed team
        val remaining = thirdTeams.toMutableList()
        val assigned = mutableMapOf<String, String>() // group -> teamName

        // Order of third-placed slots (matches that need 3rd teams)
        val thirdSlots = dieciseisavoSlots.filter { it.awayType is SlotTeam.ThirdParty }
            .map { it.awayType as SlotTeam.ThirdParty }

        for (slot in thirdSlots) {
            val eligibleGroups = slot.eligibleGroups.toSet()
            val eligible = remaining.filter { it.group in eligibleGroups }
            val chosen = eligible.firstOrNull()
            if (chosen != null) {
                assigned[chosen.group] = chosen.teamName
                remaining.remove(chosen)
            }
        }

        return assigned
    }

    private fun ordinal(n: Int): String = when (n) {
        1 -> "1º"
        2 -> "2º"
        3 -> "3º"
        else -> "${n}º"
    }
}
