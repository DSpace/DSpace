/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks.virtualfields;

import static org.dspace.builder.CollectionBuilder.createCollection;
import static org.dspace.builder.CommunityBuilder.createCommunity;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.text.SimpleDateFormat;
import java.util.Date;

import org.dspace.AbstractIntegrationTestWithDatabase;
import org.dspace.builder.ItemBuilder;
import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.utils.DSpace;
import org.junit.Before;
import org.junit.Test;

/**
 * Integration tests for {@link VirtualFieldDateFormatter}.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class VirtualFieldDateFormatterIT extends AbstractIntegrationTestWithDatabase {

    private VirtualFieldDateFormatter virtualField;

    private Collection collection;

    @Before
    public void setup() {

        virtualField = new DSpace().getServiceManager().getServiceByName("virtualFieldDateFormatter",
            VirtualFieldDateFormatter.class);

        context.setCurrentUser(admin);
        parentCommunity = createCommunity(context).build();
        collection = createCollection(context, parentCommunity).build();
    }

    @Test
    public void testWithDateAndTimeFormat() {

        Item item = ItemBuilder.createItem(context, collection)
            .withIssueDate("2021-02-13T12:36:50Z")
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.dc-date-issued.dd MM yyyy HH:mm");
        assertThat(dates.length, is(1));
        assertThat(dates[0], is("13 02 2021 12:36"));

    }

    @Test
    public void testWithDateAndTimeFormatWithoutTime() {

        Item item = ItemBuilder.createItem(context, collection)
            .withIssueDate("2021-02-13")
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.dc-date-issued.dd MM yyyy HH:mm");
        assertThat(dates.length, is(1));
        assertThat(dates[0], is("13 02 2021 00:00"));

    }

    @Test
    public void testWithOnlyDateFormat() {

        Item item = ItemBuilder.createItem(context, collection)
            .withIssueDate("2021-02-13T12:36:50Z")
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.dc-date-issued.dd.MM.yyyy");
        assertThat(dates.length, is(1));
        assertThat(dates[0], is("13.02.2021"));

    }

    @Test
    public void testWithCurrentTimestamp() {

        Item item = ItemBuilder.createItem(context, collection)
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.timestamp.dd.MM.yyyy");
        assertThat(dates.length, is(1));
        assertThat(dates[0], is(getCurrentDate("dd.MM.yyyy")));

        dates = virtualField.getMetadata(context, item, "virtual.date.timestamp.yyyy");
        assertThat(dates.length, is(1));
        assertThat(dates[0], is(getCurrentDate("yyyy")));

        dates = virtualField.getMetadata(context, item, "virtual.date.timestamp.yyyy-MM-dd'T'HH:mm:ss'Z'");
        assertThat(dates.length, is(1));
        assertThat(dates[0], is(getCurrentDate("yyyy-MM-dd'T'HH:mm:ss'Z'")));

    }

    @Test
    public void testWithRepetableDate() {

        Item item = ItemBuilder.createItem(context, collection)
            .withIssueDate("2021-02-13T12:36:50Z")
            .withIssueDate("2022-05-12")
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.dc-date-issued.yyyy/MM/dd");
        assertThat(dates.length, is(2));
        assertThat(dates[0], is("2021/02/13"));
        assertThat(dates[1], is("2022/05/12"));

    }

    @Test
    public void testWithInvalidPattern() {

        Item item = ItemBuilder.createItem(context, collection)
            .withIssueDate("2021-02-13T12:36:50Z")
            .withIssueDate("2022-05-12")
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.dc-date-issued.abcde");
        assertThat(dates.length, is(0));

    }

    @Test
    public void testInvalidDate() {

        Item item = ItemBuilder.createItem(context, collection)
            .withIssueDate("invalid-date")
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.dc-date-issued.yyyy/MM/dd");
        assertThat(dates.length, is(0));

    }

    @Test
    public void testWithoutDate() {

        Item item = ItemBuilder.createItem(context, collection)
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.dc-date-issued.yyyy/MM/dd");
        assertThat(dates.length, is(0));

    }

    @Test
    public void testWithInvalidVirtualField() {

        Item item = ItemBuilder.createItem(context, collection)
            .build();

        String[] dates = virtualField.getMetadata(context, item, "virtual.date.dc-date-issued");
        assertThat(dates.length, is(0));

    }

    private String getCurrentDate(String pattern) {
        return new SimpleDateFormat(pattern).format(new Date());
    }
}
