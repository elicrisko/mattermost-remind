package io.github.scottleedavis.mattermost.remind.reminders;

import io.github.scottleedavis.mattermost.remind.db.Reminder;
import io.github.scottleedavis.mattermost.remind.db.ReminderService;
import io.github.scottleedavis.mattermost.remind.io.Webhook;
import io.github.scottleedavis.mattermost.remind.messages.Attachment;
import io.github.scottleedavis.mattermost.remind.messages.ParsedRequest;
import io.github.scottleedavis.mattermost.remind.messages.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

@Service
public class Scheduler {

    private static Logger logger = LoggerFactory.getLogger(Scheduler.class);

    @Value("${version}")
    private String version;

    @Autowired
    private Parser parser;

    @Autowired
    private Webhook webhook;

    @Autowired
    private Options options;

    @Autowired
    private Formatter formatter;

    @Autowired
    private ReminderService reminderService;

    public Response setReminder(String userName, String payload, String userId, String channelName) {

        Response response = new Response();
        try {

            ParsedRequest parsedRequest = parser.extract(payload);
            switch (parsedRequest.getTarget()) {
                case "help":
                    response.setText(Options.helpMessage);
                    break;
                case "list":
                    if (channelName.contains(userId)) {
                        response.setAttachments(options.listRemindersAttachments(userName));
                    } else {
                        response.setText(options.listReminders(userName));
                    }
                    break;
                case "version":
                    response.setText(version);
                    break;
                default:
                    if (parsedRequest.getTarget().charAt(0) == '@' &&
                            !(parsedRequest.getTarget().equalsIgnoreCase("@" + userName)) &&
                            (parsedRequest.getWhen().contains("every")) &&
                            !(userId.equalsIgnoreCase(parsedRequest.getTarget()))) {
                        response.setText(options.noUserRepeatText);
                    } else {
                        Reminder reminder = reminderService.schedule(userName, parsedRequest);
                        if (userId.equalsIgnoreCase(parsedRequest.getTarget())) {
                            parsedRequest.setTarget("me");
                        }
                        String responseText = formatter.reminderResponse(parsedRequest);
                        if (channelName.contains(userId)) {
                            Attachment attachment = new Attachment();
                            attachment.setActions(options.setActions(reminder.getId()));
                            attachment.setText(responseText);
                            response.setAttachments(Arrays.asList(attachment));
                        } else {
                            response.setText(responseText);
                        }
                    }
                    break;
            }

        } catch (Exception e) {
            response.setText(options.exceptionText);
        }

        response.setResponseType(channelName.contains(userId) ? Response.ResponseType.IN_CHANNEL : Response.ResponseType.EPHEMERAL);

        return response;
    }

    @Scheduled(fixedRate = 1000)
    public void runSchedule() {

        LocalDateTime ldt = LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);

        reminderService.findByOccurrence(ldt).forEach(reminderOccurrence -> {
            logger.info("Sending reminder {} to {} ", reminderOccurrence.getId(), reminderOccurrence.getReminder().getTarget());
            try {
                webhook.invoke(reminderOccurrence);
                if (reminderOccurrence.getRepeat() != null)
                    reminderService.reschedule(reminderOccurrence);
            } catch (Exception e) {
                logger.error("Not able to send reminder {}", e);
            }
        });

        reminderService.findBySnoozed(ldt).forEach(reminderOccurrence -> {
            logger.info("Sending snoozed reminder {} to {} ", reminderOccurrence.getId(), reminderOccurrence.getReminder().getTarget());
            try {
                webhook.invoke(reminderOccurrence);
            } catch (Exception e) {
                logger.error("Not able to send snoozed reminder {}", e);
            }
        });

    }

}
