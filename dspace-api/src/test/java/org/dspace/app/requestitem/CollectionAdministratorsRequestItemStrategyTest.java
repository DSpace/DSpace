/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.requestitem;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.dspace.content.Collection;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.eperson.Group;
import org.junit.Test;
import org.mockito.Mockito;

/**
 *
 * @author Mark H. Wood <mwood@iupui.edu>
 */
public class CollectionAdministratorsRequestItemStrategyTest {
    private static final String NAME = "John Q. Public";
    private static final String EMAIL = "jqpublic@example.com";

    /**
     * Test of getRequestItemAuthor method, of class CollectionAdministratorsRequestItemStrategy.
     * @throws java.lang.Exception passed through.
     */
    @Test
    public void testGetRequestItemAuthor()
            throws Exception {
        System.out.println("getRequestItemAuthor");

        Context context = Mockito.mock(Context.class);

        EPerson eperson1 = Mockito.mock(EPerson.class);
        Mockito.when(eperson1.getEmail()).thenReturn(EMAIL);
        Mockito.when(eperson1.getFullName()).thenReturn(NAME);

        Group group1 = Mockito.mock(Group.class);
        Mockito.when(group1.getMembers()).thenReturn(List.of(eperson1));

        Collection collection1 = Mockito.mock(Collection.class);
        Mockito.when(collection1.getAdministrators()).thenReturn(group1);

        Item item = Mockito.mock(Item.class);
        Mockito.when(item.getOwningCollection()).thenReturn(collection1);
        Mockito.when(item.getSubmitter()).thenReturn(eperson1);

        CollectionAdministratorsRequestItemStrategy instance = new CollectionAdministratorsRequestItemStrategy();
        List<RequestItemAuthor> result = instance.getRequestItemAuthor(context,
                item);
        assertEquals("Should be one author", 1, result.size());
        assertEquals("Name should match " + NAME, NAME, result.get(0).getFullName());
        assertEquals("Email should match " + EMAIL, EMAIL, result.get(0).getEmail());
    }
}
