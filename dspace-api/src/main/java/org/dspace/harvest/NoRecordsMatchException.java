/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.harvest;

/**
 * Extension of {@link HarvestingException} to be thrown when no records match
 * the given harvest query.
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class NoRecordsMatchException extends HarvestingException {

    private static final long serialVersionUID = 3119283898630576664L;

    public NoRecordsMatchException() {
        super();
    }

    public NoRecordsMatchException(String message) {
        super(message);
    }
}
