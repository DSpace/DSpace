/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/21/13
 * Time: 12:54 AM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "bitstream")
public class Bitstream extends DSpaceObject {
    Logger log = Logger.getLogger(Bitstream.class);

    String bundleName;
    String description;
    String format;
    String mimeType;
    String sizeBytes;

    String retrieveLink;

    public Bitstream() {

    }

    public Bitstream(org.dspace.content.Bitstream bitstream, String expand) {
        super(bitstream);
        setup(bitstream, expand);
    }

    public void setup(org.dspace.content.Bitstream bitstream, String expand) {
        try {
            bundleName = bitstream.getBundles()[0].getName();
            description = bitstream.getDescription();
            format = bitstream.getFormatDescription();
            sizeBytes = bitstream.getSize() + "";
            retrieveLink = "/bitstreams/" + bitstream.getID() + "/retrieve";
            mimeType = bitstream.getFormat().getMIMEType();
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    public String getBundleName() {
        return bundleName;
    }

    public String getDescription() {
        return description;
    }

    public String getFormat() {
        return format;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getSizeBytes() {
        return sizeBytes;
    }

    public String getRetrieveLink() {
        return retrieveLink;
    }
}
