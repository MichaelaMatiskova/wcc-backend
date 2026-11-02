package com.wcc.platform.service;

import static com.wcc.platform.domain.cms.PageType.PROGRAMMES;
import static com.wcc.platform.domain.platform.type.ProgramType.BOOK_CLUB;
import static com.wcc.platform.factories.SetupProgrammeFactories.createProgrammePageTest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wcc.platform.domain.cms.pages.programme.ProgrammesPage;
import com.wcc.platform.domain.exceptions.ContentNotFoundException;
import com.wcc.platform.domain.exceptions.PlatformInternalException;
import com.wcc.platform.repository.PageRepository;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/** Test class for ProgrammeService. */
class ProgrammeServiceTest {
  private ProgrammeService service;

  @Mock private PageRepository pageRepository;
  @Mock private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    objectMapper = new ObjectMapper();
    objectMapper.findAndRegisterModules();

    service = new ProgrammeService(pageRepository, objectMapper);
  }

  @Test
  @SuppressWarnings("unchecked")
  void whenGetTeamGivenRecordExistOnDatabaseThenReturnValidResponse() {
    var programmePage = createProgrammePageTest("init-data/bookClubPage.json");
    var mapPage = objectMapper.convertValue(programmePage, Map.class);
    when(pageRepository.findById(BOOK_CLUB.toPageId())).thenReturn(Optional.of(mapPage));

    var response = service.getProgramme(BOOK_CLUB);

    assertThat(response.getSection()).isEqualTo(programmePage.getSection());
    assertThat(response.getContact()).isEqualTo(programmePage.getContact());
    assertThat(response.getProgrammeDetails()).isEqualTo(programmePage.getProgrammeDetails());
  }

  @Test
  @SuppressWarnings("unchecked")
  @Disabled(
      "This test is disabled because the equals "
          + "method is different for a record object which is strange.")
  void whenGetTeamGivenRecordExistThenReturnInTheResponse() {
    var programmePage = createProgrammePageTest("init-data/bookClubPage.json");
    var mapPage = objectMapper.convertValue(programmePage, Map.class);

    when(pageRepository.findById(BOOK_CLUB.toPageId())).thenReturn(Optional.of(mapPage));

    var response = service.getProgramme(BOOK_CLUB);

    assertThat(response).isEqualTo(programmePage);
  }

  @Test
  @DisplayName(
      "Given no programme page in the repository, when getting a programme, "
          + "then throw ContentNotFoundException")
  void shouldThrowContentNotFoundExceptionWhenProgrammeIsMissing() {
    when(pageRepository.findById(BOOK_CLUB.toPageId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> service.getProgramme(BOOK_CLUB))
        .isInstanceOf(ContentNotFoundException.class);
  }

  @Test
  @DisplayName("Given a programme page, when updating it, then convert and persist the page")
  @SuppressWarnings("unchecked")
  void shouldConvertAndUpdateProgrammePage() {
    var programmePage = createProgrammePageTest("init-data/bookClubPage.json");
    var pageMap = objectMapper.convertValue(programmePage, Map.class);
    when(pageRepository.update(eq(BOOK_CLUB.toPageId()), anyMap())).thenReturn(pageMap);

    var result = service.update(BOOK_CLUB, programmePage);

    assertThat(result).isEqualTo(pageMap);
    verify(pageRepository).update(eq(BOOK_CLUB.toPageId()), anyMap());
  }

  @Test
  @DisplayName(
      "Given an invalid programme page, when updating it, "
          + "then throw PlatformInternalException")
  void shouldWrapUpdateConversionFailure() {
    var mapper = mock(ObjectMapper.class);
    var failingService = new ProgrammeService(pageRepository, mapper);
    var page = new Object();
    when(mapper.convertValue(page, Map.class))
        .thenThrow(new IllegalArgumentException("invalid programme"));

    assertThatThrownBy(() -> failingService.update(BOOK_CLUB, page))
        .isInstanceOf(PlatformInternalException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Given a programme page, when creating it, then convert and persist the page")
  @SuppressWarnings("unchecked")
  void shouldConvertAndCreateProgrammePage() {
    var programmePage = createProgrammePageTest("init-data/bookClubPage.json");
    var pageMap = objectMapper.convertValue(programmePage, Map.class);
    when(pageRepository.create(anyMap())).thenReturn(pageMap);

    var result = service.create(BOOK_CLUB, programmePage);

    assertThat(result).isEqualTo(pageMap);
    verify(pageRepository).create(anyMap());
  }

  @Test
  @DisplayName(
      "Given an invalid programme page, when creating it, "
          + "then throw PlatformInternalException")
  void shouldWrapCreateConversionFailure() {
    var mapper = mock(ObjectMapper.class);
    var failingService = new ProgrammeService(pageRepository, mapper);
    var page = new Object();
    when(mapper.convertValue(page, Map.class))
        .thenThrow(new IllegalArgumentException("invalid programme"));

    assertThatThrownBy(() -> failingService.create(BOOK_CLUB, page))
        .isInstanceOf(PlatformInternalException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName(
      "Given no programmes page in the repository, when getting all programmes, "
          + "then return the fallback page")
  void shouldReturnFallbackWhenAllProgrammesPageIsMissing() {
    var fallback = new ProgrammesPage();
    when(pageRepository.findById(PROGRAMMES.getId())).thenReturn(Optional.empty());
    when(pageRepository.getFallback(PROGRAMMES, ProgrammesPage.class, objectMapper))
        .thenReturn(fallback);

    var result = service.getAllProgrammes();

    assertThat(result).isSameAs(fallback);
  }

  @Test
  @DisplayName(
      "Given an invalid programmes page, when getting all programmes, "
          + "then throw PlatformInternalException")
  void shouldWrapAllProgrammesConversionFailure() {
    Map<String, Object> invalidPage = Map.of("invalid", "page");
    when(pageRepository.findById(PROGRAMMES.getId())).thenReturn(Optional.of(invalidPage));
    var mapper = mock(ObjectMapper.class);
    var failingService = new ProgrammeService(pageRepository, mapper);
    when(mapper.convertValue(invalidPage, ProgrammesPage.class))
        .thenThrow(new IllegalArgumentException("invalid programmes"));

    assertThatThrownBy(failingService::getAllProgrammes)
        .isInstanceOf(PlatformInternalException.class)
        .hasCauseInstanceOf(IllegalArgumentException.class);
  }
}
