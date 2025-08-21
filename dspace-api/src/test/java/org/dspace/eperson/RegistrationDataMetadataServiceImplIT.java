/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.MetadataFieldBuilder;
import org.dspace.content.MetadataField;
import org.dspace.eperson.factory.EPersonServiceFactory;
import org.dspace.eperson.service.RegistrationDataMetadataService;
import org.dspace.eperson.service.RegistrationDataService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 */
public class RegistrationDataMetadataServiceImplIT extends AbstractIntegrationTestWithDatabase {

    RegistrationDataMetadataService registrationDataMetadataService =
        EPersonServiceFactory.getInstance().getRegistrationDAtaDataMetadataService();

    RegistrationDataService registrationDataService =
        EPersonServiceFactory.getInstance().getRegistrationDataService();

    MetadataField metadataField;
    RegistrationData registrationData;
    RegistrationDataMetadata metadata;

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();
        this.registrationData =
            this.registrationDataService.create(context);

        this.metadataField =
            MetadataFieldBuilder.createMetadataField(context, "dc", "identifier", "custom")
                                .build();

        context.restoreAuthSystemState();
    }

    @After
    @Override
    public void destroy() throws Exception {
        this.registrationDataService.delete(context, registrationData);
        super.destroy();
    }


    @Test
    public void testEmptyMetadataCreation() throws Exception {
        try {
            metadata = registrationDataMetadataService.create(context, registrationData, metadataField);

            assertThat(metadata, notNullValue());
            assertThat(metadata.getValue(), nullValue());
            assertThat(metadata.getRegistrationData().getID(), is(registrationData.getID()));
            assertThat(metadata.getMetadataField(), is(metadataField));

        } finally {
            registrationDataMetadataService.delete(context, metadata);
        }
    }

    @Test
    public void testValidMetadataCreation() throws Exception {
        try {
            metadata =
                registrationDataMetadataService.create(context, registrationData, metadataField, "my-identifier");

            assertThat(metadata, notNullValue());
            assertThat(metadata.getValue(), is("my-identifier"));
            assertThat(metadata.getRegistrationData().getID(), is(registrationData.getID()));
            assertThat(metadata.getMetadataField(), is(metadataField));

        } finally {
            registrationDataMetadataService.delete(context, metadata);
        }
    }

    @Test
    public void testExistingMetadataFieldMetadataCreation() throws Exception {
        try {
            metadata =
                registrationDataMetadataService.create(
                    context, registrationData, "dc", "identifier", "other", "my-identifier"
                );

            assertThat(metadata, notNullValue());
            assertThat(metadata.getValue(), is("my-identifier"));
            assertThat(metadata.getRegistrationData().getID(), is(registrationData.getID()));

        } finally {
            registrationDataMetadataService.delete(context, metadata);
        }
    }


    @Test
    public void testFindMetadata() throws Exception {
        try {
            metadata = registrationDataMetadataService.create(context, registrationData, metadataField);

            RegistrationDataMetadata found =
                registrationDataMetadataService.find(context, metadata.getID());

            assertThat(found.getID(), is(metadata.getID()));

        } finally {
            registrationDataMetadataService.delete(context, metadata);
        }
    }

    @Test
    public void testUpdateMetadata() throws Exception {
        try {
            metadata = registrationDataMetadataService.create(context, registrationData, metadataField);
            metadata.setValue("custom-value");
            registrationDataMetadataService.update(context, metadata);

            RegistrationDataMetadata found =
                registrationDataMetadataService.find(context, metadata.getID());

            assertThat(found.getID(), is(metadata.getID()));
            assertThat(found.getValue(), is("custom-value"));

        } finally {
            registrationDataMetadataService.delete(context, metadata);
        }
    }

    @Test
    public void testDeleteMetadata() throws Exception {
        try {
            metadata = registrationDataMetadataService.create(context, registrationData, metadataField);

            RegistrationDataMetadata found =
                registrationDataMetadataService.find(context, metadata.getID());

            assertThat(found, notNullValue());

            registrationDataMetadataService.delete(context, metadata);

            found = registrationDataMetadataService.find(context, metadata.getID());

            assertThat(found, nullValue());

        } finally {
            registrationDataMetadataService.delete(context, metadata);
        }
    }

}