/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson.dto;

import org.dspace.eperson.RegistrationData;

/**
 * @author Vincenzo Mecca (vins01-4science - vincenzo.mecca at 4science.com)
 **/
public class RegistrationDataPatch {

    private final RegistrationData oldRegistration;
    private final RegistrationDataChanges changes;

    public RegistrationDataPatch(RegistrationData oldRegistration, RegistrationDataChanges changes) {
        this.oldRegistration = oldRegistration;
        this.changes = changes;
    }

    public RegistrationData getOldRegistration() {
        return oldRegistration;
    }

    public RegistrationDataChanges getChanges() {
        return changes;
    }
}
