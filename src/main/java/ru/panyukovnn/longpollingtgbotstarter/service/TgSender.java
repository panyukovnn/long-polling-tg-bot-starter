package ru.panyukovnn.longpollingtgbotstarter.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import ru.panyukovnn.longpollingtgbotstarter.config.TgBotApi;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TgSender {

    private static final Logger log = LoggerFactory.getLogger(TgSender.class);

    public static final int MAX_TG_MESSAGE_LENGTH = 4096;

    private final TgBotApi tgBotApi;

    public TgSender(TgBotApi tgBotApi) {
        this.tgBotApi = tgBotApi;
    }

    public void send(Long chatId, String message) {
        try {
            String markdownV2Message = convertMarkdownToTelegramMarkdownV2(message);

            sendMessageWithParseModeAndThrow(chatId, markdownV2Message, ParseMode.MARKDOWNV2);
            log.info("Сообщение успешно отправлено в чат '{}' с MarkdownV2. Первые 100 символов: '{}'",
                chatId, message.substring(0, Math.min(100, message.length())));
        } catch (Exception e) {
            log.warn("Ошибка при отправке с MarkdownV2 в чат '{}', пробуем отправить с HTML. Ошибка: {}",
                chatId, e.getMessage());

            sendSimpleHtmlMessage(chatId, message);
        }
    }

    protected void sendSimpleHtmlMessage(Long chatId, String message) {
        String escapedMessage = escapeHtml(message);

        try {
            sendMessageWithParseModeAndThrow(chatId, escapedMessage, ParseMode.HTML);

            log.info("Сообщение успешно отправлено в чат '{}' при Simple HTML форматировании. " +
                            "Первые 100 символов: '{}'",
                    chatId, message.substring(0, Math.min(100, message.length())));
        } catch (Exception e) {
            log.warn("Ошибка при отправке сообщения в чат '{}' при Simple HTML форматировании. " +
                            "Первые 100 символов: '{}'. Ошибка: {}",
                    chatId, message.substring(0, Math.min(100, message.length())), e.getMessage(), e);
        }
    }

    protected String escapeHtml(String text) {
        if (text == null) {
            return null;
        }

        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    /**
     * Преобразует обычный markdown в Telegram MarkdownV2 формат
     * Основные преобразования:
     * - **text** или __text__ -> *text* (жирный)
     * - *text* или _text_ -> _text_ (курсив)
     * После преобразования экранирует специальные символы
     */
    protected String convertMarkdownToTelegramMarkdownV2(String text) {
        if (text == null) {
            return null;
        }

        String result = text;

        // Используем уникальные маркеры для временной замены
        String boldMarkerStart = "\u0001BOLD_START\u0001";
        String boldMarkerEnd = "\u0001BOLD_END\u0001";
        String italicMarkerStart = "\u0001ITALIC_START\u0001";
        String italicMarkerEnd = "\u0001ITALIC_END\u0001";
        String codeMarkerStart = "\u0001CODE_START\u0001";
        String codeMarkerEnd = "\u0001CODE_END\u0001";
        String linkMarkerStart = "\u0001LINK_START\u0001";
        String linkMarkerMid = "\u0001LINK_MID\u0001";
        String linkMarkerEnd = "\u0001LINK_END\u0001";

        // Сохраняем блоки кода (```code```)
        result = result.replaceAll("```([^`]+)```", codeMarkerStart + "$1" + codeMarkerEnd);

        // Сохраняем инлайн код (`code`)
        result = result.replaceAll("`([^`]+)`", codeMarkerStart + "$1" + codeMarkerEnd);

        // Сохраняем ссылки [text](url)
        result = result.replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)",
            linkMarkerStart + "$1" + linkMarkerMid + "$2" + linkMarkerEnd);

        // Преобразуем жирный текст: **text** или __text__ -> маркеры
        result = result.replaceAll("\\*\\*([^*]+)\\*\\*", boldMarkerStart + "$1" + boldMarkerEnd);
        result = result.replaceAll("__([^_]+)__", boldMarkerStart + "$1" + boldMarkerEnd);

        // Преобразуем курсив: *text* или _text_ -> маркеры
        result = result.replaceAll("\\*([^*]+)\\*", italicMarkerStart + "$1" + italicMarkerEnd);
        result = result.replaceAll("_([^_]+)_", italicMarkerStart + "$1" + italicMarkerEnd);

        // Экранируем все специальные символы MarkdownV2
        result = escapeMarkdownV2(result);

        // Восстанавливаем форматирование из маркеров
        result = result.replace(boldMarkerStart, "*");
        result = result.replace(boldMarkerEnd, "*");
        result = result.replace(italicMarkerStart, "_");
        result = result.replace(italicMarkerEnd, "_");
        result = result.replace(codeMarkerStart, "`");
        result = result.replace(codeMarkerEnd, "`");
        result = result.replace(linkMarkerStart, "[");
        result = result.replace(linkMarkerMid, "](");
        result = result.replace(linkMarkerEnd, ")");

        return result;
    }

    /**
     * Экранирует специальные символы для Telegram MarkdownV2
     * Символы для экранирования: _ * [ ] ( ) ~ ` > # + - = | { } . !
     * Согласно документации: https://core.telegram.org/bots/api#markdownv2-style
     */
    protected String escapeMarkdownV2(String text) {
        if (text == null) {
            return null;
        }

        return text.replace("_", "\\_")
            .replace("*", "\\*")
            .replace("[", "\\[")
            .replace("]", "\\]")
            .replace("(", "\\(")
            .replace(")", "\\)")
            .replace("~", "\\~")
            .replace("`", "\\`")
            .replace(">", "\\>")
            .replace("#", "\\#")
            .replace("+", "\\+")
            .replace("-", "\\-")
            .replace("=", "\\=")
            .replace("|", "\\|")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace(".", "\\.")
            .replace("!", "\\!");
    }

    /**
     * Отправляет одно сообщение с указанным режимом парсинга
     */
    protected void sendSingleMessage(Long chatId, String message, String parseMode) throws Exception {
        SendMessage sendMessage = SendMessage.builder()
                .chatId(chatId)
                .parseMode(parseMode)
                .text(message)
                .build();

        tgBotApi.execute(sendMessage);
    }

    /**
     * Отправляет сообщение с указанным режимом парсинга и пробрасывает исключения
     */
    protected void sendMessageWithParseModeAndThrow(Long chatId, String message, String parseMode) throws Exception {
        if (message.length() <= MAX_TG_MESSAGE_LENGTH) {
            sendSingleMessage(chatId, message, parseMode);
            return;
        }

        log.info("Сообщение слишком длинное ({} символов) в чат '{}', разбиваем на части",
                message.length(), chatId);

        List<String> parts = splitMessageIntoParts(message, parseMode);

        log.info("Сообщение в чат '{}' разбито на {} частей", chatId, parts.size());

        for (int i = 0; i < parts.size(); i++) {
            sendSingleMessage(chatId, parts.get(i), parseMode);

            if (i < parts.size() - 1) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Обрезает сообщение и исправляет незакрытые markdown теги
     * Удаляет незакрытые теги в конце обрезанного сообщения
     *
     * @deprecated Метод больше не используется. Вместо обрезания сообщений
     * теперь используется разбиение на части через {@link #splitMessageIntoParts}
     */
    @Deprecated
    protected String truncateWithMarkdownFix(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        String truncated = text.substring(0, maxLength);

        // Удаляем незакрытые markdown теги в конце
        truncated = fixUnclosedMarkdownTags(truncated);

        return truncated + "...";
    }

    /**
     * Исправляет незакрытые markdown теги, удаляя их из конца строки
     * Обрабатывает основные markdown символы: *, _, `, ~, [, (
     */
    protected String fixUnclosedMarkdownTags(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // Подсчитываем количество незакрытых символов
        int asterisks = countUnescapedChar(text, '*');
        int underscores = countUnescapedChar(text, '_');
        int backticks = countUnescapedChar(text, '`');
        int tildes = countUnescapedChar(text, '~');
        int openBrackets = countUnescapedChar(text, '[');
        int closeBrackets = countUnescapedChar(text, ']');
        int openParens = countUnescapedChar(text, '(');
        int closeParens = countUnescapedChar(text, ')');

        // Если есть незакрытые теги, удаляем их с конца
        StringBuilder result = new StringBuilder(text);

        // Удаляем незакрытые парные символы с конца
        if (asterisks % 2 != 0) {
            removeLastUnescapedChar(result, '*');
        }
        if (underscores % 2 != 0) {
            removeLastUnescapedChar(result, '_');
        }
        if (backticks % 2 != 0) {
            removeLastUnescapedChar(result, '`');
        }
        if (tildes % 2 != 0) {
            removeLastUnescapedChar(result, '~');
        }

        // Удаляем незакрытые скобки с конца
        while (openBrackets > closeBrackets) {
            removeLastUnescapedChar(result, '[');
            openBrackets--;
        }
        while (openParens > closeParens) {
            removeLastUnescapedChar(result, '(');
            openParens--;
        }

        return result.toString();
    }

    /**
     * Подсчитывает количество неэкранированных символов в тексте
     */
    protected int countUnescapedChar(String text, char ch) {
        int count = 0;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == ch) {
                // Проверяем, не экранирован ли символ
                if (i == 0 || text.charAt(i - 1) != '\\') {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Удаляет последний неэкранированный символ из StringBuilder
     */
    protected void removeLastUnescapedChar(StringBuilder sb, char ch) {
        for (int i = sb.length() - 1; i >= 0; i--) {
            if (sb.charAt(i) == ch) {
                // Проверяем, не экранирован ли символ
                if (i == 0 || sb.charAt(i - 1) != '\\') {
                    sb.deleteCharAt(i);
                    return;
                }
            }
        }
    }

    /**
     * Находит оптимальную позицию для разбиения текста
     */
    protected int findSplitPosition(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text.length();
        }

        int searchStart = Math.max(0, maxLength - 200);

        int pos = text.lastIndexOf('\n', maxLength);
        if (pos >= searchStart) {
            return pos + 1;
        }

        pos = text.lastIndexOf(' ', maxLength);
        if (pos >= searchStart) {
            return pos + 1;
        }

        return maxLength;
    }

    /**
     * Извлекает открытые теги в зависимости от режима парсинга
     */
    protected List<String> extractOpenTags(String text, String parseMode) {
        List<String> openTags = new ArrayList<>();

        if (ParseMode.MARKDOWNV2.equals(parseMode)) {
            if (countUnescapedChar(text, '*') % 2 != 0) {
                openTags.add("*");
            }
            if (countUnescapedChar(text, '_') % 2 != 0) {
                openTags.add("_");
            }
            if (countUnescapedChar(text, '`') % 2 != 0) {
                openTags.add("`");
            }
            if (countUnescapedChar(text, '~') % 2 != 0) {
                openTags.add("~");
            }
        } else if (ParseMode.HTML.equals(parseMode)) {
            openTags = extractOpenHtmlTags(text);
        }

        return openTags;
    }

    /**
     * Извлекает открытые HTML теги из текста
     */
    protected List<String> extractOpenHtmlTags(String text) {
        List<String> openTags = new ArrayList<>();
        Stack<String> tagStack = new Stack<>();

        Pattern tagPattern = Pattern.compile("<(/?)([a-z]+)(?:\\s[^>]*)?>",
                Pattern.CASE_INSENSITIVE);
        Matcher matcher = tagPattern.matcher(text);

        while (matcher.find()) {
            String slash = matcher.group(1);
            String tagName = matcher.group(2);

            if (slash.isEmpty()) {
                tagStack.push(tagName);
            } else if (!tagStack.isEmpty() && tagStack.peek().equals(tagName)) {
                tagStack.pop();
            }
        }

        openTags.addAll(tagStack);

        return openTags;
    }

    /**
     * Закрывает открытые теги в конце части сообщения
     */
    protected String closeOpenTagsInPart(String part, List<String> openTags, String parseMode) {
        if (openTags.isEmpty()) {
            return part;
        }

        StringBuilder result = new StringBuilder(part);

        if (ParseMode.MARKDOWNV2.equals(parseMode)) {
            for (int i = openTags.size() - 1; i >= 0; i--) {
                result.append(openTags.get(i));
            }
        } else if (ParseMode.HTML.equals(parseMode)) {
            for (int i = openTags.size() - 1; i >= 0; i--) {
                result.append("</").append(openTags.get(i)).append(">");
            }
        }

        return result.toString();
    }

    /**
     * Открывает теги в начале следующей части сообщения
     */
    protected String reopenTagsInPart(String part, List<String> openTags, String parseMode) {
        if (openTags.isEmpty()) {
            return part;
        }

        StringBuilder result = new StringBuilder();

        if (ParseMode.MARKDOWNV2.equals(parseMode)) {
            for (String tag : openTags) {
                result.append(tag);
            }
        } else if (ParseMode.HTML.equals(parseMode)) {
            for (String tag : openTags) {
                result.append("<").append(tag).append(">");
            }
        }

        result.append(part);

        return result.toString();
    }

    /**
     * Вычисляет overhead для закрывающих тегов
     */
    protected int calculateTagsOverhead(List<String> openTags, String parseMode) {
        if (openTags.isEmpty()) {
            return 0;
        }

        int overhead = 0;

        if (ParseMode.MARKDOWNV2.equals(parseMode)) {
            overhead = openTags.size();
        } else if (ParseMode.HTML.equals(parseMode)) {
            for (String tag : openTags) {
                overhead += 3 + tag.length();
            }
        }

        return overhead;
    }

    /**
     * Разбивает длинное сообщение на части с учетом markdown/HTML тегов
     */
    protected List<String> splitMessageIntoParts(String message, String parseMode) {
        List<String> parts = new ArrayList<>();
        String remainingText = message;
        List<String> currentOpenTags = new ArrayList<>();

        while (remainingText.length() > MAX_TG_MESSAGE_LENGTH) {
            int tagsOverhead = calculateTagsOverhead(currentOpenTags, parseMode);
            int maxChunkSize = MAX_TG_MESSAGE_LENGTH - tagsOverhead;

            int splitPos = findSplitPosition(remainingText, maxChunkSize);

            String part = remainingText.substring(0, splitPos);

            List<String> openTagsInPart = extractOpenTags(part, parseMode);

            String closedPart = closeOpenTagsInPart(part, openTagsInPart, parseMode);
            parts.add(closedPart);

            currentOpenTags = openTagsInPart;

            remainingText = remainingText.substring(splitPos);

            if (!currentOpenTags.isEmpty()) {
                remainingText = reopenTagsInPart(remainingText, currentOpenTags, parseMode);
            }
        }

        if (!remainingText.isEmpty()) {
            parts.add(remainingText);
        }

        return parts;
    }
}
