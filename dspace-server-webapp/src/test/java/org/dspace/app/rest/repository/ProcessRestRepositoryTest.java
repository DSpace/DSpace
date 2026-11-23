/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import jakarta.annotation.PostConstruct;
import org.junit.Test;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

/**
 * Unit tests for {@link ProcessRestRepository}.
 */
public class ProcessRestRepositoryTest {

    @Test
    public void testInitRunsAfterApplicationReady() throws Exception {
        Method init = ProcessRestRepository.class.getMethod("init");
        EventListener eventListener = init.getAnnotation(EventListener.class);

        assertNotNull(eventListener);
        assertArrayEquals(new Class<?>[] { ApplicationReadyEvent.class }, eventListener.value());
        assertNull(init.getAnnotation(PostConstruct.class));
    }
}
