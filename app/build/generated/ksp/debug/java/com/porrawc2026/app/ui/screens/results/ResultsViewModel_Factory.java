package com.porrawc2026.app.ui.screens.results;

import com.porrawc2026.app.data.remote.ApiService;
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
public final class ResultsViewModel_Factory implements Factory<ResultsViewModel> {
  private final Provider<PorraRepository> repositoryProvider;

  private final Provider<ApiService> apiServiceProvider;

  public ResultsViewModel_Factory(Provider<PorraRepository> repositoryProvider,
      Provider<ApiService> apiServiceProvider) {
    this.repositoryProvider = repositoryProvider;
    this.apiServiceProvider = apiServiceProvider;
  }

  @Override
  public ResultsViewModel get() {
    return newInstance(repositoryProvider.get(), apiServiceProvider.get());
  }

  public static ResultsViewModel_Factory create(Provider<PorraRepository> repositoryProvider,
      Provider<ApiService> apiServiceProvider) {
    return new ResultsViewModel_Factory(repositoryProvider, apiServiceProvider);
  }

  public static ResultsViewModel newInstance(PorraRepository repository, ApiService apiService) {
    return new ResultsViewModel(repository, apiService);
  }
}
