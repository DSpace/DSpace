/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;

/**
 * An association between a {@link org.dspace.curate.CurationTask curation task}
 * and the workflow system.  A single curation task may be associated with more
 * than one workflow, and each association may be configured differently.
 *
 * <p>A curation task can be given {@link addPower "powers"} to affect the
 * progress of a workflow.  For example, a curation task can cause a workflow
 * item to be rejected if it fails.
 *
 * <p>A curation task can be associated with {@link addContact "contacts"}
 * (email addresses) to be notified when the curation task returns a given status.
 */
public class Task {
    /** Name of the curation task. */
    public final String name;

    /** Effects of curation task completion on the workflow step. */
    public final List<String> powers = new ArrayList<>();

    /** Contacts to be notified on a given completion status. */
    public final Map<String, List<String>> contacts = new HashMap<>();

    /**
     * Create an instance of an association with a given curation task.
     * @param name the name of a curation task to be attached to some workflow.
     */
    public Task(@NotNull String name) {
        this.name = name;
    }

    /**
     * Empower this attachment to affect a workflow in some way.
     * @param power the given power.  See {@link org.dspace.curate.XmlWorkflowCuratorServiceImpl#curate}.
     *              TODO should use schema-generated {@code enum}?
     */
    public void addPower(@NotNull String power) {
        powers.add(power);
    }

    /**
     * Associate a contact with a given curation status such as
     * {@link org.dspace.curate.Curator#CURATE_ERROR}.
     *
     * @param status an exit status of the curation task.
     *               TODO should use schema-generated {@code enum}?
     * @param contact an address to be notified of this status.
     */
    public void addContact(@NotNull String status, @NotNull String contact) {
        List<String> sContacts = contacts.get(status);
        if (sContacts == null) {
            sContacts = new ArrayList<>();
            contacts.put(status, sContacts);
        }
        sContacts.add(contact);
    }

    /**
     * Get the collection of contacts for a given status such as
     * {@link org.dspace.curate.Curator#CURATE_SUCCESS}.
     *
     * @param status the given status.
     *               TODO should use schema-generated {@code enum}?
     * @return contacts associated with this status.
     */
    @NotNull
    public List<String> getContacts(@NotNull String status) {
        List<String> ret = contacts.get(status);
        return (ret != null) ? ret : new ArrayList<>();
    }
}
