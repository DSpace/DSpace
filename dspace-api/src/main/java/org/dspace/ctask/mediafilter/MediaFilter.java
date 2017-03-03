/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.mediafilter;

import java.io.InputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.factory.AuthorizeServiceFactory;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Bitstream;
import org.dspace.content.BitstreamFormat;
import org.dspace.content.Bundle;
import org.dspace.content.DCDate;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamFormatService;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.BundleService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.eperson.EPerson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MediaFilter creates derivatives from bitstreams and stores them in
 * designated bundles. Roughly equivalent to MediaFilter command-line
 * tools implemented in org.dspace.app.mediafilter, with improvements.
 * The task fails if any eligible bitstream fails to produce a derivative, 
 * otherwise it succeeds. This is an abstract class that must be subclassed
 * with specific derivative functions (images, extracted text, etc). Subclasses
 * *must* annotate themselves as Mutative - annotations are not inherited.
 *
 * @author richardrodgers
 */
public abstract class MediaFilter extends AbstractCurationTask
{
	private static final Logger log = LoggerFactory.getLogger(MediaFilter.class);

    private AuthorizeService authorizeService;

    private BitstreamFormatService bitstreamFormatService;

	// source configuration parameters
	private String sourceBundle = null;
	private Pattern sourceSelector = null;
	private int sourceMinSize = 0;
	protected List<String> sourceFormats = new ArrayList<>();
	// filter process parameters
	protected boolean filterForce = false;
	// target (if any) parameters
	private String targetBundle = null;
	private String targetSpec = null;
	protected String targetFormat = null;
	private String targetDescription = null;
	private String targetPolicy = null;
    
