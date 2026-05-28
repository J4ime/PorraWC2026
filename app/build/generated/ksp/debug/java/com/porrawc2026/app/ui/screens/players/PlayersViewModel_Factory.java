package com.porrawc2026.app.ui.screens.players;

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
public final class PlayersViewModel_Factory implements Factory<PlayersViewModel> {
  private final Provider<PorraRepository> repositoryProvider;

  public PlayersViewModel_Factory(Provider<PorraRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public PlayersViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static PlayersViewModel_Factory create(Provider<PorraRepository> repositoryProvider) {
    return new PlayersViewModel_Factory(repositoryProvider);
  }

  public static PlayersViewModel newInstance(PorraRepository repository) {
    return new PlayersViewModel(repository);
  }
}
