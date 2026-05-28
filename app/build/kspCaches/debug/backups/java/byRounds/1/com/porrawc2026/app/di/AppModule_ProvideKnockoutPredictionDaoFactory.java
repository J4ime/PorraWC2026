package com.porrawc2026.app.di;

import com.porrawc2026.app.data.local.AppDatabase;
import com.porrawc2026.app.data.local.dao.KnockoutPredictionDao;
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
public final class AppModule_ProvideKnockoutPredictionDaoFactory implements Factory<KnockoutPredictionDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideKnockoutPredictionDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public KnockoutPredictionDao get() {
    return provideKnockoutPredictionDao(dbProvider.get());
  }

  public static AppModule_ProvideKnockoutPredictionDaoFactory create(
      Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideKnockoutPredictionDaoFactory(dbProvider);
  }

  public static KnockoutPredictionDao provideKnockoutPredictionDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideKnockoutPredictionDao(db));
  }
}
