/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.rest.common;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.servlet.ServletContext;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.dspace.content.Bundle;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.utils.DSpace;

/**
 * Created with IntelliJ IDEA.
 * User: peterdietz
 * Date: 9/21/13
 * Time: 12:54 AM
 * To change this template use File | Settings | File Templates.
 */
@XmlRootElement(name = "bitstream")
public class Bitstream extends DSpaceObject {
    protected BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    protected BundleService bundleService = ContentServiceFactory.getInstance().getBundleService();

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
    
    private ResourcePolicy[] policies = null;
    
    public Bitstream() {

    }

    public Bitstream(org.dspace.content.Bitstream bitstream, ServletContext servletContext, String expand, Context context) throws SQLException{
        super(bitstream, servletContext);
        setup(bitstream, servletContext, expand, context);
    }

    public void setup(org.dspace.content.Bitstream bitstream, ServletContext servletContext, String expand, Context context) throws SQLException{
        List<String> expandFields = new ArrayList<String>();
        if(expand != null) {
            expandFields = Arrays.asList(expand.split(","));
        }

        //A logo bitstream might not have a bundle...
        if(bitstream.getBundles() != null & bitstream.getBundles().size() >= 0) {
            if(bitstreamService.getParentObject(context, bitstream).getType() == Constants.ITEM) {
                bundleName = bitstream.getBundles().get(0).getName();
            }
        }

        description = bitstream.getDescription();
        format = bitstreamService.getFormatDescription(context, bitstream);
        sizeBytes = bitstream.getSize();
        String path = new DSpace().getRequestService().getCurrentRequest().getHttpServletRequest().getContextPath();
        retrieveLink = path + "/bitstreams/" + bitstream.getID() + "/retrieve";
        mimeType = bitstreamService.getFormat(context, bitstream).getMIMEType();
        sequenceId = bitstream.getSequenceID();
        CheckSum checkSum = new CheckSum();
        checkSum.setCheckSumAlgorith(bitstream.getChecksumAlgorithm());
        checkSum.setValue(bitstream.getChecksum());
        this.setCheckSum(checkSum);

        if(expandFields.contains("parent") || expandFields.contains("all")) {
            parentObject = new DSpaceObject(bitstreamService.getParentObject(context, bitstream), servletContext);
        } else {
            this.addExpand("parent");
        }

        if(expandFields.contains("policies") || expandFields.contains("all")) {
            // Find policies without context.
        	List<ResourcePolicy> tempPolicies = new ArrayList<ResourcePolicy>();
        	List<Bundle> bundles = bitstream.getBundles();
			for (Bundle bundle : bundles) {
				List<org.dspace.authorize.ResourcePolicy> bitstreamsPolicies = bundleService.getBitstreamPolicies(context, bundle);
				for (org.dspace.authorize.ResourcePolicy policy : bitstreamsPolicies) {
                    if(policy.getdSpaceObject().equals(bitstream)) {
                        tempPolicies.add(new ResourcePolicy(policy));
                    }
				}
			}
			
			policies = tempPolicies.toArray(new ResourcePolicy[0]);
        } else {
            this.addExpand("policies");
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

	public ResourcePolicy[] getPolicies() {
		return policies;
	}

	public void setPolicies(ResourcePolicy[] policies) {
		this.policies = policies;
	}
    
    
}
