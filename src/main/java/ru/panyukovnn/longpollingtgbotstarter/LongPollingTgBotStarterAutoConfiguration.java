package ru.panyukovnn.longpollingtgbotstarter;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.panyukovnn.longpollingtgbotstarter.config.TgBotApi;
import ru.panyukovnn.longpollingtgbotstarter.property.TgBotProperties;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(TgBotProperties.class)
public class LongPollingTgBotStarterAutoConfiguration {

    @Bean
    public TgBotApi botApi(
            ApplicationEventPublisher eventPublisher,
            TgBotProperties botProperties,
            List<BotCommand> commands
    ) throws TelegramApiException {
        TgBotApi botApi = new TgBotApi(eventPublisher, botProperties.getName(), botProperties.getToken());

        new TelegramBotsApi(DefaultBotSession.class).registerBot(botApi);
        commands.forEach(botApi::register);

        return botApi;
    }
}
