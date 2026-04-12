package ru.panyukovnn.longpollingtgbotstarter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendChatAction;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.time.Instant;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Отправляет действие typing периодически, пока задача не будет остановлена
 */
public class TypingIndicator {

    private static final Logger log = LoggerFactory.getLogger(TypingIndicator.class);

    static final int TYPING_REFRESH_INTERVAL_SECONDS = 4;
    static final int MAX_TYPING_LIFETIME_SECONDS = 30;

    private final TelegramClient telegramClient;
    private final ScheduledExecutorService typingScheduler;
    private final Supplier<Instant> nowSupplier;

    private volatile ScheduledFuture<?> typingTask;
    private volatile Instant deadline;

    public TypingIndicator(TelegramClient telegramClient,
                           ScheduledExecutorService typingScheduler) {
        this(telegramClient, typingScheduler, Instant::now);
    }

    /**
     * Конструктор с возможностью подмены источника текущего времени для тестирования
     */
    TypingIndicator(TelegramClient telegramClient,
                    ScheduledExecutorService typingScheduler,
                    Supplier<Instant> nowSupplier) {
        this.telegramClient = telegramClient;
        this.typingScheduler = typingScheduler;
        this.nowSupplier = nowSupplier;
    }

    /**
     * Отправляет действие typing немедленно и запускает периодическую отправку каждые 4 секунды.
     * Возвращает задачу для последующей отмены через {@link #stop(ScheduledFuture)}
     */
    public ScheduledFuture<?> start(Long chatId) {
        sendTypingAction(chatId);
        deadline = nowSupplier.get().plusSeconds(MAX_TYPING_LIFETIME_SECONDS);

        typingTask = typingScheduler.scheduleAtFixedRate(
            () -> sendTypingAction(chatId),
            TYPING_REFRESH_INTERVAL_SECONDS,
            TYPING_REFRESH_INTERVAL_SECONDS,
            TimeUnit.SECONDS
        );

        return typingTask;
    }

    /**
     * Останавливает периодическую отправку статуса typing
     */
    public void stop(ScheduledFuture<?> typingTask) {
        if (typingTask != null) {
            typingTask.cancel(false);
        }
    }

    void sendTypingAction(Long chatId) {
        if (deadline != null && nowSupplier.get().isAfter(deadline)) {
            log.info("Дедлайн typing истёк для чата {}, задача отменена", chatId);
            stop(typingTask);

            return;
        }

        try {
            telegramClient.execute(SendChatAction.builder()
                .chatId(String.valueOf(chatId))
                .action("typing")
                .build());
        } catch (Exception e) {
            log.warn("Не удалось отправить статус typing для чата {}", chatId, e);
        }
    }
}