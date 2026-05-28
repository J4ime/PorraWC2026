package com.porrawc2026.app.ui.screens.groups;

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
public final class GroupsViewModel_Factory implements Factory<GroupsViewModel> {
  private final Provider<PorraRepository> repositoryProvider;

  public GroupsViewModel_Factory(Provider<PorraRepository> repositoryProvider) {
    this.repositoryProvider = repositoryProvider;
  }

  @Override
  public GroupsViewModel get() {
    return newInstance(repositoryProvider.get());
  }

  public static GroupsViewModel_Factory create(Provider<PorraRepository> repositoryProvider) {
    return new GroupsViewModel_Factory(repositoryProvider);
  }

  public static GroupsViewModel newInstance(PorraRepository repository) {
    return new GroupsViewModel(repository);
  }
}
