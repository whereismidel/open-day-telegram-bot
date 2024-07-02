package com.midel.opendaybottelegram.telegram;

import com.midel.opendaybottelegram.telegram.action.Callback;
import com.midel.opendaybottelegram.telegram.action.Command;
import com.midel.opendaybottelegram.telegram.annotation.Action;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${bot.username}")
    private String username;

    private final TelegramHandler telegramHandler;

    @Autowired
    public TelegramBot(
            @Value("${bot.token}") String botToken,
            @Lazy TelegramHandler telegramHandler
    ) {
        super(botToken);

        this.telegramHandler = telegramHandler;
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public void onUpdateReceived(Update update) {

//        System.out.println(update);
        try {

            if (update.hasCallbackQuery()) {
                handleCallbackQuery(update);
                return;
            }

            if (update.hasPollAnswer()) {
                handlePollAnswer(update);
                return;
            }

            if (update.hasMessage()) {
                handleMessage(update);
                return;
            }

        } catch (Exception e) {
            log.error("An error occurred while processing a message from a user", e);
        }
    }

    private void handleMessage(Update update) {
        Message message = update.getMessage();

        String text;
        if (message.hasText()) {
            text = message.getText().trim();
        } else if (message.hasContact()) {
            text = message.getContact().getPhoneNumber();
            message.setText(text);
        } else {
            telegramHandler.getResult(Action.COMMAND, Command.CommandIdentifier.START.getName(), null, update);
            return;
        }

        logUserAction(update);

        if (text.startsWith(Command.PREFIX)){
            handleCommand(update, text);
        } else {
            handleTextMessage(update);
        }

    }

    private void handleTextMessage(Update update) {
        try {

            if (update.getMessage().getReplyToMessage() != null && update.getMessage().getReplyToMessage().getFrom().getIsBot()) {
                telegramHandler.getResult(Action.REPLY_TO, "BY_USER_STATE", null, update);
            } else {
                telegramHandler.getResult(Action.COMMAND, Command.CommandIdentifier.START.getName(), null, update);
            }

        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void handleCommand(Update update, String text) {
        try {

            Command command = new Command(text.replace(Command.MENTIONED + getBotUsername().toLowerCase(), ""));
            telegramHandler.getResult(Action.COMMAND, command.getIdentifier().getName(), command.getArguments(), update);

        } catch (IllegalArgumentException iae) {
            telegramHandler.getResult(Action.COMMAND, "not_found", null, update);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void handleCallbackQuery(Update update) {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackData = callbackQuery.getData();

        logUserAction(update);

        try {

            Callback callback = new Callback(callbackData);
            telegramHandler.getResult(Action.INLINE_ANSWER, callback.getIdentifier().getName(), callback.getArguments(), update);

        } catch (IllegalArgumentException e) {
            telegramHandler.getResult(Action.INLINE_ANSWER, "unknown-callback", null, update);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }
    }

    private void handlePollAnswer(Update update) {

        try {
            telegramHandler.getResult(Action.POLL_ANSWER, "user_answer", null, update);
        } catch (Exception e) {
            log.warn(e.getMessage(), e);
        }

    }

    private void logUserAction(Update update) {
        Message message;
        String prefix;
        String text;
        User from;

        if (update.hasCallbackQuery()) {
            message = (Message) update.getCallbackQuery().getMessage();
            prefix = "Received callback query ";
            text = update.getCallbackQuery().getData();
            from = update.getCallbackQuery().getFrom();
        } else {
            message = update.getMessage();
            prefix = "Received message ";
            text = update.getMessage().getText();
            from = message.getFrom();
        }

        if (message.getChat().getType().equals("private")) {
            log.info("{}from user @{}(id={}): {}\n{}", prefix, from.getUserName(), from.getId(), text, update);
        } else {
            log.info("{}from group \"{}\"(id={}), user @{}(id={}): {}\n{}", prefix, message.getChat().getTitle(), message.getChat().getId(), from.getUserName(), from.getId(), text, update);
        }
    }

}
