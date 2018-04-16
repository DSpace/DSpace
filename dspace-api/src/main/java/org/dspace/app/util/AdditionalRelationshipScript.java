/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.util;

import java.sql.SQLException;

import org.apache.commons.cli.ParseException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Item;
import org.dspace.content.Relationship;
import org.dspace.content.RelationshipType;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.EntityTypeService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.RelationshipService;
import org.dspace.content.service.RelationshipTypeService;
import org.dspace.core.Context;

public class AdditionalRelationshipScript {

    private RelationshipTypeService relationshipTypeService;
    private RelationshipService relationshipService;
    private EntityTypeService entityTypeService;
    private ItemService itemService;

    private AdditionalRelationshipScript() {
        relationshipTypeService = ContentServiceFactory.getInstance().getRelationshipTypeService();
        relationshipService = ContentServiceFactory.getInstance().getRelationshipService();
        entityTypeService = ContentServiceFactory.getInstance().getEntityTypeService();
        itemService = ContentServiceFactory.getInstance().getItemService();
    }

    public static void main(String[] argv) throws SQLException, AuthorizeException, ParseException {
        AdditionalRelationshipScript additionalRelationshipScript = new AdditionalRelationshipScript();
        additionalRelationshipScript.execute();
    }

    private void execute() throws SQLException, AuthorizeException {
        Context context = new Context();
        context.turnOffAuthorisationSystem();

        //(left label) -> isAuthorOfPublication => Publication is leftItem

        Item article1 = itemService.findByIdOrLegacyId(context, "e98b0f27-5c19-49a0-960d-eb6ad5287067");
        Item article2 = itemService.findByIdOrLegacyId(context, "96715576-3748-4761-ad45-001646632963");
        Item article3 = itemService.findByIdOrLegacyId(context, "047556d1-3d01-4c53-bc68-0cee7ad7ed4e");
        Item article4 = itemService.findByIdOrLegacyId(context, "2f4ec582-109e-4952-a94a-b7d7615a8c69");
        Item article5 = itemService.findByIdOrLegacyId(context, "99c2e55c-6326-4442-9f36-fcac333b0e8c");
        Item article6 = itemService.findByIdOrLegacyId(context, "e7bd0d24-e83a-486a-bc0c-8aaaeb19dc7d");
        Item article7 = itemService.findByIdOrLegacyId(context, "72635f7f-37b5-4875-b4f2-5ff45d97a09b");
        Item article8 = itemService.findByIdOrLegacyId(context, "674f695e-8001-4150-8f9c-095c536a6bcb");
        Item article9 = itemService.findByIdOrLegacyId(context, "a64719f8-ba7b-41d1-8eb6-f8feb0c000b7");

        Item author1 = itemService.findByIdOrLegacyId(context, "0ffbee3f-e7ea-42bc-92fe-2fbef1a52c0f");
        Item author2 = itemService.findByIdOrLegacyId(context, "5a3f7c7a-d3df-419c-b8a2-f00ede62c60a");
        Item author3 = itemService.findByIdOrLegacyId(context, "f2235aa6-6fe7-4174-a690-598b72dd8e44");

        Item orgUnit1 = itemService.findByIdOrLegacyId(context, "d30de96b-1e76-40ae-8ef9-ab426b6f9763");
        Item orgUnit2 = itemService.findByIdOrLegacyId(context, "506a7e54-8d7c-4d5b-8636-d5f6411483de");
        Item orgUnit3 = itemService.findByIdOrLegacyId(context, "c216201f-ed10-4361-b0e0-5a065405bd3e");

        Item project1 = itemService.findByIdOrLegacyId(context, "0de99067-c898-4d02-a82c-9555f3311288");
        Item project2 = itemService.findByIdOrLegacyId(context, "b1bc3a49-49b1-417a-ac90-8d5c7ba5e0ac");
        Item project3 = itemService.findByIdOrLegacyId(context, "18e7924c-f15b-4953-9fe3-3de370bccc97");

        Item journal1 = itemService.findByIdOrLegacyId(context, "d4af6c3e-53d0-4757-81eb-566f3b45d63a");
        Item journal2 = itemService.findByIdOrLegacyId(context, "a23eae5a-7857-4ef9-8e52-989436ad2955");

        Item journalVolume1OfJournal1 = itemService.findByIdOrLegacyId(context, "07c6249f-4bf7-494d-9ce3-6ffdb2aed538");
        Item journalVolume2OfJournal1 = itemService.findByIdOrLegacyId(context, "66bb4e5d-b419-42b7-a648-f270a527f17c");

        Item journalVolume1OfJournal2 = itemService.findByIdOrLegacyId(context, "f9b89a11-b44e-4a64-a3b4-ab24a33553c7");
        Item journalVolume2OfJournal2 = itemService.findByIdOrLegacyId(context, "343d3263-2733-4367-9dc4-216a01b4a461");

        Item journalIssue1OfJournalVolume1OfJournal1 = itemService.findByIdOrLegacyId
            (context, "44c29473-5de2-48fa-b005-e5029aa1a50b");
        Item journalIssue2OfJournalVolume1OfJournal1 = itemService.findByIdOrLegacyId
            (context, "c3076837-e5df-4221-80bc-2661cd390a7b");
        Item journalIssue1OfJournalVolume2OfJournal1 = itemService.findByIdOrLegacyId
            (context, "a4a63ab5-8c0b-4456-b5f7-5b5d9828cb69");

        Item journalIssue1OfJournalVolume1OfJournal2 = itemService.findByIdOrLegacyId
            (context, "77877343-3f75-4c33-9492-6ed7c98ed84e");
        Item journalIssue2OfJournalVolume1OfJournal2 = itemService.findByIdOrLegacyId
            (context, "f4dcd8a6-4cc4-4806-8bb9-a7e8202e05b0");
        Item journalIssue1OfJournalVolume2OfJournal2 = itemService.findByIdOrLegacyId
            (context, "b7003f66-80e9-4c98-99a2-3695e8150b80");
        Item journalIssue2OfJournalVolume2OfJournal2 = itemService.findByIdOrLegacyId
            (context, "db55298c-a21f-4677-8793-a21f1194a226");

        RelationshipType isAuthorOfPublication = relationshipTypeService.find(context, 1);
        RelationshipType isProjectOfPublication = relationshipTypeService.find(context, 2);
        RelationshipType isOrgUnitOfPublication = relationshipTypeService.find(context, 3);
        RelationshipType isProjectOfPerson = relationshipTypeService.find(context, 4);
        RelationshipType isOrgUnitOfPerson = relationshipTypeService.find(context, 5);
        RelationshipType isOrgUnitOfProject = relationshipTypeService.find(context, 6);
        RelationshipType isVolumeOfJournal = relationshipTypeService.find(context, 7);
        RelationshipType isIssueOfJournalVolume = relationshipTypeService.find(context, 8);
        RelationshipType isPublicationOfJournalIssue = relationshipTypeService.find(context, 9);

        RelationshipType isAuthorOfPublicationNew = relationshipTypeService.find(context, 10);
        constructRelationshipAndStore(context, article1, orgUnit1, isAuthorOfPublicationNew, 1);
        constructRelationshipAndStore(context, article1, orgUnit2, isAuthorOfPublicationNew, 1);
        constructRelationshipAndStore(context, article1, orgUnit3, isAuthorOfPublicationNew, 1);

        constructRelationshipAndStore(context, article1, author1, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article1, author2, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article1, author3, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article2, author1, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article2, author3, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article3, author3, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article4, author3, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article4, author2, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article4, author1, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article5, author1, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article6, author2, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article6, author3, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article7, author3, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article7, author2, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article7, author1, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article8, author2, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article9, author3, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article9, author1, isAuthorOfPublication, 1);

        constructRelationshipAndStore(context, article1, project1, isProjectOfPublication, 1);
        constructRelationshipAndStore(context, article6, project1, isProjectOfPublication, 1);
        constructRelationshipAndStore(context, article7, project1, isProjectOfPublication, 1);
        constructRelationshipAndStore(context, article1, project2, isProjectOfPublication, 1);
        constructRelationshipAndStore(context, article9, project3, isProjectOfPublication, 1);
        constructRelationshipAndStore(context, article8, project3, isProjectOfPublication, 1);
        constructRelationshipAndStore(context, article4, project3, isProjectOfPublication, 1);
        constructRelationshipAndStore(context, article5, project3, isProjectOfPublication, 1);
        constructRelationshipAndStore(context, article2, project3, isProjectOfPublication, 1);

        constructRelationshipAndStore(context, article1, orgUnit1, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article1, orgUnit2, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article1, orgUnit3, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article2, orgUnit1, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article2, orgUnit3, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article3, orgUnit3, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article4, orgUnit3, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article4, orgUnit2, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article4, orgUnit1, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article5, orgUnit1, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article6, orgUnit2, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article6, orgUnit3, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article7, orgUnit3, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article7, orgUnit2, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article7, orgUnit1, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article8, orgUnit2, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article9, orgUnit3, isOrgUnitOfPublication, 1);
        constructRelationshipAndStore(context, article9, orgUnit1, isOrgUnitOfPublication, 1);

        constructRelationshipAndStore(context, project1, orgUnit1, isOrgUnitOfProject, 1);
        constructRelationshipAndStore(context, project2, orgUnit2, isOrgUnitOfProject, 1);
        constructRelationshipAndStore(context, project3, orgUnit2, isOrgUnitOfProject, 1);
        constructRelationshipAndStore(context, project2, orgUnit3, isOrgUnitOfProject, 1);
        constructRelationshipAndStore(context, project1, orgUnit3, isOrgUnitOfProject, 1);
        constructRelationshipAndStore(context, project3, orgUnit3, isOrgUnitOfProject, 1);


        constructRelationshipAndStore(context, author1, project1, isProjectOfPerson, 1);
        constructRelationshipAndStore(context, author2, project2, isProjectOfPerson, 1);
        constructRelationshipAndStore(context, author2, project3, isProjectOfPerson, 1);
        constructRelationshipAndStore(context, author3, project1, isProjectOfPerson, 1);
        constructRelationshipAndStore(context, author3, project2, isProjectOfPerson, 1);
        constructRelationshipAndStore(context, author3, project3, isProjectOfPerson, 1);

        constructRelationshipAndStore(context, author1,orgUnit1 ,isOrgUnitOfPerson, 1);
        constructRelationshipAndStore(context, author2,orgUnit2 ,isOrgUnitOfPerson, 1);
        constructRelationshipAndStore(context, author2,orgUnit3 ,isOrgUnitOfPerson, 1);
        constructRelationshipAndStore(context, author3,orgUnit1 ,isOrgUnitOfPerson, 1);
        constructRelationshipAndStore(context, author3,orgUnit2 ,isOrgUnitOfPerson, 1);
        constructRelationshipAndStore(context, author3,orgUnit3 ,isOrgUnitOfPerson, 1);

        constructRelationshipAndStore(context, journal1, journalVolume1OfJournal1, isVolumeOfJournal, 1);
        constructRelationshipAndStore(context, journal1, journalVolume2OfJournal1, isVolumeOfJournal, 1);

        constructRelationshipAndStore(context, journal2, journalVolume1OfJournal2, isVolumeOfJournal, 1);
        constructRelationshipAndStore(context, journal2, journalVolume2OfJournal2, isVolumeOfJournal, 1);


        constructRelationshipAndStore(context, journalVolume1OfJournal1,
                                      journalIssue1OfJournalVolume1OfJournal1, isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal1,
                                      journalIssue2OfJournalVolume1OfJournal1, isIssueOfJournalVolume, 1);

        constructRelationshipAndStore(context, journalVolume2OfJournal1,
                                      journalIssue1OfJournalVolume2OfJournal1, isIssueOfJournalVolume, 1);

        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue1OfJournalVolume1OfJournal2,isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue2OfJournalVolume1OfJournal2,isIssueOfJournalVolume, 1);

