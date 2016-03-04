package cz.cuni.mff.ufal.curation;

import org.apache.commons.io.IOUtils;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.*;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.mockito.internal.exceptions.ExceptionIncludingMockitoWarnings;

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
                            if (placeholderLicenseText.equals(bitLicense)) {
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

    private String placeholderLicenseText = "NOTE: PLACE YOUR OWN LICENSE HERE\n" +
            "This sample license is provided for informational purposes only.\n" +
            "\n" +
            "NON-EXCLUSIVE DISTRIBUTION LICENSE\n" +
            "\n" +
            "By signing and submitting this license, you (the author(s) or copyright\n" +
            "owner) grants to DSpace University (DSU) the non-exclusive right to reproduce,\n" +
            "translate (as defined below), and/or distribute your submission (including\n" +
            "the abstract) worldwide in print and electronic format and in any medium,\n" +
            "including but not limited to audio or video.\n" +
            "\n" +
            "You agree that DSU may, without changing the content, translate the\n" +
            "submission to any medium or format for the purpose of preservation.\n" +
            "\n" +
            "You also agree that DSU may keep more than one copy of this submission for\n" +
            "purposes of security, back-up and preservation.\n" +
            "\n" +
            "You represent that the submission is your original work, and that you have\n" +
            "the right to grant the rights contained in this license. You also represent\n" +
            "that your submission does not, to the best of your knowledge, infringe upon\n" +
            "anyone's copyright.\n" +
            "\n" +
            "If the submission contains material for which you do not hold copyright,\n" +
            "you represent that you have obtained the unrestricted permission of the\n" +
            "copyright owner to grant DSU the rights required by this license, and that\n" +
            "such third-party owned material is clearly identified and acknowledged\n" +
            "within the text or content of the submission.\n" +
            "\n" +
            "IF THE SUBMISSION IS BASED UPON WORK THAT HAS BEEN SPONSORED OR SUPPORTED\n" +
            "BY AN AGENCY OR ORGANIZATION OTHER THAN DSU, YOU REPRESENT THAT YOU HAVE\n" +
            "FULFILLED ANY RIGHT OF REVIEW OR OTHER OBLIGATIONS REQUIRED BY SUCH\n" +
            "CONTRACT OR AGREEMENT.\n" +
            "\n" +
            "DSU will clearly identify your name(s) as the author(s) or owner(s) of the\n" +
            "submission, and will not make any alteration, other than as allowed by this\n" +
            "license, to your submission.\n";
}
