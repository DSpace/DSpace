/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.itemupdate;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dspace.content.Bitstream;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;

/**
 * BitstreamFilter implementation to filter by filename pattern
 */
public class BitstreamFilterByFilename extends BitstreamFilter {
    protected final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();

    protected Pattern pattern;
    protected String filenameRegex;

    public BitstreamFilterByFilename() {
        //empty
    }

    /**
     * Tests bitstream by matching the regular expression in the
     * properties against the bitstream name
     *
     * @param bitstream Bitstream
     * @return whether bitstream name matches the regular expression
     * @throws BitstreamFilterException if filter error
     */
    @Override
    public boolean accept(Bitstream bitstream) throws BitstreamFilterException {
        if (filenameRegex == null) {
            filenameRegex = props.getProperty("filename");
            if (filenameRegex == null) {
                throw new BitstreamFilterException("BitstreamFilter property 'filename' not found.");
            }
            pattern = Pattern.compile(filenameRegex);
        }

        Matcher m = pattern.matcher(bitstreamService.getName(bitstream));
        return m.matches();
    }

}
