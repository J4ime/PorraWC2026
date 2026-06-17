package com.porrawc2026.app.data.repository

import com.porrawc2026.app.data.local.dao.*
import com.porrawc2026.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PorraRepository @Inject constructor(
    private val teamDao: TeamDao,
    private val matchDao: MatchDao,
    private val questionDao: QuestionDao,
    private val playerPredictionDao: PlayerPredictionDao,
    private val knockoutPredictionDao: KnockoutPredictionDao,
    private val groupStandingDao: GroupStandingDao
) {
    fun getAllTeams(): Flow<List<TeamEntity>> = teamDao.getAllTeams()
    fun getTeamsByGroup(group: String): Flow<List<TeamEntity>> = teamDao.getTeamsByGroup(group)

    fun getAllGroupMatches(): Flow<List<MatchEntity>> = matchDao.getAllGroupMatches()
    fun getGroupMatches(group: String): Flow<List<MatchEntity>> = matchDao.getGroupMatches(group)
    fun getKnockoutMatches(): Flow<List<MatchEntity>> = matchDao.getKnockoutMatches()
    fun getAllMatches(): Flow<List<MatchEntity>> = matchDao.getAllMatches()

    fun getAllQuestions(): Flow<List<QuestionEntity>> = questionDao.getAllQuestions()

    fun getPlayerPredictions(): Flow<List<PlayerPredictionEntity>> = playerPredictionDao.getAll()

    suspend fun getPlayerPredictionsList(): List<PlayerPredictionEntity> = playerPredictionDao.getAllList()

    fun getKnockoutPredictions(): Flow<List<KnockoutPredictionEntity>> = knockoutPredictionDao.getAll()
    fun getKnockoutPredictionsByRound(round: String): Flow<List<KnockoutPredictionEntity>> =
        knockoutPredictionDao.getByRound(round)

    fun getGroupStandings(): Flow<List<GroupStandingEntity>> = groupStandingDao.getAll()
    fun getGroupStandingsByGroup(group: String): Flow<List<GroupStandingEntity>> =
        groupStandingDao.getByGroup(group)

    suspend fun updateMatchPrediction(match: MatchEntity) = matchDao.updateMatch(match)
    suspend fun updateQuestionPrediction(question: QuestionEntity) = questionDao.updateQuestion(question)
    suspend fun updatePlayerPrediction(prediction: PlayerPredictionEntity) =
        playerPredictionDao.update(prediction)
    suspend fun updateKnockoutPrediction(prediction: KnockoutPredictionEntity) =
        knockoutPredictionDao.update(prediction)

    suspend fun calculateTotalPoints(): Int {
        val matchPoints = matchDao.getTotalMatchPoints()
        val questionPoints = questionDao.getTotalQuestionPoints()
        val playerPoints = playerPredictionDao.getTotalPoints()
        val knockoutPoints = knockoutPredictionDao.getTotalPoints()
        return matchPoints + questionPoints + playerPoints + knockoutPoints
    }

    suspend fun insertAllData(
        teams: List<TeamEntity>,
        matches: List<MatchEntity>,
        questions: List<QuestionEntity>,
        playerPredictions: List<PlayerPredictionEntity>,
        knockoutPredictions: List<KnockoutPredictionEntity>,
        standings: List<GroupStandingEntity>
    ) {
        teamDao.deleteAll()
        matchDao.deleteAll()
        questionDao.deleteAll()
        playerPredictionDao.deleteAll()
        knockoutPredictionDao.deleteAll()
        groupStandingDao.deleteAll()

        teamDao.insertAll(teams)
        matchDao.insertAll(matches)
        questionDao.insertAll(questions)
        playerPredictionDao.insertAll(playerPredictions)
        knockoutPredictionDao.insertAll(knockoutPredictions)
        groupStandingDao.insertAll(standings)
    }

    suspend fun insertMatches(matches: List<MatchEntity>) = matchDao.insertAll(matches)

    suspend fun updateMatchResults(matchId: Int, homeGoals: Int, awayGoals: Int) {
        matchDao.updateMatchResult(matchId, homeGoals, awayGoals)
    }

    suspend fun updateMatchCards(matchId: Int, homeReds: Int, awayReds: Int, homeYellows: Int, awayYellows: Int) {
        matchDao.updateMatchCards(matchId, homeReds, awayReds, homeYellows, awayYellows)
    }

    suspend fun updateMatchScorers(matchId: Int, homeScorers: String?, awayScorers: String?) {
        matchDao.updateMatchScorers(matchId, homeScorers, awayScorers)
    }

    suspend fun clearAllMatchScores() {
        matchDao.clearAllMatchScores()
    }
}