    /**
     * Initialize task - parameters inform the task of it's invoking curator.
     * Since the curator can provide services to the task, this represents
     * curation DI.
     * 
     * @param curator the Curator controlling this task
     * @param taskId identifier task should use in invoking services
     * @throws IOException
     */
    @Override 
    public void init(Curator curator, String taskId) throws IOException  {
        super.init(curator, taskId);

        authorizeService
            = AuthorizeServiceFactory.getInstance().getAuthorizeService();
        bitstreamFormatService
            = ContentServiceFactory.getInstance().getBitstreamFormatService();

        String[] srcSpecs = taskProperty("source.selector").split("/");
        sourceBundle = srcSpecs[0];
        if (srcSpecs.length > 1) {
        	sourceSelector = glob2regex(srcSpecs[1]);
        }
        sourceMinSize = taskIntProperty("source.minsize", 0);
        String fmtList = taskProperty("source.formats");
        if (fmtList != null)  {
        	sourceFormats = Arrays.asList(fmtList.split(","));
        }
        filterForce = taskBooleanProperty("filter.force", false);
        String[] trgSpecs = taskProperty("target.spec").split("/");
        targetBundle = trgSpecs[0];
        if (trgSpecs.length > 1) {
        	targetSpec = trgSpecs[1];
        }
        targetFormat = taskProperty("target.format");
        targetDescription = taskProperty("target.description");
        targetPolicy = taskProperty("target.policy");
        
        // many media filter transformations use GUI libraries that
        // expect a display device: make sure the JVM knows otherwise
        System.setProperty("java.awt.headless", "true");
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException  {
    	if (Constants.ITEM != dso.getType()) {
    		setResult("Object skipped");
    		return Curator.CURATE_SKIP;
    	}
    	Item item = (Item)dso;
        int eligible = 0, filtered = 0;
        try {
            Context c = Curator.curationContext();
            for (Bundle bundle : itemService.getBundles(item, sourceBundle)) {
                for (Bitstream bitstream : bundle.getBitstreams()) {
                	if (isEligible(c, item, bitstream)) {
                		++eligible;
                		if (filterBitstream(item, bitstream)) {
                			++filtered;
                		}
                	}
                }
            }
        } catch (AuthorizeException | SQLException authE) {
            throw new IOException(authE.getMessage());
        }
        String itemId = item.getHandle();
        if (itemId == null) {
            itemId = "workspace item: " + item.getID();
        }
        // need a lot more detail here about specific bistreams, etc
        String msg = "Filtered item: " + itemId;
        setResult(msg);
        int status = (eligible == 0) ? Curator.CURATE_SKIP :
                     (filtered == eligible) ? Curator.CURATE_SUCCESS : Curator.CURATE_FAIL;
        if (status == Curator.CURATE_FAIL) {
            report(msg + ": failed!");
        }
        return status;
    }
           
    // Concrete subclasses must implement
    protected abstract boolean canFilter(Item item, Bitstream bitstream);
    protected abstract boolean filterBitstream(Item item, Bitstream bitstream) 
    		  throws AuthorizeException, IOException, SQLException;

    protected boolean createDerivative(Item item, Bitstream source, InputStream targetStream)
    	throws AuthorizeException, IOException, SQLException  {
        BundleService bundleService
                = ContentServiceFactory.getInstance().getBundleService();
        BitstreamService bitstreamService
                = ContentServiceFactory.getInstance().getBitstreamService();
        Bundle targBundle = null;
        Context c = Curator.curationContext();
        Bitstream existingBitstream = existingTarget(c, item, source);
        
        List<Bundle> bundles = itemService.getBundles(item, targetBundle);
        // create new bundle if needed
        if (bundles.isEmpty()) {
            targBundle = bundleService.create(c, item, targetBundle);
        } else {
            // take the first match
            targBundle = bundles.get(0);
        }

        Bitstream target = bitstreamService.create(c, targBundle, targetStream);
        targetStream.close();

        // Now set the format, name, etc of the target bitstream
        target.setName(c, targetName(c, source));
        target.setSource(c, "Written by curation task " + taskId + " on " + DCDate.getCurrent() + " (GMT).");
        target.setDescription(c, targetDescription);

        // Find the proper format
        BitstreamFormat bf = bitstreamFormatService.findByShortDescription(c, targetFormat);
        target.setFormat(c, bf);
        bitstreamService.update(c, target);

        //Inherit or set policies based on policy declaration
        setTargetPolicies(c, source, item, targBundle, target);

        // fixme - set date?
        // if we are overwriting, remove old bitstream
        if (existingBitstream != null) {
            bundleService.removeBitstream(c, targBundle, existingBitstream);
        }
        // update item changes
        itemService.update(c, item);
        return true;
    }

    private boolean isEligible(Context context, Item item, Bitstream bitstream) throws SQLException  {
    	if (bitstream.getSize() < sourceMinSize) {
    		log.debug("Bitstream: '{}' size: {} below minimum: {}",
                    bitstream.getName(), bitstream.getSize(), sourceMinSize);
    		return false;
    	} else if ((sourceSelector != null) && ! sourceSelector.matcher(bitstream.getName()).matches()) {
    		log.debug("Bitstream: '{}' does not match selector: {}",
                    bitstream.getName(), sourceSelector.toString());
    		return false;
    	} else if (! filterForce && existingTarget(context, item, bitstream) != null) {
    		log.debug("Bitstream: '{}' target already exists", bitstream.getName());
    		return false;
    	} else if (sourceFormats.size() > 0 && 
    			  ! sourceFormats.contains(bitstream.getFormat(context).getShortDescription())) {
    		log.debug("Bitstream: '{}' format: {} not listed",
                    bitstream.getName(),
                    bitstream.getFormat(context).getShortDescription());
    		return false;
    	} else {
    		return canFilter(item, bitstream);
    	}
    }
    
    private String targetName(Context context, Bitstream source) throws SQLException {
    	String bsName = source.getName();
    	String targName = null;
    	BitstreamFormat bsf = bitstreamFormatService.findByShortDescription(context, targetFormat);
    	if (targetSpec == null) {
    		// use old convention - append suffix of target format
    		targName = bsName + "." + bsf.getExtensions().get(0);
    	} else {
    		// interpret the spec - $src = source name, $ext = extension from target format
    		targName = targetSpec.replace("$src", bsName).replace("$ext", bsf.getExtensions().get(0));
    	}
    	return targName;
    }
    
    private Bitstream existingTarget(Context context, Item item, Bitstream source) throws SQLException {
    	String targName = targetName(context, source);
        // check whether destination bitstream exists
        for (Bundle bnd : itemService.getBundles(item, targetBundle)) {
        	for (Bitstream bs: bnd.getBitstreams()) {
        		if (bs.getName().equals(targName)) {
        			return bs;
        		}
        	}
        }
    	return null;
    }
    
    private Pattern glob2regex(String theGlob) {
    	String glob = (theGlob == null) ? "*" : theGlob;
    	StringBuilder sb = new StringBuilder("^");
    	for (int i = 0; i < glob.length(); i++) {
    		char c = glob.charAt(i);
    		switch (c) {
    			case '*': sb.append(".*"); break;
    			case '?': sb.append('.'); break;
    			case '.': sb.append("\\."); break;
    			case '\\': sb.append("\\\\"); break;
    			default: sb.append(c);
    		}
    	}
    	sb.append("$");
    	return Pattern.compile(sb.toString());
    }
    
    private void setTargetPolicies(Context c, Bitstream source, Item item, Bundle targBundle, Bitstream target)
    		throws AuthorizeException, SQLException  {
        // first, remove any existing policies
    	authorizeService.removeAllPolicies(c, target);

    	DSpaceObject policyDonor = null;
    	if (null != targetPolicy) switch (targetPolicy)
        {
        case "bitstream":
            policyDonor = source;
            break;
        case "bundle":
            policyDonor = targBundle;
            break;
        case "item":
            policyDonor = item;
            break;
        case "collection":
            policyDonor = item.getOwningCollection();
            break;
        case "open":
            authorizeService.addPolicy(c, target, Constants.READ, (EPerson)null);
            break;
        case "closed":
            // set a default, then remove read
            authorizeService.inheritPolicies(c, item, target);
            authorizeService.removePoliciesActionFilter(c, target, Constants.READ);
            break;
        default:
            log.error("Unknown policy: '" + targetPolicy + "'");
            break;
        }
    	if (policyDonor != null) {
    		authorizeService.inheritPolicies(c, policyDonor, target);
    	}
    }
}
