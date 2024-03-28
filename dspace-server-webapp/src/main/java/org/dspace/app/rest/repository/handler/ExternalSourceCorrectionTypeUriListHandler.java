/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest.repository.handler;

import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.servlet.http.HttpServletRequest;
import org.dspace.app.rest.exception.DSpaceBadRequestException;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.Context;
import org.dspace.correctiontype.CorrectionType;
import org.dspace.correctiontype.service.CorrectionTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * This class extends the {@link ExternalSourceEntryItemUriListHandler} abstract class and implements it specifically
 * for the List<{@link CorrectionType}> objects.
 * 
 * @author Mohamed Eskander (mohamed.eskander at 4science.com)
 */
@Component
public class ExternalSourceCorrectionTypeUriListHandler extends ExternalSourceEntryItemUriListHandler<CorrectionType> {

    @Autowired
    private CorrectionTypeService correctionTypeService;

    @Override
    @SuppressWarnings("rawtypes")
    public boolean supports(List<String> uriList, String method,Class clazz) {
        return clazz != CorrectionType.class ? false : true;
    }

    @Override
    public CorrectionType handle(Context context, HttpServletRequest request, List<String> uriList)
            throws SQLException, AuthorizeException {
        return getObjectFromUriList(context, uriList);
    }

    @Override
    public boolean validate(Context context, HttpServletRequest request, List<String> uriList)
        throws AuthorizeException {
        return uriList.size() > 1 ? false : true;
    }


    private CorrectionType getObjectFromUriList(Context context, List<String> uriList) {
        CorrectionType correctionType = null;
        String url = uriList.get(0);
        Pattern pattern = Pattern.compile("\\/api\\/config\\/correctiontypes\\/(.*)");
        Matcher matcher = pattern.matcher(url);
        if (!matcher.find()) {
            throw new DSpaceBadRequestException("The uri: " + url + " doesn't resolve to an correction type");
        }
        String id = matcher.group(1);
        try {
            correctionType = correctionTypeService.findOne(id);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        return correctionType;
    }

}