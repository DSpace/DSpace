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
