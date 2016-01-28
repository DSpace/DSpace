/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.authority.orcid.model;

/**
 * http://support.orcid.org/knowledgebase/articles/118843-anatomy-of-a-contributor
 *
 * @author Antoine Snyers (antoine at atmire.com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public enum ContributorAttributeRole {

    AUTHOR,
    ASSIGNEE,
    EDITOR,
    CHAIR_OR_TRANSLATOR,
    CO_INVESTIGATOR,
    CO_INVENTOR,
    GRADUATE_STUDENT,
    OTHER_INVENTOR,
    PRINCIPAL_INVESTIGATOR,
    POSTDOCTORAL_RESEARCHER,
    SUPPORT_STAFF

}
