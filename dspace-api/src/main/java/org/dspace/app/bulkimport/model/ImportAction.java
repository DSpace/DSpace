/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.bulkimport.model;

import java.util.stream.Stream;

/**
 * Enumerates the actions that can be requested for a single row (item) of a bulk
 * import, as specified in the {@code ACTION} column of the import file.
 * <p>
 * The available actions belong to three families:
 * </p>
 * <ul>
 * <li><b>ADD</b> actions create a new item: {@link #ADD}, {@link #ADD_WORKSPACE},
 * {@link #ADD_ARCHIVE};</li>
 * <li><b>UPDATE</b> actions modify an existing item: {@link #UPDATE},
 * {@link #UPDATE_WORKFLOW}, {@link #UPDATE_ARCHIVE};</li>
 * <li>{@link #DELETE} removes an existing item.</li>
 * </ul>
 * <p>
 * When the {@code ACTION} column is left empty the {@link #NOT_SPECIFIED} value is
 * used as the default.
 * </p>
 */
public enum ImportAction {

    /**
     * Create a new workspace item and start the configured submission workflow.
     * The item is validated first: if it is valid the workflow is started (which
     * archives the item when the target collection has no workflow steps),
     * otherwise the item is left in the submitter's workspace.
     */
    ADD,

    /**
     * Create a new workspace item only, leaving it in the submitter's workspace
     * without starting the workflow or archiving it.
     */
    ADD_WORKSPACE,

    /**
     * Create a new item and archive it directly, bypassing the submission workflow
     * and its validation. Reserved to administrators: if the current user is not an
     * administrator the item is left in the workspace instead.
     */
    ADD_ARCHIVE,

    /**
     * Update an existing item in place, without starting a workflow or changing its
     * archived state.
     */
    UPDATE,

    /**
     * Update an existing item and start the configured submission workflow, with the
     * same validation behavior described for {@link #ADD}.
     */
    UPDATE_WORKFLOW,

    /**
     * Update an existing item and archive it directly, bypassing the submission
     * workflow and its validation. Reserved to administrators, as for
     * {@link #ADD_ARCHIVE}.
     */
    UPDATE_ARCHIVE,

    /**
     * Delete the existing item identified by the row's ID.
     */
    DELETE,

    /**
     * Default action used when the {@code ACTION} column is left empty. It behaves
     * like {@link #UPDATE} when an existing item is found for the row's ID, and like
     * {@link #ADD} (creating the item and starting its workflow) otherwise.
     */
    NOT_SPECIFIED;

    /**
     * Checks whether the given string matches, ignoring case, one of the defined
     * actions.
     *
     * @param actionAsString the action name to check
     * @return {@code true} if the string corresponds to a defined action, {@code false} otherwise
     */
    public static boolean isValid(String actionAsString) {
        return Stream.of(values()).anyMatch(action -> action.name().equalsIgnoreCase(actionAsString));
    }

    /**
     * Returns whether this action creates a new item.
     *
     * @return {@code true} for {@link #ADD}, {@link #ADD_WORKSPACE} and {@link #ADD_ARCHIVE}
     */
    public boolean isAddAction() {
        return this == ADD || this == ADD_WORKSPACE || this == ADD_ARCHIVE;
    }

    /**
     * Returns whether this action updates an existing item.
     *
     * @return {@code true} for {@link #UPDATE}, {@link #UPDATE_WORKFLOW} and {@link #UPDATE_ARCHIVE}
     */
    public boolean isUpdateAction() {
        return this == UPDATE || this == UPDATE_WORKFLOW || this == UPDATE_ARCHIVE;
    }

}
