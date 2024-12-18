/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.app.mediafilter.model;

import java.util.Objects;

public class Page {

    private int pageNumber;
    private String text;

    public Page(){}
    public Page(int pageNumber, String text) {
        this.pageNumber = pageNumber;
        this.text = text;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public String getText() {
        return text;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final Page page = (Page) o;
        return Objects.equals(pageNumber, page.pageNumber) && Objects.equals(text, page.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pageNumber, text);
    }
}