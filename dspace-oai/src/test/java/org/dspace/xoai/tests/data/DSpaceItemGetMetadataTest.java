/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.tests.data;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.lyncode.xoai.dataprovider.core.ItemMetadata;
import com.lyncode.xoai.dataprovider.core.ReferenceSet;
import com.lyncode.xoai.dataprovider.xml.xoai.Element;
import com.lyncode.xoai.dataprovider.xml.xoai.Metadata;
import org.dspace.xoai.data.DSpaceItem;
import org.junit.Test;
public class DSpaceItemGetMetadataTest {

    private static Element.Field value(String v) {
        Element.Field f = new Element.Field();
        f.setName("value");
        f.setValue(v);
        return f;
    }

    private static Element named(String name) {
        Element e = new Element();
        e.setName(name);
        return e;
    }

    private static Metadata build(boolean unqualifiedFirst) {
        Element noneUnq = named("none");
        noneUnq.getField().add(value("article"));         // type -> none -> value=article

        Element noneQ = named("none");
        noneQ.getField().add(value("foo"));
        Element something = named("something");
        something.getElement().add(noneQ);                // type -> something -> none -> value=foo

        Element type = named("type");
        if (unqualifiedFirst) {
            type.getElement().add(noneUnq);
            type.getElement().add(something);
        } else {
            type.getElement().add(something);
            type.getElement().add(noneUnq);
        }
        Element dc = named("dc");
        dc.getElement().add(type);
        Metadata md = new Metadata();
        md.getElement().add(dc);
        return md;
    }

    private static class TestItem extends DSpaceItem {
        private final ItemMetadata im;
        TestItem(Metadata m) {
            im = new ItemMetadata(m);
        }
        @Override public ItemMetadata getMetadata() {
            return im;
        }
        @Override protected String getHandle() {
            return "123/1";
        }
        @Override public Date getDatestamp() {
            return new Date();
        }
        @Override public List<ReferenceSet> getSets() {
            return new ArrayList<>();
        }
        @Override public boolean isDeleted() {
            return false;
        }
    }

    @Test
    public void dcTypeReturnsUnqualifiedIfItsFirst() {
        assertEquals(List.of("article"), new TestItem(build(true)).getMetadata("dc.type"));
    }
    // this is the fixed case
    @Test
    public void dcTypeReturnsUnqualifiedIfItsNotFirst() {
        assertEquals(List.of("article"), new TestItem(build(false)).getMetadata("dc.type"));
    }
    @Test
    public void qualifiedLookup() {
        assertEquals(List.of("foo"), new TestItem(build(true)).getMetadata("dc.type.something"));
    }

}