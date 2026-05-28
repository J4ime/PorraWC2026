package com.porrawc2026.app.di;

import com.porrawc2026.app.data.local.AppDatabase;
import com.porrawc2026.app.data.local.dao.PlayerPredictionDao;
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
public final class AppModule_ProvidePlayerPredictionDaoFactory implements Factory<PlayerPredictionDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvidePlayerPredictionDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public PlayerPredictionDao get() {
    return providePlayerPredictionDao(dbProvider.get());
  }

  public static AppModule_ProvidePlayerPredictionDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvidePlayerPredictionDaoFactory(dbProvider);
  }

  public static PlayerPredictionDao providePlayerPredictionDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.providePlayerPredictionDao(db));
  }
}
