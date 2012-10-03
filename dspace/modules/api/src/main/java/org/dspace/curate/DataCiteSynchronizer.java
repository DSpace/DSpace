package org.dspace.curate;

import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.doi.CDLDataCiteService;
import org.dspace.identifier.DOIIdentifierProvider;
import org.dspace.services.ConfigurationService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

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
        String response = dataCiteService.update(doi, null, metadatalist);

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
