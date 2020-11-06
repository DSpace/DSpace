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
import java.util.Objects;
import java.util.Optional;

import de.undercouch.citeproc.CSL;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link StreamDisseminationCrosswalk} to serialize the given
 * items using the CSL processor with the configured style and producing the result
 * in the given output format.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CSLItemDataCrosswalk implements StreamDisseminationCrosswalk, FileNameDisseminator {

    @Autowired
    private ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    @Autowired
    private ItemService itemService;

    private String mimeType;

    private String style;

    private String format;

    private String fileName;

    private CrosswalkMode crosswalkMode;

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.ITEM && isPublication((Item) dso);
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
                throw new CrosswalkObjectNotSupported("CSLItemDataCrosswalk can only crosswalk a Publication item.");
            }

            dSpaceListItemDataProvider.processItem((Item) dso);
        }

        if (getMIMEType() != null && getMIMEType().startsWith("application/json")) {
            print(out, dSpaceListItemDataProvider.toJson());
        } else {
            CSL citeproc = new CSL(dSpaceListItemDataProvider, style);
            citeproc.setOutputFormat(format);
            citeproc.registerCitationItems(dSpaceListItemDataProvider.getIds());
            print(out, citeproc.makeBibliography().makeString());
        }

    }

    private void print(OutputStream out, String value) {
        try (PrintWriter writer = new PrintWriter(out, true, StandardCharsets.UTF_8)) {
            writer.print(value);
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

    private boolean isPublication(Item item) {
        String relationshipType = itemService.getMetadataFirstValue(item, "relationship", "type", null, Item.ANY);
        return Objects.equals(relationshipType, "Publication");
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public void setStyle(String style) {
        this.style = style;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setCrosswalkMode(CrosswalkMode crosswalkMode) {
        this.crosswalkMode = crosswalkMode;
    }

    public CrosswalkMode getCrosswalkMode() {
        return this.crosswalkMode != null ? this.crosswalkMode : StreamDisseminationCrosswalk.super.getCrosswalkMode();
    }

    @Override
    public Optional<String> getEntityType() {
        return Optional.of("Publication");
    }

}
