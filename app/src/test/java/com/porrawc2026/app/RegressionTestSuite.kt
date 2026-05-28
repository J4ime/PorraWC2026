package com.porrawc2026.app

import com.porrawc2026.app.data.local.entity.*
import com.porrawc2026.app.data.repository.PorraRepositoryTest
import com.porrawc2026.app.util.ExcelParserValidationTest
import com.porrawc2026.app.util.PointsCalculationTest
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite::class)
@Suite.SuiteClasses(
    value = [
        ExcelParserValidationTest::class,
        PointsCalculationTest::class,
        PorraRepositoryTest::class
    ]
)
class RegressionTestSuite
