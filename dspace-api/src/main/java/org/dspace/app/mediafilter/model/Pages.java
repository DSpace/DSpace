/**
 /**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.mediafilter.model;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

@JacksonXmlRootElement(localName = "pages")
public class Pages {

    @JacksonXmlProperty(localName = "page")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<Page> pageList;

    public Pages() {}
    public Pages(final List<Page> pageList) {
        this.pageList = pageList;
    }

    public List<Page> getPageList() {
        return pageList;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Pages pages = (Pages) o;
        return Objects.equals(pageList, pages.pageList);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageList);
    }
}