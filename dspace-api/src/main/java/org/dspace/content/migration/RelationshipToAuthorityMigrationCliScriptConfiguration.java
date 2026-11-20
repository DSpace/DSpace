/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

import org.dspace.scripts.configuration.ScriptConfiguration;

/**
 * The {@link ScriptConfiguration} for the {@link RelationshipToAuthorityMigrationScript} CLI script.
 *
 * <p>This script shares the same options between its REST and CLI invocations, so this CLI
 * configuration simply inherits the options declared by
 * {@link RelationshipToAuthorityMigrationScriptConfiguration}.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class RelationshipToAuthorityMigrationCliScriptConfiguration
    extends RelationshipToAuthorityMigrationScriptConfiguration<RelationshipToAuthorityMigrationScript> {

}
