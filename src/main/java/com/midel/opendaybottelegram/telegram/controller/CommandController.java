package com.midel.opendaybottelegram.telegram.controller;

import com.midel.opendaybottelegram.entity.User;
import com.midel.opendaybottelegram.entity.enums.State;
import com.midel.opendaybottelegram.repository.UserRepository;
import com.midel.opendaybottelegram.telegram.TelegramSender;
import com.midel.opendaybottelegram.telegram.action.CommandMessages;
import com.midel.opendaybottelegram.telegram.annotation.Action;
import com.midel.opendaybottelegram.telegram.annotation.AdminAction;
import com.midel.opendaybottelegram.telegram.annotation.Handle;
import com.midel.opendaybottelegram.telegram.annotation.TelegramController;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@TelegramController
@RequiredArgsConstructor
@Slf4j
public class CommandController {

    private final TelegramSender telegramSender;
    private final UserRepository userRepository;

    @Value("${bot.admins}")
    private List<Long> adminList;


    @Handle(value = Action.COMMAND, command = "start")
    public void startCommand(List<String> arguments, Update update) {

        User user = userRepository.getUserById(update.getMessage().getFrom().getId()).orElse(null);

        if (user == null) {
            telegramSender.htmlMessage(update.getMessage().getChatId(), CommandMessages.START_MESSAGE);

            user = User.builder()
                    .id(update.getMessage().getFrom().getId())
                    .state(State.NAME)
                    .username(update.getMessage().getFrom().getUserName())
                    .build();

            user = userRepository.save(user);
        }

        handleState(update, user);
    }

