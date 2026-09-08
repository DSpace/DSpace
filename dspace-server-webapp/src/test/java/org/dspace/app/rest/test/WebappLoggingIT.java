/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.config.Property;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.After;
import org.junit.Test;

/**
 * Test basic log4j logging functionality, extending AbstractControllerIntegrationTest
 * purely to make sure we are testing the *web application* and not just the kernel
 * as that is where logging has broken in the past.
 *
 * @author Kim Shepherd
 */
public class WebappLoggingIT extends AbstractControllerIntegrationTest {

    private static final Logger logger = LogManager.getLogger(WebappLoggingIT.class);
    private static final String APPENDER_NAME = "DSpaceTestAppender";

    static class InMemoryAppender extends AbstractAppender {
        private final List<String> messages = new ArrayList<>();

        protected InMemoryAppender(String name) {
            super(
                name,
                null,
                PatternLayout.newBuilder().withPattern("%m").build(),
                false,
                Property.EMPTY_ARRAY
            );
            start();
        }

        @Override
        public void append(LogEvent event) {
            messages.add(event.getMessage().getFormattedMessage());
        }

        public List<String> getMessages() {
            return messages;
        }
    }

    @Test
    public void testLogging() throws Exception {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        InMemoryAppender appender = new InMemoryAppender(APPENDER_NAME);
        config.addAppender(appender);

        LoggerConfig testLoggerConfig = new LoggerConfig(logger.getName(), Level.INFO, false);
        testLoggerConfig.addAppender(appender, null, null);
        config.addLogger(logger.getName(), testLoggerConfig);
        context.updateLoggers();

        logger.info("DSPACE TEST LOG ENTRY");

        List<String> messages = appender.getMessages();
        assertTrue(messages.stream().anyMatch(msg -> msg.contains("DSPACE TEST LOG ENTRY")));
    }

    @After
    public void cleanupAppender() {
        LoggerContext context = (LoggerContext) LogManager.getContext(false);
        Configuration config = context.getConfiguration();

        config.removeLogger(logger.getName());

        Appender appender = config.getAppender(APPENDER_NAME);
        if (appender != null) {
            appender.stop();
            config.getAppenders().remove(APPENDER_NAME);
        }

        context.updateLoggers();
}

}