        constructRelationshipAndStore(context, journalVolume2OfJournal2,
                                      journalIssue1OfJournalVolume2OfJournal2,isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume2OfJournal2,
                                      journalIssue2OfJournalVolume2OfJournal2,isIssueOfJournalVolume, 1);


        Item journalIssue3 = itemService.findByIdOrLegacyId(context, "522eea91-096f-4a30-bd00-731b2dde33f3");

        constructRelationshipAndStore(context, journalIssue3,
                                      itemService.findByIdOrLegacyId(context, "434d9911-fc4e-4d8f-98a1-0343a84b637a"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue3,
                                      itemService.findByIdOrLegacyId(context, "e9aaa1de-5927-4ced-af36-bc6750ff33af"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue4 = itemService.findByIdOrLegacyId(context, "df376455-2790-434a-9957-5e1ba740fff9");

        constructRelationshipAndStore(context, journalIssue4,
                                      itemService.findByIdOrLegacyId(context, "10bc6f8b-0796-486f-94d8-4d2e1814586f"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue4,
                                      itemService.findByIdOrLegacyId(context, "75c0f7f5-5a69-40e8-aa1f-8f35b1ce5a63"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue5 = itemService.findByIdOrLegacyId(context, "9c75d673-412a-49ac-a58d-64df4fa1463a");

        constructRelationshipAndStore(context, journalIssue5,
                                      itemService.findByIdOrLegacyId(context, "5e945357-995a-424a-9ce8-bf3296120e30"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue5,
                                      itemService.findByIdOrLegacyId(context, "87123332-a615-427e-92bc-885f31f161a5"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue6 = itemService.findByIdOrLegacyId(context, "d5944ff4-3dc1-407a-b49a-1a2681d75267");

        constructRelationshipAndStore(context, journalIssue6,
                                      itemService.findByIdOrLegacyId(context, "72ec3575-d9b5-46ab-8a1b-698dd6d73262"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue6,
                                      itemService.findByIdOrLegacyId(context, "5ee81de6-aaac-409d-986f-bf21a7fb543c"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue7 = itemService.findByIdOrLegacyId(context, "4a45cacd-9653-437b-92cf-5824b0461f84");

        constructRelationshipAndStore(context, journalIssue7,
                                      itemService.findByIdOrLegacyId(context, "5f414d41-06ab-419a-b3c6-7e85f8480b31"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue7,
                                      itemService.findByIdOrLegacyId(context, "6e9d30d4-6b1c-4a45-aadc-c81d88444c1a"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue8 = itemService.findByIdOrLegacyId(context, "7bba8606-37a1-4a78-b544-b3f1b7c79cb7");

        constructRelationshipAndStore(context, journalIssue8,
                                      itemService.findByIdOrLegacyId(context, "c5f0f7b6-7ba5-4d4f-94a3-c0d53b0cb62d"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue8,
                                      itemService.findByIdOrLegacyId(context, "2e241414-dfea-4452-b2c4-89f51d89e0b2"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue9 = itemService.findByIdOrLegacyId(context, "c136663a-9925-453c-9aae-8cb8ed5e9e4b");

        constructRelationshipAndStore(context, journalIssue9,
                                      itemService.findByIdOrLegacyId(context, "d2dd2905-b178-45df-a5e1-8e2510bd33f8"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue9,
                                      itemService.findByIdOrLegacyId(context, "17101e66-686b-42d5-b3eb-17488f8e3a9c"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue10 = itemService.findByIdOrLegacyId(context, "09f27238-8374-4da8-b442-295fd1c5fef7");

        constructRelationshipAndStore(context, journalIssue10,
                                      itemService.findByIdOrLegacyId(context, "249b1235-2793-4ad2-b40a-1745bcac7d52"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue10,
                                      itemService.findByIdOrLegacyId(context, "dc1e6daa-8aa5-4b10-b70d-517792ab9801"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue11 = itemService.findByIdOrLegacyId(context, "f88bc89e-dcfa-4eca-8db6-1ef808c30564");

        constructRelationshipAndStore(context, journalIssue11,
                                      itemService.findByIdOrLegacyId(context, "786df549-c1c3-4818-8fee-10f07858b41f"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue11,
                                      itemService.findByIdOrLegacyId(context, "2a351411-1614-4bfa-af50-752af07acf1c"),
                                      isPublicationOfJournalIssue, 1);

        Item journalIssue12 = itemService.findByIdOrLegacyId(context, "961e137c-d815-4ade-aff1-0bb12f1fe965");

        constructRelationshipAndStore(context, journalIssue12,
                                      itemService.findByIdOrLegacyId(context, "19b25bea-59e6-4c3c-a461-19a2c18ec602"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue12,
                                      itemService.findByIdOrLegacyId(context, "dde41d4d-55cd-4107-9d91-4407cdb441c1"),
                                      isPublicationOfJournalIssue, 1);


        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue3,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue4,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue5,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue6,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue7,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue8,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue9,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue10,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue11,
                                      isIssueOfJournalVolume, 1);
        constructRelationshipAndStore(context, journalVolume1OfJournal2,
                                      journalIssue12,
                                      isIssueOfJournalVolume, 1);


        constructRelationshipAndStore(context, journalIssue1OfJournalVolume1OfJournal1,
                                      article1 ,isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue2OfJournalVolume1OfJournal1,
                                      article2 ,isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context, journalIssue1OfJournalVolume2OfJournal1,
                                      article3 ,isPublicationOfJournalIssue, 1);


        constructRelationshipAndStore(context, article8, author1, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article8, orgUnit1, isAuthorOfPublicationNew, 1);
        constructRelationshipAndStore(context, article8, author3, isAuthorOfPublication, 1);
        constructRelationshipAndStore(context, article8, orgUnit2, isAuthorOfPublicationNew, 1);

        constructRelationshipAndStore(context,
                                      itemService.findByIdOrLegacyId(context, "77877343-3f75-4c33-9492-6ed7c98ed84e"),
                                      itemService.findByIdOrLegacyId(context, "32513cea-6b89-4bce-87c0-214ad118af3e"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context,
                                      itemService.findByIdOrLegacyId(context, "77877343-3f75-4c33-9492-6ed7c98ed84e"),
                                      itemService.findByIdOrLegacyId(context, "d10c32e6-2b14-41f7-be3e-46a41bbe1cb4"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context,
                                      itemService.findByIdOrLegacyId(context, "f4dcd8a6-4cc4-4806-8bb9-a7e8202e05b0"),
                                      itemService.findByIdOrLegacyId(context, "6a85aea7-a78b-43be-a2b6-c7948f4e12b7"),
                                      isPublicationOfJournalIssue, 1);
        constructRelationshipAndStore(context,
                                      itemService.findByIdOrLegacyId(context, "f4dcd8a6-4cc4-4806-8bb9-a7e8202e05b0"),
                                      itemService.findByIdOrLegacyId(context, "56f39a97-2f57-48cc-867d-8fa58804793a"),
                                      isPublicationOfJournalIssue, 1);

        context.complete();
    }

    private void constructRelationshipAndStore(Context context, Item leftItem,
                                               Item rightItem, RelationshipType relationshipType, int place)
        throws SQLException, AuthorizeException {
        Relationship relationship = new Relationship();
        relationship.setLeftItem(leftItem);
        relationship.setRightItem(rightItem);
        relationship.setRelationshipType(relationshipType);
        relationship.setLeftPlace(place);
        relationship.setRightPlace(place);
        relationshipService.create(context, relationship);
    }


}
