/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package org.dspace.core;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.dspace.core.service.LicenseService;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Encapsulate the deposit license.
 *
 * @author mhwood
 */
public class LicenseServiceImpl implements LicenseService
{
    private final Logger log = LoggerFactory.getLogger(LicenseServiceImpl.class);

    /** The default license */
    protected String license;

    protected LicenseServiceImpl()
    {

    }

    @Override
    public void writeLicenseFile(String licenseFile,
            String newLicense)
    {
        try
        {
            FileOutputStream fos = new FileOutputStream(licenseFile);
            OutputStreamWriter osr = new OutputStreamWriter(fos, "UTF-8");
            PrintWriter out = new PrintWriter(osr);
            out.print(newLicense);
            out.close();
        } catch (IOException e)
        {
            log.warn("license_write: " + e.getLocalizedMessage());
        }
        license = newLicense;
    }

    @Override
    public String getLicenseText(String licenseFile)
    {
        InputStream is = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        try
        {
            is = new FileInputStream(licenseFile);
            ir = new InputStreamReader(is, "UTF-8");
            br = new BufferedReader(ir);
            String lineIn;
            license = "";
            while ((lineIn = br.readLine()) != null)
            {
                license = license + lineIn + '\n';
            }
        } catch (IOException e)
        {
            log.error("Can't load configuration", e);
            throw new IllegalStateException("Failed to read default license.", e);
        } finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                } catch (IOException ioe)
                {
                }
            }
            if (ir != null)
            {
                try
                {
                    ir.close();
                }
                catch (IOException ioe)
                {
                }
            }
            if (is != null)
            {
                try
                {
                    is.close();
                } catch (IOException ioe)
                {
                }
            }
        }
        return license;
    }

    /**
     * Get the site-wide default license that submitters need to grant
     *
     * @return the default license
     */
    @Override
    public String getDefaultSubmissionLicense()
    {
        if (null == license)
        {
            init();
        }
        return license;
    }

    /**
     * Load in the default license.
     */
    protected void init()
    {
        File licenseFile = new File(DSpaceServicesFactory.getInstance().getConfigurationService().getProperty("dspace.dir")
                + File.separator + "config" + File.separator + "default.license");

        FileInputStream  fir = null;
        InputStreamReader ir = null;
        BufferedReader br = null;
        try
        {

            fir = new FileInputStream(licenseFile);
            ir = new InputStreamReader(fir, "UTF-8");
            br = new BufferedReader(ir);
            String lineIn;
            license = "";

            while ((lineIn = br.readLine()) != null)
            {
                license = license + lineIn + '\n';
            }

            br.close();

        }
        catch (IOException e)
        {
            log.error("Can't load license: " + licenseFile.toString() , e);

            // FIXME: Maybe something more graceful here, but with the
            // configuration we can't do anything
            throw new IllegalStateException("Cannot load license: "
                    + licenseFile.toString(),e);
        }
        finally
        {
            if (br != null)
            {
                try
                {
                    br.close();
                }
                catch (IOException ioe)
                {
                }
            }

            if (ir != null)
            {
                try
                {
                    ir.close();
                }
                catch (IOException ioe)
                {
                }
            }

            if (fir != null)
            {
                try
                {
                    fir.close();
                }
                catch (IOException ioe)
                {
                }
            }
        }
    }
}
