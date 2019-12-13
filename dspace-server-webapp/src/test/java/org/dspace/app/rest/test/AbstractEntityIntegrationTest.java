/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.test;

import org.dspace.app.rest.builder.EntityTypeBuilder;
import org.dspace.app.rest.builder.RelationshipTypeBuilder;
import org.dspace.content.EntityType;
import org.dspace.content.service.EntityTypeService;
import org.junit.Before;
import org.springframework.beans.factory.annotation.Autowired;

public class AbstractEntityIntegrationTest extends AbstractControllerIntegrationTest {

    @Autowired
    private EntityTypeService entityTypeService;

    /**
     * This method will call the setUp method from AbstractControllerIntegrationTest.
     * Afterwards it will setUp the entity relation structure as defined in
     * dspace-api/src/test/data/dspaceFolder/config/entities/relationship-types.xml
     *
     * This method will first build the following EntityTypes:
     * - Publication
     * - Person
     * - Project
     * - OrgUnit
     * - Journal
     * - JournalVolume
     * - JournalIssue
     *
     * After the EntityTypes are created, RelationshipTypes are set up between the different EntityTypes as indicated
     * in relationship-types.xml
     */
    @Before
    public void setUp() throws Exception {
        super.setUp();

        if (entityTypeService.findAll(context).size() > 0) {
            //Don't initialize the setup more than once
            return;
        }

        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType project = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnit = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        EntityType journal = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        EntityType journalVolume = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume").build();
        EntityType journalIssue = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();


        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, person, "isAuthorOfPublication",
                                                              "isPublicationOfAuthor", 0, null, 0,
                                                              null).withCopyToLeft(false).withCopyToRight(true).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, project, "isProjectOfPublication",
                                                              "isPublicationOfProject", 0, null, 0,
                                                              null).withCopyToRight(true).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, orgUnit, "isOrgUnitOfPublication",
                                                              "isPublicationOfOrgUnit", 0, null, 0,
                                                              null).withCopyToLeft(false).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, person, project, "isProjectOfPerson",
                                                              "isPersonOfProject", 0, null, 0,
                                                              null).withCopyToLeft(true).withCopyToRight(true).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, person, orgUnit, "isOrgUnitOfPerson",
                                                              "isPersonOfOrgUnit", 0, null, 0,
                                                              null).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, project, orgUnit, "isOrgUnitOfProject",
                                                              "isProjectOfOrgUnit", 0, null, 0,
                                                              null).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journal, journalVolume, "isVolumeOfJournal",
                                                              "isJournalOfVolume", 0, null, 1,
                                                              null).withCopyToLeft(true).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journalVolume, journalIssue,
                                                              "isIssueOfJournalVolume", "isJournalVolumeOfIssue", 0,
                                                              null, 1,
                                                              1).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journalIssue, journalVolume,
                                                                "isJournalVolumeOfIssue", "isIssueOfJournalVolume",
                                                                null, null, null,
                                                                null).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, orgUnit, "isAuthorOfPublication",
                                                              "isPublicationOfAuthor", 0, null, 0,
                                                              null).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journalIssue, publication,
                                                              "isPublicationOfJournalIssue",
                                                              "isJournalIssueOfPublication", 0, null, 0,
                                                              1).build();

        context.restoreAuthSystemState();
    }


}
