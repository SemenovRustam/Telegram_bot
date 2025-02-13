package com.rustam_semenov.telegramm_bot.demo.command;

import com.rustam_semenov.telegramm_bot.demo.javarushclient.dto.GroupDiscussionInfo;
import com.rustam_semenov.telegramm_bot.demo.javarushclient.dto.GroupRequestArgs;
import com.rustam_semenov.telegramm_bot.demo.javarushclient.dto.JavaRushGroupClient;
import com.rustam_semenov.telegramm_bot.demo.repository.entity.GroupSub;
import com.rustam_semenov.telegramm_bot.demo.service.GroupSubService;
import com.rustam_semenov.telegramm_bot.demo.service.SendBotMessageService;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.stream.Collectors;

import static com.rustam_semenov.telegramm_bot.demo.command.CommandName.ADD_GROUP_SUB;
import static com.rustam_semenov.telegramm_bot.demo.command.CommandUtils.getChatId;
import static com.rustam_semenov.telegramm_bot.demo.command.CommandUtils.getMessage;
import static java.util.Objects.isNull;
import static org.apache.commons.lang3.StringUtils.SPACE;
import static org.hibernate.query.criteria.internal.ValueHandlerFactory.isNumeric;

public class AddGroupSubCommand implements Command {

    private final SendBotMessageService sendBotMessageService;
    private final JavaRushGroupClient javaRushGroupClient;
    private final GroupSubService groupSubService;

    public AddGroupSubCommand(SendBotMessageService sendBotMessageService, JavaRushGroupClient javaRushGroupClient,
                              GroupSubService groupSubService) {
        this.sendBotMessageService = sendBotMessageService;
        this.javaRushGroupClient = javaRushGroupClient;
        this.groupSubService = groupSubService;
    }

    @Override
    public void execute(Update update) {
        if (getMessage(update).equalsIgnoreCase(ADD_GROUP_SUB.getCommandName())) {
            sendGroupIdList(getChatId(update));
            return;
        }
        String groupId = getMessage(update).split(SPACE)[1];
        String chatId = getChatId(update);
        if (isNumeric(groupId)) {
            GroupDiscussionInfo groupById = javaRushGroupClient.getGroupById(Integer.parseInt(groupId));
            if (isNull(groupById.getId())) {
                sendGroupNotFound(chatId, groupId);
            }
            GroupSub savedGroupSub = groupSubService.save(chatId, groupById);
            sendBotMessageService.sendMessage(chatId, "Подписал на группу " + savedGroupSub.getTitle());
        } else {
                sendNotValidGroupID(chatId, groupId);
        }
    }

    private void sendGroupNotFound(String chatId, String groupId) {
        String groupNotFoundMessage = "Нет группы с ID = \"%s\"";
        sendBotMessageService.sendMessage(chatId, String.format(groupNotFoundMessage, groupId));
    }

    private void sendNotValidGroupID(String chatId, String groupId) {
        String groupNotFoundMessage = "Неправильный ID группы = \"%s\"";
        sendBotMessageService.sendMessage(chatId, String.format(groupNotFoundMessage, groupId));
    }

    private void sendGroupIdList(String chatId) {
        String groupIds = javaRushGroupClient.getGroupList(GroupRequestArgs.builder().build()).stream()
                .map(group -> String.format("%s - %s \n", group.getTitle(), group.getId()))
                .collect(Collectors.joining());

        String message = "Чтобы подписаться на группу - передай комадну вместе с ID группы. \n" +
                "Например: /addgroupsub 16 \n\n" +
                "я подготовил список всех групп - выбирай какую хочешь :) \n\n" +
                "имя группы - ID группы \n\n" +
                "%s";

        sendBotMessageService.sendMessage(chatId, String.format(message, groupIds));
    }
}
