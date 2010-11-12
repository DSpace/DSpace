/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.mediafilter;

/**
 * Interface to allow filters to register the input formats they handle
 * (useful for exposing underlying capabilities of libraries used)
 */
public interface SelfRegisterInputFormats
{
    public String[] getInputMIMETypes();

    public String[] getInputDescriptions();

    public String[] getInputExtensions();
}
