package com.wcc.platform.service;

import static com.wcc.platform.utils.MenteeApplicationTestBuilder.baseBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.wcc.platform.domain.platform.mentorship.ApplicationStatus;
import com.wcc.platform.domain.platform.mentorship.MatchStatus;
import com.wcc.platform.domain.platform.mentorship.Mentee;
import com.wcc.platform.domain.platform.mentorship.MenteeApplication;
import com.wcc.platform.domain.platform.mentorship.Mentor;
import com.wcc.platform.domain.platform.mentorship.MentorshipCycleEntity;
import com.wcc.platform.domain.platform.mentorship.MentorshipMatch;
import com.wcc.platform.domain.platform.mentorship.MentorshipType;
import com.wcc.platform.repository.MenteeApplicationRepository;
import com.wcc.platform.repository.MenteeRepository;
import com.wcc.platform.repository.MentorRepository;
import com.wcc.platform.repository.MentorshipCycleRepository;
import com.wcc.platform.repository.MentorshipMatchRepository;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MentorshipMatchingServiceTest {

  private static final Long MENTEE_ID = 10L;
  private static final Long MENTOR_ID = 20L;
  private static final Long CYCLE_ID = 5L;
  private static final Long APPLICATION_ID = 1L;
  private static final Long MATCH_ID = 100L;
  private static final LocalDate FIXED_DATE = LocalDate.of(2026, Month.JANUARY, 1);

  @Mock private MentorshipMatchRepository matchRepository;
  @Mock private MenteeApplicationRepository applicationRepository;
  @Mock private MentorshipCycleRepository cycleRepository;
  @Mock private MentorshipService mentorshipService;
  @Mock private MenteeRepository menteeRepository;
  @Mock private MentorRepository mentorRepository;
  @Mock private MentorshipNotificationService notificationService;

  @InjectMocks private MentorshipMatchingService service;

  @BeforeEach
  void setUp() {
    lenient().when(mentorshipService.getNotificationService()).thenReturn(notificationService);
    lenient().when(mentorshipService.getMentorRepository()).thenReturn(mentorRepository);
  }

  @Test
  @DisplayName(
      "Given a MENTOR_ACCEPTED application, when confirmMatch is called, "
          + "then sendMatchUpdate is called with the created match")
  void shouldSendMatchUpdateOnConfirmMatch() {
    final MenteeApplication application =
        baseBuilder(APPLICATION_ID, MENTEE_ID, MENTOR_ID, 1)
            .status(ApplicationStatus.MENTOR_ACCEPTED)
            .build();
    final MentorshipCycleEntity cycle =
        MentorshipCycleEntity.builder().cycleId(CYCLE_ID).maxMenteesPerMentor(3).build();
    final MentorshipMatch createdMatch =
        MentorshipMatch.builder()
            .matchId(MATCH_ID)
            .mentorId(MENTOR_ID)
            .menteeId(MENTEE_ID)
            .cycleId(CYCLE_ID)
            .applicationId(APPLICATION_ID)
            .status(MatchStatus.ACTIVE)
            .startDate(FIXED_DATE)
            .build();
    final Mentee mentee =
        Mentee.menteeBuilder().id(MENTEE_ID).spokenLanguages(List.of("ENGLISH")).build();

    when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
    when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
    when(matchRepository.countActiveMenteesByMentorAndCycle(MENTOR_ID, CYCLE_ID)).thenReturn(0);
    when(matchRepository.isMenteeMatchedInCycle(MENTEE_ID, CYCLE_ID)).thenReturn(false);
    when(matchRepository.create(any(MentorshipMatch.class))).thenReturn(createdMatch);
    when(applicationRepository.findByMenteeAndCycleOrderByPriority(MENTEE_ID, CYCLE_ID))
        .thenReturn(List.of(application));
    when(mentorRepository.findById(MENTOR_ID))
        .thenReturn(Optional.of(Mentor.mentorBuilder().build()));
    when(menteeRepository.findById(MENTEE_ID)).thenReturn(Optional.of(mentee));

    service.confirmMatch(APPLICATION_ID);

    verify(notificationService).sendMatchUpdate(Optional.empty(), createdMatch);
  }

  @Test
  @DisplayName(
      "Given a MENTOR_ACCEPTED application, when confirmMatch is called, "
          + "then sendPairingConfirmation is called with mentor, mentee and cycle")
  void shouldSendPairingConfirmationOnConfirmMatch() {
    final MenteeApplication application =
        baseBuilder(APPLICATION_ID, MENTEE_ID, MENTOR_ID, 1)
            .status(ApplicationStatus.MENTOR_ACCEPTED)
            .build();
    final MentorshipCycleEntity cycle =
        MentorshipCycleEntity.builder().cycleId(CYCLE_ID).maxMenteesPerMentor(3).build();
    final Mentor mentor = Mentor.mentorBuilder().build();
    final Mentee mentee =
        Mentee.menteeBuilder().id(MENTEE_ID).spokenLanguages(List.of("ENGLISH")).build();

    when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
    when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
    when(matchRepository.countActiveMenteesByMentorAndCycle(MENTOR_ID, CYCLE_ID)).thenReturn(0);
    when(matchRepository.isMenteeMatchedInCycle(MENTEE_ID, CYCLE_ID)).thenReturn(false);
    when(matchRepository.create(any(MentorshipMatch.class)))
        .thenReturn(
            MentorshipMatch.builder()
                .matchId(MATCH_ID)
                .mentorId(MENTOR_ID)
                .menteeId(MENTEE_ID)
                .cycleId(CYCLE_ID)
                .applicationId(APPLICATION_ID)
                .status(MatchStatus.ACTIVE)
                .startDate(FIXED_DATE)
                .build());
    when(applicationRepository.findByMenteeAndCycleOrderByPriority(MENTEE_ID, CYCLE_ID))
        .thenReturn(List.of(application));
    when(mentorRepository.findById(MENTOR_ID)).thenReturn(Optional.of(mentor));
    when(menteeRepository.findById(MENTEE_ID)).thenReturn(Optional.of(mentee));

    service.confirmMatch(APPLICATION_ID);

    verify(notificationService).sendPairingConfirmation(mentor, mentee, cycle);
  }

  @Test
  @DisplayName(
      "Given an AD_HOC cycle and MENTOR_REVIEWING application, "
          + "when confirmMatch is called, then match is created successfully")
  void shouldConfirmMatchForAdHocCycleWithMentorReviewingStatus() {
    final MenteeApplication application =
        baseBuilder(APPLICATION_ID, MENTEE_ID, MENTOR_ID, 1)
            .status(ApplicationStatus.MENTOR_REVIEWING)
            .build();
    final MentorshipCycleEntity cycle =
        MentorshipCycleEntity.builder()
            .cycleId(CYCLE_ID)
            .mentorshipType(MentorshipType.AD_HOC)
            .maxMenteesPerMentor(3)
            .build();
    final Mentee mentee =
        Mentee.menteeBuilder().id(MENTEE_ID).spokenLanguages(List.of("ENGLISH")).build();
    final MentorshipMatch createdMatch =
        MentorshipMatch.builder()
            .matchId(MATCH_ID)
            .mentorId(MENTOR_ID)
            .menteeId(MENTEE_ID)
            .cycleId(CYCLE_ID)
            .applicationId(APPLICATION_ID)
            .status(MatchStatus.ACTIVE)
            .startDate(FIXED_DATE)
            .build();

    when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
    when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
    when(matchRepository.countActiveMenteesByMentorAndCycle(MENTOR_ID, CYCLE_ID)).thenReturn(0);
    when(matchRepository.isMenteeMatchedInCycle(MENTEE_ID, CYCLE_ID)).thenReturn(false);
    when(matchRepository.create(any(MentorshipMatch.class))).thenReturn(createdMatch);
    when(applicationRepository.findByMenteeAndCycleOrderByPriority(MENTEE_ID, CYCLE_ID))
        .thenReturn(List.of(application));
    when(mentorRepository.findById(MENTOR_ID))
        .thenReturn(Optional.of(Mentor.mentorBuilder().build()));
    when(menteeRepository.findById(MENTEE_ID)).thenReturn(Optional.of(mentee));

    service.confirmMatch(APPLICATION_ID);

    verify(matchRepository).create(any(MentorshipMatch.class));
  }

  @Test
  @DisplayName(
      "Given a LONG_TERM cycle and MENTOR_REVIEWING application, "
          + "when confirmMatch is called, then IllegalStateException is thrown")
  void shouldThrowExceptionForLongTermCycleWithMentorReviewingStatus() {
    final MenteeApplication application =
        baseBuilder(APPLICATION_ID, MENTEE_ID, MENTOR_ID, 1)
            .status(ApplicationStatus.MENTOR_REVIEWING)
            .build();
    final MentorshipCycleEntity cycle =
        MentorshipCycleEntity.builder()
            .cycleId(CYCLE_ID)
            .mentorshipType(MentorshipType.LONG_TERM)
            .maxMenteesPerMentor(3)
            .build();

    when(applicationRepository.findById(APPLICATION_ID)).thenReturn(Optional.of(application));
    when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));

    assertThatThrownBy(() -> service.confirmMatch(APPLICATION_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("MENTOR_ACCEPTED");
  }

  @Test
  @DisplayName(
      "Given an active match, when completing it, then update the match and notify listeners")
  void shouldCompleteActiveMatch() {
    var match = activeMatch();
    var updatedMatch = matchWithStatus(MatchStatus.COMPLETED, 0);
    when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
    when(matchRepository.update(eq(MATCH_ID), any(MentorshipMatch.class))).thenReturn(updatedMatch);

    var result = service.completeMatch(MATCH_ID, "Goals achieved");

    assertThat(result).isSameAs(updatedMatch);
    verify(matchRepository).update(eq(MATCH_ID), any(MentorshipMatch.class));
    verify(notificationService)
        .sendMatchUpdate(
            eq(Optional.of(match)),
            argThat(updated -> updated.getStatus() == MatchStatus.COMPLETED));
  }

  @Test
  @DisplayName(
      "Given a completed match, when completing it again, then reject the invalid transition")
  void shouldRejectCompletingNonActiveMatch() {
    var match = matchWithStatus(MatchStatus.COMPLETED, 0);
    when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

    assertThatThrownBy(() -> service.completeMatch(MATCH_ID, "Already completed"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("ACTIVE");

    verify(matchRepository, never()).update(any(), any());
  }

  @Test
  @DisplayName(
      "Given an active match, when cancelling it, then persist cancellation details and notify")
  void shouldCancelActiveMatch() {
    var match = activeMatch();
    var updatedMatch = matchWithStatus(MatchStatus.CANCELLED, 0);
    when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
    when(matchRepository.update(eq(MATCH_ID), any(MentorshipMatch.class))).thenReturn(updatedMatch);

    var result = service.cancelMatch(MATCH_ID, "Schedule conflict", "MENTEE");

    assertThat(result).isSameAs(updatedMatch);
    verify(matchRepository).update(eq(MATCH_ID), any(MentorshipMatch.class));
    verify(notificationService)
        .sendMatchUpdate(
            eq(Optional.of(match)),
            argThat(
                updated ->
                    updated.getStatus() == MatchStatus.CANCELLED
                        && "Schedule conflict".equals(updated.getCancellationReason())
                        && "MENTEE".equals(updated.getCancelledBy())));
  }

  @Test
  @DisplayName("Given a cancelled match, when cancelling it again, then reject the terminal state")
  void shouldRejectCancellingTerminalMatch() {
    var match = matchWithStatus(MatchStatus.CANCELLED, 0);
    when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

    assertThatThrownBy(() -> service.cancelMatch(MATCH_ID, "Duplicate request", "ADMIN"))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("terminal state");

    verify(matchRepository, never()).update(any(), any());
  }

  @Test
  @DisplayName("Given an active match, when incrementing sessions, then increase the session count")
  void shouldIncrementSessionCountForActiveMatch() {
    var match = matchWithStatus(MatchStatus.ACTIVE, 2);
    var updatedMatch = matchWithStatus(MatchStatus.ACTIVE, 3);
    when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));
    when(matchRepository.update(eq(MATCH_ID), any(MentorshipMatch.class))).thenReturn(updatedMatch);

    var result = service.incrementSessionCount(MATCH_ID);

    assertThat(result).isSameAs(updatedMatch);
    verify(matchRepository).update(eq(MATCH_ID), any(MentorshipMatch.class));
  }

  @Test
  @DisplayName("Given a completed match, when incrementing sessions, then reject session tracking")
  void shouldRejectIncrementingSessionCountForInactiveMatch() {
    var match = matchWithStatus(MatchStatus.COMPLETED, 2);
    when(matchRepository.findById(MATCH_ID)).thenReturn(Optional.of(match));

    assertThatThrownBy(() -> service.incrementSessionCount(MATCH_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("active matches");

    verify(matchRepository, never()).update(any(), any());
  }

  @Test
  @DisplayName(
      "Given match repository queries, when retrieving matches, then delegate and return results")
  void shouldDelegateMatchRetrievalQueries() {
    var match = activeMatch();
    var matches = List.of(match);
    when(matchRepository.findActiveMenteesByMentor(MENTOR_ID)).thenReturn(matches);
    when(matchRepository.findActiveMentorByMentee(MENTEE_ID)).thenReturn(Optional.of(match));
    when(matchRepository.findByCycle(CYCLE_ID)).thenReturn(matches);

    assertThat(service.getActiveMentorMatches(MENTOR_ID)).containsExactly(match);
    assertThat(service.getActiveMenteeMatch(MENTEE_ID)).isSameAs(match);
    assertThat(service.getCycleMatches(CYCLE_ID)).containsExactly(match);

    verify(matchRepository).findActiveMenteesByMentor(MENTOR_ID);
    verify(matchRepository).findActiveMentorByMentee(MENTEE_ID);
    verify(matchRepository).findByCycle(CYCLE_ID);
  }

  private MentorshipMatch activeMatch() {
    return matchWithStatus(MatchStatus.ACTIVE, 0);
  }

  private MentorshipMatch matchWithStatus(final MatchStatus status, final int totalSessions) {
    return MentorshipMatch.builder()
        .matchId(MATCH_ID)
        .mentorId(MENTOR_ID)
        .menteeId(MENTEE_ID)
        .cycleId(CYCLE_ID)
        .applicationId(APPLICATION_ID)
        .status(status)
        .startDate(FIXED_DATE)
        .expectedEndDate(FIXED_DATE.plusMonths(6))
        .sessionFrequency("Weekly")
        .totalSessions(totalSessions)
        .build();
  }
}
