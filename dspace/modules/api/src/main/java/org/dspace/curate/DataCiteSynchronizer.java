package org.dspace.curate;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.doi.CDLDataCiteService;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.services.ConfigurationService;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.dspace.doi.DOI;
import org.dspace.doi.DryadDOIRegistrationHelper;

/**
 * Created by IntelliJ IDEA.
 * User: fabio.bolognesi
 * Date: 10/24/11
 * Time: 2:10 PM
 * To change this template use File | Settings | File Templates.
 */
public class DataCiteSynchronizer extends AbstractCurationTask{

    private static final int NOT_SYNCH = 1;
    private static final int SYNCH = 0;

    private ConfigurationService configurationService=null;

    @Override
    public int perform(DSpaceObject dso) throws IOException {

        if(!ConfigurationManager.getBooleanProperty("doi.datacite.connected", false)){
            this.setResult("Functionality not supported in test environment.");
            return Curator.CURATE_FAIL;
        }


        if(!(dso instanceof Item)) return Curator.CURATE_NOTASK;

        Item item = (Item)dso;

        CDLDataCiteService dataCiteService = new CDLDataCiteService(ConfigurationManager.getProperty("doi.username"), ConfigurationManager.getProperty("doi.password"));


        // for Local TEST
        //String doi = "10.5061/DRYAD.2222";
        String doi = DOIIdentifierProvider.getDoiValue(item);

        Map<String, String> metadatalist = dataCiteService.createMetadataList(item);
        DOI aDOI = new DOI(doi, item);
        String target = aDOI.getTargetURL().toString();
        try {
            // if item is in blackout, change target to the blackout URL
            if(DryadDOIRegistrationHelper.isDataPackageInPublicationBlackout(item)) {
                target = ConfigurationManager.getProperty("dryad.blackout.url");
            }
        } catch (SQLException ex) {
            this.setResult("Unable to check if item is in publication blackout: " + ex.getLocalizedMessage());
            return Curator.CURATE_FAIL;
        }
        String response = dataCiteService.update(aDOI.toID(), target, metadatalist);

        if(response.contains("bad request") || response.contains("BAD REQUEST")){
            this.setResult(response);
            return Curator.CURATE_FAIL;
        }
        else if(!response.contains("OK")){
            this.setResult("Verified generic error. Try again later.");
            return Curator.CURATE_FAIL;
        }


        this.setResult("Synchronization executed.");
        return Curator.CURATE_SUCCESS;

    }

    public ConfigurationService getConfigurationService() {
        return configurationService;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

}
