 /**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.sword.client;

import org.dspace.services.factory.DSpaceServicesFactory;
import org.purl.sword.base.Collection;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.SwordAcceptPackaging;
import org.purl.sword.base.Workspace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * User: Robin Taylor
 * Date: 15/02/11
 * Time: 21:12
 */
public class ServiceDocumentHelper {

    public static List<Collection> getCollections(ServiceDocument serviceDoc)
    {
        List<Collection> allCollections = new ArrayList<Collection>();
        List<Workspace> workspaces = serviceDoc.getService().getWorkspacesList();

        for (Workspace ws : workspaces)
        {
            List<Collection> collections = ws.getCollections();
            allCollections.addAll(collections);
        }

        return allCollections;
    }

    public static Collection getCollection(ServiceDocument serviceDoc, String location)
    {
        List<Collection> allCollections =  getCollections(serviceDoc);
        for (Collection collection : allCollections)
        {
            if (collection.getLocation().equals(location))
            {
                return collection;
            }
        }

        // If we got here then we didn't find a match.
        return null;
    }

    public static String[] getCommonFileTypes(ServiceDocument serviceDoc, String location)
    {
        String[] clientFTsArray = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("sword-client.file-types");
        List<String> clientFTs = Arrays.asList(clientFTsArray);

        List<String> commonFTs = new ArrayList<String>();

        Collection collection = ServiceDocumentHelper.getCollection(serviceDoc, location);
        String[] serverFTs = collection.getAccepts();
        for (String serverFT : serverFTs)
        {
             if (clientFTs.contains(serverFT))
            {
                commonFTs.add(serverFT);
            }
        }

        return commonFTs.toArray(new String[commonFTs.size()]);
    }

    public static String[] getCommonPackageFormats(ServiceDocument serviceDoc, String location)
    {
        String[] clientPFsArray = DSpaceServicesFactory.getInstance().getConfigurationService().getArrayProperty("sword-client.package-formats");
        List<String> clientPFs = Arrays.asList(clientPFsArray);

        List<String> commonPFs = new ArrayList<String>();

        Collection collection = ServiceDocumentHelper.getCollection(serviceDoc, location);
        List<SwordAcceptPackaging> serverPFs = collection.getAcceptPackaging();
        for (SwordAcceptPackaging serverPF : serverPFs)
        {
            if (clientPFs.contains(serverPF.getContent()))
            {
                commonPFs.add(serverPF.getContent());
            }
        }

        return commonPFs.toArray(new String[commonPFs.size()]);
    }

    public static String[] getPackageFormats(Collection collection)
    {
        List<String> packageFormats = new ArrayList<String>();
        List<SwordAcceptPackaging> pfs = collection.getAcceptPackaging();
        for (SwordAcceptPackaging pf : pfs)
        {
            packageFormats.add(pf.getContent());
        }

        return packageFormats.toArray(new String[pfs.size()]);
    }

}
