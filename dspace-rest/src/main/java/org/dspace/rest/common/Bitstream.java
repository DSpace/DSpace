/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import org.apache.log4j.Logger;
import org.dspace.core.Constants;

import javax.xml.bind.annotation.XmlRootElement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private String bundleName;
    private String description;
    private String format;
    private String mimeType;
    private Long sizeBytes;
    private DSpaceObject parentObject;
    private String retrieveLink;
    private CheckSum checkSum;
    private Integer sequenceId;

    public Bitstream() {

    }

    public Bitstream(org.dspace.content.Bitstream bitstream, String expand) throws SQLException{
        super(bitstream);
        setup(bitstream, expand);
    }

    public void setup(org.dspace.content.Bitstream bitstream, String expand) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        //A logo bitstream might not have a bundle...
        if(bitstream.getBundles() != null & bitstream.getBundles().length >= 0) {
            if(bitstream.getParentObject().getType() == Constants.ITEM) {
                bundleName = bitstream.getBundles()[0].getName();
            }
        }

        description = bitstream.getDescription();
        format = bitstream.getFormatDescription();
        sizeBytes = bitstream.getSize();
        retrieveLink = "/bitstreams/" + bitstream.getID() + "/retrieve";
        mimeType = bitstream.getFormat().getMIMEType();
        sequenceId = bitstream.getSequenceID();
        CheckSum checkSum = new CheckSum();
        checkSum.setCheckSumAlgorith(bitstream.getChecksumAlgorithm());
        checkSum.setValue(bitstream.getChecksum());
        this.setCheckSum(checkSum);

        if(expandFields.contains("parent") || expandFields.contains("all")) {
            parentObject = new DSpaceObject(bitstream.getParentObject());
        } else {
            this.addExpand("parent");
        }

        if(!expandFields.contains("all")) {
            this.addExpand("all");
        }
    }

    public Integer getSequenceId() {
		return sequenceId;
	}

	public void setSequenceId(Integer sequenceId) {
		this.sequenceId = sequenceId;
	}

	public String getBundleName() {
        return bundleName;
    }

    public void setBundleName(String bundleName) {
		this.bundleName = bundleName;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setFormat(String format) {
		this.format = format;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public void setSizeBytes(Long sizeBytes) {
		this.sizeBytes = sizeBytes;
	}

	public void setParentObject(DSpaceObject parentObject) {
		this.parentObject = parentObject;
	}

	public void setRetrieveLink(String retrieveLink) {
		this.retrieveLink = retrieveLink;
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

    public Long getSizeBytes() {
        return sizeBytes;
    }

    public String getRetrieveLink() {
        return retrieveLink;
    }

    public DSpaceObject getParentObject() {
        return parentObject;
    }
    
    public CheckSum getCheckSum() {
		return checkSum;
	}
    
    public void setCheckSum(CheckSum checkSum) {
		this.checkSum = checkSum;
	}

}
