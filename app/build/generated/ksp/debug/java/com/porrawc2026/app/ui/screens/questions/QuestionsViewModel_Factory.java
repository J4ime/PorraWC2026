package com.porrawc2026.app.ui.screens.questions;

import com.porrawc2026.app.data.repository.PorraRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class QuestionsViewModel_Factory implements Factory<QuestionsViewModel> {
  private final Provider<PorraRepository> repositoryProvider;

  public QuestionsViewModel_Factory(Provider<PorraRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public QuestionsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static QuestionsViewModel_Factory create(Provider<PorraRepository> repositoryProvider) {
    return new QuestionsViewModel_Factory(repositoryProvider);
  }

  public static QuestionsViewModel newInstance(PorraRepository repository) {
    return new QuestionsViewModel(repository);
  }
}
