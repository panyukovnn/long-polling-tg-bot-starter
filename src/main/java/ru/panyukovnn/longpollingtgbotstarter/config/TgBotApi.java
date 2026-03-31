package ru.panyukovnn.longpollingtgbotstarter.config;

import org.springframework.context.ApplicationEventPublisher;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

/**
 * Обработчик входящих обновлений от Telegram, публикует каждое обновление как Spring событие
 */
public class TgBotApi implements LongPollingUpdateConsumer {

    private final ApplicationEventPublisher eventPublisher;

    public TgBotApi(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(eventPublisher::publishEvent);
    }
}
