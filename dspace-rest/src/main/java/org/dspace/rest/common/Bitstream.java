package org.dspace.rest.common;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/21/13
 * Time: 12:54 AM
 * To change this template use File | Settings | File Templates.
 */
public class Bitstream {

    String bundleName;
    String name;
    String description;
    String format;
    String sizeBytes;

    public Bitstream() {

    }

    public Bitstream(org.dspace.content.Bitstream bitstream) {
        try {
            bundleName = bitstream.getBundles()[0].getName();
            name = bitstream.getName();
            description = bitstream.getDescription();
            format = bitstream.getFormatDescription();
            sizeBytes = bitstream.getSize() + "";
        } catch (Exception e) {
            //todo
        }

    }

    public String getBundleName() {
        return bundleName;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getFormat() {
        return format;
    }

    public String getSizeBytes() {
        return sizeBytes;
    }


}
