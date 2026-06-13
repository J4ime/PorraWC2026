package com.porrawc2026.app

import com.porrawc2026.app.data.remote.MatchScheduleProviderTest
import com.porrawc2026.app.data.repository.PorraRepositoryTest
import com.porrawc2026.app.domain.model.StandingsCalculatorTest
import com.porrawc2026.app.domain.model.TeamNameNormalizerTest
import com.porrawc2026.app.ui.screens.goalscorers.GoalscorersViewModelTest
import com.porrawc2026.app.ui.screens.groups.GroupsViewModelTest
import com.porrawc2026.app.ui.screens.home.HomeViewModelTest
import com.porrawc2026.app.ui.screens.knockout.KnockoutViewModelTest
import com.porrawc2026.app.ui.screens.players.PlayersViewModelTest
import com.porrawc2026.app.ui.screens.questions.QuestionsViewModelTest
import com.porrawc2026.app.ui.screens.results.ResultsViewModelTest
import com.porrawc2026.app.util.ExcelParserValidationTest
import com.porrawc2026.app.util.PointsCalculationTest
import com.porrawc2026.app.util.ShareUtilTest
import com.porrawc2026.app.util.UpdateManagerTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    ExcelParserValidationTest::class,
    PointsCalculationTest::class,
    PorraRepositoryTest::class,
    TeamNameNormalizerTest::class,
    StandingsCalculatorTest::class,
    MatchScheduleProviderTest::class,
    ShareUtilTest::class,
    UpdateManagerTest::class,
    GroupsViewModelTest::class,
    KnockoutViewModelTest::class,
    PlayersViewModelTest::class,
    GoalscorersViewModelTest::class,
    QuestionsViewModelTest::class,
    ResultsViewModelTest::class,
    HomeViewModelTest::class
)
class RegressionTestSuite
