/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.storage.rdbms.xmlworkflow;

/**
 * This class automatically migrates your DSpace Database to use the XML-based
 * Configurable Workflow system whenever it is enabled. It is just a empty
 * extension of the v6.0 version so that, according to the version / naming
 * mapping, flyway will pick it for whatever 6.x version
 * <P>
 * Because XML-based Configurable Workflow existed prior to our migration, this
 * class first checks for the existence of the "cwf_workflowitem" table before
 * running any migrations.
 * <P>
 * This class represents a Flyway DB Java Migration
 * http://flywaydb.org/documentation/migration/java.html
 * <P>
 * It can upgrade any (6.0-7.0) version of DSpace to use the XMLWorkflow.
 *
 */
public class V6_99_2021_10_21__Enable_XMLWorkflow_Migration
        extends V6_0_2015_09_01__DS_2701_Enable_XMLWorkflow_Migration {
}