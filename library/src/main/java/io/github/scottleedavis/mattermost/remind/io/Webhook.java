package io.github.scottleedavis.mattermost.remind.io;

import io.github.scottleedavis.mattermost.remind.db.ReminderOccurrence;
import io.github.scottleedavis.mattermost.remind.exceptions.WebhookException;
import io.github.scottleedavis.mattermost.remind.messages.Attachment;
import io.github.scottleedavis.mattermost.remind.messages.Interaction;
import io.github.scottleedavis.mattermost.remind.messages.Response;
import io.github.scottleedavis.mattermost.remind.reminders.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;


@Service
public class Webhook {

    private static Logger logger = LoggerFactory.getLogger(Webhook.class);

    private String webhookUrl;

    @Autowired
    private Options options;

    @Autowired
    public Webhook(@Value("${remind.webhookUrl}") String webhookUrl) {
        if (webhookUrl == null)
            webhookUrl = "http://localhost:8065/";

        this.webhookUrl = webhookUrl;
        logger.info("remind.webhookUrl = {}", this.webhookUrl);
    }

    public ResponseEntity<String> remind(ReminderOccurrence reminderOccurrence) throws Exception {

        boolean isOtherUser = !reminderOccurrence.getReminder().getTarget().contains(reminderOccurrence.getReminder().getUserName());
        boolean isChannel = reminderOccurrence.getReminder().getTarget().charAt(0) == '~';

        Response response = new Response();
        response.setChannel(isChannel ?
                reminderOccurrence.getReminder().getTarget().substring(1) :
                reminderOccurrence.getReminder().getTarget());
        response.setUsername("mattermost-remind");
        response.setResponseType(Response.ResponseType.EPHEMERAL);
        Attachment attachment = new Attachment();
        attachment.setActions(options.finishedActions(reminderOccurrence.getId(),
                reminderOccurrence.getRepeat() != null, isChannel));
        attachment.setText((isOtherUser ? "@" + reminderOccurrence.getReminder().getUserName() : "You") +
                " asked me to remind you \"" + reminderOccurrence.getReminder().getMessage() + "\".");
        response.setAttachments(Arrays.asList(attachment));

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity(response, headers);
        try {
            ResponseEntity<String> out = restTemplate.exchange(webhookUrl, HttpMethod.POST, entity, String.class);
            logger.info(out.toString());
            return out;
        } catch (HttpClientErrorException e) {
            throw new WebhookException("Unable to send reminder occurrence (id " +
                    reminderOccurrence.getId() + "), target = " + reminderOccurrence.getReminder().getTarget() + ", " +
                    "userName = " + reminderOccurrence.getReminder().getUserName(), e);
        }

    }


    public ResponseEntity<String> page(Interaction interaction) throws Exception {

        Response response = new Response();
        response.setChannel("@" + interaction.getContext().getUserName());
        response.setUsername("mattermost-remind");

        if (interaction.getContext().getLastIndex() != null) {
            Integer firstIndex = interaction.getContext().getAction().equals("next") ?
                    interaction.getContext().getLastIndex() + 1 :
                    (interaction.getContext().getFirstIndex() - Options.remindListMaxLength);
            firstIndex = firstIndex < 0 ? 0 : firstIndex;
            response.setAttachments(
                    options.listRemindersAttachments(interaction.getContext().getUserName(), firstIndex)
            );
        } else {
            response.setAttachments(
                    options.listRemindersAttachments(interaction.getContext().getUserName(), 0)
            );
        }

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity entity = new HttpEntity(response, headers);
        try {
            ResponseEntity<String> out = restTemplate.exchange(webhookUrl, HttpMethod.POST, entity, String.class);
            logger.info(out.toString());
            return out;
        } catch (HttpClientErrorException e) {
            throw new WebhookException("Unable to send page reminder", e);
        }

    }

}
