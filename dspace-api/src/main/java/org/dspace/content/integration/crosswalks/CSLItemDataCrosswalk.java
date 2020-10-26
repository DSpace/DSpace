/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Iterator;

import de.undercouch.citeproc.CSL;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.ObjectFactory;

/**
 * Implementation of {@link StreamDisseminationCrosswalk} to serialize the given
 * items using the CSL processor with the configured style and producing the result
 * in the given output format.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CSLItemDataCrosswalk implements StreamDisseminationCrosswalk, FileNameDisseminator {

    private ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    private final String mimeType;

    private final String style;

    private final String format;

    private final String fileName;

    public CSLItemDataCrosswalk(ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory,
        String mimeType, String style, String format, String fileName) {
        this.dSpaceListItemDataProviderObjectFactory = dSpaceListItemDataProviderObjectFactory;
        this.mimeType = mimeType;
        this.style = style;
        this.format = format;
        this.fileName = fileName;
    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.ITEM;
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {
        this.disseminate(context, Arrays.asList(dso).iterator(), out);
    }

    @Override
    public void disseminate(Context context, Iterator<? extends DSpaceObject> dsoIterator, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        DSpaceListItemDataProvider dSpaceListItemDataProvider = getDSpaceListItemDataProviderInstance();

        while (dsoIterator.hasNext()) {
            DSpaceObject dso = dsoIterator.next();

            if (!canDisseminate(context, dso)) {
                throw new CrosswalkObjectNotSupported("CSLItemDataCrosswalk can only crosswalk an Item.");
            }

            dSpaceListItemDataProvider.processItem((Item) dso);

        }

        CSL citeproc = new CSL(dSpaceListItemDataProvider, style);
        citeproc.setOutputFormat(format);
        citeproc.registerCitationItems(dSpaceListItemDataProvider.getIds());

        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.print(citeproc.makeBibliography().makeString());
        }

    }

    @Override
    public String getMIMEType() {
        return mimeType;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    private DSpaceListItemDataProvider getDSpaceListItemDataProviderInstance() {
        return dSpaceListItemDataProviderObjectFactory.getObject();
    }

}
