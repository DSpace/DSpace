/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.shell.config;

import javax.sql.DataSource;

import org.dspace.core.Context;
import org.dspace.kernel.ServiceManager;
//import org.dspace.servicemanager.DSpaceKernelImpl;
//import org.dspace.servicemanager.DSpaceKernelInit;
import org.dspace.services.ConfigurationService;
import org.flywaydb.core.Flyway;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/**
 * Test configuration for Spring Shell.
 *
 * This class provides mocks of DSpace services to allow
 * Shell integration tests to run without a real
 * DSpace instance.
 *
 * Use with the "test" or "shell-test" profile.
 */
@TestConfiguration
@Profile({"test", "shell-test"})
public class TestShellConfiguration {

    /**
     * DataSource Mock for testing.
     */
    @Bean
    @Primary
    public DataSource testDataSource() {
        // Opção 1: Mock simples
        //return Mockito.mock(DataSource.class);

        // Opção 2: H2 embedded (descomentado se necessário)

        return new EmbeddedDatabaseBuilder()
            .setType(EmbeddedDatabaseType.H2)
            .setName("testdb;MODE=PostgreSQL")
            .build();
        
    }

    /**
     * Flyway Mock for testing.
     */
    @Bean
    @Primary
    public Flyway testFlyway(DataSource dataSource) {
        Flyway flyway = Mockito.mock(Flyway.class);

        // Default behavior configuration
        org.flywaydb.core.api.MigrationInfoService infoService =
            Mockito.mock(org.flywaydb.core.api.MigrationInfoService.class);
        Mockito.when(flyway.info()).thenReturn(infoService);

        return flyway;
    }

    /**
     * ConfigurationService Mock for DSpace.
     */
    @Bean
    @Primary
    public ConfigurationService testConfigurationService() {
        ConfigurationService configService = Mockito.mock(ConfigurationService.class);

        // Default configurations for testing
        Mockito.when(configService.getProperty("dspace.dir"))
            .thenReturn("/tmp/dspace-test");
        Mockito.when(configService.getProperty("db.driver"))
            .thenReturn("org.h2.Driver");
        Mockito.when(configService.getProperty("db.url"))
            .thenReturn("jdbc:h2:mem:testdb;MODE=PostgreSQL");

        return configService;
    }

    /**
     * Mock do ServiceManager do DSpace.
     */
    @Bean
    @Primary
    public ServiceManager testServiceManager(DataSource dataSource,
                                              ConfigurationService configService) {
        ServiceManager serviceManager = Mockito.mock(ServiceManager.class);

        Mockito.when(serviceManager.getServiceByName("javax.sql.DataSource", DataSource.class))
            .thenReturn(dataSource);
        Mockito.when(serviceManager.getServiceByName(
            "org.dspace.services.ConfigurationService", ConfigurationService.class))
            .thenReturn(configService);

        return serviceManager;
    }

    /**
     * Factory for mocking Context for testing.
     */
    @Bean
    public TestContextFactory testContextFactory() {
        return new TestContextFactory();
    }

    /**
     * Factory class for Context instances for testing.
     */
    public static class TestContextFactory {

        /**
         * Creates a basic mocking Context.
         */
        public Context createMockContext() {
            Context context = Mockito.mock(Context.class);
            Mockito.when(context.isValid()).thenReturn(true);
            return context;
        }

        /**
         * Creates a mocking Context with an authenticated user.
         */
        public Context createAuthenticatedContext(String email) {
            Context context = createMockContext();

            org.dspace.eperson.EPerson eperson = Mockito.mock(org.dspace.eperson.EPerson.class);
            Mockito.when(eperson.getEmail()).thenReturn(email);
            Mockito.when(context.getCurrentUser()).thenReturn(eperson);

            return context;
        }

        /**
         * Creates a mocking Context with admin permissions.
         */
        public Context createAdminContext() {
            Context context = createAuthenticatedContext("admin@dspace.org");

            try {
                // Simulate admin permissions
                Mockito.when(context.getCurrentUser().getID())
                    .thenReturn(java.util.UUID.randomUUID());
            } catch (Exception e) {
                // Ignore in mocking context
            }

            return context;
        }
    }
}
