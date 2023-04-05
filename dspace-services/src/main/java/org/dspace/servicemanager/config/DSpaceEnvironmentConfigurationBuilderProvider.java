/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.servicemanager.config;

import java.util.Collections;

import org.apache.commons.configuration2.builder.BasicBuilderParameters;
import org.apache.commons.configuration2.builder.combined.BaseConfigurationBuilderProvider;

/**
 * Configures DSpaceEnvironmentConfiguration. Reuses BasicConfigurationBuilder and its parameters.
 *
 * @author Pascal-Nicolas Becker -- dspace at pascal dash becker dot de
 */
public class DSpaceEnvironmentConfigurationBuilderProvider extends BaseConfigurationBuilderProvider {

    /**
     * Creates a new instance of {@code BaseConfigurationBuilderProvider} and
     * initializes all its properties.
     */
    public DSpaceEnvironmentConfigurationBuilderProvider() {
        super("org.apache.commons.configuration2.builder.BasicConfigurationBuilder",
                null,
                "org.dspace.servicemanager.config.DSpaceEnvironmentConfiguration",
                // this probably contains much more than we need, nevertheless reusing it is easier than rewriting
                Collections.singleton(BasicBuilderParameters.class.getName()));
    }
}
