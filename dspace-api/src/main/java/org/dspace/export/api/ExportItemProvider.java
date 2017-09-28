/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.export.api;

import java.io.File;
import java.io.OutputStream;

import org.dspace.content.Item;

/**
 *
 * @author João Melo <jmelo@lyncode.com>
 * 
 */
public interface ExportItemProvider
{
    /**
     * Get a valid stylesheet of an export provider. 
     * 
     * @return the XSLT file of the export provider.
     */
    public File getXSLT();

    /**
     * Get the string identifies of this export provider.
     * 
     * @return unique string that identifies this export provider.
     */
    public String getId();

    /**
     * Get the image of this export provider.
     *
     * @return the path of the image of this export provider.
     */
    public String getImage();

    /**
     * Export the item metadata based on the stylesheet of the export provider.
     * 
     * @param item that metadata should be export from.
     * @param output to put the result in.
     * @throws ExportItemException 
     */
    public void export(Item item, OutputStream output) throws ExportItemException;

    /**
     * Get the content type of this export provider.
     *
     * @return the content type of this export provider.
     */
    public String getContentType();

    /**
     * Get the file extension of this export provider.
     *
     * @return the file extension of this export provider.
     */
    public String getFileExtension();
}
