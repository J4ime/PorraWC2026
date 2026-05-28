package com.porrawc2026.app.domain.model

data class Team(
    val id: String,
    val name: String,
    val groupLetter: String,
    val rank: Int,
    val flagEmoji: String
)

data class GroupStanding(
    val teamId: String,
    val teamName: String,
    val groupLetter: String,
    val position: Int,
    val played: Int,
    val won: Int,
    val drawn: Int,
    val lost: Int,
    val goalsFor: Int,
    val goalsAgainst: Int,
    val points: Int
) {
    val goalDifference: Int get() = goalsFor - goalsAgainst
}

data class Match(
    val id: Int,
    val groupName: String,
    val matchday: String,
    val dateTime: String,
    val homeTeam: String,
    val awayTeam: String,
    val homeGoals: Int?,
    val awayGoals: Int?,
    val predictedHomeGoals: Int?,
    val predictedAwayGoals: Int?,
    val isKnockout: Boolean,
    val knockoutRound: String?,
    val pointsEarned: Int
)

data class Question(
    val id: Int,
    val text: String,
    val predictedAnswer: Boolean?,
    val correctAnswer: Boolean?,
    val pointsEarned: Int
)

data class PlayerPrediction(
    val rank: Int,
    val playerName: String,
    val predictedName: String?,
    val goalsScored: Int,
    val pointsPerGoal: Int,
    val pointsEarned: Int
)

data class KnockoutMatch(
    val matchNumber: Int,
    val round: String,
    val homeTeamRef: String,
    val awayTeamRef: String,
    val winner: Int?,
    val pointsEarned: Int
)

data class TotalPoints(
    val groupStagePoints: Int = 0,
    val knockoutPoints: Int = 0,
    val questionPoints: Int = 0,
    val playerPoints: Int = 0
) {
    val total: Int get() = groupStagePoints + knockoutPoints + questionPoints + playerPoints
}
