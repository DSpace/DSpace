/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import java.io.IOException;
import java.sql.SQLException;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.dspace.services.ConfigurationService;
import org.dspace.services.factory.DSpaceServicesFactory;

/**
 * IdentifyFormats re-runs bitstream format identification
 * ({@link BitstreamFormatService#guessFormat(Context, Bitstream)}) over the bitstreams of
 * the passed object and updates any whose format can now be determined. Because the modern
 * identification is content-based (Apache Tika), this can correct bitstreams that were
 * previously stored as "Unknown" (application/octet-stream) because their filename had a
 * missing, wrong, or unregistered extension.
 *
 * <p>By default only bitstreams currently labelled "Unknown" are re-identified. Set
 * {@code curate.identifyformats.process-all = true} to re-identify every bitstream (this
 * may reclassify bitstreams whose stored format disagrees with their actual content).
 *
 * <p>The task writes changes, so it must be run by an administrator (e.g.
 * {@code dspace curate -e admin@example.org -t identifyformats -i <handle|uuid>}).
 *
 * @author DSpace
 */
@Distributive
public class IdentifyFormats extends AbstractCurationTask {

    protected static final String CFG_PROCESS_ALL = "curate.identifyformats.process-all";

    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BitstreamFormatService bitstreamFormatService = ContentServiceFactory.getInstance()
                                                                                   .getBitstreamFormatService();
    protected ConfigurationService configurationService = DSpaceServicesFactory.getInstance()
                                                                               .getConfigurationService();

    // counters accumulated across the distributed items
    private int examined;
    private int corrected;
    private int stillUnidentified;
    private StringBuilder changes;

    @Override
    public int perform(Context context, DSpaceObject dso) throws IOException {
        examined = 0;
        corrected = 0;
        stillUnidentified = 0;
        changes = new StringBuilder();

        distribute(context, dso);

        StringBuilder result = new StringBuilder();
        result.append("Bitstream format identification: examined ").append(examined)
              .append(" bitstream(s), corrected ").append(corrected)
              .append(", still unidentified ").append(stillUnidentified).append(".\n");
        if (changes.length() > 0) {
            result.append(changes);
        }
        report(result.toString());
        setResult(result.toString());
        return Curator.CURATE_SUCCESS;
    }

    @Override
    protected void performItem(Context context, Item item) throws SQLException, IOException {
        boolean processAll = configurationService.getBooleanProperty(CFG_PROCESS_ALL, false);
        BitstreamFormat unknownFormat = bitstreamFormatService.findUnknown(context);

        for (Bundle bundle : item.getBundles()) {
            for (Bitstream bitstream : bundle.getBitstreams()) {
                BitstreamFormat currentFormat = bitstream.getFormat(context);
                boolean isUnknown = currentFormat == null
                    || currentFormat.getID().equals(unknownFormat.getID());

                // By default only attempt to fix bitstreams that are currently unidentified.
                if (!processAll && !isUnknown) {
                    continue;
                }
                examined++;

                BitstreamFormat guessed = bitstreamFormatService.guessFormat(context, bitstream);
                boolean guessedUsable = guessed != null && !guessed.getID().equals(unknownFormat.getID());

                if (guessedUsable && (currentFormat == null || !guessed.getID().equals(currentFormat.getID()))) {
                    String from = currentFormat == null ? "Unknown" : currentFormat.getShortDescription();
                    try {
                        bitstreamService.setFormat(context, bitstream, guessed);
                        bitstreamService.update(context, bitstream);
                    } catch (AuthorizeException e) {
                        throw new IOException("Not authorized to update format of bitstream "
                            + bitstream.getID(), e);
                    }
                    corrected++;
                    changes.append(" - bitstream ").append(bitstream.getID())
                           .append(" (item ").append(item.getHandle()).append("): ")
                           .append(from).append(" -> ").append(guessed.getShortDescription())
                           .append(" (").append(guessed.getMIMEType()).append(")\n");
                } else if (isUnknown) {
                    stillUnidentified++;
                }
            }
        }
    }
}
