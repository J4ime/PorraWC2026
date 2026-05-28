package com.porrawc2026.app.ui.screens.home;

import android.content.Context;
import com.porrawc2026.app.data.repository.PorraRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class HomeViewModel_Factory implements Factory<HomeViewModel> {
  private final Provider<PorraRepository> repositoryProvider;

  private final Provider<Context> contextProvider;

  public HomeViewModel_Factory(Provider<PorraRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    this.repositoryProvider = repositoryProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public HomeViewModel get() {
    return newInstance(repositoryProvider.get(), contextProvider.get());
  }

  public static HomeViewModel_Factory create(Provider<PorraRepository> repositoryProvider,
      Provider<Context> contextProvider) {
    return new HomeViewModel_Factory(repositoryProvider, contextProvider);
  }

  public static HomeViewModel newInstance(PorraRepository repository, Context context) {
    return new HomeViewModel(repository, context);
  }
}
