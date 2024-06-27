package com.midel.opendaybottelegram.telegram.controller;

import com.midel.opendaybottelegram.telegram.TelegramSender;
import com.midel.opendaybottelegram.telegram.annotation.TelegramController;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;

@TelegramController
@RequiredArgsConstructor
public class InlineController {

    private final TelegramSender telegramSender;

    @Value("${bot.admins}")
    private List<Long> adminList;

    @Value("${bot.destination-chat}")
    private Long conferenceChatId;

}
