package scottleedavis.mattermost.remind.db;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.transaction.annotation.Transactional;
import scottleedavis.mattermost.remind.messages.ParsedRequest;

import javax.annotation.Resource;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReminderServiceTests {

    private Reminder reminder;

    @Autowired
    ReminderService reminderService;

    @Resource
    ReminderRepository reminderRepository;

    @Resource
    ReminderOccurrenceRepository reminderOccurrenceRepository;

    @Before
    public void setUp() {
        Reminder reminder = new Reminder();
        reminder.setMessage("foo to the bar");
        reminder.setTarget("@foo");
        reminder.setUserName("@foo");
        ReminderOccurrence reminderOccurrence = new ReminderOccurrence();
        reminderOccurrence.setOccurrence(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        reminderOccurrence.setReminder(reminder);
        ReminderOccurrence reminderOccurrence2 = new ReminderOccurrence();
        reminderOccurrence2.setOccurrence(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        reminderOccurrence2.setReminder(reminder);
        reminder.setOccurrences(Arrays.asList(reminderOccurrence, reminderOccurrence2));
        reminderRepository.save(reminder);
        this.reminder = reminder;
    }

    @After
    public void tearDown() {
        reminderRepository.deleteAll();
    }

    @Test
    public void findByOccurrence() {

        List<ReminderOccurrence> reminderOccurrences = reminderService.findByOccurrence(LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));
        assertTrue(reminderOccurrences.size() == 2);
    }

    @Test
    public void findByUsername() {
        List<Reminder> reminders = reminderService.findByUsername("@foo");
        assertTrue(reminders.size() == 1);
    }

    @Test
    public void schedule() throws Exception {
        ParsedRequest parsedRequest = new ParsedRequest();
        parsedRequest.setWhen("on 12/18");
        parsedRequest.setTarget("@scottd");
        parsedRequest.setMessage("Super doo");
        String userName = "scottd";
        Reminder reminder = reminderService.schedule(userName, parsedRequest);

        assertEquals(reminder.getUserName(), userName);

        assertEquals(reminder.getMessage(), "Super doo");

        assertEquals(reminder.getTarget(), "@scottd");

        assertTrue(reminder.getOccurrences().size() == 1);

    }

    @Test
    public void delete() {

        reminderService.delete(reminder);

        List<Reminder> reminders = reminderRepository.findAll();

        assertTrue(reminders.size() == 0);
    }

    @Test
    public void complete() {

        reminderService.complete(reminder);

        Reminder reminder1 = reminderRepository.findById(reminder.getId()).orElse(null);

        assertNotNull(reminder1);

        assertTrue(reminder1.isComplete());
    }

    @Test
    @Transactional
    public void snooze() {

        LocalDateTime testTime = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);
        reminderService.snooze(reminder, testTime);

        List<ReminderOccurrence> reminderOccurrence = reminderOccurrenceRepository.findAllByOccurrence(testTime);

        assertTrue(reminderOccurrence.size() == 1);

        assertEquals(reminderOccurrence.get(0).getOccurrence(), testTime);

    }

    @Test
    @Transactional
    public void reschedule() throws Exception {

        ReminderOccurrence reminderOccurrence = new ReminderOccurrence();
        reminderOccurrence.setOccurrence(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).minusWeeks(1l).atStartOfDay().truncatedTo(ChronoUnit.SECONDS));
        reminderOccurrence.setReminder(reminder);
        reminderOccurrence.setRepeat("every wednesday");
        LocalDateTime testTime = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).atTime(9, 0).truncatedTo(ChronoUnit.SECONDS);
        reminderService.reschedule(reminderOccurrence);
        assertEquals(reminderOccurrence.getOccurrence(), testTime);

    }

    @Test
    @Transactional
    public void rescheduleEveryOther() throws Exception {

        ReminderOccurrence reminderOccurrence = new ReminderOccurrence();
        reminderOccurrence.setOccurrence(LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).minusWeeks(1l).atStartOfDay().truncatedTo(ChronoUnit.SECONDS));
        reminderOccurrence.setReminder(reminder);
        reminderOccurrence.setRepeat("every other wednesday");
        LocalDateTime testTime = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.WEDNESDAY)).atTime(9, 0).truncatedTo(ChronoUnit.SECONDS);
        reminderService.reschedule(reminderOccurrence);
        assertEquals(reminderOccurrence.getOccurrence(), testTime);

    }

    @Test
    @Transactional
    public void rescheduleEveryYear() throws Exception {

        ReminderOccurrence reminderOccurrence = new ReminderOccurrence();
        reminderOccurrence.setOccurrence(LocalDate.now().withMonth(1).withDayOfMonth(18).atStartOfDay().truncatedTo(ChronoUnit.SECONDS));
        reminderOccurrence.setReminder(reminder);
        reminderOccurrence.setRepeat("every 1/18");
        LocalDateTime testTime = LocalDate.now().withMonth(1).withDayOfMonth(18).plusYears(1L).atTime(9, 0).truncatedTo(ChronoUnit.SECONDS);
        reminderService.reschedule(reminderOccurrence);
        assertEquals(reminderOccurrence.getOccurrence(), testTime);

    }
}