package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.response.SendResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.service.NotificationTaskService;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private static final String START_COMMAND = "/start";
    private static final Pattern DATE_TIME_PATTERN =
            Pattern.compile("(\\d{1,2}\\.\\d{1,2}\\.\\d{4} \\d{1,2}:\\d{2})\\s+([\\[A-я\\s.,!?:;]+)");

    private static final DateTimeFormatter TASK_DATE_TIME_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);

    private final TelegramBot telegramBot;
    private final NotificationTaskService notificationTaskService;

    public static String HELLO_MESSAGE = "Привет, %s! Я помогу тебе запланировать задачу. \nОтправь ее в формате: \"12.08.2023 21:00 Сдать домашку.\"";
    public static String INCORRECT_TASK_FORMAT_MESSAGE = "Некорректный формат формулировки задачи";
    public static String INCORRECT_DATE_TIME_FORMAT_MESSAGE = "Некорректный формат даты/времени";
    public static String TASK_SCHEDULE_SUCCESS_MESSAGE = "Задача успешно запланирована!";

    public TelegramBotUpdatesListener(TelegramBot telegramBot, NotificationTaskService notificationTaskService) {
        this.telegramBot = telegramBot;
        this.notificationTaskService = notificationTaskService;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        try {
            updates.stream()
                    .filter(update -> update.message() != null)
                    .forEach(update -> {
                logger.info("Processing update: {}", update);
                // Process your updates here
                Message message = update.message();
                Long chatId = message.chat().id();
                String textMessage = message.text();
                if (START_COMMAND.equals(textMessage)) {
                    String answer = String.format(HELLO_MESSAGE, message.chat().firstName());
                    sendAnswerMessage(chatId, answer);
                } else if (textMessage != null) {
                    Matcher matcher = DATE_TIME_PATTERN.matcher(textMessage);
                    if (matcher.find()) {
                        String taskDateTime = matcher.group(1);
                        LocalDateTime notificationDateTime = parseDateTimeFromString(taskDateTime);
                        if (notificationDateTime == null) {
                            sendAnswerMessage(chatId, INCORRECT_DATE_TIME_FORMAT_MESSAGE);
                        } else {
                            String taskMessage = matcher.group(2);
                            notificationTaskService.saveNotificationTask(chatId, taskMessage, notificationDateTime);
                            sendAnswerMessage(chatId, TASK_SCHEDULE_SUCCESS_MESSAGE);
                        }
                    } else {
                        sendAnswerMessage(chatId, INCORRECT_TASK_FORMAT_MESSAGE);
                    }
                }
            });
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public LocalDateTime parseDateTimeFromString(String taskDateTime) {
        try {
            return  LocalDateTime.parse(taskDateTime, TASK_DATE_TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private void sendAnswerMessage(Long chatId, String answer) {
        SendMessage request = new SendMessage(chatId, answer)
                .parseMode(ParseMode.HTML)
                .disableWebPagePreview(true)
                .disableNotification(true);

        SendResponse sendResponse = telegramBot.execute(request);
        if (!sendResponse.isOk()) {
            logger.error("Error during sending response: {}", sendResponse.description());
        }
    }

}
