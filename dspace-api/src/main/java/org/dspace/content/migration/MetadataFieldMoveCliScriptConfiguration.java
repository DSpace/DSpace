/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.migration;

/**
 * The {@link org.dspace.scripts.configuration.ScriptConfiguration} for the {@link MetadataFieldMoveScript} CLI script.
 *
 * <p>This script is only exposed via the command line, so this CLI configuration simply inherits the
 * options declared by {@link MetadataFieldMoveScriptConfiguration}.</p>
 *
 * @author Adamo Fapohunda (adamo.fapohunda at 4science.com)
 */
public class MetadataFieldMoveCliScriptConfiguration
    extends MetadataFieldMoveScriptConfiguration<MetadataFieldMoveScript> {

}
