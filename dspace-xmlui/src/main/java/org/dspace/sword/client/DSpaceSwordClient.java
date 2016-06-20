/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.sword.client;

import org.apache.log4j.Logger;
import org.dspace.content.DSpaceObject;
import org.dspace.content.packager.PackageDisseminator;
import org.dspace.content.packager.PackageParameters;
import org.dspace.core.Context;
import org.dspace.handle.factory.HandleServiceFactory;
import org.dspace.handle.service.HandleService;
import org.dspace.sword.client.exceptions.HttpException;
import org.dspace.sword.client.exceptions.InvalidHandleException;
import org.dspace.sword.client.exceptions.PackageFormatException;
import org.dspace.sword.client.exceptions.PackagerException;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.client.Client;
import org.purl.sword.client.PostMessage;
import org.purl.sword.client.SWORDClientException;
import org.purl.sword.client.Status;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.UUID;
import org.dspace.core.factory.CoreServiceFactory;

/**
 * User: Robin Taylor
 * Date: 15/02/11
 * Time: 21:12
 */

public class DSpaceSwordClient
{
    private Client client;
    private PostMessage message;
    private String onBehalfOf;
    private String serviceDocUrl;

    private String filename;
    private String tempDirectory;

    private String packageFormat;
    private PackageParameters pkgParams;

    private static Logger log = Logger.getLogger(DSpaceSwordClient.class);
    protected HandleService handleService = HandleServiceFactory.getInstance().getHandleService();


    public DSpaceSwordClient()
    {
        client = new Client();
        // The default timeout is way too low so increase it x10.
        client.setSocketTimeout(200000);
        client.setUserAgent("DSpace Sword Client");

        message = new PostMessage();
        message.setUseMD5(false);
        message.setChecksumError(false);
        message.setVerbose(false);
        message.setNoOp(false);
        message.setUserAgent("DSpace Sword Client");

        setFilename();
    }


    public void setFilename()
    {
        if ((tempDirectory == null) || (tempDirectory.equals("")))
        {
            tempDirectory = System.getProperty("java.io.tmpdir");
        }

        if (!tempDirectory.endsWith(System.getProperty("file.separator")))
        {
            tempDirectory += System.getProperty("file.separator");
        }

        filename = tempDirectory + UUID.randomUUID().toString();
    }


    public void setRemoteServer(String chosenUrl) throws MalformedURLException
    {
        serviceDocUrl = chosenUrl;
        URL url = new URL(chosenUrl);
        client.setServer(url.getHost(), url.getPort());
    }

    public void setCredentials(String username, String password, String onBehalfOf)
    {
        client.setCredentials(username, password);
        this.onBehalfOf = onBehalfOf;
    }


    public ServiceDocument getServiceDocument() throws HttpException, SWORDClientException
    {
        log.info("Getting Sword Service Document from " + serviceDocUrl);
        ServiceDocument sd = client.getServiceDocument(serviceDocUrl, onBehalfOf);

        Status status = client.getStatus();

        if (status.getCode() == 200)
        {
            log.info("Sword Service Document successfully retrieved from " + serviceDocUrl);
            return sd;
        }
        else
        {
            log.info("Error retrieving Sword Service Document from " + serviceDocUrl);
            throw new HttpException("No service document available - Http status code " + status);
        }
    }


    public void setCollection(String destination)
    {
        message.setDestination(destination);
    }

    public void setFileType(String fileType)
    {
        message.setFiletype(fileType);
    }


    public void setPackageFormat(String packageFormat) throws PackageFormatException
    {
        // todo : Read all this stuff from config
        if (packageFormat.equals("http://purl.org/net/sword-types/METSDSpaceSIP"))
        {
            this.packageFormat = "METS";
            pkgParams = new PackageParameters();
            pkgParams.addProperty("dmd", "MODS");
            message.setFormatNamespace("http://purl.org/net/sword-types/METSDSpaceSIP");
        }
        else
        {
            throw new PackageFormatException("Invalid package format selected");
        }
    }


    public void deposit(Context context, String handle) throws InvalidHandleException, PackagerException, SWORDClientException, PackageFormatException, HttpException
    {
        File file = new File(filename);
        createPackage(context, handle, file);
        sendMessage();
    }

    /**
     * Create the package and write it to disk.
     * @param context session context.
     * @param handle object to be packaged.
     * @param file write the package here.
     * @throws org.dspace.sword.client.exceptions.InvalidHandleException
     *              if handle cannot be resolved.
     * @throws org.dspace.sword.client.exceptions.PackagerException
     *              on error.
     * @throws org.dspace.sword.client.exceptions.PackageFormatException
     *              on unknown package type.
     */
    public void createPackage(Context context, String handle, File file) throws InvalidHandleException, PackagerException, PackageFormatException
    {
        // Note - in the future we may need to allow for more than zipped up packages.

        PackageDisseminator dip = (PackageDisseminator) CoreServiceFactory.getInstance().getPluginService()
                .getNamedPlugin(PackageDisseminator.class, packageFormat);

        if (dip == null)
        {
            log.error("Error - unknown package type " + packageFormat);
            throw new PackageFormatException("Unknown package type " + packageFormat);
        }

        DSpaceObject dso = null;
        try
        {
            dso = handleService.resolveToObject(context, handle);
        }
        catch (SQLException e)
        {
            log.error("Unable to resolve handle " + handle);
            throw new InvalidHandleException("Unable to resolve handle " + handle);
        }

        if (dso == null)
        {
            log.error("Unable to resolve handle " + handle);
            throw new InvalidHandleException("Unable to resolve handle " + handle);
        }

        try
        {
            dip.disseminate(context, dso, pkgParams, file);
        }
        catch (Exception e)
        {
            log.error("Error creating package", e);
            throw new PackagerException("Error creating package", e);
        }
    }


    /**
     * Reads the file, probably a zipped package, and sends it to the Sword server.
     *
     * @return A unique ID returned by a successful deposit
     * @throws org.purl.sword.client.SWORDClientException passed through.
     * @throws org.dspace.sword.client.exceptions.HttpException
     *              on error.
     */
    public String sendMessage() throws SWORDClientException, HttpException
    {
        message.setFilepath(filename);
        DepositResponse resp = client.postFile(message);
        Status status = client.getStatus();

		if ((status.getCode() == 201) || (status.getCode() == 202))
        {
			SWORDEntry se = resp.getEntry();
            return se.getId();
        }
        else
        {
            String error = status.getCode() + " " + status.getMessage() + " - " + resp.getEntry().getSummary().getContent();
			log.info("Error depositing Sword package : " + error);
            throw new HttpException(error);
        }

    }

}
