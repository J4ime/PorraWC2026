package com.porrawc2026.app.ui.screens.knockout;

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
public final class KnockoutViewModel_Factory implements Factory<KnockoutViewModel> {
  private final Provider<PorraRepository> repositoryProvider;

  public KnockoutViewModel_Factory(Provider<PorraRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public KnockoutViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static KnockoutViewModel_Factory create(Provider<PorraRepository> repositoryProvider) {
    return new KnockoutViewModel_Factory(repositoryProvider);
  }

  public static KnockoutViewModel newInstance(PorraRepository repository) {
    return new KnockoutViewModel(repository);
  }
}
