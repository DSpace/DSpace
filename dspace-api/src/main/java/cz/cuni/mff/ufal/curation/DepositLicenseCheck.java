package cz.cuni.mff.ufal.curation;

import org.apache.commons.io.IOUtils;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;

public class DepositLicenseCheck extends AbstractCurationTask {

    private String msg = null;
    private int status = Curator.CURATE_UNSET;

    @Override
    public int perform(DSpaceObject dso) {
        if(dso.getType() == Constants.ITEM){
            Item item = (Item)dso;
            //everything OK unless prove otherwise
            msg = String.format("Item %s OK", item.getHandle());
            status = Curator.CURATE_SUCCESS;
            try {
                Collection col = item.getOwningCollection();
                String colLicense = col.getLicense();
                //we have some deposited content
                if(item.getBundles("ORIGINAL").length > 0 && item.getBundles("ORIGINAL")[0].getBitstreams().length > 0) {
                    //if we have a license check if it's ok or some junk.
                    if (item.getBundles("LICENSE").length == 1 && item.getBundles("LICENSE")[0].getBitstreams().length == 1) {
                        Bundle bundle = item.getBundles("LICENSE")[0];
                        Bitstream bit = bundle.getBitstreams()[0];
                        String bitLicense = IOUtils.toString(bit.retrieve(), "UTF-8");
                        if (!colLicense.equals(bitLicense)) {
                            //This is the generic "Replace me" license, use colLicense instead
                            if (bitLicense != null && bitLicense.contains(placeholderLicenseText)) {
                                item.removeDSpaceLicense();
                                Context context = new Context();
                                LicenseUtils.grantLicense(context, item, colLicense);
                                context.complete();
                                msg = String.format("Replaced default \"placeholder\" deposit license in %s", item.getHandle());
                                //Something unexpected, just report
                            } else {
                                msg = String.format("Deposit license for item %s (/xmlui/bitstream/id/%s/?sequence=%s) and collection %s differ.",
                                        item.getHandle(), bit.getID(), bit.getSequenceID(), col.getName());
                                status = Curator.CURATE_FAIL;
                            }
                        }
                        //there is no license bundle or bitstream, add it
                    } else if (item.getBundles("LICENSE").length == 0 || (item.getBundles("LICENSE").length == 1 && item.getBundles("LICENSE")[0].getBitstreams().length == 0)) {
                        //if the bundle is there remove it
                        item.removeDSpaceLicense();
                        Context context = new Context();
                        LicenseUtils.grantLicense(context, item, colLicense);
                        context.complete();
                        msg = String.format("Adding missing deposit license for %s", item.getHandle());
                    } else {
                        throw new Exception("More than one LICENSE bundles or license bitstreams.");
                    }
                }
            } catch (Exception e) {
                msg = String.format("Exception while running DepositLicenseCheck on %s: %s", item.getHandle(), e.getMessage());
                status =  Curator.CURATE_ERROR;
            }

        } else {
            status = Curator.CURATE_SKIP;
        }
        if(msg != null){
            report(msg);
            setResult(msg);
        }
        return status;
    }

    private String placeholderLicenseText = "NOTE: PLACE YOUR OWN LICENSE HERE";
}
