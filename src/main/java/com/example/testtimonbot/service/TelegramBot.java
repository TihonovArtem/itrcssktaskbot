package com.example.testtimonbot.service;


import com.example.testtimonbot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    ArrayList<String> taskContentKey = new ArrayList<String>();
    ArrayList<String> taskContentKey2 = new ArrayList<String>();
    HashMap<Long, Long> messageMap = new HashMap<>();
    HashMap<Long, Long> messageCountMap = new HashMap<>();

    Map<Long, ArrayList<String>> taskContent = new HashMap<>();


    final BotConfig config;

    private int count;

    int messageRepeatStartCommand = 0;
    int messageRepeatStartCommand2 = 0;
    int messageRepeatNewPlace = 0;
    int messageRepeatNewPlace2 = 0;
    int messageRepeatSendTask = 0;
    int messageRepeatSendTask2 = 0;
    int messageIdNow = 0;
    int messageIdNow2 = 0;
    int messageIdStart = 0;
    int messageIdStart2 = 0;
    long messageDone = 0;
    long messageDone2 = 0;
    int telefonAddSet = 0;
    int telefonAddSet2 = 0;
    long chatIdDone = 0;
    long chatIdDone2 = 0;

    long firstUserId = 0;

    // Пошаговое состояние опроса для каждого из двух пользователей
    int user1Step = -1; // -1 нет активного опроса; 0 кабинет; 1 ФИО; 2 сроки; 3 содержание; 4 телефон; 5 подтверждение
    int user2Step = -1;

    private static ScheduledExecutorService scheduler1 = Executors.newSingleThreadScheduledExecutor();
    private static ScheduledExecutorService scheduler2 = Executors.newSingleThreadScheduledExecutor();
    private static Future<?> scheduledTask1;
    private static Future<?> scheduledTask2;

    String smile = " \uD83D\uDE01 " + "Новая заявка";



    static final String HELP_TEXT = "This bot is craeted deomnstrate Spring capabilities. \n\n" +
            "You can execute commands from this main menu on the left or nycommand:\n\n" +
            "Type /start yp see welcome message\n\n" +
            "Type /mydata to see data stored yoursely\n\n" +
            "Type /help to see this message again";

    public TelegramBot(BotConfig config) {


        this.config = config;

        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "Кнопка перезапуска Бота"));
        listofCommands.add(new BotCommand("/description", "Кнопка для получения информации о боте"));



        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's list " + e.getMessage());

        }
    }



    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }


    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
           long chatId = update.getMessage().getChatId();

                // Сохраняем дату и время сообщения
                SimpleDateFormat formater = new SimpleDateFormat("dd.MM.yyyy HH:mm");
                Date date1 = new Date();
                String date = formater.format(date1);

                //Имя пользователя
                String firstName = update.getMessage().getChat().getFirstName();
                // Получаем ID пользователя
                long userId = update.getMessage().getFrom().getId();
                // Получаем userName
                String userName = update.getMessage().getFrom().getUserName();
                // Получаем lastName
                String lastName = update.getMessage().getFrom().getLastName();

                System.out.println(userId + " " + userName + " " + lastName);

                //Блок кода для проверки UserId, что бы 2 пользователя независимо от друга заполяняли каждый свою заявку
                long currentUserId = userId;

                if (firstUserId == 0) {
                // Сохраняем ID первого обратившегося пользователя
                firstUserId = currentUserId;
                }
                //Блок кода для 1-го пользователя
                else if (currentUserId == firstUserId) {
                //Если Пользователь совпадает с первым обращением, то этот цикл кода для него
                    // Id обновлённого сообщения для первого пользователя
                    messageIdNow = update.getMessage().getMessageId();
                    System.out.println("\nonUpdateReceived messegeIdNow = " + messageIdNow);

                    switch (messageText) {
                        case "/start":
                            messageRepeatStartCommand = 0;
                            messageRepeatNewPlace = 0;
                            messageRepeatSendTask = 0;
                            messageIdNow = 0;
                            messageIdStart = 0;
                            taskContentKey.clear();
                            user1Step = -1;
                            System.out.println("сработал /start для User1 " + "(" + DateForLog() + ")");
                            startCommandReceived(chatId, firstName);
                            loadMessageMapFromCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");
                            //отменяем таймер
                            cancelTimerIfActiveUser1();
                            break;

                        case "/description":
                            messageRepeatStartCommand = 0;
                            messageRepeatNewPlace = 0;
                            messageRepeatSendTask = 0;
                            messageIdNow = 0;
                            messageIdStart = 0;
                            taskContentKey.clear();
                            user1Step = -1;
                            System.out.println("сработал /description для User1 " + "(" + DateForLog() + ")");
                            menuDescriptionAnswer(chatId, firstName);
                            //отменяем таймер
                            cancelTimerIfActiveUser1();
                            break;

                        case "\u25B6 Новая заявка":
                            System.out.println("сработала кнопка Новая заявка для User1 " + "(" + DateForLog() + ")");
                            //таймер, что бы через 30 минут сбросить незаконченную заявку
                            startTimerUser1(chatId, "От Вас не поступало никаких действий 30 минут, пожалуйста если есть необходимость, то заполните заявку заново. \n\nНиже под этим сообщением есть кнопка --->\u25B6 Новая заявка <---  Нажмите её \n                                \uD83D\uDC47"

);
                            //выбор места где надо выполнить заявку
                            newPlace(chatId, firstName);
                            System.out.println(chatId);
                            user1Step = -1; // пока ожидаем выбор места
                            break;

                        case "\uD83C\uDFD0 КА":
                            System.out.println("сработала кнопка КА для User1 " + "(" + DateForLog() + ")");
                            newСabinet(chatId, firstName);
                            taskContentKey.add("Место выполнения заявки :" + messageText);
                            user1Step = 0; // далее ждём кабинет
                            break;

                        case "\uD83C\uDFD2 ЛДК":
                            System.out.println("сработала кнопка ЛДК для User1 " + "(" + DateForLog() + ")");
                            newСabinet(chatId, firstName);
                            taskContentKey.add("Место выполнения заявки :" + messageText);
                            user1Step = 0;
                            break;

                        case "\uD83C\uDFBE ТЦК":
                            System.out.println("сработала кнопка ТЦК для User1 " + "(" + DateForLog() + ")");
                            newСabinet(chatId, firstName);
                            taskContentKey.add("Место выполнения заявки :" + messageText);
                            user1Step = 0;
                            break;


                        case "\uD83C\uDFCD\uFE0F Люскус":
                            System.out.println("сработала кнопка Люскус для User1 " + "(" + DateForLog() + ")");
                            newСabinet(chatId, firstName);
                            taskContentKey.add("Место выполнения заявки :" + messageText);
                            user1Step = 0;
                            break;

                        case "\uD83C\uDFAE ФЦ":
                            System.out.println("сработала кнопка Фиджитал для User1 " + "(" + DateForLog() + ")");
                            newСabinet(chatId, firstName);
                            taskContentKey.add("Место выполнения заявки :" + messageText);
                            user1Step = 0;
                            break;

                        case "\u2B05 Отмена":
                            messageRepeatStartCommand = 0;
                            messageRepeatNewPlace = 0;
                            messageRepeatSendTask = 0;
                            messageIdNow = 0;
                            messageIdStart = 0;
                            taskContentKey.clear();
                            user1Step = -1;
                            System.out.println("сработала кнопка Отмена для User1 " + "(" + DateForLog() + ")");
                            startCommandReceived(chatId, firstName);
                            //отменяем таймер
                            cancelTimerIfActiveUser1();
                            break;

                        case "\u2709 Отправить заявку":
                            System.out.println("сработала кнопка Отправить заявку для User1 " + "(" + DateForLog() + ")");
                            //отменяем таймер
                            cancelTimerIfActiveUser1();
                            if (telefonAddSet == 1 && taskContentKey.size() >= 6) {
                                newTaskSendToBase(chatId, messageText, date, firstName, userId, lastName, userName);
                                endOfApplication(chatId, update.getMessage().getChat().getFirstName());
                                taskContentKey.clear();
                            } else {
                                sendMessageButtomWithPlaceAndButtomCancle(chatId, "Пожалуйста, заполните все поля и укажите телефон");
                            }
                            break;

                        //Бот отвечает пользователю, что его заявка выполнена, если в группе мы ответили на его заявку словами:
                        case "выполнено":
                        case "выполнено.":
                        case "Выполнено":
                        case "Выполнено.":
                        case "выполнена":
                        case "выполнена.":
                        case "Выполнена":
                        case "Выполнена.":
                        case "выполнил":
                        case "выполнил.":
                        case "Выполнил":
                        case "Выполнил.":
                        case "готово":
                        case "готово.":
                        case "Готово":
                        case "Готово.":
                        case "решено":
                        case "решено.":
                        case "Решено":
                        case "Решено.":
                        case "Сделано":
                        case "Сделано.":
                        case "сделано":
                        case "сделано.":
                        case "не актуально":
                        case "☑\uFE0F":
                        case "✅":
                        case "➕":
                        case "+":

                            try {
                                String chanIdStr = config.getChanallId();
                                long chanId = Long.parseLong(chanIdStr);
                                if (update.getMessage().getChatId() == chanId && update.getMessage().getReplyToMessage() != null) {
                                    //repliedMessageId - Id сообщения на которое отвечаем в группе(т.е. ID заявки)
                                    long repliedMessageId = update.getMessage().getReplyToMessage().getMessageId();
                                    String repliedText = update.getMessage().getReplyToMessage().getText();
                                    System.out.println("repliedMessageId = " + repliedMessageId + " (" + DateForLog() + ")");
                                    //номер заявки на который поступает ответ о выполнении
                                    String ticketNumber = extractTicketNumber(repliedText);
                                    //ответ бота на выполненную заявку
                                    replyToUserDone(repliedMessageId, ticketNumber);

                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                            break;


            /*    case "/help":
                    sendMessageWithButtomNewTask(chatId, HELP_TEXT);
                    break;
*/
                        case "/register":

                            //register()
                            break;

                        default:

                            //взял переменную chanallId из applicatio.properties и преобразовал её в тип long, что бы можно было использовать в условиях ниже.
                            //т.е. сравнивая текущую переменную chatId и longchanallId
                            String chanallId = config.getChanallId();
                            long longchanallId = Long.parseLong(chanallId);
                            System.out.println(longchanallId);

//                    long longchanallId = -4730781475l;

//                        if (messageText == "выполнено") {
//                            System.out.println("Заявка Выполнена!!!");
//                            break;
//                        }

//                        //условия для ответа, отсекает сообщение, когда в группе жмёшь Ответить и @
//                        if (messageRepeatStartCommand == 1 && chatId == longchanallId) {
//                            System.out.println("сработало условие break");
//                            break;
//                        }





                            // Обработка ответов в канале на заявки пользователей
                            if (chatId == longchanallId && update.getMessage().getReplyToMessage() != null) {
                                long repliedMessageId = update.getMessage().getReplyToMessage().getMessageId();
                                String replyText = update.getMessage().getText();
                                System.out.println("Получен ответ в канале на заявку " + repliedMessageId + ": " + replyText + " (" + DateForLog() + ")");
                                
                                // Пропускаем ответы, начинающиеся с @ (внутренние комментарии)
                                if (replyText != null && replyText.trim().startsWith("@")) {
                                    System.out.println("Пропущен ответ с @ (внутренний комментарий): " + replyText + " (" + DateForLog() + ")");
                                    break;
                                }
                                
                                // Проверяем, является ли ответ словом "выполнено" (закрытие заявки)
                                if (isCompletionMessage(replyText)) {
                                    String ticketNumber = extractTicketNumber(update.getMessage().getReplyToMessage().getText());
                                    try {
                                        replyToUserDone(repliedMessageId, ticketNumber);
                                    } catch (IOException e) {
                                        System.err.println("Ошибка при закрытии заявки: " + e.getMessage() + " (" + DateForLog() + ")");
                                        e.printStackTrace();
                                    }
                                } else {
                                    // Обычный ответ - пересылаем пользователю без удаления данных
                                    String originalMessageText = update.getMessage().getReplyToMessage().getText();
                                    forwardReplyToUser(repliedMessageId, replyText, originalMessageText);
                                }
                                break; // Выходим из switch, чтобы не обрабатывать дальше
                            }

                            //условия для ответа, если ввёл сообщение не нажав кнопку "Новая заявка..."
                            if (messageRepeatStartCommand == 1 && chatId != longchanallId) {
                                repeatNewStart(messageText, chatId, update.getMessage().getChat().getFirstName());
                                System.out.println("сработал метод NewStart для User1 " + "(" + DateForLog() + ")");
                            }

                            //условия для ответа, если ввёл сообщение не нажав кнопку "Кузбасс-Арена" или кнопку "ЛДК"
                            if (messageRepeatNewPlace == 1) {
                                repeatNewPlace(messageText, chatId, update.getMessage().getChat().getFirstName());
                                System.out.println("сработал метод NewPlace для User1 " + "(" + DateForLog() + ")");
                            }

                            //условия для ответа, если ввёл сообщение не нажав кнопку "Отправить заявку"
                            if (messageRepeatSendTask == 1) {
                                newSendTask(chatId, update.getMessage().getChat().getFirstName());
                                System.out.println("сработал метод newSendTask для User1 " + "(" + DateForLog() + ")");

                            }

                            // Пошаговый сценарий для первого пользователя
                            if (user1Step == 0) {
                                System.out.println("сработал метод newnewFullName для User1 " + "(" + DateForLog() + ")");
                                taskContentKey.add("\nКабинет или помещение : " + messageText);
                                newFullName(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user1Step = 1;
                            } else if (user1Step == 1) {
                                System.out.println("сработал метод newDesiredDates для User1 " + "(" + DateForLog() + ")");
                                taskContentKey.add("\nФИО : " + messageText);
                                newDesiredDates(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user1Step = 2;
                            } else if (user1Step == 2) {
                                System.out.println("сработал метод newContentTask для User1 " + "(" + DateForLog() + ")");
                                taskContentKey.add("\nСроки : " + messageText);
                                newContentTask(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user1Step = 3;
                            } else if (user1Step == 3) {
                                System.out.println("сработал метод newTelefonNumber для User1 " + "(" + DateForLog() + ")");
                                taskContentKey.add("\nЧто нужно сделать : " + messageText);
                                newTelefonNumber(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user1Step = 4;
                            } else if (user1Step == 4) {
                                System.out.println("сработал метод telefoneAdd для User1 " + "(" + DateForLog() + ")");
                                taskContentKey.add("\nТелефон пользователя : " + messageText);
                                telefoneAdd(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user1Step = 5; // подтверждение
                            }



                   /* newTaskSendToBase(messageText, update.getMessage().getChat().getFirstName(), date, firstName);
                    break;*/

                            // sendMessageWithButtomNewTask(chatId, "I,m Sorry, command was not recognize");


                    }
                }

                else
                {
                    // Id обновлённого сообщения для второго пользователя
                    messageIdNow2 = update.getMessage().getMessageId();

                    switch (messageText) {
                        case "/start":
                            messageRepeatStartCommand2 = 0;
                            messageRepeatNewPlace2 = 0;
                            messageRepeatSendTask2 = 0;
                            messageIdNow2 = 0;
                            messageIdStart2 = 0;
                            taskContentKey2.clear();
                            user2Step = -1;
                            System.out.println("сработал /start для User2 " + "(" + DateForLog() + ")");
                            startCommandReceived2(chatId, firstName);
                            loadMessageMapFromCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");
                            //отменяем таймер
                            cancelTimerIfActiveUser2();
                            break;

                        case "/description":
                            messageRepeatStartCommand2 = 0;
                            messageRepeatNewPlace2 = 0;
                            messageRepeatSendTask2 = 0;
                            messageIdNow2 = 0;
                            messageIdStart2 = 0;
                            taskContentKey2.clear();
                            user2Step = -1;
                            System.out.println("сработал /description для User2 " + "(" + DateForLog() + ")");
                            menuDescriptionAnswer2(chatId, firstName);
                            //отменяем таймер
                            cancelTimerIfActiveUser2();
                            break;

                        case "\u25B6 Новая заявка":
                            System.out.println("сработала кнопка Новая заявка для User2 " + "(" + DateForLog() + ")");
                            //таймер, что бы через 30 минут сбросить незаконченную заявку
                            startTimerUser2(chatId, "От Вас не поступало никаких действий 30 минут, пожалуйста если есть необходимость, то заполните заявку заново. \n\nНиже под этим сообщением есть кнопка --->\u25B6 Новая заявка <---  Нажмите её \n                                \uD83D\uDC47");
                            //выбор места где надо выполнить заявку
                            newPlace2(chatId, firstName);
                            System.out.println(chatId);
                            user2Step = -1;
                            break;

                        case "\uD83C\uDFD0 КА":
                            System.out.println("сработала кнопка КА для User2 " + "(" + DateForLog() + ")");
                            newСabinet2(chatId, firstName);
                            taskContentKey2.add("Место выполнения заявки :" + messageText);
                            user2Step = 0;
                            break;

                        case "\uD83C\uDFD2 ЛДК":
                            System.out.println("сработала кнопка ЛДК для User2 " + "(" + DateForLog() + ")");
                            newСabinet2(chatId, firstName);
                            taskContentKey2.add("Место выполнения заявки :" + messageText);
                            user2Step = 0;
                            break;

                        case "\uD83C\uDFBE ТЦК":
                            System.out.println("сработала кнопка ТЦК для User2 " + "(" + DateForLog() + ")");
                            newСabinet2(chatId, firstName);
                            taskContentKey2.add("Место выполнения заявки :" + messageText);
                            user2Step = 0;
                            break;


                        case "\uD83C\uDFCD\uFE0F Люскус":
                            System.out.println("сработала кнопка Люскус для User2 " + "(" + DateForLog() + ")");
                            newСabinet2(chatId, firstName);
                            taskContentKey2.add("Место выполнения заявки :" + messageText);
                            user2Step = 0;
                            break;

                        case "\uD83C\uDFAE ФЦ":
                            System.out.println("сработала кнопка Фиджитал для User2 " + "(" + DateForLog() + ")");
                            newСabinet2(chatId, firstName);
                            taskContentKey2.add("Место выполнения заявки :" + messageText);
                            user2Step = 0;
                            break;

                        case "\u2B05 Отмена":
                            messageRepeatStartCommand2 = 0;
                            messageRepeatNewPlace2 = 0;
                            messageRepeatSendTask2 = 0;
                            messageIdNow2 = 0;
                            messageIdStart2 = 0;
                            taskContentKey2.clear();
                            user2Step = -1;
                            System.out.println("сработала кнопка Отмена для User2 " + "(" + DateForLog() + ")");
                            startCommandReceived2(chatId, firstName);
                            //отменяем таймер
                            cancelTimerIfActiveUser2();
                            break;

                        case "\u2709 Отправить заявку":
                            System.out.println("сработала кнопка Отправить заявку для User2 " + "(" + DateForLog() + ")");
                            //отменяем таймер
                            cancelTimerIfActiveUser2();
                            if (telefonAddSet2 == 1 && taskContentKey2.size() >= 6) {
                                newTaskSendToBase2(chatId, messageText, date, firstName, userId, lastName, userName);
                                endOfApplication2(chatId, update.getMessage().getChat().getFirstName());
                                taskContentKey2.clear();
                            } else {
                                sendMessageButtomWithPlaceAndButtomCancle(chatId, "Пожалуйста, заполните все поля и укажите телефон");
                            }
                            break;


                        case "выполнено":
                        case "выполнено.":
                        case "Выполнено":
                        case "Выполнено.":
                        case "выполнена":
                        case "выполнена.":
                        case "Выполнена":
                        case "Выполнена.":
                        case "выполнил":
                        case "выполнил.":
                        case "Выполнил":
                        case "Выполнил.":
                        case "готово":
                        case "готово.":
                        case "Готово":
                        case "Готово.":
                        case "решено":
                        case "решено.":
                        case "Решено":
                        case "Решено.":
                        case "Сделано":
                        case "Сделано.":
                        case "сделано":
                        case "сделано.":
                        case "не актуально":
                        case "☑\uFE0F":
                        case "✅":
                        case "➕":
                        case "+":

                            try {
                                String chanIdStr = config.getChanallId();
                                long chanId = Long.parseLong(chanIdStr);
                                if (update.getMessage().getChatId() == chanId && update.getMessage().getReplyToMessage() != null) {
                                    long repliedMessageId = update.getMessage().getReplyToMessage().getMessageId();
                                    String repliedText = update.getMessage().getReplyToMessage().getText();
                                    System.out.println("repliedMessageId = " + repliedMessageId + " (" + DateForLog() + ")");
                                    String ticketNumber = extractTicketNumber(repliedText);
                                    replyToUserDone(repliedMessageId, ticketNumber);

                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }

                            break;


            /*    case "/help":
                    sendMessageWithButtomNewTask(chatId, HELP_TEXT);
                    break;
*/
                        case "/register":

                            //register()
                            break;

                        default:

                            //взял переменную chanallId из applicatio.properties и преобразовал её в тип long, что бы можно было использовать в условиях ниже.
                            //т.е. сравнивая текущую переменную chatId и longchanallId
                            String chanallId = config.getChanallId();
                            long longchanallId = Long.parseLong(chanallId);
                            System.out.println(longchanallId);

//                    long longchanallId = -4730781475l;

//                        if (messageText == "выполнено") {
//                            System.out.println("Заявка Выполнена!!!");
//                            break;
//                        }

//                        //условия для ответа, отсекает сообщение, когда в группе жмёшь Ответить и @
//                        if (messageRepeatStartCommand == 1 && chatId == longchanallId) {
//                            System.out.println("сработало условие break");
//                            break;
//                        }





                            //условия для ответа, если ввёл сообщение не нажав кнопку "Новая заявка..."
                            if (messageRepeatStartCommand2 == 1 && chatId != longchanallId) {
                                repeatNewStart(messageText, chatId, update.getMessage().getChat().getFirstName());
                                System.out.println("сработал метод NewStart для User2 " + "(" + DateForLog() + ")");
                            }

                            //условия для ответа, если ввёл сообщение не нажав кнопку "Кузбасс-Арена" или кнопку "ЛДК"
                            if (messageRepeatNewPlace2 == 1) {
                                repeatNewPlace(messageText, chatId, update.getMessage().getChat().getFirstName());
                                System.out.println("сработал метод NewPlace для User2 " + "(" + DateForLog() + ")");
                            }

                            //условия для ответа, если ввёл сообщение не нажав кнопку "Отправить заявку"
                            if (messageRepeatSendTask2 == 1) {
                                newSendTask2(chatId, update.getMessage().getChat().getFirstName());
                                System.out.println("сработал метод newSendTask для User2 " + "(" + DateForLog() + ")");

                            }

                            // Пошаговый сценарий для второго пользователя
                            if (user2Step == 0) {
                                System.out.println("сработал метод newFullName для User2 " + "(" + DateForLog() + ")");
                                taskContentKey2.add("\nКабинет или помещение : " + messageText);
                                newFullName(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user2Step = 1;
                            } else if (user2Step == 1) {
                                System.out.println("сработал метод newDesiredDates для User2 " + "(" + DateForLog() + ")");
                                taskContentKey2.add("\nФИО : " + messageText);
                                newDesiredDates(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user2Step = 2;
                            } else if (user2Step == 2) {
                                System.out.println("сработал метод newContentTask для User2 " + "(" + DateForLog() + ")");
                                taskContentKey2.add("\nСроки : " + messageText);
                                newContentTask(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user2Step = 3;
                            } else if (user2Step == 3) {
                                System.out.println("сработал метод newTelefonNumber для User2 " + "(" + DateForLog() + ")");
                                taskContentKey2.add("\nЧто нужно сделать : " + messageText);
                                newTelefonNumber(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user2Step = 4;
                            } else if (user2Step == 4) {
                                System.out.println("сработал метод telefoneAdd2 для User2 " + "(" + DateForLog() + ")");
                                taskContentKey2.add("\nТелефон пользователя :" + messageText);
                                telefoneAdd2(messageText, chatId, update.getMessage().getChat().getFirstName());
                                user2Step = 5;
                            }

                            // Обработка ответов в канале на заявки пользователей (для User2)
                            if (chatId == longchanallId && update.getMessage().getReplyToMessage() != null) {
                                long repliedMessageId = update.getMessage().getReplyToMessage().getMessageId();
                                String replyText = update.getMessage().getText();
                                System.out.println("Получен ответ в канале на заявку " + repliedMessageId + ": " + replyText + " (" + DateForLog() + ")");
                                
                                // Пропускаем ответы, начинающиеся с @ (внутренние комментарии)
                                if (replyText != null && replyText.trim().startsWith("@")) {
                                    System.out.println("Пропущен ответ с @ (внутренний комментарий): " + replyText + " (" + DateForLog() + ")");
                                    break;
                                }
                                
                                // Проверяем, является ли ответ словом "выполнено" (закрытие заявки)
                                if (isCompletionMessage(replyText)) {
                                    String ticketNumber = extractTicketNumber(update.getMessage().getReplyToMessage().getText());
                                    try {
                                        replyToUserDone(repliedMessageId, ticketNumber);
                                    } catch (IOException e) {
                                        System.err.println("Ошибка при закрытии заявки: " + e.getMessage() + " (" + DateForLog() + ")");
                                        e.printStackTrace();
                                    }
                                } else {
                                    // Обычный ответ - пересылаем пользователю без удаления данных
                                    String originalMessageText = update.getMessage().getReplyToMessage().getText();
                                    forwardReplyToUser(repliedMessageId, replyText, originalMessageText);
                                }
                                break; // Выходим из switch, чтобы не обрабатывать дальше
                            }

                   /* newTaskSendToBase(messageText, update.getMessage().getChat().getFirstName(), date, firstName);
                    break;*/

                            // sendMessageWithButtomNewTask(chatId, "I,m Sorry, command was not recognize");


                    }

                }





        }

        // Сохраняем фотографию полученную от пользователя
        else if (update.hasMessage() && update.getMessage().hasPhoto()) {

            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            //Имя пользователя
            String firstName = update.getMessage().getChat().getFirstName();
            // Получаем ID пользователя
            long userId = update.getMessage().getFrom().getId();
            // Получаем userName
            String userName = update.getMessage().getFrom().getUserName();
            // Получаем lastName
            String lastName = update.getMessage().getFrom().getLastName();

            System.out.println(userId + " " + userName + " " + lastName);


            // Обновляем messageIdNow для соответствующего слота пользователя
            if (firstUserId == 0 || userId == firstUserId) {
                messageIdNow = update.getMessage().getMessageId();
            } else {
                messageIdNow2 = update.getMessage().getMessageId();
            }

            // Получаем фотографию из сообщения
            
            PhotoSize photo = update.getMessage().getPhoto().stream()
                    .sorted((ps1, ps2) -> Integer.compare(ps2.getFileSize(), ps1.getFileSize()))
                    .findFirst()
                    .orElse(null);


            if (photo != null) {
                System.out.println("Получено фото " + "(" + DateForLog() + ")");
                // Скачиваем фотографию на диск
                try {
                    File localFile = new File("C:\\distr\\Java\\downloaded_photo.jpg");
                    URL photoUrl = new URL(getFileUrl(photo.getFileId()));
                    FileUtils.copyURLToFile(photoUrl, localFile);
                    try {
                        // Раздельная логика подтверждения для двух пользователей
                        if (firstUserId == 0 || userId == firstUserId) {
                            if (telefonAddSet == 1) {
                                sendPhoto(firstName, userId, userName,lastName);
                            } else {
                                photoSentNotNow(chatId, "Abab");
                            }
                        } else {
                            if (telefonAddSet2 == 1) {
                                sendPhoto(firstName, userId, userName,lastName);
                            } else {
                                photoSentNotNow(chatId, "Abab");
                            }
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }


/*  Блок кода для ответа на отправленный файл, который Бот не умеет обрабатывать (отменяет таймер и очищает ранее записанные ответы,
    что бы пользователю дать возможность созать заявку заново */

        else  {
            //проверяем от какого пользователя поступают данные, что бы далее только по нему очищать данные массива и отменять таймер
            long userId = update.getMessage().getFrom().getId();
            long currentUserId = userId;
            if (firstUserId == 0) {
                // Сохраняем ID первого обратившегося пользователя
                firstUserId = currentUserId;
            }
            else if (currentUserId == firstUserId) {

                long chatId = update.getMessage().getChatId();
                // Информируем, что с файлами бот не умеет работать
                FileNotSend(chatId, update.getMessage().getChat().getFirstName());
                //отменяем таймер
                cancelTimerIfActiveUser1();
                messageRepeatStartCommand = 0;
                messageRepeatNewPlace = 0;
                messageRepeatSendTask = 0;
                messageIdNow = 0;
                messageIdStart = 0;
                taskContentKey.clear();
                user1Step = -1;
            }
            else {
                //Блок кода для ответа на отправленный файл, который Бот не умеет обрабатывать
                long chatId = update.getMessage().getChatId();
                // Информируем, что с файлами бот не умеет работать
                FileNotSend(chatId, update.getMessage().getChat().getFirstName());
                //отменяем таймер
                cancelTimerIfActiveUser2();
                messageRepeatStartCommand2 = 0;
                messageRepeatNewPlace2 = 0;
                messageRepeatSendTask2 = 0;
                messageIdNow2 = 0;
                messageIdStart2 = 0;
                taskContentKey2.clear();
                user2Step = -1;
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {

        messageRepeatNewPlace = 0;
        messageRepeatStartCommand = 1;
        telefonAddSet = 0;

        System.out.println("messegeIdStartCommand - " + messageRepeatStartCommand + " (" + DateForLog() + ")");

        String answer = "\uD83D\uDC4B Здравствуйте, " + name + ", я Бот. Принимаю заявки в ИТ отдел ГАУ <<РЦСС Кузбасса>>. Ниже под этим сообщением есть кнопка ---> \u25B6 Новая заявка <---  Нажмите её \n                                \uD83D\uDC47";

        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);

    }
    private void startCommandReceived2(long chatId, String name) {

        messageRepeatNewPlace2 = 0;
        messageRepeatStartCommand2 = 1;
        telefonAddSet2 = 0;

        System.out.println("messegeIdStartCommand - " + messageRepeatStartCommand2 + " (" + DateForLog() + ")");

        String answer = "\uD83D\uDC4B Здравствуйте, " + name + ", я Бот. Принимаю заявки в ИТ отдел ГАУ <<РЦСС Кузбасса>>. Ниже под этим сообщением есть кнопка ---> \u25B6 Новая заявка <---  Нажмите её \n                                \uD83D\uDC47";

        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);

    }

    private void repeatNewStart(String messageText, long chatId, String name) {

        String answer = "Нажмите кнопку \u25B6 Новая заявка\"";
        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);
    }

    private void menuDescriptionAnswer(long chatId, String name) {

        messageRepeatNewPlace = 0;
        messageRepeatStartCommand = 1;
        telefonAddSet = 0;

        System.out.println("messegeIdStartCommand - " + messageRepeatStartCommand + " (" + DateForLog() + ")");

        String answer = "\nВ ходе диалога заполните необходимые данные для отправки заявки в отдел ИТ \n" +
                "При необходимости прикрепите фотографии в конце диалога, перед нажатием кнопки \"Отправить заявку\"";

        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);

    }
    private void menuDescriptionAnswer2(long chatId, String name) {

        messageRepeatNewPlace2 = 0;
        messageRepeatStartCommand2 = 1;
        telefonAddSet2 = 0;

        System.out.println("messegeIdStartCommand - " + messageRepeatStartCommand2 + " (" + DateForLog() + ")");

        String answer = "\nВ ходе диалога заполните необходимые данные для отправки заявки в отдел ИТ \n" +
                "При необходимости прикрепите фотографии в конце диалога, перед нажатием кнопки \"Отправить заявку\"";

        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);

    }


    private void newPlace(long chatId, String name) {

        messageRepeatNewPlace = 1;
        messageRepeatStartCommand = 0;

        String answer = "Где будем выполнять заявку? \n(выберете ниже соответствующую кнопку) \n \n---Расшифровка кнопок--" +
                "\n\uD83C\uDFD0КА - Кузбасс-арена\n\uD83C\uDFD2ЛДК - Ледовый дворец Кузбасс\n\uD83C\uDFBEТЦК - Теннисный центр Кузбасс" +
                "\n\uD83C\uDFCD\uFE0FЛюскус - Мотоциклетная трасса Люскус\n\uD83C\uDFAEФЦ - Фиджитал-центр ";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomPlaceAndButtomCancle());


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());

        }

    }

    private void newPlace2(long chatId, String name) {

        messageRepeatNewPlace2 = 1;
        messageRepeatStartCommand2 = 0;

        String answer = "Где будем выполнять заявку? \n(выберете ниже соответствующую кнопку) \n \n---Расшифровка кнопок--" +
                "\n\uD83C\uDFD0КА - Кузбасс-арена\n\uD83C\uDFD2ЛДК - Ледовый дворец Кузбасс\n\uD83C\uDFBEТЦК - Теннисный центр Кузбасс" +
                "\n\uD83C\uDFCD\uFE0FЛюскус - Мотоциклетная трасса Люскус\n\uD83C\uDFAEФЦ - Фиджитал-центр ";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomPlaceAndButtomCancle());


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());

        }

    }

    private void repeatNewPlace(String messageText, long chatId, String name) {

        String answer = "Нажмите необходимую кнопку";
        log.info("Replied to user " + name);
        sendMessageButtomWithPlaceAndButtomCancle(chatId, answer);
    }


    private void newСabinet(long chatId, String name) {

        messageIdStart = messageIdNow;
        messageRepeatNewPlace = 0;

        String answer = "Напишите номер кабинета или номер помещения \uD83D\uDEAA" +
                "\n(затем отправьте сообщение)";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomCancel());



        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void newСabinet2(long chatId, String name) {

        messageIdStart2 = messageIdNow2;
        messageRepeatNewPlace2 = 0;

        String answer = "Напишите номер кабинета или номер помещения \uD83D\uDEAA" +
                "\n(затем отправьте сообщение)";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomCancel());



        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void newFullName(String messageText, long chatId, String name) {

        String answer = "Напишите ФИО \uD83D\uDDC3 \n(затем отправьте сообщение)";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomCancel());


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void newDesiredDates(String messageText, long chatId, String name) {

        String answer = "Напишите желаемые сроки выполнения заявки \uD83D\uDCC6 \n(затем отправьте сообщение)";
        log.info("Replied to user " + name);
        log.info("Replied to user ");

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomCancel());


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void newContentTask(String messageText, long chatId, String name) {

        String answer = "Напишите, что необходимо выполнить или предоставить, например \"у меня не работает интернет =)\" или \"замените картридж, принтер Xerox 3025\" и т.п. \uD83D\uDDA8 \uD83D\uDDA5" +
                "\n(затем отправьте сообщение)";
        log.info("Replied to user " + name);
        log.info("Replied to user ");

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomCancel());


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void newTelefonNumber(String messageText, long chatId, String name) {

        String answer = "Напишите контактный телефон \u260E \n(затем отправьте сообщение)";
        log.info("Replied to user " + name);
        log.info("Replied to user ");

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);


        message.setReplyMarkup(KeyboardUtils.buttomCancel());


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

// Метод отрабатывает, тогда, когда пользователь ввёл номер телефона и номер принят в заявку
    private void telefoneAdd(String messageText, long chatId, String name) {

        telefonAddSet = 1;
        messageRepeatSendTask = 1;
        String answer = "Подтвердите или отмените отправку заявки" +
                "\n(выберете ниже соответствующую кнопку)" + "\n Перед отправкой заявки можете прикрепить фотографии";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomSendTask());


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    // Метод для второго пользователя: после ввода телефона показать свои кнопки подтверждения
    private void telefoneAdd2(String messageText, long chatId, String name) {
        System.out.println("Отработал telefoneAdd2 " + "(" + DateForLog() + ")");
        telefonAddSet2 = 1;
        messageRepeatSendTask2 = 1;
        String answer = "Подтвердите или отмените отправку заявки" +
                "\n(выберете ниже соответствующую кнопку)" + "\n Перед отправкой заявки можете прикрепить фотографии";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomSendTask());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }


    private void sendPhoto(String firstName,long  userId, String userName, String lastName) throws TelegramApiException {

        String chanallId = config.getChanallId();

        //отправка файла
        File localFile = new File("C:\\distr\\Java\\downloaded_photo.jpg");
        InputFile inputFile = new InputFile(localFile);

        SendPhoto sendPhoto = new SendPhoto();
        sendPhoto.setChatId(String.valueOf(chanallId));
        sendPhoto.setPhoto(inputFile);

        //Для склеивания имя пользователя
        StringBuilder captionBuilder = new StringBuilder();
        if (firstName != null) {
            captionBuilder.append("Имя: ").append(firstName).append("\n");
        }

        if (lastName != null) {
            captionBuilder.append("Фамилия: ").append(lastName).append("\n");
        }

        if (userId != 0) { // Предполагаем, что userId не может быть 0, если это ID пользователя
            captionBuilder.append("ID: ").append(userId).append("\n");
        }

        if (userName != null) {
            captionBuilder.append("Никнейм для поиска в тг: @").append(userName).append("\n");
        }

        //а теперь склеиваем имя
        String caption = captionBuilder.toString();
        //подпись к отправленной фотографии
        sendPhoto.setCaption("Фотография отправлена пользователем:\n" + caption);
        System.out.println("Отправлено фото " + "(" + DateForLog() + ")");
        // Отправка фото
        try {
            execute(sendPhoto);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }

    }



    // Метод используется при получении фотографии
    private String getFileUrl(String fileId) {
        try {
            GetFile getFile = new GetFile();
            getFile.setFileId(fileId);
            org.telegram.telegrambots.meta.api.objects.File file = execute(getFile);
            return "https://api.telegram.org/file/bot" + getBotToken() + "/" + file.getFilePath();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return null;
        }
    }


    // метод-заглушка. Если пользователь отправил фото или файл приходит такой ответ
    private void FileNotSend(long chatId, String name) {


        String answer = "К сожалению, обрабатывать файлы я пока не умею. \n\nПожалуйста, если есть необходимость, то заполните заявку заново. \nНиже под этим сообщением есть кнопка --->\u25B6 Новая заявка <---  Нажмите её \n                                \uD83D\uDC47";
        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);


    }

    private void photoSentNotNow(long chatId, String name) {

        messageRepeatNewPlace = 0;
        messageRepeatStartCommand = 1;
        telefonAddSet = 0;

        String answer = "Могу принять Ваши фотографии только в конце диалога, перед нажатием кнопки \"Отправить заявку\". " +
                "А сейчас, пожалуйста,  начните заполнять заявку заново";
        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);
        taskContentKey.clear();

    }


    // Метод отрабатывает, тогда, когда пользователь вводит повторно символы вместо того что бы нажать кнопку "Отправить" или "Отменить"
    private void newSendTask(long chatId, String name) {
        telefonAddSet = 1;
        System.out.println("Отработал newSendTask " + "(" + DateForLog() + ")");
        String answer = "Подтвердите или отмените отправку заявки" +
                "\n(выберете ниже соответствующую кнопку) ";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);



        message.setReplyMarkup(KeyboardUtils.buttomSendTask());


        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    // Метод-заглушка для второго пользователя, если он печатает вместо нажатия кнопки
    private void newSendTask2(long chatId, String name) {
        telefonAddSet2 = 1;
        System.out.println("Отработал newSendTask2 " + "(" + DateForLog() + ")");
        String answer = "Подтвердите или отмените отправку заявки" +
                "\n(выберете ниже соответствующую кнопку) ";
        log.info("Replied to user " + name);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(answer);

        message.setReplyMarkup(KeyboardUtils.buttomSendTask());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void newTaskSendToBase(long chatId, String messageText, String date, String firstName, long userId, String lastName, String userName) {
        System.out.println("Отработал newTaskSendToBase " + "(" + DateForLog() + ")");




            // проверяем есть ли файл для переменной count (номер заявки)
            String filePath = "C:\\distr\\Java\\count.txt";
            Path path = Paths.get(filePath);

            boolean exists = Files.exists(path);
            boolean notExists = Files.notExists(path);
            boolean isDir = Files.isDirectory(path);


            if (isDir) {
                System.out.println("File is a Directory " + "(" + DateForLog() + ")");
            }
            else if (exists) {

                //если есть,то считываем из него переменную count (номер заявки)

                try {
                    // Чтение числа из файла
                    BufferedReader reader = new BufferedReader(new FileReader(filePath));
                    String line = reader.readLine();
                    int number = Integer.parseInt(line);
                    reader.close();

                    // Прибавление 1
                    count = number + 1;
                    String answer = "Ваша заявка №" + count + ", поступила в отдел ИТ ГАУ <<РЦССК>>\n" + taskContentKey.get(0) + taskContentKey.get(1) + taskContentKey.get(2) + taskContentKey.get(3) + taskContentKey.get(4) + taskContentKey.get(5);
                    log.info("Replied to user " + firstName);

                    sendMessageWithButtomNewTask(chatId, answer);


                    //записываем в массив номер заявки и chatId с пользователем, что бы потом прислать ответ на состояние "выполнено"
                    chatIdDone = chatId;
                    //конец блока для записи массива



                    // Запись обратно в файл переменной count
                    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
                    writer.write(Integer.toString(count));
                    writer.close();

                    System.out.println("Number successfully updated. " + "(" + DateForLog() + ")");


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            else if (notExists) {

                //если нет файла, то создаём файл для записи переменной count и записываем туда значение 1

                try {
                    FileOutputStream fos = new FileOutputStream("C:\\distr\\Java\\count.txt");
                    fos.write(Integer.toString(1).getBytes());
                    fos.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

                System.out.println("File doesn't exist!! " + "(" + DateForLog() + ")");
            }
            else {
                System.out.println("Program doesn't have access to the file!! " + "(" + DateForLog() + ")");
            }


            //отправка заявки в канал
            String chanallId = config.getChanallId();
            //long chanallId = -4730781475l;

            //Для склеивания данных из профиля пользователя в Tg
            StringBuilder captionBuilder = new StringBuilder();
            if (firstName != null) {
                captionBuilder.append("Имя: ").append(firstName).append("\n");
            }

            if (lastName != null) {
                captionBuilder.append("Фамилия: ").append(lastName).append("\n");
            }

            if (userId != 0) { // Предполагаем, что userId не может быть 0, если это ID пользователя
                captionBuilder.append("ID: ").append(userId).append("\n");
            }

            if (userName != null) {
                captionBuilder.append("Никнейм для поиска в тг: @").append(userName).append("\n");
            }

            //а теперь склеиваем в одно имя
            String caption = captionBuilder.toString();

            //собираем заявку
            String task = "Заявка № " + count + "  " + "\nот " + date + "\n" + "\n" + "Данные заполненные пользователем:" + "\n" + taskContentKey.get(0) +
                    taskContentKey.get(1) + taskContentKey.get(2) + taskContentKey.get(3) + taskContentKey.get(4) + taskContentKey.get(5)
                    + "\n" + "\n" + "Данные профиля пользователя из Tg:" + "\n" + caption;
            log.info("Replied to user " + firstName);

            int sentMessageId = sendMessageInChanall(chanallId, task);

//тут записывается номер сообщения заявки из канала, на которое потом можно будет ответить "выполнено"
// и бот по этим данным ответит пользователю, что его заявка выполнена
        messageDone = sentMessageId;
        messageMap.put(messageDone, chatIdDone);
        messageCountMap.put(messageDone, (long) count);
        System.out.println("Наконец-то записался массив messageMap = " + messageMap + " (" + DateForLog() + ")");

        // Если бот запущен заново и подана заявка, то он сначала записывает данные последней заявки в массив messageMap в коде выше, затем
// проверяет этот момент, если есть файл, то подгружает данные из него, если нет то создаёт новый файл и записывает в него данные последней заявки

        if (messageMap.size() == 1) {
            System.out.println("сработало условие messageMap.isEmpty " + messageMap + " (" + DateForLog() + ")");

            // проверяем есть ли файл для записи массива messageMap
            String filePath2 = "C:\\distr\\Java\\messageMap.csv";
            Path path2 = Paths.get(filePath2);

            boolean exists2 = Files.exists(path2);
            boolean notExists2 = Files.notExists(path2);
            boolean isDir2 = Files.isDirectory(path2);

            if (isDir2) {
                System.out.println("File is a Directory " + "(" + DateForLog() + ")");
            }

            else if (exists2) {

                //если есть,то загружаем из него данные в messageMap и загружаем в него данные
                loadMessageMapFromCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");
                saveMessageMapToCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");
            }

            else if (notExists2) {

                //если нет файла, то создаём файл для записи массива и записываем туда текущие значение
                saveMessageMapToCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");

            }

        }
        else {
            System.out.println("сработало условие messageMap с данными " + messageMap);
            saveMessageMapToCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");
        }

        // Если бот запущен заново и подана заявка, то он сначала записывает данные последней заявки в массив messageCountMap в коде выше, затем
// проверяет этот момент, если есть файл, то подгружает данные из него, если нет то создаёт новый файл и записывает в него данные последней заявки

        if (messageCountMap.size() == 1) {
            System.out.println("сработало условие messageCountMap.isEmpty " + messageCountMap + " (" + DateForLog() + ")");

            // проверяем есть ли файл для записи массива messageCountMap
            String filePath2 = "C:\\distr\\Java\\messageCountMap.csv";
            Path path2 = Paths.get(filePath2);

            boolean exists2 = Files.exists(path2);
            boolean notExists2 = Files.notExists(path2);
            boolean isDir2 = Files.isDirectory(path2);

            if (isDir2) {
                System.out.println("File is a Directory " + "(" + DateForLog() + ")");
            }

            else if (exists2) {

                //если есть,то загружаем из него данные в messageCountMap и загружаем в него данные
                loadMessageMapFromCsv(messageCountMap, "C:\\distr\\Java\\messageCountMap.csv");
                saveMessageMapToCsv(messageCountMap, "C:\\distr\\Java\\messageCountMap.csv");
            }

            else if (notExists2) {

                //если нет файла, то создаём файл для записи массива и записываем туда текущие значение
                saveMessageMapToCsv(messageCountMap, "C:\\distr\\Java\\messageCountMap.csv");

            }

        }
        else {
            System.out.println("сработало условие messageCountMap с данными " + messageCountMap);
            saveMessageMapToCsv(messageCountMap, "C:\\distr\\Java\\messageCountMap.csv");
        }



//        telefonAddSet = 1;
            telefonAddSet = 0;
            user1Step = -1;


    }
    private void newTaskSendToBase2(long chatId, String messageText, String date, String firstName, long userId, String lastName, String userName) {
        System.out.println("Отработал newTaskSendToBase2 " + "(" + DateForLog() + ")");




            // проверяем есть ли файл для переменной count (номер заявки)
            String filePath = "C:\\distr\\Java\\count.txt";
            Path path = Paths.get(filePath);

            boolean exists = Files.exists(path);
            boolean notExists = Files.notExists(path);
            boolean isDir = Files.isDirectory(path);


            if (isDir) {
                System.out.println("File is a Directory " + "(" + DateForLog() + ")");
            }
            else if (exists) {

                //если есть,то считываем из него переменную count (номер заявки)

                try {
                    // Чтение числа из файла
                    BufferedReader reader = new BufferedReader(new FileReader(filePath));
                    String line = reader.readLine();
                    int number = Integer.parseInt(line);
                    reader.close();

                    // Прибавление 1
                    count = number + 1;
                    String answer = "Ваша заявка №" + count + ", поступила в отдел ИТ ГАУ <<РЦССК>>\n" + taskContentKey2.get(0) + taskContentKey2.get(1) + taskContentKey2.get(2) + taskContentKey2.get(3) + taskContentKey2.get(4) + taskContentKey2.get(5);
                    log.info("Replied to user " + firstName);
                    sendMessageWithButtomNewTask(chatId, answer);


                    //записываем в массив номер заявки и chatId с пользователем, что бы потом прислать ответ на состояние "выполнено"
                    chatIdDone2 = chatId;
                    //конец блока для записи массива



                    // Запись обратно в файл переменной count
                    BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
                    writer.write(Integer.toString(count));
                    writer.close();

                    System.out.println("Number successfully updated. " + "(" + DateForLog() + ")");


                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            else if (notExists) {

                //если нет файла, то создаём файл для записи переменной count и записываем туда значение 1

                try {
                    FileOutputStream fos = new FileOutputStream("C:\\distr\\Java\\count.txt");
                    fos.write(Integer.toString(1).getBytes());
                    fos.close();
                }
                catch (IOException e)
                {
                    throw new RuntimeException(e);
                }

                System.out.println("File doesn't exist!! " + "(" + DateForLog() + ")");
            }
            else {
                System.out.println("Program doesn't have access to the file!! " + "(" + DateForLog() + ")");
            }


            //отправка заявки в канал
            String chanallId = config.getChanallId();
            //long chanallId = -4730781475l;

            //Для склеивания данных из профиля пользователя в Tg
            StringBuilder captionBuilder = new StringBuilder();
            if (firstName != null) {
                captionBuilder.append("Имя: ").append(firstName).append("\n");
            }

            if (lastName != null) {
                captionBuilder.append("Фамилия: ").append(lastName).append("\n");
            }

            if (userId != 0) { // Предполагаем, что userId не может быть 0, если это ID пользователя
                captionBuilder.append("ID: ").append(userId).append("\n");
            }

            if (userName != null) {
                captionBuilder.append("Никнейм для поиска в тг: @").append(userName).append("\n");
            }

            //а теперь склеиваем в одно имя
            String caption = captionBuilder.toString();

            //собираем заявку
            String task = "Заявка № " + count + "  " + "\nот " + date + "\n" + "\n" + "Данные заполненные пользователем:" + "\n" + taskContentKey2.get(0) +
                    taskContentKey2.get(1) + taskContentKey2.get(2) + taskContentKey2.get(3) + taskContentKey2.get(4) + taskContentKey2.get(5)
                    + "\n" + "\n" + "Данные профиля пользователя из Tg:" + "\n" + caption;
            log.info("Replied to user " + firstName);

            int sentMessageId = sendMessageInChanall(chanallId, task);

//тут записывается номер сообщения заявки из канала, на которое потом можно будет ответить "выполнено"
// и бот по этим данным ответит пользователю, что его заявка выполнена
        messageDone2 = sentMessageId;
        messageMap.put(messageDone2, chatIdDone2);
        messageCountMap.put(messageDone2, (long) count);
        System.out.println("Наконец-то записался массив messageMap = " + messageMap + " (" + DateForLog() + ")");

        // Если бот запущен заново и подана заявка, то он сначала записывает данные последней заявки в массив messageMap в коде выше, затем
// проверяет этот момент, если есть файл, то подгружает данные из него, если нет то создаёт новый файл и записывает в него данные последней заявки

        if (messageMap.size() == 1) {
            System.out.println("сработало условие messageMap.isEmpty " + messageMap + " (" + DateForLog() + ")");

            // проверяем есть ли файл для записи массива messageMap
            String filePath2 = "C:\\distr\\Java\\messageMap.csv";
            Path path2 = Paths.get(filePath2);

            boolean exists2 = Files.exists(path2);
            boolean notExists2 = Files.notExists(path2);
            boolean isDir2 = Files.isDirectory(path2);

            if (isDir2) {
                System.out.println("File is a Directory " + "(" + DateForLog() + ")");
            }

            else if (exists2) {

                //если есть,то загружаем из него данные в messageMap и загружаем в него данные
                loadMessageMapFromCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");
                saveMessageMapToCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");
            }

            else if (notExists2) {

                //если нет файла, то создаём файл для записи массива и записываем туда текущие значение
                saveMessageMapToCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");

            }

        }
        else {
            System.out.println("сработало условие messageMap с данными " + messageMap + " (" + DateForLog() + ")");
            saveMessageMapToCsv(messageMap, "C:\\distr\\Java\\messageMap.csv");
        }


        // Если бот запущен заново и подана заявка, то он сначала записывает данные последней заявки в массив messageCountMap в коде выше, затем
// проверяет этот момент, если есть файл, то подгружает данные из него, если нет то создаёт новый файл и записывает в него данные последней заявки

        if (messageCountMap.size() == 1) {
            System.out.println("сработало условие messageCountMap.isEmpty " + messageCountMap + " (" + DateForLog() + ")");

            // проверяем есть ли файл для записи массива messageCountMap
            String filePath2 = "C:\\distr\\Java\\messageCountMap.csv";
            Path path2 = Paths.get(filePath2);

            boolean exists2 = Files.exists(path2);
            boolean notExists2 = Files.notExists(path2);
            boolean isDir2 = Files.isDirectory(path2);

            if (isDir2) {
                System.out.println("File is a Directory " + "(" + DateForLog() + ")");
            }

            else if (exists2) {

                //если есть,то загружаем из него данные в messageCountMap и загружаем в него данные
                loadMessageMapFromCsv(messageCountMap, "C:\\distr\\Java\\messageCountMap.csv");
                saveMessageMapToCsv(messageCountMap, "C:\\distr\\Java\\messageCountMap.csv");
            }

            else if (notExists2) {

                //если нет файла, то создаём файл для записи массива и записываем туда текущие значение
                saveMessageMapToCsv(messageCountMap, "C:\\distr\\Java\\messageCountMap.csv");

            }

        }
        else {
            System.out.println("сработало условие messageCountMap с данными " + messageCountMap);
            saveMessageMapToCsv(messageCountMap, "C:\\distr\\Java\\messageCountMap.csv");
        }


//        telefonAddSet = 1;
            telefonAddSet2 = 0;
            user2Step = -1;


    }


    private void sendMessageWithButtomNewTask(long chatId, String testToSend) {

          SendMessage message = new SendMessage();
          message.setChatId(String.valueOf(chatId));
          message.setText(testToSend);

          message.setReplyMarkup(KeyboardUtils.buttomNewTask());

          try {
              execute(message);
          } catch (TelegramApiException e) {
              log.error("Error occurred: " + e.getMessage());
          }
      }

    private void sendMessageButtomWithPlaceAndButtomCancle(long chatId, String testToSend) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(testToSend);

        message.setReplyMarkup(KeyboardUtils.buttomPlaceAndButtomCancle());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void sendMessageWithButtomSendTask(long chatId, String testToSend) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(testToSend);

        message.setReplyMarkup(KeyboardUtils.buttomSendTask());

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private int sendMessageInChanall(String chanallId, String testToSend) {
        SendMessage messagebase = new SendMessage();
        messagebase.setChatId(chanallId);
        messagebase.setText(testToSend);

        try {
            Message sent = execute(messagebase);
            return sent.getMessageId();
        } catch (TelegramApiException e) {
            log.error("Error occurred: " + e.getMessage());
            return -1;
        }
    }

    // Метод для проверки, является ли сообщение словом "выполнено" (закрытие заявки)
    private boolean isCompletionMessage(String messageText) {
        if (messageText == null) {
            return false;
        }
        
        String text = messageText.toLowerCase().trim();
        return text.equals("выполнено") || 
               text.equals("выполнено.") || 
               text.equals("выполнено!") ||
               text.equals("выполнено?") ||
               text.equals("готово") ||
               text.equals("готово.") ||
               text.equals("готово!") ||
               text.equals("завершено") ||
               text.equals("завершено.") ||
               text.equals("завершено!") ||
               text.equals("сделано") ||
               text.equals("сделано.") ||
               text.equals("сделано!");
    }

    // Метод для пересылки ответа пользователю без удаления данных из файлов
    private void forwardReplyToUser(long repliedMessageId, String replyText, String originalMessageText) {
        System.out.println("Метод forwardReplyToUser начал работу " + "(" + DateForLog() + ")");
        
        Long chatIdObj = messageMap.get(repliedMessageId);
        if (chatIdObj == null) {
            String reply = "Соответствие для repliedMessageId не найдено в messageMap";
            System.out.println(reply);
            sendMessageInChanall(config.getChanallId(), reply);
            return;
        }
        
        long chatId = chatIdObj;
        System.out.println("Пересылаем ответ пользователю с chatId = " + chatId + " (" + DateForLog() + ")");
        
        // Извлекаем номер заявки из оригинального сообщения
        String ticketNumber = extractTicketNumber(originalMessageText);
        String messagePrefix = ticketNumber != null && !ticketNumber.isEmpty() 
            ? "\uD83D\uDCAC Ответ на вашу заявку №" + ticketNumber + ":\n"
            : "\uD83D\uDCAC Ответ на вашу заявку:\n";
        
        // Пересылаем текст ответа пользователю
        sendMessageWithButtomNewTask(chatId, messagePrefix + replyText);
        sendMessageInChanall(config.getChanallId(), "\uD83D\uDCAC Пользователь получил ответ на заявку");
        System.out.println("Пользователь получил ответ на заявку " + "(" + DateForLog() + ")");
        
        // НЕ удаляем данные из файлов - заявка остается активной
    }

    //метод для ответа пользователю, что его заявка выполнена
    private void replyToUserDone  (long repliedMessageId, String ticketNumber) throws IOException {
        System.out.println("метод replyToUserDone начал свою работу " + "(" + DateForLog() + ")");
        System.out.println(repliedMessageId);

        Long chatIdObj = messageMap.get(repliedMessageId);
        if (chatIdObj == null) {
            String reply = "Соответствие для repliedMessageId не найдено в messageMap";
            System.out.println(reply);
            sendMessageInChanall(config.getChanallId(), reply);
            return;
        }
        long chatId = chatIdObj;
        System.out.println("chatId из массива messageMap = "+ chatId + " (" + DateForLog() + ")");
        String answer = ticketNumber != null && !ticketNumber.isEmpty()
                ? "☑\uFE0F Ваша заявка №" + ticketNumber + " выполнена"
                : "☑\uFE0F Ваша заявка выполнена";
        sendMessageWithButtomNewTask(chatId, answer);
        sendMessageInChanall(config.getChanallId(), "☑\uFE0F Пользователь получил сообщение, что его заявка выполнена");
        System.out.println("Пользователь получил сообщение, что его заявка выполнена " + "(" + DateForLog() + ")");
        //удаляем из массива выполненную заявку
        messageMap.remove(repliedMessageId);
        messageCountMap.remove(repliedMessageId);
        System.out.println("Теперь в messageMap такие значения = " + messageMap + " (" + DateForLog() + ")");
        
        // Удаляем только конкретную запись из файлов вместо полной перезаписи
        try {
            removeEntryFromCsvFile("C:\\distr\\Java\\messageMap.csv", repliedMessageId);
            removeEntryFromCsvFile("C:\\distr\\Java\\messageCountMap.csv", repliedMessageId);
        } catch (IOException e) {
            System.err.println("Ошибка при удалении записи из файлов: " + e.getMessage() + " (" + DateForLog() + ")");
            e.printStackTrace();
        }
    }

    // Извлекает номер заявки из текста сообщения канала вида: "Заявка № <число> ..."
    private String extractTicketNumber(String text) {
        if (text == null) {
            return null;
        }
        try {
            java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("Заявка №\\s*(\\d+)").matcher(text);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    //Метод сначала загружает существующие ключи, а затем добавляет только те данные, которых ещё нет в файле.
    //добавляются новые данные в CSV-файл, без дубликатов.
    public void saveMessageMapToCsv(Map<Long, Long> map, String filename) {

        Set<Long> existingKeys = readExistingKeysMessageMap(filename);

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filename, true))) {
            for (Map.Entry<Long, Long> entry : map.entrySet()) {
                if (!existingKeys.contains(entry.getKey())) { // Проверка на дубликаты
                    writer.write(entry.getKey() + ";" + entry.getValue());
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Метод читает существующие ключи из файла и добавляет их в Set<Long>. Это позволяет быстро проверять наличие ключей (поскольку операции поиска в Set имеют сложность O(1)).
    private Set<Long> readExistingKeysMessageMap(String filename) {
        Set<Long> keys = new HashSet<>();
        File file = new File(filename);

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(";");
                    if (parts.length > 0) {
                        String rawKey = parts[0] == null ? "" : parts[0];
                        // Удаляем возможный BOM, пробелы и проверяем пустую строку
                        String keyStr = rawKey.replace("\uFEFF", "").trim();
                        if (keyStr.isEmpty()) {
                            continue;
                        }
                        try {
                            keys.add(Long.parseLong(keyStr)); // Добавляем ключ в множество
                        } catch (NumberFormatException e) {
                            // Логируем и пропускаем некорректные строки, чтобы не срывать сохранение
                            System.err.println("Некорректный ключ в файле " + filename + ": '" + rawKey + "' (" + DateForLog() + ")");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return keys;
    }

    //метод для загрузки данных из файла messageMap.csv
    public void loadMessageMapFromCsv(Map<Long, Long> map, String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            System.out.println("Файл не найден: " + filename + " (" + DateForLog() + ")");
            return;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 2) {
                    try {
                        Long key = Long.parseLong(parts[0].trim());
                        Long value = Long.parseLong(parts[1].trim());
                        map.put(key, value);
                    } catch (NumberFormatException e) {
                        System.err.println("Ошибка парсинга строки: " + line + " (" + DateForLog() + ")");
                    }
                }
            }
            System.out.println("файл messageMap.csv загружен = вот его содержимое: " + messageMap + " (" + DateForLog() + ")");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    //Метод для перезаписи данных в файле messageMap.csv из массива messageMap после выполнения заявки и удаления её данных из массива
    // ВНИМАНИЕ: Этот метод больше не используется, так как он приводит к потере данных при перезаписи файла.
    // Вместо него используется removeEntryFromCsvFile для точечного удаления записей.
    @Deprecated
    private void rewritingMessageMapfile(String filePath, HashMap<Long, Long> messageMap) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            // Если нужна шапка, раскомментируйте следующую строку
            // writer.write("Key,Value");
            // writer.newLine();

            for (HashMap.Entry<Long, Long> entry : messageMap.entrySet()) {
                String line = entry.getKey() + ";" + entry.getValue();
                writer.write(line);
                writer.newLine();
            }
        }
    }

    // Метод для удаления конкретной записи из CSV файла без потери остальных данных
    private void removeEntryFromCsvFile(String filePath, Long keyToRemove) throws IOException {
        File file = new File(filePath);
       if (!file.exists()) {
            System.out.println("Файл не найден для удаления записи: " + filePath + " (" + DateForLog() + ")");
            return;
        }
        
        // Читаем все строки кроме удаляемой
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length > 0) {
                    try {
                        Long key = Long.parseLong(parts[0].trim());
                        if (!key.equals(keyToRemove)) {
                            lines.add(line);
                        } else {
                            System.out.println("Удалена запись с ключом " + key + " из файла " + filePath + " (" + DateForLog() + ")");
                        }
                    } catch (NumberFormatException e) {
                        // Пропускаем некорректные строки, но сохраняем их
                        lines.add(line);
                        System.err.println("Ошибка парсинга строки при удалении: " + line + " (" + DateForLog() + ")");
                    }
                } else {
                    // Сохраняем пустые строки
                    lines.add(line);
                }
            }
        }
        
        // Перезаписываем файл без удаленной строки
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false))) {
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        }
        
        System.out.println("Файл " + filePath + " обновлен. Осталось записей: " + lines.size() + " (" + DateForLog() + ")");
    }

    private void endOfApplication(long chatId, String name) {

        messageRepeatSendTask = 0;
        messageRepeatStartCommand = 1;
        System.out.println("Конец приложения " + "(" + DateForLog() + ")");

        String answer = "Что бы подать новую заявку нажмите на кнопку \"Новая заявка в ИТ отдел ГАУ РЦСС Кузбасса\" \n \uD83D\uDC47";
        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);
    }
    private void endOfApplication2(long chatId, String name) {

        messageRepeatSendTask2 = 0;
        messageRepeatStartCommand2 = 1;
        System.out.println("Конец приложения " + "(" + DateForLog() + ")");

        String answer = "Что бы подать новую заявку нажмите на кнопку \"Новая заявка в ИТ отдел ГАУ РЦСС Кузбасса\" \n \uD83D\uDC47";
        log.info("Replied to user " + name);
        sendMessageWithButtomNewTask(chatId, answer);
    }


    //метод для запуска таймера, в случае если пользователь перестал заполнять заявку
    private void startTimerUser1(long chatID, String answer) {
        // Устанавливаем задание, которое сработает через 30 минут
        scheduledTask1 = scheduler1.schedule(() -> returnToStartUser1(chatID, answer), 30, TimeUnit.MINUTES);
        System.out.println("метод startTimer1 сработал " + "(" + DateForLog() + ")");
    }
    private void startTimerUser2(long chatID, String answer) {
        // Устанавливаем задание, которое сработает через 30 минут
        scheduledTask2 = scheduler2.schedule(() -> returnToStartUser1(chatID, answer), 30, TimeUnit.MINUTES);
        System.out.println("метод startTimer2 сработал " + "(" + DateForLog() + ")");
    }

    //метод для отмены таймера сброса заполнения заявки
    public void cancelTimerIfActiveUser1() {
        if (scheduledTask1 != null && !scheduledTask1.isCancelled()) {
            scheduledTask1.cancel(false); // Отменяем задание
            System.out.println("метод cancelTimerIfActiveUser1 сработал " + "(" + DateForLog() + ")");
        }
    }
       public void cancelTimerIfActiveUser2() {
        if (scheduledTask2 != null && !scheduledTask2.isCancelled()) {
            scheduledTask2.cancel(false); // Отменяем задание
            System.out.println("метод cancelTimerIfActiveUser2 сработал " + "(" + DateForLog() + ")");
        }
    }

    //метод для возврата к началу заполнения заявки после сработки таймера
    private void returnToStartUser1(long chatID, String answer) {
        System.out.println("Метод returnToStartUser1 выполнен автоматически через 30 минут ожидания. " + "(" + DateForLog() + ")");
        sendMessageWithButtomNewTask(chatID, answer);
        messageRepeatStartCommand = 0;
        messageRepeatNewPlace = 0;
        messageRepeatSendTask = 0;
        messageIdNow = 0;
        messageIdStart = 0;
        taskContentKey.clear();
        user1Step = -1;
    }
    private void returnToStartUser2(long chatID, String answer) {
        System.out.println("Метод returnToStartUser2 выполнен автоматически через 30 минут ожидания. " + "(" + DateForLog() + ")");
        sendMessageWithButtomNewTask(chatID, answer);
        messageRepeatStartCommand2 = 0;
        messageRepeatNewPlace2 = 0;
        messageRepeatSendTask2 = 0;
        messageIdNow2 = 0;
        messageIdStart2 = 0;
        taskContentKey2.clear();
        user2Step = -1;
    }

    private String DateForLog() {

        // Сохраняем дату и время для Логирования
        SimpleDateFormat formater = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.SSS");
        Date datenew = new Date();
        String dateForLog = formater.format(datenew);
        return dateForLog;

    }
}


