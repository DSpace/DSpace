/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell;

import static org.assertj.core.api.Assertions.assertThat;

import org.dspace.servicemanager.DSpaceKernelImpl;
import org.dspace.shell.config.TestShellConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

/**
 * Spring context loading tests for the Shell application.
 *
 * Validates that the application starts correctly and that all
 * required components are available in the context.
 */
@SpringBootTest
@ActiveProfiles("test")
@ContextConfiguration(classes = {DSpaceShellApplication.class, TestShellConfiguration.class})
@DisplayName("DSpaceShellApplication - Context tests")
class DSpaceShellApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    @MockBean
    private DSpaceKernelImpl dspaceKernel;

    @Test
    @DisplayName("Application context should load as expected")
    void contextLoads() {
        assertThat(applicationContext).isNotNull();
    }
}