    @Handle(value = Action.COMMAND, command = "notify")
    @AdminAction
    public void notifyCommand(List<String> arguments, Update update) {

        if (update.getMessage().getReplyToMessage() == null) {
            telegramSender.htmlMessage(update.getMessage().getChatId(),
        """
            Використання:
            Відміть повідомлення для розсилки і напиши /notify
            """);
            return;
        }

        List<User> userList = userRepository.findAll();
        long delay = 200;

        try (ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1)) {
            for (int i = 0; i < userList.size(); i++) {
                int finalI = i;
                scheduler.schedule(() ->
                        telegramSender.forwardMessage(update.getMessage().getChatId(), update.getMessage().getReplyToMessage().getMessageId(), userList.get(finalI).getId()),
                        delay * i,
                        TimeUnit.MILLISECONDS
                );
            }

            scheduler.schedule(() -> {
                telegramSender.htmlMessage(update.getMessage().getChatId(), "Розсилка завершена.");
                scheduler.shutdown();
            }, delay * userList.size(), TimeUnit.MILLISECONDS);
        }
    }

    @Handle(value = Action.COMMAND, command = "export")
    @AdminAction
    public void exportCommand(List<String> arguments, Update update) {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Data");

            List<User> userList = userRepository.findAll();

            // Create header row
            Row headerRow = sheet.createRow(0);
            String[] headers = {"Учасник", "Телефон", "Спеціальність", "Телеграм", "Зареєстрований"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);

                sheet.setColumnWidth(i, 4500);
            }

            // Create data rows
            int rowCount = 1;
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"); // Формат дати
            CreationHelper creationHelper = workbook.getCreationHelper();
            CellStyle dateCellStyle = workbook.createCellStyle();
            dateCellStyle.setDataFormat(creationHelper.createDataFormat().getFormat("dd.MM.yyyy HH:mm:ss"));

            for (User user : userList) {
                Row row = sheet.createRow(rowCount++);
                row.createCell(0).setCellValue(user.getFullName());
                row.createCell(1).setCellValue(user.getPhoneNumber());
                row.createCell(2).setCellValue(user.getSpeciality());
                row.createCell(3).setCellValue(user.getUsername() == null? "-" : ("@" + user.getUsername()));

                Cell dateCell = row.createCell(4);
                dateCell.setCellValue(user.getRegisteredAt().atZone(ZoneId.of("Europe/Kiev")).toLocalDateTime());
                dateCell.setCellStyle(dateCellStyle);
            }

            // Write the output to a ByteArrayOutputStream
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            workbook.write(byteArrayOutputStream);

            // Convert ByteArrayOutputStream to ByteArrayInputStream
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(byteArrayOutputStream.toByteArray());

            // Create InputFile from ByteArrayInputStream
            InputFile inputFile = new InputFile(byteArrayInputStream, "users_data.xlsx");

            telegramSender.sendDocument(update.getMessage().getChatId(), inputFile);

        } catch (IOException e) {
            log.warn(e.getMessage());
        }
    }


    @Handle(value = Action.COMMAND, command = "not_found")
    @AdminAction
    public void notFoundCommand(List<String> arguments, Update update) {
        telegramSender.htmlMessage(update.getMessage().getChatId(), "Command doesn't exist.");
    }

    @Handle(value = Action.REPLY_TO, command = "BY_USER_STATE")
    public void stateReply(List<String> arguments, Update update) {

        User user = userRepository.getUserById(update.getMessage().getFrom().getId()).orElse(null);
        if (user == null) {
            startCommand(arguments, update);
            return;
        }

        String userInput;
        if (update.getMessage().hasContact()) {
            userInput = update.getMessage().getContact().getPhoneNumber();
        } else {
            userInput = update.getMessage().getText();
        }

        user.setState(
            switch (user.getState()) {
                case NAME -> {
                    user.setFullName(userInput);
                    yield State.SURNAME;
                }
                case SURNAME -> {
                    user.setFullName(user.getFullName() + " " + userInput);
                    user.setFullName(
                            Arrays.stream(user.getFullName().split("\\s+"))
                                .map(word -> Character.toUpperCase(word.charAt(0)) + word.substring(1).toLowerCase())
                                .collect(java.util.stream.Collectors.joining(" "))
                    );
                    yield State.PHONE;
                }
                case PHONE -> {
                    user.setPhoneNumber(userInput);
                    yield State.SPECIALTY;
                }
                case SPECIALTY -> {
                    user.setSpeciality(userInput);
                    telegramSender.htmlMessage(update.getMessage().getChatId(), "<b>Дякуємо за реєстрацію!</b>");
                    yield State.REGISTERED;
                }
                case REGISTERED -> State.REGISTERED;
            }
        );

        userRepository.save(user);
        handleState(update, user);
    }

    private void handleState(Update update, User user) {
        String message;
        switch (user.getState()) {
            case NAME -> {
                message = """
                        Вкажи своє <u><b>ім'я</b></u>.
                        
                        <i>*у відповідь на це повідомлення</i>
                        """;
                telegramSender.htmlForceReplyMessage(update.getMessage().getChatId(), message);
            }
            case SURNAME -> {
                message = """
                        Вкажи своє <u><b>прізвище</b></u>.
                        
                        <i>*у відповідь на це повідомлення</i>
                        """;
                telegramSender.htmlForceReplyMessage(update.getMessage().getChatId(), message);
            }
            case PHONE -> {
                message = """
                        Вкажи свій <u><b>номер телефону.</b></u>.
                        
                        <i>*скористайтесь кнопкою нижче</i>
                        """;
                telegramSender.requestContact(update.getMessage().getChatId(), message);
            }
            case SPECIALTY -> {
                message = """
                        Напиши <u><b>спеціальність</b></u> на яку плануєш вступати.
                        
                        <i>*у відповідь на це повідомлення</i>
                        """;
                telegramSender.htmlForceReplyMessage(update.getMessage().getChatId(), message);
            }
            case REGISTERED -> {
                message = """
                        <b>Приходь <u>5 липня о 12:00</u> на NAU Open Day. Ти дізнаєшся:</b>
                        
                        • які є високотехнологічні та авіаційні спеціальності, зокрема кіберзахист, штучний інтелект, радіозв'язок, авіаційна та ракетно-космічна техніка й авіаційний транспорт;
                        • у яких дружніх технологічних компаніях можна почати кар'єру;\s
                        • як проходитиме твоє студентське життя поза навчанням.
                        
                        <b>Я тобі надішлю нагадування перед початком з уточненням місця проведення, лише не блокуй бота</b> 😉
                        
                        Більше анонсів подій та інформації про вступну кампанію на каналі Вступник НАУ 2024. <a href="http://t.me/pknau">Підписуйся</a>!
                        """;
                telegramSender.htmlMessage(update.getMessage().getChatId(), message);
            }
        }
    }

}
