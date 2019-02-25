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
import org.junit.Before;

public class AbstractEntityIntegrationTest extends AbstractControllerIntegrationTest {


    @Before
    public void setUp() throws Exception {
        super.setUp();

        context.turnOffAuthorisationSystem();

        EntityType publication = EntityTypeBuilder.createEntityTypeBuilder(context, "Publication").build();
        EntityType person = EntityTypeBuilder.createEntityTypeBuilder(context, "Person").build();
        EntityType project = EntityTypeBuilder.createEntityTypeBuilder(context, "Project").build();
        EntityType orgUnit = EntityTypeBuilder.createEntityTypeBuilder(context, "OrgUnit").build();
        EntityType journal = EntityTypeBuilder.createEntityTypeBuilder(context, "Journal").build();
        EntityType journalVolume = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalVolume").build();
        EntityType journalIssue = EntityTypeBuilder.createEntityTypeBuilder(context, "JournalIssue").build();


        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, person, "isAuthorOfPublication",
                                                              "isPublicationOfAuthor", 0, Integer.MAX_VALUE, 0,
                                                              Integer.MAX_VALUE).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, project, "isProjectOfPublication",
                                                              "isPublicationOfProject", 0, Integer.MAX_VALUE, 0,
                                                              Integer.MAX_VALUE).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, orgUnit, "isOrgUnitOfPublication",
                                                              "isPublicationOfOrgUnit", 0, Integer.MAX_VALUE, 0,
                                                              Integer.MAX_VALUE).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, person, project, "isProjectOfPerson",
                                                              "isPersonOfProject", 0, Integer.MAX_VALUE, 0,
                                                              Integer.MAX_VALUE).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, person, orgUnit, "isOrgUnitOfPerson",
                                                              "isPersonOfOrgUnit", 0, Integer.MAX_VALUE, 0,
                                                              Integer.MAX_VALUE).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, project, orgUnit, "isOrgUnitOfProject",
                                                              "isProjectOfOrgUnit", 0, Integer.MAX_VALUE, 0,
                                                              Integer.MAX_VALUE).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journal, journalVolume, "isVolumeOfJournal",
                                                              "isJournalOfVolume", 0, Integer.MAX_VALUE, 1,
                                                              Integer.MAX_VALUE).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journalVolume, journalIssue,
                                                              "isIssueOfJournalVolume", "isJournalVolumeOfIssue", 0,
                                                              Integer.MAX_VALUE, 1,
                                                              1).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, publication, orgUnit, "isAuthorOfPublication",
                                                              "isPublicationOfAuthor", 0, Integer.MAX_VALUE, 0,
                                                              Integer.MAX_VALUE).build();

        RelationshipTypeBuilder.createRelationshipTypeBuilder(context, journalIssue, publication,
                                                              "isPublicationOfJournalIssue",
                                                              "isJournalIssueOfPublication", 0, Integer.MAX_VALUE, 0,
                                                              1).build();
    }


}
