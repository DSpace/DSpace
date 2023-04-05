/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.service;

import java.util.Iterator;

import org.dspace.app.bulkedit.DSpaceCSV;
import org.dspace.content.Community;
import org.dspace.content.Item;
import org.dspace.core.Context;
import org.dspace.scripts.handler.DSpaceRunnableHandler;

/**
 * This is the interface to be implemented by a Service that deals with the exporting of Metadata
 */
public interface MetadataDSpaceCsvExportService {

    /**
     * This method will export DSpaceObject objects depending on the parameters it gets. It can export all the items
     * in the repository, all the items in a community, all the items in a collection or a specific item. The latter
     * three are specified by the handle parameter. The entire repository can be exported by defining the
     * exportAllItems parameter as true
     * @param context           The relevant DSpace context
     * @param exportAllItems    A boolean indicating whether or not the entire repository should be exported
     * @param exportAllMetadata Defines if all metadata should be exported or only the allowed ones
     * @param identifier        The handle or UUID for the DSpaceObject to be exported, can be a Community,
     *                          Collection or Item
     * @return                  A DSpaceCSV object containing the exported information
     * @throws Exception        If something goes wrong
     */
    public DSpaceCSV handleExport(Context context, boolean exportAllItems, boolean exportAllMetadata,
                                  String identifier, DSpaceRunnableHandler dSpaceRunnableHandler) throws Exception;

    /**
     * This method will export all the Items in the given toExport iterator to a DSpaceCSV
     * @param context       The relevant DSpace context
     * @param toExport      The iterator containing the items to export
     * @param exportAll     Defines if all metadata should be exported or only the allowed ones
     * @return              A DSpaceCSV object containing the exported information
     * @throws Exception    If something goes wrong
     */
    public DSpaceCSV export(Context context, Iterator<Item> toExport, boolean exportAll) throws Exception;

    /**
     * This method will export all the Items within the given Community to a DSpaceCSV
     * @param context       The relevant DSpace context
     * @param community     The Community that contains the Items to be exported
     * @param exportAll     Defines if all metadata should be exported or only the allowed ones
     * @return              A DSpaceCSV object containing the exported information
     * @throws Exception    If something goes wrong
     */
    public DSpaceCSV export(Context context, Community community, boolean exportAll) throws Exception;

}