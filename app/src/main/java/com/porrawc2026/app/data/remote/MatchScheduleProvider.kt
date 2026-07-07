package com.porrawc2026.app.data.remote

import com.porrawc2026.app.data.local.entity.MatchEntity
import com.porrawc2026.app.domain.model.TeamNameNormalizer
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object MatchScheduleProvider {

    private val groups = listOf("A","B","C","D","E","F","G","H","I","J","K","L")

    private val rtveMatchIds = setOf(1, 7, 13, 25, 43, 49, 67, 9, 21, 33, 45, 57, 69, 17, 30, 48, 65, 73, 74, 78, 80, 84, 87, 90, 91, 95)

    data class MatchSchedule(
        val id: Int,
        val date: String,
        val tv: String,
        val home: String,
        val away: String,
        val espnId: String? = null
    )

    private val scheduleCache by lazy { buildHardcodedSchedule() }

    fun getHardcodedSchedule(): Map<Int, MatchSchedule> = scheduleCache

    private fun buildHardcodedSchedule(): Map<Int, MatchSchedule> {
        val data = mutableMapOf<Int, MatchSchedule>()

        data[1] = MatchSchedule(1, "2026-06-11T21:00:00", "RTVE", "México", "Sudáfrica")
        data[2] = MatchSchedule(2, "2026-06-12T04:00:00", "DAZN", "Corea del Sur", "República Checa")
        data[3] = MatchSchedule(3, "2026-06-18T18:00:00", "DAZN", "República Checa", "Sudáfrica")
        data[4] = MatchSchedule(4, "2026-06-19T03:00:00", "DAZN", "México", "Corea del Sur")
        data[5] = MatchSchedule(5, "2026-06-25T03:00:00", "DAZN", "República Checa", "México")
        data[6] = MatchSchedule(6, "2026-06-25T03:00:00", "DAZN", "Sudáfrica", "Corea del Sur")

        data[7] = MatchSchedule(7, "2026-06-12T21:00:00", "RTVE", "Canadá", "Bosnia y Herzegovina")
        data[8] = MatchSchedule(8, "2026-06-13T21:00:00", "DAZN", "Catar", "Suiza")
        data[9] = MatchSchedule(9, "2026-06-18T21:00:00", "RTVE", "Suiza", "Bosnia y Herzegovina")
        data[10] = MatchSchedule(10, "2026-06-19T00:00:00", "DAZN", "Canadá", "Catar")
        data[11] = MatchSchedule(11, "2026-06-24T21:00:00", "DAZN", "Suiza", "Canadá")
        data[12] = MatchSchedule(12, "2026-06-24T21:00:00", "DAZN", "Bosnia y Herzegovina", "Catar")

        data[13] = MatchSchedule(13, "2026-06-14T00:00:00", "RTVE", "Brasil", "Marruecos")
        data[14] = MatchSchedule(14, "2026-06-20T00:00:00", "RTVE", "Escocia", "Marruecos")
        data[15] = MatchSchedule(15, "2026-06-20T02:30:00", "DAZN", "Brasil", "Haití")
        data[16] = MatchSchedule(16, "2026-06-14T03:00:00", "DAZN", "Haití", "Escocia")
        data[17] = MatchSchedule(17, "2026-06-25T00:00:00", "RTVE", "Escocia", "Brasil")
        data[18] = MatchSchedule(18, "2026-06-25T00:00:00", "DAZN", "Marruecos", "Haití")

        data[19] = MatchSchedule(19, "2026-06-13T03:00:00", "RTVE", "Estados Unidos", "Paraguay")
        data[20] = MatchSchedule(20, "2026-06-14T06:00:00", "DAZN", "Australia", "Turquía")
        data[21] = MatchSchedule(21, "2026-06-19T21:00:00", "RTVE", "Estados Unidos", "Australia")
        data[22] = MatchSchedule(22, "2026-06-20T05:00:00", "RTVE", "Turquía", "Paraguay")
        data[23] = MatchSchedule(23, "2026-06-26T04:00:00", "DAZN", "Turquía", "Estados Unidos")
        data[24] = MatchSchedule(24, "2026-06-26T04:00:00", "RTVE", "Paraguay", "Australia")

        data[25] = MatchSchedule(25, "2026-06-14T19:00:00", "RTVE", "Alemania", "Curazao")
        data[26] = MatchSchedule(26, "2026-06-15T01:00:00", "DAZN", "Costa de Marfil", "Ecuador")
        data[27] = MatchSchedule(27, "2026-06-20T22:00:00", "RTVE", "Alemania", "Costa de Marfil")
        data[28] = MatchSchedule(28, "2026-06-21T02:00:00", "DAZN", "Ecuador", "Curazao")
        data[29] = MatchSchedule(29, "2026-06-25T22:00:00", "DAZN", "Curazao", "Costa de Marfil")
        data[30] = MatchSchedule(30, "2026-06-25T22:00:00", "RTVE", "Ecuador", "Alemania")

        data[31] = MatchSchedule(31, "2026-06-14T22:00:00", "DAZN", "Países Bajos", "Japón")
        data[32] = MatchSchedule(32, "2026-06-15T04:00:00", "DAZN", "Suecia", "Túnez")
        data[33] = MatchSchedule(33, "2026-06-20T19:00:00", "RTVE", "Países Bajos", "Suecia")
        data[34] = MatchSchedule(34, "2026-06-21T06:00:00", "DAZN", "Túnez", "Japón")
        data[35] = MatchSchedule(35, "2026-06-26T01:00:00", "DAZN", "Japón", "Suecia")
        data[36] = MatchSchedule(36, "2026-06-26T01:00:00", "DAZN", "Túnez", "Países Bajos")

        data[37] = MatchSchedule(37, "2026-06-15T21:00:00", "DAZN", "Bélgica", "Egipto")
        data[38] = MatchSchedule(38, "2026-06-16T03:00:00", "DAZN", "Irán", "Nueva Zelanda")
        data[39] = MatchSchedule(39, "2026-06-21T21:00:00", "RTVE", "Bélgica", "Irán")
        data[40] = MatchSchedule(40, "2026-06-22T03:00:00", "DAZN", "Nueva Zelanda", "Egipto")
        data[41] = MatchSchedule(41, "2026-06-27T05:00:00", "DAZN", "Egipto", "Irán")
        data[42] = MatchSchedule(42, "2026-06-27T05:00:00", "DAZN", "Nueva Zelanda", "Bélgica")

        data[43] = MatchSchedule(43, "2026-06-15T18:00:00", "RTVE", "España", "Cabo Verde")
        data[44] = MatchSchedule(44, "2026-06-16T00:00:00", "DAZN", "Arabia Saudita", "Uruguay")
        data[45] = MatchSchedule(45, "2026-06-21T18:00:00", "RTVE", "España", "Arabia Saudita")
        data[46] = MatchSchedule(46, "2026-06-22T00:00:00", "DAZN", "Uruguay", "Cabo Verde")
        data[47] = MatchSchedule(47, "2026-06-27T02:00:00", "DAZN", "Cabo Verde", "Arabia Saudita")
        data[48] = MatchSchedule(48, "2026-06-27T02:00:00", "RTVE", "Uruguay", "España")

        data[49] = MatchSchedule(49, "2026-06-16T21:00:00", "RTVE", "Francia", "Senegal")
        data[50] = MatchSchedule(50, "2026-06-17T00:00:00", "DAZN", "Irak", "Noruega")
        data[51] = MatchSchedule(51, "2026-06-22T23:00:00", "RTVE", "Francia", "Irak")
        data[52] = MatchSchedule(52, "2026-06-23T02:00:00", "DAZN", "Noruega", "Senegal")
        data[53] = MatchSchedule(53, "2026-06-26T21:00:00", "DAZN", "Noruega", "Francia")
        data[54] = MatchSchedule(54, "2026-06-26T21:00:00", "DAZN", "Senegal", "Irak")

        data[55] = MatchSchedule(55, "2026-06-17T03:00:00", "RTVE", "Argentina", "Argelia")
        data[56] = MatchSchedule(56, "2026-06-17T06:00:00", "DAZN", "Austria", "Jordania")
        data[57] = MatchSchedule(57, "2026-06-22T19:00:00", "RTVE", "Argentina", "Austria")
        data[58] = MatchSchedule(58, "2026-06-23T05:00:00", "DAZN", "Jordania", "Argelia")
        data[59] = MatchSchedule(59, "2026-06-28T04:00:00", "DAZN", "Argelia", "Austria")
        data[60] = MatchSchedule(60, "2026-06-28T04:00:00", "RTVE", "Jordania", "Argentina")

        data[61] = MatchSchedule(61, "2026-06-17T19:00:00", "DAZN", "Portugal", "RD Congo")
        data[62] = MatchSchedule(62, "2026-06-18T04:00:00", "RTVE", "Uzbekistán", "Colombia")
        data[63] = MatchSchedule(63, "2026-06-23T19:00:00", "RTVE", "Portugal", "Uzbekistán")
        data[64] = MatchSchedule(64, "2026-06-24T04:00:00", "DAZN", "Colombia", "RD Congo")
        data[65] = MatchSchedule(65, "2026-06-28T01:30:00", "RTVE", "Colombia", "Portugal")
        data[66] = MatchSchedule(66, "2026-06-28T01:30:00", "RTVE", "RD Congo", "Uzbekistán")

        data[67] = MatchSchedule(67, "2026-06-17T22:00:00", "RTVE", "Inglaterra", "Croacia")
        data[68] = MatchSchedule(68, "2026-06-18T01:00:00", "DAZN", "Ghana", "Panamá")
        data[69] = MatchSchedule(69, "2026-06-23T22:00:00", "RTVE", "Inglaterra", "Ghana")
        data[70] = MatchSchedule(70, "2026-06-24T01:00:00", "DAZN", "Panamá", "Croacia")
        data[71] = MatchSchedule(71, "2026-06-27T23:00:00", "RTVE", "Panamá", "Inglaterra")
        data[72] = MatchSchedule(72, "2026-06-27T23:00:00", "DAZN", "Croacia", "Ghana")

        // Dieciseisavos (Round of 32) — IDs 73-88
        data[73] = MatchSchedule(73, "2026-06-28T21:00:00", "RTVE", "", "", "760486")  // SA vs Canada 19:00Z
        data[74] = MatchSchedule(74, "2026-06-29T19:00:00", "RTVE", "", "", "760487")  // Brazil vs Japan 17:00Z
        data[75] = MatchSchedule(75, "2026-06-29T22:30:00", "DAZN", "", "", "760489")  // Germany vs Paraguay 20:30Z
        data[76] = MatchSchedule(76, "2026-06-30T03:00:00", "DAZN", "", "", "760488")  // Netherlands vs Morocco 01:00Z
        data[77] = MatchSchedule(77, "2026-06-30T19:00:00", "DAZN", "", "", "760490")  // Ivory Coast vs Norway 17:00Z
        data[78] = MatchSchedule(78, "2026-06-30T23:00:00", "RTVE", "", "", "760492")  // France vs Sweden 21:00Z
        data[79] = MatchSchedule(79, "2026-07-01T04:00:00", "DAZN", "", "", "760491")  // Mexico vs Ecuador 02:00Z
        data[80] = MatchSchedule(80, "2026-07-01T18:00:00", "DAZN", "", "", "760495")  // England vs Congo DR 16:00Z
        data[81] = MatchSchedule(81, "2026-07-01T22:00:00", "DAZN", "", "", "760493")  // Belgium vs Senegal 20:00Z
        data[82] = MatchSchedule(82, "2026-07-02T02:00:00", "DAZN", "", "", "760494")  // USA vs Bosnia 00:00Z
        data[83] = MatchSchedule(83, "2026-07-03T01:00:00", "DAZN", "", "", "760496")  // Portugal vs Croatia 23:00Z (Jul 2)
        data[84] = MatchSchedule(84, "2026-07-02T21:00:00", "RTVE", "", "", "760497")  // Spain vs Austria 19:00Z
        data[85] = MatchSchedule(85, "2026-07-03T05:00:00", "DAZN", "", "", "760498")  // Switzerland vs Algeria 03:00Z
        data[86] = MatchSchedule(86, "2026-07-03T20:00:00", "DAZN", "", "", "760499")  // Australia vs Egypt 18:00Z
        data[87] = MatchSchedule(87, "2026-07-04T00:00:00", "DAZN", "", "", "760500")  // Argentina vs C Verde 22:00Z (Jul 3)
        data[88] = MatchSchedule(88, "2026-07-04T03:30:00", "DAZN", "", "", "760501")  // Colombia vs Ghana 01:30Z

        // Octavos (Round of 16) — IDs 89-96
        data[89] = MatchSchedule(89, "2026-07-04T19:00:00", "DAZN", "Canadá",    "Marruecos",   "760502")  // CAN vs MAR
        data[90] = MatchSchedule(90, "2026-07-04T23:00:00", "RTVE", "Francia",   "Paraguay",    "760503")  // FRA vs PAR
        data[91] = MatchSchedule(91, "2026-07-05T22:00:00", "RTVE", "Brasil",    "Noruega",     "760504")  // BRA vs NOR
        data[92] = MatchSchedule(92, "2026-07-06T02:00:00", "DAZN", "México",    "Inglaterra",  "760505")  // MEX vs ENG
        data[93] = MatchSchedule(93, "2026-07-06T21:00:00", "DAZN", "Portugal",  "España",      "760506")  // POR vs ESP
        data[94] = MatchSchedule(94, "2026-07-07T02:00:00", "DAZN", "USA",       "Bélgica",     "760507")  // USA vs BEL
        data[95] = MatchSchedule(95, "2026-07-07T18:00:00", "DAZN", "Argentina", "Egipto",      "760509")  // ARG vs EGY
        data[96] = MatchSchedule(96, "2026-07-07T22:00:00", "DAZN", "Suiza",     "Colombia",    "760508")  // SUI vs COL

        // Cuartos (Quarter-finals) — IDs 97-100
        data[97] = MatchSchedule(97, "2026-07-09T22:00:00", "DAZN", "Ganador 89", "Ganador 90", "760510")  // R16 W1 vs R16 W2 20:00Z
        data[98] = MatchSchedule(98, "2026-07-10T21:00:00", "DAZN", "Ganador 93", "Ganador 94", "760511")  // R16 W5 vs R16 W6 19:00Z
        data[99] = MatchSchedule(99, "2026-07-11T23:00:00", "DAZN", "Ganador 91", "Ganador 92", "760512")  // R16 W3 vs R16 W4 21:00Z
        data[100] = MatchSchedule(100, "2026-07-12T03:00:00", "DAZN", "Ganador 95", "Ganador 96", "760513")  // R16 W7 vs R16 W8 01:00Z

        // Semifinales — IDs 101-102
        data[101] = MatchSchedule(101, "2026-07-14T22:00:00", "DAZN", "Ganador 97", "Ganador 98")
        data[102] = MatchSchedule(102, "2026-07-15T21:00:00", "DAZN", "Ganador 99", "Ganador 100")

        // Tercer puesto — ID 103
        data[103] = MatchSchedule(103, "2026-07-18T21:00:00", "DAZN", "Perdedor 101", "Perdedor 102")

        // Final — ID 104
        data[104] = MatchSchedule(104, "2026-07-19T21:00:00", "DAZN", "Ganador 101", "Ganador 102")

        return data
    }

    fun buildMatchEntities(): List<MatchEntity> {
        val schedule = getHardcodedSchedule()
        return schedule.mapNotNull { (id, s) ->
            if (id > 104) return@mapNotNull null
            when {
                id > 100 -> {
                    val round = when (id) {
                        101, 102 -> "Semifinales"
                        103 -> "3er puesto"
                        104 -> "Final"
                        else -> "Fase final"
                    }
                    MatchEntity(
                        id = id,
                        groupName = round,
                        matchday = round,
                        dateTime = s.date,
                        homeTeam = s.home,
                        awayTeam = s.away,
                        isKnockout = true,
                        knockoutRound = round,
                        matchNumber = id,
                        tvChannel = if (rtveMatchIds.contains(id)) "DAZN,RTVE" else "DAZN",
                        espnId = s.espnId
                    )
                }
                id > 96 -> {
                    MatchEntity(
                        id = id,
                        groupName = "Cuartos",
                        matchday = "Cuartos",
                        dateTime = s.date,
                        homeTeam = s.home,
                        awayTeam = s.away,
                        isKnockout = true,
                        knockoutRound = "Cuartos",
                        matchNumber = id,
                        tvChannel = if (rtveMatchIds.contains(id)) "DAZN,RTVE" else "DAZN",
                        espnId = s.espnId
                    )
                }
                id > 88 -> {
                    MatchEntity(
                        id = id,
                        groupName = "Octavos",
                        matchday = "Octavos",
                        dateTime = s.date,
                        homeTeam = s.home,
                        awayTeam = s.away,
                        isKnockout = true,
                        knockoutRound = "Octavos",
                        matchNumber = id,
                        tvChannel = if (rtveMatchIds.contains(id)) "DAZN,RTVE" else "DAZN",
                        espnId = s.espnId
                    )
                }
                id > 72 -> {
                    MatchEntity(
                        id = id,
                        groupName = "Dieciseisavos",
                        matchday = "Dieciseisavos",
                        dateTime = s.date,
                        homeTeam = s.home,
                        awayTeam = s.away,
                        isKnockout = true,
                        knockoutRound = "Dieciseisavos",
                        matchNumber = id,
                        tvChannel = if (rtveMatchIds.contains(id)) "DAZN,RTVE" else "DAZN",
                        espnId = s.espnId
                    )
                }
                else -> {
                    val groupIndex = (id - 1) / 6
                    MatchEntity(
                        id = id,
                        groupName = "Grupo ${groups.getOrElse(groupIndex) { "?" }}",
                        matchday = "J${(id - 1) % 6 + 1}",
                        dateTime = s.date,
                        homeTeam = s.home,
                        awayTeam = s.away,
                        tvChannel = if (rtveMatchIds.contains(id)) "DAZN,RTVE" else "DAZN",
                        isKnockout = false,
                        espnId = s.espnId
                    )
                }
            }
        }.sortedBy { it.id }
    }

    fun getDieciseisavosSchedule(): Map<Int, MatchSchedule> =
        getHardcodedSchedule().filterKeys { it in 73..88 }

    fun getRtveMatchIds(): Set<Int> = rtveMatchIds

    fun getMaxVisibleRoundId(): Int {
        val schedule = getHardcodedSchedule()
        val now = Instant.now()
        val madridZone = ZoneId.of("Europe/Madrid")
        val fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss", Locale.US)

        fun parseInstant(dateStr: String): Instant? {
            if (dateStr.isBlank()) return null
            return try {
                LocalDateTime.parse(dateStr, fmt).atZone(madridZone).toInstant()
            } catch (e: Exception) { android.util.Log.e("MatchSchedule", "parseInstant failed for dateStr=$dateStr", e); null }
        }

        val firstSemi = parseInstant(schedule[101]?.date ?: "")
        val firstCuarto = parseInstant(schedule[97]?.date ?: "")
        val firstOctavo = parseInstant(schedule[89]?.date ?: "")
        val firstDieciseisavo = parseInstant(schedule[73]?.date ?: "")

        return when {
            firstSemi != null && now.isAfter(firstSemi) -> 104
            firstCuarto != null && now.isAfter(firstCuarto) -> 102
            firstOctavo != null && now.isAfter(firstOctavo) -> 100
            firstDieciseisavo != null && now.isAfter(firstDieciseisavo) -> 96
            else -> 88
        }
    }

    fun enrichSchedule(matches: List<MatchEntity>): List<MatchEntity> {
        val fallback = getHardcodedSchedule()
        return matches.map { match ->
            val fb = fallback[match.id]
            val date = fb?.date ?: match.dateTime
            val tv = if (fb != null && rtveMatchIds.contains(fb.id)) "DAZN,RTVE" else "DAZN"
            val espnId = fb?.espnId ?: match.espnId
            match.copy(dateTime = date, tvChannel = tv, espnId = espnId)
        }
    }
}
