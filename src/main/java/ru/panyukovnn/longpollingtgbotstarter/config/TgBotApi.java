package ru.panyukovnn.longpollingtgbotstarter.config;

import org.springframework.context.ApplicationEventPublisher;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.objects.Update;

public class TgBotApi extends TelegramLongPollingCommandBot {

    private final ApplicationEventPublisher eventPublisher;
    private final String username;
    private final String token;

    public TgBotApi(ApplicationEventPublisher eventPublisher, String username, String token) {
        super();
        this.eventPublisher = eventPublisher;
        this.username = username;
        this.token = token;
    }

    @Override
    public String getBotUsername() {
        return this.username;
    }

    @Override
    public String getBotToken() {
        return this.token;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        eventPublisher.publishEvent(update);
    }
}
