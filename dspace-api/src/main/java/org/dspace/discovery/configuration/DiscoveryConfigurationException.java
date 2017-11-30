/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.discovery.configuration;

/**
 * Exception that can be thrown if there are issues with the discovery configuration
 *
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class DiscoveryConfigurationException extends Exception{

    public DiscoveryConfigurationException() {
    }

    public DiscoveryConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public DiscoveryConfigurationException(String message) {
        super(message);
    }

    public DiscoveryConfigurationException(Throwable cause) {
        super(cause);
    }
}
