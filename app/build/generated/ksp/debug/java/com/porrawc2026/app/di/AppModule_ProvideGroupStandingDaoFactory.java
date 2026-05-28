package com.porrawc2026.app.di;

import com.porrawc2026.app.data.local.AppDatabase;
import com.porrawc2026.app.data.local.dao.GroupStandingDao;
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
public final class AppModule_ProvideGroupStandingDaoFactory implements Factory<GroupStandingDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideGroupStandingDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public GroupStandingDao get() {
    return provideGroupStandingDao(dbProvider.get());
  }

  public static AppModule_ProvideGroupStandingDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideGroupStandingDaoFactory(dbProvider);
  }

  public static GroupStandingDao provideGroupStandingDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideGroupStandingDao(db));
  }
}
