package ru.panyukovnn.longpollingtgbotstarter;

import org.springframework.boot.ApplicationRunner;
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
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;

import java.util.List;

@AutoConfiguration
@EnableConfigurationProperties(TgBotProperties.class)
public class LongPollingTgBotStarterAutoConfiguration {

    @Bean
    public TgBotApi botApi(ApplicationEventPublisher eventPublisher, TgBotProperties botProperties) throws TelegramApiException {
        TgBotApi botApi = new TgBotApi(eventPublisher, botProperties.getName(), botProperties.getToken());

        new TelegramBotsApi(DefaultBotSession.class).registerBot(botApi);

        return botApi;
    }

    @Bean
    public TgSender tgSender(TgBotApi botApi) {
        return new TgSender(botApi);
    }

    /**
     * Регистрирует команды в боте после старта приложения
     *
     * @param botApi интерфейс бота
     * @param commands список команд
     * @return ignore
     */
    @Bean
    public ApplicationRunner commandRegistrar(TgBotApi botApi, List<BotCommand> commands) {
        return args -> commands.forEach(botApi::register);
    }
}
