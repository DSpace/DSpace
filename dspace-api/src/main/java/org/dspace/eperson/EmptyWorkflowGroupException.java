/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.eperson;

import java.util.UUID;

/**
 * <p>This exception class is used to distinguish the following condition:
 * EPerson cannot be deleted because that would lead to one (or more)
 * workflow groups being empty.</p>
 *
 * <p>The message of this exception can be disclosed in the REST response to
 * provide more granular feedback to the user.</p>
 *
 * @author Bruno Roemers (bruno.roemers at atmire.com)
 */
public class EmptyWorkflowGroupException extends IllegalStateException {

    public static final String msgFmt = "Refused to delete user %s because it is the only member of the " +
        "workflow group %s. Delete the tasks and group first if you want to remove this user.";

    private final UUID ePersonId;
    private final UUID groupId;

    public EmptyWorkflowGroupException(UUID ePersonId, UUID groupId) {
        super(String.format(msgFmt, ePersonId, groupId));
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
