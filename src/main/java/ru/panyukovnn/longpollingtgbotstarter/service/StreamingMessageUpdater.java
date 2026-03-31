package ru.panyukovnn.longpollingtgbotstarter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Управляет жизненным циклом одного стримящегося сообщения в Telegram.
 * Накапливает токены в буфере и периодически обновляет сообщение через editMessageText
 */
public class StreamingMessageUpdater {

    private static final Logger log = LoggerFactory.getLogger(StreamingMessageUpdater.class);

    private static final String CURSOR = "▍";

    private final TelegramClient telegramClient;
    private final TgSender tgSender;
    private final Long chatId;
    private final Integer messageId;
    private final long updateIntervalMs;
    private final ScheduledExecutorService scheduler;

    private final StringBuilder buffer = new StringBuilder();
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);

    private ScheduledFuture<?> flushTask;

    public StreamingMessageUpdater(TelegramClient telegramClient,
                                   TgSender tgSender,
                                   Long chatId,
                                   Integer messageId,
                                   long updateIntervalMs) {
        this.telegramClient = telegramClient;
        this.tgSender = tgSender;
        this.chatId = chatId;
        this.messageId = messageId;
        this.updateIntervalMs = updateIntervalMs;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "streaming-updater-" + chatId + "-" + messageId);
            thread.setDaemon(true);

            return thread;
        });

        startFlushScheduler();
    }

    /**
     * Добавляет токен в буфер для последующей отправки
     */
    public void appendToken(String token) {
        if (completed.get()) {
            log.warn("Попытка добавить токен в завершённый StreamingMessageUpdater для чата '{}', сообщения '{}'",
                    chatId, messageId);

            return;
        }

        lock.lock();
        try {
            buffer.append(token);
            dirty.set(true);
        } finally {
            lock.unlock();
        }
    }

    /**
     * Завершает стриминг: отправляет финальное обновление с полным текстом без курсора
     */
    public void complete() {
        if (completed.compareAndSet(false, true)) {
            stopFlushScheduler();
            flush(false);
        }
    }

    /**
     * Возвращает текущее содержимое буфера
     */
    public String getCurrentText() {
        lock.lock();
        try {
            return buffer.toString();
        } finally {
            lock.unlock();
        }
    }

    private void startFlushScheduler() {
        flushTask = scheduler.scheduleAtFixedRate(
                () -> flush(true),
                updateIntervalMs,
                updateIntervalMs,
                TimeUnit.MILLISECONDS
        );
    }

    private void stopFlushScheduler() {
        if (flushTask != null) {
            flushTask.cancel(false);
        }

        scheduler.shutdown();
    }

    private void flush(boolean withCursor) {
        if (!dirty.compareAndSet(true, false)) {
            return;
        }

        String text;
        lock.lock();
        try {
            text = buffer.toString();
        } finally {
            lock.unlock();
        }

        if (text.isEmpty()) {
            return;
        }

        String displayText = withCursor ? text + CURSOR : text;

        try {
            String markdownV2Text = tgSender.convertMarkdownToTelegramMarkdownV2(displayText);

            executeEdit(markdownV2Text, ParseMode.MARKDOWNV2);
        } catch (Exception e) {
            log.warn("Ошибка при обновлении стримящегося сообщения с MarkdownV2 в чате '{}', сообщение '{}', " +
                            "пробуем HTML: {}",
                    chatId, messageId, e.getMessage());

            try {
                String htmlText = tgSender.escapeHtml(displayText);

                executeEdit(htmlText, ParseMode.HTML);
            } catch (Exception htmlException) {
                log.warn("Ошибка при обновлении стримящегося сообщения с HTML в чате '{}', сообщение '{}': {}",
                        chatId, messageId, htmlException.getMessage());
            }
        }
    }

    private void executeEdit(String text, String parseMode) throws Exception {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(messageId)
                .text(text)
                .parseMode(parseMode)
                .build();

        telegramClient.execute(editMessage);
    }
}