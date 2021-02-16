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
 * A workflow task.
 */
public class Task {
    public final String name;
    public final List<String> powers = new ArrayList<>();
    public final Map<String, List<String>> contacts = new HashMap<>();

    /**
     * Create a task with a given name.
     * @param name the task's name.
     */
    public Task(@NotNull String name) {
        this.name = name;
    }

    /**
     * Add a task power to this task.
     * @param power the given power.  TODO should use schema-generated {@code enum}?
     */
    public void addPower(@NotNull String power) {
        powers.add(power);
    }

    /**
     * Associate a contact with a given status.
     *
     * @param status the given status.  TODO should use schema-generated {@code enum}?
     * @param contact the associated contact.
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
     * Get the collection of contacts for a given status.
     * @param status the given status.  TODO should use schema-generated {@code enum}?
     * @return contacts associated with this status.
     */
    @NotNull
    public List<String> getContacts(@NotNull String status) {
        List<String> ret = contacts.get(status);
        return (ret != null) ? ret : new ArrayList<>();
    }
}
