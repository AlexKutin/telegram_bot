package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.BotUtils;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.sky.telegrambot.service.NotificationTaskService;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Objects;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelegramBotUpdatesListenerTest {
    private static final String TEST_FIRST_NAME = "Test_User_123";
    private static final String CORRECT_TASK_DATE_TIME = "12.08.2023 11:00";
    private static final String CORRECT_TASK_TEXT = "Сдать домашку.";
    private static final String CORRECT_TASK_MESSAGE = CORRECT_TASK_DATE_TIME + " " + CORRECT_TASK_TEXT;

    @Mock
    private TelegramBot telegramBot;

    @Mock
    NotificationTaskService notificationTaskService;

    @InjectMocks
    private TelegramBotUpdatesListener telegramBotUpdatesListener;

    @Test
    public void handleStartTest() throws URISyntaxException, IOException {
        Update update = prepareUpdate("/start");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(update.message().chat().id());

        String expectedText = String.format(TelegramBotUpdatesListener.HELLO_MESSAGE, TEST_FIRST_NAME);
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo(expectedText);
    }

    @Test
    public void handleIncorrectTaskFormatMessage() throws URISyntaxException, IOException {
        Update update = prepareUpdate("12.08.2023 21:20 3333.");  // Нет текста задачи
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(update.message().chat().id());
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo(TelegramBotUpdatesListener.INCORRECT_TASK_FORMAT_MESSAGE);
    }

    @Test
    public void handleIncorrectDateTimeFormatMessage() throws URISyntaxException, IOException {
        Update update = prepareUpdate("12.08.2023 31:00 Сдать домашку.");
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(update.message().chat().id());
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo(TelegramBotUpdatesListener.INCORRECT_DATE_TIME_FORMAT_MESSAGE);
    }

    @Test
    public void handleCorrectTaskMessage() throws URISyntaxException, IOException {
        Update update = prepareUpdate(CORRECT_TASK_MESSAGE);
        long chatId = update.message().chat().id();
        telegramBotUpdatesListener.process(Collections.singletonList(update));

        ArgumentCaptor<SendMessage> argumentCaptor = ArgumentCaptor.forClass(SendMessage.class);
        Mockito.verify(telegramBot).execute(argumentCaptor.capture());
        Mockito.verify(notificationTaskService, Mockito.times(1))
                .saveNotificationTask(chatId, CORRECT_TASK_TEXT, telegramBotUpdatesListener.parseDateTimeFromString(CORRECT_TASK_DATE_TIME));
        SendMessage actual = argumentCaptor.getValue();

        Assertions.assertThat(actual.getParameters().get("chat_id")).isEqualTo(chatId);
        Assertions.assertThat(actual.getParameters().get("text")).isEqualTo(TelegramBotUpdatesListener.TASK_SCHEDULE_SUCCESS_MESSAGE);
    }

    private Update prepareUpdate(String userText) throws URISyntaxException, IOException {
        URL url = TelegramBotUpdatesListenerTest.class.getResource("update.json");
        String json = Files.readString(Path.of(Objects.requireNonNull(url).toURI()));
        when(telegramBot.execute(any())).thenReturn(BotUtils.fromJson("{ \"ok\": true}", SendResponse.class));
        return BotUtils.fromJson(json.replace("%text%", userText).replace("%first_name%", TEST_FIRST_NAME), Update.class);
    }
}