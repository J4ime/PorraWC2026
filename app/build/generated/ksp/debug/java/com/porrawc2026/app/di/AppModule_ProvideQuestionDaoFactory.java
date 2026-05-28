package com.porrawc2026.app.di;

import com.porrawc2026.app.data.local.AppDatabase;
import com.porrawc2026.app.data.local.dao.QuestionDao;
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
public final class AppModule_ProvideQuestionDaoFactory implements Factory<QuestionDao> {
  private final Provider<AppDatabase> dbProvider;

  public AppModule_ProvideQuestionDaoFactory(Provider<AppDatabase> dbProvider) {
    this.dbProvider = dbProvider;
  }

  @Override
  public QuestionDao get() {
    return provideQuestionDao(dbProvider.get());
  }

  public static AppModule_ProvideQuestionDaoFactory create(Provider<AppDatabase> dbProvider) {
    return new AppModule_ProvideQuestionDaoFactory(dbProvider);
  }

  public static QuestionDao provideQuestionDao(AppDatabase db) {
    return Preconditions.checkNotNullFromProvides(AppModule.INSTANCE.provideQuestionDao(db));
  }
}
