package com.porrawc2026.app.di;

import com.porrawc2026.app.data.local.AppDatabase;
import com.porrawc2026.app.data.local.dao.TeamDao;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
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
public final class AppModule_ProvideTeamDaoFactory implements Factory<TeamDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideTeamDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public TeamDao get() {
    return provideTeamDao(dbProvider.get());
  }

  public static AppModule_ProvideTeamDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideTeamDaoFactory(dbProvider);
  }

  public static TeamDao provideTeamDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideTeamDao(db));
  }
}
