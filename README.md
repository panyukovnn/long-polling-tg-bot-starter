# Long Polling Telegram Bot Spring Boot Starter

Spring Boot Starter для быстрого создания Telegram ботов с использованием Long Polling механизма.

## Подключение

Добавьте зависимость в ваш `build.gradle`:

```gradle
dependencies {
    implementation 'ru.panyukovnn:long-polling-tg-bot-starter:VERSION'
}
```

## Настройка

Добавьте обязательные настройки бота в `application.yml`:

```yaml
telegram:
  bot:
    name: your_bot_name
    token: your_bot_token
```

Или в `application.properties`:

```properties
telegram.bot.name=your_bot_name
telegram.bot.token=your_bot_token
```

## Использование

### Обработка входящих сообщений

Создайте слушатель для обработки входящих сообщений от Telegram

- рекомендуется использовать асинхронную обработку сообщений, например, с помощью аннотации `@Async`

```java
@Slf4j
@Service
@RequiredArgsConstructor
public class TgBotListener {

    private final TgBotApi botApi;

    @Async("tgBotListenerExecutor")
    @EventListener(Update.class)
    public void onUpdate(Update update) {
        Long userId = Optional.ofNullable(update.getMessage())
                .map(Message::getFrom)
                .map(User::getId)
                .orElse(0L);
        String messageText = Optional.ofNullable(update.getMessage())
                .map(Message::getText)
                .orElse("Не удалось извлечь текст сообщения");

        log.info("Received message from user: {}. Text: {}", userId, messageText);

        try {
            Long chatId = Optional.ofNullable(update.getMessage())
                    .map(Message::getChatId)
                    .orElseThrow();

            botApi.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("Message received")
                    .build());
        } catch (TelegramApiException e) {
            log.error(e.getMessage(), e);
        }
    }
}
```

### Создание команд

- Для создания команд бота используйте класс `BotCommand` из библиотеки Telegram Bot API.
- Все команды, созданные в качестве бинов, автоматически регистрируются в боте.
- Бин `TgSender` в командах необходимо помечать как `@Lazy`, т.к. из-за ограничения библиотеки он зависит от `TgBotApi` бина, в котором региструются команды, что приводит к циклической зависимости бинов

```java
@Slf4j
@Service
public class StartCommand extends BotCommand {

    private final TgSender tgSender;
    
    public StartCommand(@Lazy TgSender tgSender) {
        super("start", "Start command");
        this.tgSender = tgSender;
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] strings) {
        tgSender.send(chat.getId(), "Hello, friend!");
    }
}
```


### Отправка сообщений через TgSender

Для отправки сообщений в Telegram используйте сервис `TgSender`:

```java
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final TgSender tgSender;

    public NotificationService(TgSender tgSender) {
        this.tgSender = tgSender;
    }

    public void sendNotification(Long chatId, String message) {
        // Отправляет сообщение с поддержкой Markdown форматирования
        tgSender.send(chatId, message);
    }

    public void sendFormattedMessage(Long chatId) {
        String message = """
            *Жирный текст*
            _Курсив_
            `Код`
            [Ссылка](https://example.com)
            """;

        tgSender.send(chatId, message);
    }
}
```

**Возможности TgSender**:
- Автоматическая конвертация обычного Markdown в Telegram MarkdownV2
- Fallback на HTML при ошибках форматирования
- Автоматическая обрезка длинных сообщений (более 4096 символов)
- Поддержка базового форматирования: жирный, курсив, код, ссылки


