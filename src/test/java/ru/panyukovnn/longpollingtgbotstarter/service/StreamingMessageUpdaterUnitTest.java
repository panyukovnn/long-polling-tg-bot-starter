package ru.panyukovnn.longpollingtgbotstarter.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.io.Serializable;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StreamingMessageUpdaterUnitTest {

    private static final Long CHAT_ID = 123L;
    private static final Integer MESSAGE_ID = 456;
    private static final long UPDATE_INTERVAL_MS = 100;

    @Mock
    private TelegramClient telegramClient;

    @Mock
    private TgSender tgSender;

    private StreamingMessageUpdater updater;

    @BeforeEach
    void setUp() throws Exception {
        when(telegramClient.execute(any(EditMessageText.class))).thenReturn((Serializable) true);
        when(tgSender.convertMarkdownToTelegramMarkdownV2(any())).thenAnswer(inv -> inv.getArgument(0));
        when(tgSender.escapeHtml(any())).thenAnswer(inv -> inv.getArgument(0));

        updater = new StreamingMessageUpdater(telegramClient, tgSender, CHAT_ID, MESSAGE_ID, UPDATE_INTERVAL_MS);
    }

    @AfterEach
    void tearDown() {
        if (updater != null) {
            updater.complete();
        }
    }

    @Nested
    class AppendTokenTests {

        @Test
        void when_appendToken_then_tokenAddedToBuffer() {
            updater.appendToken("Hello");

            assertThat(updater.getCurrentText(), equalTo("Hello"));
        }

        @Test
        void when_appendMultipleTokens_then_allTokensInBuffer() {
            updater.appendToken("Hello");
            updater.appendToken(" ");
            updater.appendToken("world");

            assertThat(updater.getCurrentText(), equalTo("Hello world"));
        }

        @Test
        void when_appendTokenAfterComplete_then_tokenIgnored() {
            updater.appendToken("Hello");
            updater.complete();

            updater.appendToken(" world");

            assertThat(updater.getCurrentText(), equalTo("Hello"));
        }
    }

    @Nested
    class FlushTests {

        @Test
        void when_tokenAppended_then_messageUpdatedWithCursor() throws Exception {
            updater.appendToken("Hello");

            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient, timeout(500).atLeastOnce()).execute(captor.capture());

            EditMessageText lastEdit = captor.getValue();
            assertThat(lastEdit.getChatId(), equalTo(String.valueOf(CHAT_ID)));
            assertThat(lastEdit.getMessageId(), equalTo(MESSAGE_ID));
            assertThat(lastEdit.getText(), endsWith("▍"));
            assertThat(lastEdit.getText(), containsString("Hello"));
        }

        @Test
        void when_noTokensAppended_then_noEditSent() throws Exception {
            Thread.sleep(UPDATE_INTERVAL_MS * 3);

            verify(telegramClient, never()).execute(any(EditMessageText.class));
        }

        @Test
        void when_markdownV2Fails_then_fallbackToHtml() throws Exception {
            when(tgSender.convertMarkdownToTelegramMarkdownV2(any()))
                    .thenReturn("converted");
            when(telegramClient.execute(any(EditMessageText.class)))
                    .thenThrow(new TelegramApiException("MarkdownV2 error"))
                    .thenReturn((Serializable) true);

            updater.appendToken("**bold**");

            verify(telegramClient, timeout(500).atLeast(2)).execute(any(EditMessageText.class));
        }

        @Test
        void when_tokenAppended_then_convertMarkdownV2Called() throws Exception {
            when(tgSender.convertMarkdownToTelegramMarkdownV2(any()))
                    .thenReturn("converted\\!");

            updater.appendToken("Hello!");

            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient, timeout(500).atLeastOnce()).execute(captor.capture());

            EditMessageText lastEdit = captor.getValue();
            assertThat(lastEdit.getText(), equalTo("converted\\!"));
            assertThat(lastEdit.getParseMode(), equalTo("MarkdownV2"));
        }

        @Test
        void when_editFails_then_noExceptionPropagated() throws Exception {
            when(telegramClient.execute(any(EditMessageText.class)))
                    .thenThrow(new TelegramApiException("API error"));

            updater.appendToken("Hello");

            Thread.sleep(UPDATE_INTERVAL_MS * 3);

            assertThat(updater.getCurrentText(), equalTo("Hello"));
        }
    }

    @Nested
    class CompleteTests {

        @Test
        void when_complete_then_finalMessageWithoutCursor() throws Exception {
            updater.appendToken("Final text");

            updater.complete();

            ArgumentCaptor<EditMessageText> captor = ArgumentCaptor.forClass(EditMessageText.class);
            verify(telegramClient, atLeastOnce()).execute(captor.capture());

            EditMessageText lastEdit = captor.getAllValues()
                    .get(captor.getAllValues().size() - 1);
            assertThat(lastEdit.getText(), equalTo("Final text"));
            assertThat(lastEdit.getText(), not(containsString("▍")));
        }

        @Test
        void when_completeCalledTwice_then_secondCallIgnored() throws Exception {
            updater.appendToken("Text");

            updater.complete();
            updater.complete();

            assertThat(updater.getCurrentText(), equalTo("Text"));
        }
    }

    @Nested
    class GetCurrentTextTests {

        @Test
        void when_noTokens_then_emptyString() {
            assertThat(updater.getCurrentText(), equalTo(""));
        }

        @Test
        void when_tokensAppended_then_returnsConcatenatedText() {
            updater.appendToken("one");
            updater.appendToken("two");
            updater.appendToken("three");

            assertThat(updater.getCurrentText(), equalTo("onetwothree"));
        }
    }
}