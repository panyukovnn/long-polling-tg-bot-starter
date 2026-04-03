package ru.panyukovnn.longpollingtgbotstarter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import org.springframework.lang.Nullable;

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
    /**
     * Порог для разбиения текста на несколько сообщений при стриминге.
     * Оставляем запас для экранирования спецсимволов при конвертации в MarkdownV2
     */
    static final int STREAMING_SPLIT_THRESHOLD = TgSender.MAX_TG_MESSAGE_LENGTH - 600;

    private final TelegramClient telegramClient;
    private final TgSender tgSender;
    private final Long chatId;
    private final long updateIntervalMs;
    private final ScheduledExecutorService scheduler;

    private final StringBuilder buffer = new StringBuilder();
    private final ReentrantLock lock = new ReentrantLock();
    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean completed = new AtomicBoolean(false);

    private volatile int currentMessageId;
    private volatile int finalizedLength;
    private ScheduledFuture<?> flushTask;
    private ScheduledFuture<?> typingTask;

    public StreamingMessageUpdater(TelegramClient telegramClient,
                                   TgSender tgSender,
                                   Long chatId,
                                   Integer messageId,
                                   long updateIntervalMs) {
        this(telegramClient, tgSender, chatId, messageId, updateIntervalMs, null);
    }

    public StreamingMessageUpdater(TelegramClient telegramClient,
                                   TgSender tgSender,
                                   Long chatId,
                                   Integer messageId,
                                   long updateIntervalMs,
                                   @Nullable TypingIndicator typingIndicator) {
        this.telegramClient = telegramClient;
        this.tgSender = tgSender;
        this.chatId = chatId;
        this.currentMessageId = messageId;
        this.updateIntervalMs = updateIntervalMs;
        this.finalizedLength = 0;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "streaming-updater-" + chatId + "-" + messageId);
            thread.setDaemon(true);

            return thread;
        });

        if (typingIndicator != null) {
            this.typingTask = typingIndicator.start(chatId);
        }

        startFlushScheduler();
    }

    /**
     * Добавляет токен в буфер для последующей отправки
     */
    public void appendToken(String token) {
        if (completed.get()) {
            log.warn("Попытка добавить токен в завершённый StreamingMessageUpdater для чата '{}', сообщения '{}'",
                    chatId, currentMessageId);

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
            stopTyping();
            stopFlushScheduler();
            dirty.set(true);
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

    private void stopTyping() {
        if (typingTask != null) {
            typingTask.cancel(false);
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

        String fullText;
        lock.lock();
        try {
            fullText = buffer.toString();
        } finally {
            lock.unlock();
        }

        if (fullText.isEmpty()) {
            return;
        }

        try {
            splitAndFlush(fullText, withCursor);
        } catch (Exception e) {
            log.warn("Ошибка при обновлении стримящегося сообщения в чате '{}', сообщение '{}': {}",
                    chatId, currentMessageId, e.getMessage());
        }
    }

    /**
     * Разбивает текст на сегменты при превышении лимита и отправляет каждый в отдельное сообщение
     */
    private void splitAndFlush(String fullText, boolean withCursor) {
        String currentSegment = fullText.substring(finalizedLength);

        while (currentSegment.length() > STREAMING_SPLIT_THRESHOLD) {
            int splitPos = tgSender.findSplitPosition(currentSegment, STREAMING_SPLIT_THRESHOLD);
            String partRaw = currentSegment.substring(0, splitPos);

            finalizeCurrentMessage(partRaw);

            finalizedLength += splitPos;
            currentSegment = fullText.substring(finalizedLength);

            try {
                sendNewStreamingMessage();
            } catch (Exception e) {
                log.warn("Ошибка при создании нового сообщения для продолжения стриминга в чате '{}': {}",
                        chatId, e.getMessage());

                return;
            }
        }

        if (currentSegment.isEmpty()) {
            return;
        }

        String displayText = withCursor ? currentSegment + CURSOR : currentSegment;

        editCurrentMessage(displayText);
    }

    /**
     * Финализирует текущее сообщение: закрывает незакрытые markdown-теги и отправляет
     */
    private void finalizeCurrentMessage(String rawText) {
        try {
            String converted = tgSender.convertMarkdownToTelegramMarkdownV2(rawText);
            String fixed = tgSender.fixUnclosedMarkdownTags(converted);

            executeEdit(fixed, ParseMode.MARKDOWNV2);
        } catch (Exception e) {
            log.warn("Ошибка при финализации сообщения '{}' в чате '{}' с MarkdownV2, пробуем HTML: {}",
                    currentMessageId, chatId, e.getMessage());

            try {
                String htmlText = tgSender.escapeHtml(rawText);

                executeEdit(htmlText, ParseMode.HTML);
            } catch (Exception htmlException) {
                log.warn("Ошибка при финализации сообщения '{}' в чате '{}' с HTML: {}",
                        currentMessageId, chatId, htmlException.getMessage());
            }
        }
    }

    /**
     * Отправляет новое сообщение-заглушку и обновляет currentMessageId для продолжения стриминга
     */
    private void sendNewStreamingMessage() throws Exception {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .text(CURSOR)
                .build();

        Message sentMessage = telegramClient.execute(sendMessage);
        int newMessageId = sentMessage.getMessageId();

        log.info("Создано новое сообщение '{}' для продолжения стриминга в чате '{}'",
                newMessageId, chatId);

        currentMessageId = newMessageId;
    }

    /**
     * Редактирует текущее сообщение с попыткой MarkdownV2, затем HTML при ошибке
     */
    private void editCurrentMessage(String displayText) {
        try {
            String markdownV2Text = tgSender.convertMarkdownToTelegramMarkdownV2(displayText);

            executeEdit(markdownV2Text, ParseMode.MARKDOWNV2);
        } catch (Exception e) {
            log.warn("Ошибка при обновлении стримящегося сообщения с MarkdownV2 в чате '{}', сообщение '{}', "
                            + "пробуем HTML: {}",
                    chatId, currentMessageId, e.getMessage());

            try {
                String htmlText = tgSender.escapeHtml(displayText);

                executeEdit(htmlText, ParseMode.HTML);
            } catch (Exception htmlException) {
                log.warn("Ошибка при обновлении стримящегося сообщения с HTML в чате '{}', сообщение '{}': {}",
                        chatId, currentMessageId, htmlException.getMessage());
            }
        }
    }

    private void executeEdit(String text, String parseMode) throws Exception {
        EditMessageText editMessage = EditMessageText.builder()
                .chatId(chatId)
                .messageId(currentMessageId)
                .text(text)
                .parseMode(parseMode)
                .build();

        telegramClient.execute(editMessage);
    }
}