package com.porrawc2026.app.data.repository;

import com.porrawc2026.app.data.local.dao.GroupStandingDao;
import com.porrawc2026.app.data.local.dao.KnockoutPredictionDao;
import com.porrawc2026.app.data.local.dao.MatchDao;
import com.porrawc2026.app.data.local.dao.PlayerPredictionDao;
import com.porrawc2026.app.data.local.dao.QuestionDao;
import com.porrawc2026.app.data.local.dao.TeamDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava"
})
public final class PorraRepository_Factory implements Factory<PorraRepository> {
  private final Provider<TeamDao> teamDaoProvider;

  private final Provider<MatchDao> matchDaoProvider;

  private final Provider<QuestionDao> questionDaoProvider;

  private final Provider<PlayerPredictionDao> playerPredictionDaoProvider;

  private final Provider<KnockoutPredictionDao> knockoutPredictionDaoProvider;

  private final Provider<GroupStandingDao> groupStandingDaoProvider;

  public PorraRepository_Factory(Provider<TeamDao> teamDaoProvider,
      Provider<MatchDao> matchDaoProvider, Provider<QuestionDao> questionDaoProvider,
      Provider<PlayerPredictionDao> playerPredictionDaoProvider,
      Provider<KnockoutPredictionDao> knockoutPredictionDaoProvider,
      Provider<GroupStandingDao> groupStandingDaoProvider) {
    this.teamDaoProvider = teamDaoProvider;
    this.matchDaoProvider = matchDaoProvider;
    this.questionDaoProvider = questionDaoProvider;
    this.playerPredictionDaoProvider = playerPredictionDaoProvider;
    this.knockoutPredictionDaoProvider = knockoutPredictionDaoProvider;
    this.groupStandingDaoProvider = groupStandingDaoProvider;
  }

  @Override
  public PorraRepository get() {
    return newInstance(teamDaoProvider.get(), matchDaoProvider.get(), questionDaoProvider.get(), playerPredictionDaoProvider.get(), knockoutPredictionDaoProvider.get(), groupStandingDaoProvider.get());
  }

  public static PorraRepository_Factory create(Provider<TeamDao> teamDaoProvider,
      Provider<MatchDao> matchDaoProvider, Provider<QuestionDao> questionDaoProvider,
      Provider<PlayerPredictionDao> playerPredictionDaoProvider,
      Provider<KnockoutPredictionDao> knockoutPredictionDaoProvider,
      Provider<GroupStandingDao> groupStandingDaoProvider) {
    return new PorraRepository_Factory(teamDaoProvider, matchDaoProvider, questionDaoProvider, playerPredictionDaoProvider, knockoutPredictionDaoProvider, groupStandingDaoProvider);
  }

  public static PorraRepository newInstance(TeamDao teamDao, MatchDao matchDao,
      QuestionDao questionDao, PlayerPredictionDao playerPredictionDao,
      KnockoutPredictionDao knockoutPredictionDao, GroupStandingDao groupStandingDao) {
    return new PorraRepository(teamDao, matchDao, questionDao, playerPredictionDao, knockoutPredictionDao, groupStandingDao);
  }
}
