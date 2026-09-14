/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.layout.service.impl;

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.layout.DynamicLayoutBox;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.LayoutSecurityService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * Unit tests for {@link DynamicLayoutBoxAccessServiceImpl}, verifying that an
 * unknown/invalid box security value is denied instead of causing a
 * NullPointerException.
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
@RunWith(MockitoJUnitRunner.class)
public class DynamicLayoutBoxAccessServiceImplTest {

    @Mock
    private LayoutSecurityService layoutSecurityService;

    @Mock
    private Context context;

    @Mock
    private EPerson user;

    @Mock
    private Item item;

    @Test
    public void deniesAccessWhenSecurityValueIsInvalid() throws Exception {
        DynamicLayoutBoxAccessServiceImpl accessService =
            new DynamicLayoutBoxAccessServiceImpl(layoutSecurityService);

        DynamicLayoutBox box = new DynamicLayoutBox();
        // 99 does not map to any LayoutSecurity, so LayoutSecurity.valueOf returns null.
        box.setSecurity(Integer.valueOf(99));

        boolean access = accessService.hasAccess(context, user, box, item);

        assertFalse("Access must be denied for an invalid security value", access);
        verify(layoutSecurityService, never())
            .hasAccess(any(LayoutSecurity.class), eq(context), eq(user), any(), any(), eq(item));
    }

    @Test
    public void deniesAccessWhenSecurityValueIsNull() throws Exception {
        DynamicLayoutBoxAccessServiceImpl accessService =
            new DynamicLayoutBoxAccessServiceImpl(layoutSecurityService);

        DynamicLayoutBox box = new DynamicLayoutBox();
        box.setSecurity((Integer) null);

        boolean access = accessService.hasAccess(context, user, box, item);

        assertFalse("Access must be denied for a null security value", access);
        verify(layoutSecurityService, never())
            .hasAccess(any(LayoutSecurity.class), eq(context), eq(user), any(), any(), eq(item));
    }
}
