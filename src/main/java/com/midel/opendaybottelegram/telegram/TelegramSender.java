package com.midel.opendaybottelegram.telegram;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.CopyMessage;
import org.telegram.telegrambots.meta.api.methods.polls.SendPoll;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.LinkPreviewOptions;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ForceReplyKeyboard;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class TelegramSender {

    private final TelegramBot telegramBot;

    public Integer htmlMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        return (Integer) send(sendMessage);
    }

    public Integer htmlForceReplyMessage(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        ForceReplyKeyboard force = new ForceReplyKeyboard();
        force.setForceReply(true);
        sendMessage.setReplyMarkup(force);

        return (Integer) send(sendMessage);
    }

    public Integer htmlMessageWithBottomPhoto(Long chatId, String text, String link) {

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText(text);

        LinkPreviewOptions linkPreviewOptions = new LinkPreviewOptions();
        linkPreviewOptions.setUrlField(link);
        sendMessage.setLinkPreviewOptions(linkPreviewOptions);

        sendMessage.enableHtml(true);

        return (Integer) send(sendMessage);
    }

    public void forwardMessage(Long fromChat, int messageId, Long toChat) {
        CopyMessage copyMessage = new CopyMessage(toChat.toString(), fromChat.toString(), messageId);

        send(copyMessage);
    }

    public Integer requestContact(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setText(text);
        sendMessage.setChatId(chatId);
        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();

        ReplyKeyboardMarkup replyKeyboard = new ReplyKeyboardMarkup();
        replyKeyboard.setOneTimeKeyboard(true);
        replyKeyboard.setResizeKeyboard(true);
        replyKeyboard.setKeyboard(
                List.of(new KeyboardRow(
                        List.of(
                                KeyboardButton.builder()
                                        .requestContact(true)
                                        .text("Надіслати номер телефону")
                                        .build()
                        )
                ))
        );
        sendMessage.setReplyMarkup(replyKeyboard);

//        ForceReplyKeyboard force = new ForceReplyKeyboard();
//        force.setForceReply(true);
//        sendMessage.setReplyMarkup(force);

        return (Integer) send(sendMessage);
    }

    public void sendDocument(Long chatId, InputFile inputFile) {

        SendDocument sendDocument = new SendDocument();
        sendDocument.setChatId(chatId);
        sendDocument.setDocument(inputFile);

        try {
            telegramBot.execute(sendDocument);
        } catch (TelegramApiException e) {
            log.warn("Failed to send document: {}", e.getMessage());
        }
    }

    public Integer inlineKeyboard(Long chatId, String title, InlineKeyboardMarkup inlineKeyboardMarkup) {
        SendMessage sendMessage = new SendMessage();

        sendMessage.setChatId(chatId);
        sendMessage.setText(title);

        if (inlineKeyboardMarkup != null) {
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);
        }

        sendMessage.enableHtml(true);
        sendMessage.disableWebPagePreview();


        return (Integer) send(sendMessage);
    }

    public void deleteMessage(Long chatId, Integer messageId) {
        DeleteMessage deleteMessage = new DeleteMessage(chatId.toString(), messageId);

        send(deleteMessage);
    }

    private Object send(BotApiMethod<?> method) {
        try {
            switch (method) {
                case SendMessage sendMessage -> { return telegramBot.execute(sendMessage).getMessageId(); }
                case DeleteMessage deleteMessage -> { return telegramBot.execute(deleteMessage)?1:0; }
                case SendPoll sendPoll -> { return telegramBot.execute(sendPoll); }
                default -> telegramBot.execute(method);
            }

        } catch (TelegramApiException e) {
            log.warn("Failed to send message - {}. Method: {}", e.getMessage(), method);
        }

        return null;
    }
}
