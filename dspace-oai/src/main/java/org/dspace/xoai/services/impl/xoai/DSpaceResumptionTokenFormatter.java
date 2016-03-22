/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.xoai.services.impl.xoai;

import com.lyncode.xoai.dataprovider.core.ResumptionToken;
import com.lyncode.xoai.dataprovider.exceptions.BadResumptionToken;
import com.lyncode.xoai.dataprovider.services.api.ResumptionTokenFormatter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.dspace.xoai.util.DateUtils;

import java.util.Date;


public class DSpaceResumptionTokenFormatter implements ResumptionTokenFormatter {
    private static Logger log = LogManager
            .getLogger(DSpaceResumptionTokenFormatter.class);

    public DSpaceResumptionTokenFormatter() {
        // TODO Auto-generated constructor stub
    }

    @Override
    public ResumptionToken parse(String resumptionToken) throws BadResumptionToken {
        if (resumptionToken == null) return new ResumptionToken();
        String[] res = resumptionToken.split("/", -1);
        if (res.length != 5) throw new BadResumptionToken();
        else {
            try {
                int offset = Integer.parseInt(res[4]);
                String prefix = (res[0].equals("")) ? null : res[0];
                String set = (res[3].equals("")) ? null : res[3];
                Date from = (res[1].equals("")) ? null : DateUtils.parse(res[1]);
                Date until = res[2].equals("") ? null : DateUtils.parse(res[2]);
                return new ResumptionToken(offset, prefix, set, from, until);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new BadResumptionToken();
            }
        }
    }


    @Override
    public String format(ResumptionToken resumptionToken) {
        String result = "";
        if (resumptionToken.hasMetadataPrefix())
            result += resumptionToken.getMetadataPrefix();
        result += "/";
        if (resumptionToken.hasFrom())
            result += DateUtils.format(resumptionToken.getFrom());
        result += "/";
        if (resumptionToken.hasUntil())
            result += DateUtils.format(resumptionToken.getUntil());
        result += "/";
        if (resumptionToken.hasSet())
            result += resumptionToken.getSet();
        result += "/";
        result += resumptionToken.getOffset();
        return result;
    }

}
