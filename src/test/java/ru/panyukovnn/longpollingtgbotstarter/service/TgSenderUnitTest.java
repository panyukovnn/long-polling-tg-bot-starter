package ru.panyukovnn.longpollingtgbotstarter.service;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import ru.panyukovnn.longpollingtgbotstarter.config.TgBotApi;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TgSenderUnitTest {

    @Mock
    private TgBotApi tgBotApi;

    @InjectMocks
    private TgSender tgSender;

    @Nested
    class SendMethodTests {

        @Test
        void when_send_then_messageSentWithMarkdownV2() throws Exception {
            Long chatId = 123L;
            String message = "Hello world";

            when(tgBotApi.execute(any(SendMessage.class))).thenReturn(null);

            assertDoesNotThrow(() -> tgSender.send(chatId, message));

            verify(tgBotApi, times(1)).execute(any(SendMessage.class));
        }

        @Test
        void when_send_withMarkdownV2Error_then_fallbackToHtml() throws Exception {
            Long chatId = 123L;
            String message = "Test message";

            when(tgBotApi.execute(any(SendMessage.class)))
                .thenThrow(new TelegramApiException("MarkdownV2 parsing error"))
                .thenReturn(null);

            assertDoesNotThrow(() -> tgSender.send(chatId, message));

            verify(tgBotApi, times(2)).execute(any(SendMessage.class));
        }

        @Test
        void when_send_withEmptyMessage_then_messageSent() throws Exception {
            Long chatId = 123L;
            String message = "";

            when(tgBotApi.execute(any(SendMessage.class))).thenReturn(null);

            assertDoesNotThrow(() -> tgSender.send(chatId, message));

            verify(tgBotApi, times(1)).execute(any(SendMessage.class));
        }

        @Test
        void when_send_withSpecialCharacters_then_messageSent() throws Exception {
            Long chatId = 123L;
            String message = "test strikethrough code";

            when(tgBotApi.execute(any(SendMessage.class))).thenReturn(null);

            assertDoesNotThrow(() -> tgSender.send(chatId, message));

            verify(tgBotApi, times(1)).execute(any(SendMessage.class));
        }
    }

    @Nested
    class SendSimpleHtmlMessageTests {

        @Test
        void when_sendSimpleHtmlMessage_then_messageSentWithHtml() throws Exception {
            Long chatId = 456L;
            String message = "Simple message";

            when(tgBotApi.execute(any(SendMessage.class))).thenReturn(null);

            assertDoesNotThrow(() -> tgSender.sendSimpleHtmlMessage(chatId, message));

            verify(tgBotApi, times(1)).execute(any(SendMessage.class));
        }

        @Test
        void when_sendSimpleHtmlMessage_withLongMessage_then_messageSplit()
                throws Exception {
            Long chatId = 456L;
            String message = "x".repeat(5000);

            when(tgBotApi.execute(any(SendMessage.class))).thenReturn(null);

            assertDoesNotThrow(() -> tgSender.sendSimpleHtmlMessage(chatId, message));

            verify(tgBotApi, times(2)).execute(any(SendMessage.class));
        }

        @Test
        void when_sendSimpleHtmlMessage_withHtmlSpecialChars_then_charactersEscaped()
                throws Exception {
            Long chatId = 456L;
            String message = "Text with <tag> & ampersand > symbol";

            when(tgBotApi.execute(any(SendMessage.class))).thenReturn(null);

            assertDoesNotThrow(() -> tgSender.sendSimpleHtmlMessage(chatId, message));

            verify(tgBotApi, times(1)).execute(any(SendMessage.class));
        }

        @Test
        void when_sendSimpleHtmlMessage_withTelegramApiException_then_exceptionCaught()
                throws Exception {
            Long chatId = 456L;
            String message = "Test message";

            doThrow(new TelegramApiException("Send failed")).when(tgBotApi)
                .execute(any(SendMessage.class));

            assertDoesNotThrow(() -> tgSender.sendSimpleHtmlMessage(chatId, message));

            verify(tgBotApi, times(1)).execute(any(SendMessage.class));
        }

        @Test
        void when_sendSimpleHtmlMessage_withEmptyMessage_then_messageSent() throws Exception {
            Long chatId = 456L;
            String message = "";

            when(tgBotApi.execute(any(SendMessage.class))).thenReturn(null);

            assertDoesNotThrow(() -> tgSender.sendSimpleHtmlMessage(chatId, message));

            verify(tgBotApi, times(1)).execute(any(SendMessage.class));
        }
    }

    @Nested
    class ConvertMarkdownTests {

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withBoldDoubleAsterisks_then_convertedToMarkdownV2() {
            String text = "**bold text**";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("bold"));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withBoldUnderscores_then_convertedToMarkdownV2() {
            String text = "__bold text__";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("bold"));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withItalicAsterisks_then_convertedToMarkdownV2() {
            String text = "*italic text*";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("italic"));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withItalicUnderscores_then_convertedToMarkdownV2() {
            String text = "_italic text_";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("italic"));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withBlockCode_then_preservedWithBackticks() {
            String text = "```code block```";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("code"));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withInlineCode_then_preservedWithBackticks() {
            String text = "inline `code snippet` here";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("code"));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withLink_then_preservedWithBracketsAndParens() {
            String text = "[link text](https://example.com)";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("link"));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withMultipleFormatTags_then_allConverted() {
            String text = "**bold** and *italic* and _also italic_";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("bold"));
            assertThat(result, containsString("italic"));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withNullText_then_returnNull() {
            String result = tgSender.convertMarkdownToTelegramMarkdownV2(null);

            assertThat(result, equalTo(null));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withPlainText_then_specialCharsEscaped() {
            String text = "Hello! This costs $100 - 50% = $50";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("\\!"));
            assertThat(result, containsString("\\-"));
            assertThat(result, containsString("\\="));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withEmptyString_then_returnEmpty() {
            String result = tgSender.convertMarkdownToTelegramMarkdownV2("");

            assertThat(result, equalTo(""));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withMultilineBoldText_then_boldProperlyConverted() {
            String text = "На практике я пришел к выводу, что идеальный PR — это **одна " +
                "законченная функциональность**. Не «добавил эндпоинт», а «реализовал сценарий X для " +
                "пользователя Y». В него могут входить изменения в нескольких слоях (контроллер, сервис, " +
                "репозиторий, миграция), но все они служат одной цели.\n\n" +
                "Почему это работает?\n\n" +
                "1.  **Ревьюеру понятен контекст.** Не нужно гадать, зачем этот код и как он связан с " +
                "другими изменениями в репозитории.\n" +
                "2.  **Снижается когнитивная нагрузка.** Легче удержать в голове одну задачу, чем десяток " +
                "разнородных правок.\n" +
                "3.  **Упрощается откат.** Если что-то пошло не так, можно откатить одну фичу, не задевая " +
                "другие.";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString(" *одна законченная функциональность*\\. "));
            assertThat(result, containsString(" *Ревьюеру понятен контекст\\.* "));
            assertThat(result, containsString(" *Снижается когнитивная нагрузка\\.* "));
            assertThat(result, containsString(" *Упрощается откат\\.* "));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withMultilineBoldText_then_noMarkerArtifacts() {
            String text = "На практике я пришел к выводу, что идеальный PR — это **одна " +
                "законченная функциональность**. Не «добавил эндпоинт», а «реализовал сценарий X для " +
                "пользователя Y». В него могут входить изменения в нескольких слоях (контроллер, сервис, " +
                "репозиторий, миграция), но все они служат одной цели.\n\n" +
                "Почему это работает?\n\n" +
                "1.  **Ревьюеру понятен контекст.** Не нужно гадать, зачем этот код и как он связан с " +
                "другими изменениями в репозитории.\n" +
                "2.  **Снижается когнитивная нагрузка.** Легче удержать в голове одну задачу, чем десяток " +
                "разнородных правок.\n" +
                "3.  **Упрощается откат.** Если что-то пошло не так, можно откатить одну фичу, не задевая " +
                "другие.";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, not(containsString("\uE000")));
            assertThat(result, not(containsString("\uE001")));
            assertThat(result, not(containsString("\uE002")));
            assertThat(result, not(containsString("\uE003")));
            assertThat(result, not(containsString("\uE004")));
            assertThat(result, not(containsString("\uE005")));
            assertThat(result, not(containsString("\uE006")));
            assertThat(result, not(containsString("\uE007")));
            assertThat(result, not(containsString("\uE008")));
        }

        @Test
        void when_convertMarkdownToTelegramMarkdownV2_withMultilineBoldText_then_specialCharsEscaped() {
            String text = "На практике я пришел к выводу, что идеальный PR — это **одна " +
                "законченная функциональность**. Не «добавил эндпоинт», а «реализовал сценарий X для " +
                "пользователя Y». В него могут входить изменения в нескольких слоях (контроллер, сервис, " +
                "репозиторий, миграция), но все они служат одной цели.\n\n" +
                "Почему это работает?\n\n" +
                "1.  **Ревьюеру понятен контекст.** Не нужно гадать, зачем этот код и как он связан с " +
                "другими изменениями в репозитории.\n" +
                "2.  **Снижается когнитивная нагрузка.** Легче удержать в голове одну задачу, чем десяток " +
                "разнородных правок.\n" +
                "3.  **Упрощается откат.** Если что-то пошло не так, можно откатить одну фичу, не задевая " +
                "другие.";

            String result = tgSender.convertMarkdownToTelegramMarkdownV2(text);

            assertThat(result, containsString("\\(контроллер"));
            assertThat(result, containsString("миграция\\)"));
            assertThat(result, containsString("\\."));
        }
    }

    @Nested
    class EscapeMarkdownV2Tests {

        @Test
        void when_escapeMarkdownV2_withUnderscore_then_escaped() {
            String text = "text_with_underscore";

            String result = tgSender.escapeMarkdownV2(text);

            assertThat(result, equalTo("text\\_with\\_underscore"));
        }

        @Test
        void when_escapeMarkdownV2_withAsterisk_then_escaped() {
            String text = "text*with*asterisk";

            String result = tgSender.escapeMarkdownV2(text);

            assertThat(result, equalTo("text\\*with\\*asterisk"));
        }

        @Test
        void when_escapeMarkdownV2_withBrackets_then_escaped() {
            String text = "text[with]brackets";

            String result = tgSender.escapeMarkdownV2(text);

            assertThat(result, equalTo("text\\[with\\]brackets"));
        }

        @Test
        void when_escapeMarkdownV2_withParentheses_then_escaped() {
            String text = "text(with)parentheses";

            String result = tgSender.escapeMarkdownV2(text);

            assertThat(result, equalTo("text\\(with\\)parentheses"));
        }

        @Test
        void when_escapeMarkdownV2_withTilde_then_escaped() {
            String text = "text~strikethrough~";

            String result = tgSender.escapeMarkdownV2(text);

            assertThat(result, equalTo("text\\~strikethrough\\~"));
        }

        @Test
        void when_escapeMarkdownV2_withBacktick_then_escaped() {
            String text = "text`code`here";

            String result = tgSender.escapeMarkdownV2(text);

            assertThat(result, equalTo("text\\`code\\`here"));
        }

        @Test
        void when_escapeMarkdownV2_withAllSpecialChars_then_escaped() {
            String text = "_*[]()~`>#+-=|{}.!";

            String result = tgSender.escapeMarkdownV2(text);

            assertThat(result, equalTo("\\_\\*\\[\\]\\(\\)\\~\\`\\>\\#\\+\\-\\=\\|\\{\\}\\.\\!"));
        }

        @Test
        void when_escapeMarkdownV2_withNullText_then_returnNull() {
            String result = tgSender.escapeMarkdownV2(null);

            assertThat(result, equalTo(null));
        }

        @Test
        void when_escapeMarkdownV2_withEmptyString_then_returnEmpty() {
            String result = tgSender.escapeMarkdownV2("");

            assertThat(result, equalTo(""));
        }

        @Test
        void when_escapeMarkdownV2_withPlainText_then_unchanged() {
            String text = "Hello World";

            String result = tgSender.escapeMarkdownV2(text);

            assertThat(result, equalTo("Hello World"));
        }
    }

    @Nested
    class EscapeHtmlTests {

        @Test
        void when_escapeHtml_withAmpersand_then_escaped() {
            String text = "Tom & Jerry";

            String result = tgSender.escapeHtml(text);

            assertThat(result, equalTo("Tom &amp; Jerry"));
        }

        @Test
        void when_escapeHtml_withLessThan_then_escaped() {
            String text = "1 < 2";

            String result = tgSender.escapeHtml(text);

            assertThat(result, equalTo("1 &lt; 2"));
        }

        @Test
        void when_escapeHtml_withGreaterThan_then_escaped() {
            String text = "2 > 1";

            String result = tgSender.escapeHtml(text);

            assertThat(result, equalTo("2 &gt; 1"));
        }

        @Test
        void when_escapeHtml_withAllSpecialChars_then_escaped() {
            String text = "A & B < C > D";

            String result = tgSender.escapeHtml(text);

            assertThat(result, equalTo("A &amp; B &lt; C &gt; D"));
        }

        @Test
        void when_escapeHtml_withNullText_then_returnNull() {
            String result = tgSender.escapeHtml(null);

            assertThat(result, equalTo(null));
        }

        @Test
        void when_escapeHtml_withEmptyString_then_returnEmpty() {
            String result = tgSender.escapeHtml("");

            assertThat(result, equalTo(""));
        }

        @Test
        void when_escapeHtml_withPlainText_then_unchanged() {
            String text = "Hello World";

            String result = tgSender.escapeHtml(text);

            assertThat(result, equalTo("Hello World"));
        }

        @Test
        void when_escapeHtml_withHtmlTags_then_escaped() {
            String text = "<b>bold</b> & <i>italic</i>";

            String result = tgSender.escapeHtml(text);

            assertThat(result, equalTo("&lt;b&gt;bold&lt;/b&gt; &amp; &lt;i&gt;italic&lt;/i&gt;"));
        }
    }

    @Nested
    class TruncateWithMarkdownFixTests {

        @Test
        void when_truncateWithMarkdownFix_withTextShorterThanMax_then_unchanged() {
            String text = "Short text";
            int maxLength = 100;

            String result = tgSender.truncateWithMarkdownFix(text, maxLength);

            assertThat(result, equalTo("Short text"));
        }

        @Test
        void when_truncateWithMarkdownFix_withTextExactLength_then_unchanged() {
            String text = "12345";
            int maxLength = 5;

            String result = tgSender.truncateWithMarkdownFix(text, maxLength);

            assertThat(result, equalTo("12345"));
        }

        @Test
        void when_truncateWithMarkdownFix_withTextLongerThanMax_then_truncatedAndDotted() {
            String text = "Very long text here";
            int maxLength = 10;

            String result = tgSender.truncateWithMarkdownFix(text, maxLength);

            assertThat(result, endsWith("..."));
            assertThat(result.length(), lessThanOrEqualTo(maxLength + 3));
        }

        @Test
        void when_truncateWithMarkdownFix_withUnclosedMarkdownTags_then_fixed() {
            String text = "Text with *unclosed asterisk";
            int maxLength = 20;

            String result = tgSender.truncateWithMarkdownFix(text, maxLength);

            assertThat(result, endsWith("..."));
        }

        @Test
        void when_truncateWithMarkdownFix_withMultipleUnclosedTags_then_fixed() {
            String text = "Text with *unclosed _underscore and more";
            int maxLength = 25;

            String result = tgSender.truncateWithMarkdownFix(text, maxLength);

            assertThat(result, endsWith("..."));
        }
    }

    @Nested
    class FixUnclosedMarkdownTagsTests {

        @Test
        void when_fixUnclosedMarkdownTags_withOddAsterisks_then_lastRemoved() {
            String text = "Text with *single asterisk";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with single asterisk"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withEvenAsterisks_then_unchanged() {
            String text = "Text with *two* asterisks";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with *two* asterisks"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withOddUnderscores_then_lastRemoved() {
            String text = "Text with _single underscore";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with single underscore"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withOddBackticks_then_lastRemoved() {
            String text = "Code with `single backtick";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Code with single backtick"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withOddTildes_then_lastRemoved() {
            String text = "Text with ~strikethrough";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with strikethrough"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withMoreOpenBracketsThanClosed_then_closedBracketsRemoved() {
            String text = "Text with [link";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with link"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withMoreOpenParensThanClosed_then_closedParensRemoved() {
            String text = "Text with (parenthesis";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with parenthesis"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withEscapedCharacters_then_notRemoved() {
            String text = "Text with \\*escaped asterisk";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with \\*escaped asterisk"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withNullString_then_returnNull() {
            String result = tgSender.fixUnclosedMarkdownTags(null);

            assertThat(result, equalTo(null));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withEmptyString_then_returnEmpty() {
            String result = tgSender.fixUnclosedMarkdownTags("");

            assertThat(result, equalTo(""));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withMultipleUnclosedTags_then_allFixed() {
            String text = "Text with *odd _odd and [bracket";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with odd odd and bracket"));
        }

        @Test
        void when_fixUnclosedMarkdownTags_withBalancedTags_then_unchanged() {
            String text = "Text with *bold* and _italic_ and [link]";

            String result = tgSender.fixUnclosedMarkdownTags(text);

            assertThat(result, equalTo("Text with *bold* and _italic_ and [link]"));
        }
    }

    @Nested
    class CountUnescapedCharTests {

        @Test
        void when_countUnescapedChar_withNoCharacters_then_returnZero() {
            String text = "Text without asterisks";

            int result = tgSender.countUnescapedChar(text, '*');

            assertThat(result, equalTo(0));
        }

        @Test
        void when_countUnescapedChar_withSingleCharacter_then_returnOne() {
            String text = "Text with * asterisk";

            int result = tgSender.countUnescapedChar(text, '*');

            assertThat(result, equalTo(1));
        }

        @Test
        void when_countUnescapedChar_withMultipleCharacters_then_returnCount() {
            String text = "Text * with * asterisks *";

            int result = tgSender.countUnescapedChar(text, '*');

            assertThat(result, equalTo(3));
        }

        @Test
        void when_countUnescapedChar_withEscapedCharacters_then_ignored() {
            String text = "Text with \\* escaped \\* asterisks";

            int result = tgSender.countUnescapedChar(text, '*');

            assertThat(result, equalTo(0));
        }

        @Test
        void when_countUnescapedChar_withMixedEscapedAndNormal_then_onlyNormalCounted() {
            String text = "\\*escaped and * normal and \\* escaped";

            int result = tgSender.countUnescapedChar(text, '*');

            assertThat(result, equalTo(1));
        }

        @Test
        void when_countUnescapedChar_withUnderscore_then_counted() {
            String text = "Text_with_underscores";

            int result = tgSender.countUnescapedChar(text, '_');

            assertThat(result, equalTo(2));
        }

        @Test
        void when_countUnescapedChar_withEmptyString_then_returnZero() {
            String text = "";

            int result = tgSender.countUnescapedChar(text, '*');

            assertThat(result, equalTo(0));
        }

        @Test
        void when_countUnescapedChar_withCharacterAtStart_then_counted() {
            String text = "*start with asterisk";

            int result = tgSender.countUnescapedChar(text, '*');

            assertThat(result, equalTo(1));
        }
    }

    @Nested
    class RemoveLastUnescapedCharTests {

        @Test
        void when_removeLastUnescapedChar_withSingleCharacter_then_removed() {
            StringBuilder sb = new StringBuilder("Text with * asterisk");

            tgSender.removeLastUnescapedChar(sb, '*');

            assertThat(sb.toString(), equalTo("Text with  asterisk"));
        }

        @Test
        void when_removeLastUnescapedChar_withMultipleCharacters_then_lastRemoved() {
            StringBuilder sb = new StringBuilder("Text * with * asterisks");

            tgSender.removeLastUnescapedChar(sb, '*');

            assertThat(sb.toString(), equalTo("Text * with  asterisks"));
        }

        @Test
        void when_removeLastUnescapedChar_withEscapedCharacter_then_skipped() {
            StringBuilder sb = new StringBuilder("Text with \\* escaped and * normal");

            tgSender.removeLastUnescapedChar(sb, '*');

            assertThat(sb.toString(), equalTo("Text with \\* escaped and  normal"));
        }

        @Test
        void when_removeLastUnescapedChar_withOnlyEscapedCharacters_then_nothingRemoved() {
            StringBuilder sb = new StringBuilder("Text with \\* escaped");

            tgSender.removeLastUnescapedChar(sb, '*');

            assertThat(sb.toString(), equalTo("Text with \\* escaped"));
        }

        @Test
        void when_removeLastUnescapedChar_withNoCharacter_then_nothingRemoved() {
            StringBuilder sb = new StringBuilder("Text without that character");

            tgSender.removeLastUnescapedChar(sb, '*');

            assertThat(sb.toString(), equalTo("Text without that character"));
        }

        @Test
        void when_removeLastUnescapedChar_withEmptyStringBuilder_then_unchanged() {
            StringBuilder sb = new StringBuilder("");

            tgSender.removeLastUnescapedChar(sb, '*');

            assertThat(sb.toString(), equalTo(""));
        }

        @Test
        void when_removeLastUnescapedChar_withCharacterAtStart_then_removed() {
            StringBuilder sb = new StringBuilder("* start");

            tgSender.removeLastUnescapedChar(sb, '*');

            assertThat(sb.toString(), equalTo(" start"));
        }

        @Test
        void when_removeLastUnescapedChar_withCharacterAtEnd_then_removed() {
            StringBuilder sb = new StringBuilder("end *");

            tgSender.removeLastUnescapedChar(sb, '*');

            assertThat(sb.toString(), equalTo("end "));
        }
    }

    @Nested
    class SplitMessageIntoPartsTests {

        @Test
        void when_splitShortMessage_then_returnSinglePart() {
            String message = "Short message";

            java.util.List<String> parts = tgSender.splitMessageIntoParts(message,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWNV2);

            assertThat(parts.size(), equalTo(1));
            assertThat(parts.get(0), equalTo(message));
        }

        @Test
        void when_splitLongMessageWithoutTags_then_returnMultipleParts() {
            String message = "x".repeat(5000);

            java.util.List<String> parts = tgSender.splitMessageIntoParts(message,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWNV2);

            assertThat(parts.size(), equalTo(2));
            assertThat(parts.get(0).length(), lessThanOrEqualTo(4096));
            assertThat(parts.get(1).length(), lessThanOrEqualTo(4096));
        }

        @Test
        void when_splitLongMessageWithBoldTag_then_tagsClosedAndReopened() {
            String message = "*" + "x".repeat(5000) + "*";

            java.util.List<String> parts = tgSender.splitMessageIntoParts(message,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWNV2);

            assertThat(parts.size(), equalTo(2));
            assertThat(parts.get(0), endsWith("*"));
            assertThat(parts.get(1).substring(0, 1), equalTo("*"));
        }

        @Test
        void when_splitLongHtmlMessageWithTags_then_tagsClosedAndReopened() {
            String message = "<b>" + "x".repeat(5000) + "</b>";

            java.util.List<String> parts = tgSender.splitMessageIntoParts(message,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.HTML);

            assertThat(parts.size(), equalTo(2));
            assertThat(parts.get(0), endsWith("</b>"));
            assertThat(parts.get(1).substring(0, 3), equalTo("<b>"));
        }

        @Test
        void when_splitMessageExactlyMaxLength_then_returnSinglePart() {
            String message = "x".repeat(4096);

            java.util.List<String> parts = tgSender.splitMessageIntoParts(message,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWNV2);

            assertThat(parts.size(), equalTo(1));
        }
    }

    @Nested
    class FindSplitPositionTests {

        @Test
        void when_textShorterThanMax_then_returnLength() {
            String text = "Short text";

            int pos = tgSender.findSplitPosition(text, 100);

            assertThat(pos, equalTo(text.length()));
        }

        @Test
        void when_newlineFoundInWindow_then_splitAtNewline() {
            String text = "First line\nSecond line\nThird line";

            int pos = tgSender.findSplitPosition(text, 25);

            assertThat(text.charAt(pos - 1), equalTo('\n'));
        }

        @Test
        void when_spaceFoundInWindow_then_splitAtSpace() {
            String text = "word1 word2 word3 word4";

            int pos = tgSender.findSplitPosition(text, 20);

            assertThat(pos > 0, equalTo(true));
        }

        @Test
        void when_noGoodSplitPoint_then_splitAtMax() {
            String text = "x".repeat(5000);

            int pos = tgSender.findSplitPosition(text, 4096);

            assertThat(pos, equalTo(4096));
        }
    }

    @Nested
    class ExtractOpenTagsTests {

        @Test
        void when_noOpenTags_then_returnEmptyList() {
            String text = "Plain text without tags";

            java.util.List<String> tags = tgSender.extractOpenTags(text,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWNV2);

            assertThat(tags.isEmpty(), equalTo(true));
        }

        @Test
        void when_singleAsterisk_then_returnAsterisk() {
            String text = "Text with *unclosed bold";

            java.util.List<String> tags = tgSender.extractOpenTags(text,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWNV2);

            assertThat(tags.size(), equalTo(1));
            assertThat(tags.get(0), equalTo("*"));
        }

        @Test
        void when_multipleOpenTags_then_returnAll() {
            String text = "Text with *bold and _italic";

            java.util.List<String> tags = tgSender.extractOpenTags(text,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.MARKDOWNV2);

            assertThat(tags.size(), equalTo(2));
            assertThat(tags.contains("*"), equalTo(true));
            assertThat(tags.contains("_"), equalTo(true));
        }

        @Test
        void when_htmlOpenTag_then_returnTag() {
            String text = "Text with <b>unclosed bold";

            java.util.List<String> tags = tgSender.extractOpenTags(text,
                    org.telegram.telegrambots.meta.api.methods.ParseMode.HTML);

            assertThat(tags.size(), equalTo(1));
            assertThat(tags.get(0), equalTo("b"));
        }
    }
}