/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.plugins;

import org.apache.commons.lang.StringUtils;
import org.dspace.app.xmlui.aspect.administrative.importer.external.fileaccess.FileAccessUI;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.List;
import org.dspace.app.xmlui.wing.element.Radio;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.DSpaceObject;
import org.dspace.fileaccess.service.FileAccessFromMetadataService;
import org.dspace.core.Context;
import org.dspace.importer.external.scidir.entitlement.ArticleAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.sql.SQLException;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 26/10/15
 * Time: 17:02
 */

public class FileAccesEditBitstreamForm extends EditBitstreamFormAddition {

    @Autowired
    protected FileAccessFromMetadataService fileAccessFromMetadataService;

    @Override
    public void addBodyHook(Context context, DSpaceObject dSpaceObject, List main) throws WingException, SQLException, AuthorizeException {
        ArticleAccess fileAccess = fileAccessFromMetadataService.getFileAccess(context, (Bitstream) dSpaceObject);
        Radio radio = FileAccessUI.addAccessSelection(main, "file-access", fileAccess.getAudience(), false);
        FileAccessUI.addEmbargoDateField(main, fileAccess);

        if("public".equals(fileAccess.getAudience()) && StringUtils.isNotBlank(fileAccess.getStartDate()) && !fileAccess.getStartDate().equals("null")){
            radio.setOptionSelected("embargo");
        }
    }

    @Value("#{T(org.dspace.core.Constants).BITSTREAM}")
    public void setDsoType(int dsoType) {
        super.setDsoType(dsoType);
    }

    @Value("file_access")
    public void setKey(String key) {
        super.setKey(key);
    }

}
