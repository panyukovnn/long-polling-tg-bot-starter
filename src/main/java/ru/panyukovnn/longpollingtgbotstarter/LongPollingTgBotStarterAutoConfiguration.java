package ru.panyukovnn.longpollingtgbotstarter;

import okhttp3.OkHttpClient;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;
import ru.panyukovnn.longpollingtgbotstarter.config.TgBotApi;
import ru.panyukovnn.longpollingtgbotstarter.property.TgBotProperties;
import ru.panyukovnn.longpollingtgbotstarter.service.TgSender;
import ru.panyukovnn.longpollingtgbotstarter.service.TypingIndicator;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@AutoConfiguration
@EnableConfigurationProperties(TgBotProperties.class)
public class LongPollingTgBotStarterAutoConfiguration {

    @Bean
    public OkHttpClient okHttpClient(TgBotProperties botProperties) {
        return new OkHttpClient.Builder()
                .connectTimeout(botProperties.getConnectionTimeoutMs(), TimeUnit.MILLISECONDS)
                .readTimeout(botProperties.getSocketTimeoutMs(), TimeUnit.MILLISECONDS)
                .writeTimeout(botProperties.getRequestTimeoutMs(), TimeUnit.MILLISECONDS)
                .build();
    }

    @Bean
    public TelegramClient telegramClient(OkHttpClient okHttpClient,
                                         TgBotProperties botProperties) {
        return new OkHttpTelegramClient(okHttpClient, botProperties.getToken());
    }

    @Bean
    public TgBotApi botApi(ApplicationEventPublisher eventPublisher) {
        return new TgBotApi(eventPublisher);
    }

    @Bean
    public TelegramBotsLongPollingApplication telegramBotsApplication(
            TgBotApi botApi,
            TgBotProperties botProperties) throws TelegramApiException {
        TelegramBotsLongPollingApplication application =
                new TelegramBotsLongPollingApplication();
        application.registerBot(botProperties.getToken(), botApi);

        return application;
    }

    @Bean(destroyMethod = "shutdown")
    public ScheduledExecutorService typingScheduler() {
        return Executors.newScheduledThreadPool(3);
    }

    @Bean
    public TypingIndicator typingIndicator(TelegramClient telegramClient,
                                           ScheduledExecutorService typingScheduler) {
        return new TypingIndicator(telegramClient, typingScheduler);
    }

    @Bean
    public TgSender tgSender(TelegramClient telegramClient,
                              TgBotProperties botProperties,
                              TypingIndicator typingIndicator) {
        return new TgSender(telegramClient, botProperties, typingIndicator);
    }
}
