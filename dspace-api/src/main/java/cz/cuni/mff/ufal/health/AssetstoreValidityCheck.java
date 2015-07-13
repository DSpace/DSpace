/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 *
 * by lindat-dev team
 */
package cz.cuni.mff.ufal.health;

import cz.cuni.mff.ufal.dspace.IOUtils;
import org.dspace.core.ConfigurationManager;
import org.dspace.health.Check;
import org.dspace.health.ReportInfo;

import java.io.File;

public class AssetstoreValidityCheck extends Check {

    @Override
    public String run( ReportInfo ri ) {
        String ret = "";
        String dspace_dir = ConfigurationManager.getProperty("dspace.dir");
        String assetstore_dir = dspace_dir + "/assetstore";
        ret += IOUtils.run(new File(dspace_dir + "/bin/validators/assetstore/"),
            new String[]{"python", "main.py",
                "--dir=" + assetstore_dir});
        return ret;
    }

}
