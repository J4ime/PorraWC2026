package com.porrawc2026.app.domain.model

import java.text.Normalizer

object TeamNameNormalizer {

    private val enToEs = mapOf(
        "Mexico" to "México", "South Africa" to "Sudáfrica", "South Korea" to "Corea del Sur",
        "Korea Republic" to "Corea del Sur", "Czechia" to "República Checa",
        "Czech Republic" to "República Checa",
        "Canada" to "Canadá", "Bosnia-Herzegovina" to "Bosnia y Herzegovina",
        "Bosnia and Herzegovina" to "Bosnia y Herzegovina",
        "Türkiye" to "Turquía", "United States" to "Estados Unidos", "Paraguay" to "Paraguay",
        "Australia" to "Australia", "Turkey" to "Turquía", "Germany" to "Alemania",
        "Curaçao" to "Curazao", "Curacao" to "Curazao", "Ivory Coast" to "Costa de Marfil",
        "Ecuador" to "Ecuador", "Netherlands" to "Países Bajos", "Japan" to "Japón",
        "Sweden" to "Suecia", "Tunisia" to "Túnez", "Belgium" to "Bélgica",
        "Egypt" to "Egipto", "Iran" to "Irán", "New Zealand" to "Nueva Zelanda",
        "Spain" to "España", "Cape Verde" to "Cabo Verde",
        "Saudi Arabia" to "Arabia Saudita", "Uruguay" to "Uruguay",
        "France" to "Francia", "Senegal" to "Senegal", "Iraq" to "Irak",
        "Norway" to "Noruega", "Argentina" to "Argentina", "Algeria" to "Argelia",
        "Austria" to "Austria", "Jordan" to "Jordania", "Portugal" to "Portugal",
        "Congo DR" to "RD Congo", "Uzbekistan" to "Uzbekistán",
        "Colombia" to "Colombia", "England" to "Inglaterra", "Croatia" to "Croacia",
        "Panama" to "Panamá", "Ghana" to "Ghana", "Brazil" to "Brasil",
        "Morocco" to "Marruecos", "Scotland" to "Escocia", "Haiti" to "Haití",
        "Switzerland" to "Suiza", "Qatar" to "Catar"
    )

    private val normalizeMap = mapOf(
        "southafrica" to "sudafrica", "southkorea" to "coreadelsur",
        "korearepublic" to "coreadelsur", "czechia" to "republicacheca",
        "czechrepublic" to "republicacheca",
        "bosniaandherzegovina" to "bosniayherzegovina",
        "bosnia-herzegovina" to "bosniayherzegovina", "bosniaherzegovina" to "bosniayherzegovina",
        "unitedstates" to "estadosunidos", "usa" to "estadosunidos",
        "netherlands" to "paisesbajos", "ivorycoast" to "costademarfil",
        "saudiarabia" to "arabiasaudita", "newzealand" to "nuevazelanda",
        "capeverde" to "caboverde", "congodr" to "rdcongo",
        "turkey" to "turquia", "turkiye" to "turquia", "japan" to "japon", "sweden" to "suecia",
        "belgium" to "belgica", "egypt" to "egipto", "spain" to "espana",
        "france" to "francia", "england" to "inglaterra", "croatia" to "croacia",
        "algeria" to "argelia", "morocco" to "marruecos", "scotland" to "escocia",
        "haiti" to "haiti", "brazil" to "brasil", "germany" to "alemania",
        "ecuador" to "ecuador", "tunisia" to "tunez", "uruguay" to "uruguay",
        "norway" to "noruega", "iraq" to "irak", "uzbekistan" to "uzbekistan",
        "colombia" to "colombia", "portugal" to "portugal", "panama" to "panama",
        "canada" to "canada", "qatar" to "catar", "switzerland" to "suiza",
        "mexico" to "mexico", "argentina" to "argentina", "senegal" to "senegal",
        "ghana" to "ghana", "jordan" to "jordania", "austria" to "austria",
        "australia" to "australia", "paraguay" to "paraguay", "iran" to "iran",
        "curacao" to "curazao", "curazao" to "curazao"
    )

    fun enToEs(name: String): String = enToEs[name] ?: name

    fun normalize(name: String): String {
        val clean = Normalizer.normalize(name, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}"), "")
            .replace(" ", "")
            .replace("-", "")
            .lowercase()
        return normalizeMap[clean] ?: clean
    }

    fun matches(a: String, b: String): Boolean {
        return normalize(a) == normalize(b)
    }
}
