/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.model;

/**
 * Filtered item REST resource used by the Filtered Items report.
 * It adds an item's owning collection to the existing ItemRest class
 * (provided for free in the internal Item model class).
 *
 * @author Jean-François Morin (Université Laval)
 */
public class FilteredItemRest extends ItemRest {

    private static final long serialVersionUID = -1226709195303007186L;

    private CollectionRest owningCollection;

    public CollectionRest getOwningCollection() {
        return owningCollection;
    }

    public void setOwningCollection(CollectionRest owningCollection) {
        this.owningCollection = owningCollection;
    }

}
