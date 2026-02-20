/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authorize;

import java.util.UUID;

/**
 * VO that model an owner of a resource policy (eperson or group).
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class ResourcePolicyOwnerVO {

    private final UUID ePersonId;

    private final UUID groupId;

    public ResourcePolicyOwnerVO(UUID ePersonId, UUID groupId) {
        this.ePersonId = ePersonId;
        this.groupId = groupId;
    }

    public UUID getEPersonId() {
        return ePersonId;
    }

    public UUID getGroupId() {
        return groupId;
    }

}
