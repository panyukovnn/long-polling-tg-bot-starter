package ru.panyukovnn.longpollingtgbotstarter.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static ru.panyukovnn.longpollingtgbotstarter.service.TypingIndicator.MAX_TYPING_LIFETIME_SECONDS;

@ExtendWith(MockitoExtension.class)
class TypingIndicatorUnitTest {

    private static final Long CHAT_ID = 123L;

    @Mock
    private TelegramClient telegramClient;

    private ScheduledExecutorService scheduler;
    private TypingIndicator typingIndicator;

    @BeforeEach
    void setUp() {
        scheduler = Executors.newScheduledThreadPool(1);
        typingIndicator = new TypingIndicator(telegramClient, scheduler);
    }

    @AfterEach
    void tearDown() {
        scheduler.shutdownNow();
    }

    @Nested
    class StartTests {

        @Test
        void when_start_then_typingActionSentImmediately() throws Exception {
            when(telegramClient.execute(any(SendChatAction.class))).thenReturn(true);

            typingIndicator.start(CHAT_ID);

            ArgumentCaptor<SendChatAction> captor = ArgumentCaptor.forClass(SendChatAction.class);
            verify(telegramClient, timeout(500)).execute(captor.capture());

            SendChatAction action = captor.getValue();
            assertThat(action.getChatId(), equalTo(String.valueOf(CHAT_ID)));
            assertThat(action.getAction(), equalTo("typing"));
        }

        @Test
        void when_start_then_returnsNonNullFuture() {
            ScheduledFuture<?> future = typingIndicator.start(CHAT_ID);

            assertThat("Должен вернуть задачу", future != null);
        }
    }

    @Nested
    class StopTests {

        @Test
        void when_stop_then_taskCancelled() {
            ScheduledFuture<?> future = typingIndicator.start(CHAT_ID);

            typingIndicator.stop(future);

            assertThat("Задача должна быть отменена", future.isCancelled());
        }

        @Test
        void when_stopWithNull_then_noException() {
            typingIndicator.stop(null);
        }
    }

    @Nested
    class DeadlineTests {

        @Mock
        private ScheduledExecutorService mockScheduler;

        @Mock
        private ScheduledFuture<?> mockFuture;

        private ArgumentCaptor<Runnable> runnableCaptor;

        @BeforeEach
        void setUpDeadline() {
            runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
            when(mockScheduler.scheduleAtFixedRate(
                runnableCaptor.capture(), anyLong(), anyLong(), any(TimeUnit.class)
            )).thenReturn((ScheduledFuture) mockFuture);
        }

        @Test
        void when_deadlineExceeded_then_taskCancelledAutomatically() throws Exception {
            when(telegramClient.execute(any(SendChatAction.class))).thenReturn(true);
            Instant baseTime = Instant.parse("2025-01-01T00:00:00Z");
            AtomicReference<Instant> currentTime = new AtomicReference<>(baseTime);

            TypingIndicator indicator = new TypingIndicator(
                telegramClient, mockScheduler, currentTime::get
            );

            indicator.start(CHAT_ID);

            // Перематываем время за дедлайн
            currentTime.set(baseTime.plusSeconds(MAX_TYPING_LIFETIME_SECONDS + 1));

            // Имитируем срабатывание периодической задачи — дедлайн уже просрочен
            runnableCaptor.getValue().run();

            verify(mockFuture).cancel(false);
        }

        @Test
        void when_deadlineNotExceeded_then_typingContinues() throws Exception {
            when(telegramClient.execute(any(SendChatAction.class))).thenReturn(true);
            Instant fixedTime = Instant.parse("2025-01-01T00:00:00Z");

            TypingIndicator indicator = new TypingIndicator(
                telegramClient, mockScheduler, () -> fixedTime
            );

            indicator.start(CHAT_ID);

            // Имитируем срабатывание периодической задачи — дедлайн ещё не наступил
            runnableCaptor.getValue().run();

            verify(mockFuture, atMost(0)).cancel(false);
            verify(telegramClient, atLeast(2)).execute(any(SendChatAction.class));
        }
    }

    @Nested
    class ErrorHandlingTests {

        @Test
        void when_telegramApiThrows_then_noExceptionPropagated() throws Exception {
            when(telegramClient.execute(any(SendChatAction.class)))
                .thenThrow(new RuntimeException("API error"));

            typingIndicator.start(CHAT_ID);

            // Не должно быть исключения — ошибка логируется
        }
    }
}