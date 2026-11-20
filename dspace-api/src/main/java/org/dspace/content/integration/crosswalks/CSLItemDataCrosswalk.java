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
import java.util.Optional;

import org.dspace.authorize.AuthorizeException;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkMode;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.integration.crosswalks.csl.CSLGeneratorFactory;
import org.dspace.content.integration.crosswalks.csl.CSLResult;
import org.dspace.content.integration.crosswalks.csl.DSpaceListItemDataProvider;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Implementation of {@link ItemExportCrosswalk} to serialize the given items
 * using the CSL processor with the configured style and producing the result in
 * the given output format.
 *
 * @author Luca Giamminonni (luca.giamminonni at 4science.it)
 *
 */
public class CSLItemDataCrosswalk implements ItemExportCrosswalk {

    @Autowired
    private ObjectFactory<DSpaceListItemDataProvider> dSpaceListItemDataProviderObjectFactory;

    @Autowired
    private ItemService itemService;

    @Autowired
    private CSLGeneratorFactory cslGeneratorFactory;

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

        DSpaceListItemDataProvider itemDataProvider = createItemDataProvider(context, dsoIterator);

        if (isJsonMimeType()) {
            print(out, itemDataProvider.toJson());
            return;
        }

        CSLResult result = cslGeneratorFactory.getCSLGenerator().generate(itemDataProvider, style, format);
        if (result != null) {
            print(out, result.getCitation());
        }

    }

    private DSpaceListItemDataProvider createItemDataProvider(Context context,
        Iterator<? extends DSpaceObject> dsoIterator) throws CrosswalkObjectNotSupported {

        DSpaceListItemDataProvider dSpaceListItemDataProvider = getDSpaceListItemDataProviderInstance();

        int itemCount = 0;
        while (dsoIterator.hasNext()) {
            DSpaceObject dso = dsoIterator.next();
            dSpaceListItemDataProvider.setCitationLanguage(
                    itemService.getMetadataFirstValue((Item)dso, "dc", "language", "iso",
                            Item.ANY));
            if (!canDisseminate(context, dso)) {
                throw new CrosswalkObjectNotSupported("CSLItemDataCrosswalk can only crosswalk a Publication item.");
            }
            itemCount++;
            dSpaceListItemDataProvider.processItem((Item) dso);
        }
        if (itemCount > 1) {
            dSpaceListItemDataProvider.setCitationLanguage(null);
        }
        return dSpaceListItemDataProvider;
    }

    private boolean isJsonMimeType() {
        return getMIMEType() != null && getMIMEType().startsWith("application/json");
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
        return "Publication".equals(itemService.getEntityType(item));
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
        return Optional.ofNullable(this.crosswalkMode).orElse(ItemExportCrosswalk.super.getCrosswalkMode());
    }

    @Override
    public Optional<String> getEntityType() {
        return Optional.of("Publication");
    }

}
