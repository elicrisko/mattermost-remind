package io.github.scottleedavis.mattermost.remind.reminders;

import io.github.scottleedavis.mattermost.remind.messages.Action;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OptionsTests {

    @Autowired
    private Options options;

    @Before
    public void setUp() {
        options.setAppUrl("http://foo/");
    }

    @Test
    public void setActions() {
        List<Action> actions = options.setActions(1L);
        actions.stream().forEach(action -> assertNotNull(action));
    }

    @Test
    public void listReminders() {
        String response = options.listReminders("FOO");
        assertEquals(response, "I cannot find any reminders for you. Type `/remind` to set one.");
    }
}
