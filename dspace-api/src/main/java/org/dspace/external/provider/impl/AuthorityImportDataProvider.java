/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.external.provider.impl;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.dspace.authority.factory.AuthorityServiceFactory;
import org.dspace.authority.filler.AuthorityImportFiller;
import org.dspace.authority.filler.AuthorityImportFillerService;
import org.dspace.authority.filler.ItemMetadataImportFiller;
import org.dspace.authority.service.AuthorityValueService;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.dto.MetadataValueDTO;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.Context;
import org.dspace.external.model.ExternalDataObject;
import org.dspace.external.provider.AbstractExternalDataProvider;
import org.dspace.util.UUIDUtils;

/**
 * This class allows to configure a Live Import Provider as an External Data Provider
 *
 * @author Giuseppe Digilio (giuseppe.digilio at 4science.it)
 *
 */
public class AuthorityImportDataProvider extends AbstractExternalDataProvider {

    private final AuthorityImportFillerService authorityImportFillerService = AuthorityServiceFactory
        .getInstance()
        .getAuthorityImportFillerService();

    private final ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    /**
     * An unique human readable identifier for this provider
     */
    private String sourceIdentifier;

    private String supportedAuthority;

    private String authorityMetadata;

    @Override
    public String getSourceIdentifier() {
        return sourceIdentifier;
    }

    /**
     * This method set the SourceIdentifier for the ExternalDataProvider
     *
     * @param sourceIdentifier The UNIQUE sourceIdentifier to be set on any LiveImport data provider
     */
    public void setSourceIdentifier(String sourceIdentifier) {
        this.sourceIdentifier = sourceIdentifier;
    }

    public String getSupportedAuthority() {
        return supportedAuthority;
    }

    public void setSupportedAuthority(String supportedAuthority) {
        this.supportedAuthority = supportedAuthority;
    }

    public String getAuthorityMetadata() {
        return authorityMetadata;
    }

    public void setAuthorityMetadata(String authorityMetadata) {
        this.authorityMetadata = authorityMetadata;
    }

    @Override
    public Optional<ExternalDataObject> getExternalDataObject(String id) {
        ExternalDataObject externalDataObject = constructExternalDataObject(id);
        return Optional.of(externalDataObject);
    }

    @Override
    public boolean supports(String source) {
        return StringUtils.equalsIgnoreCase(sourceIdentifier, source);
    }

    @Override
    public List<ExternalDataObject> searchExternalDataObjects(String query, int start, int limit) {
        List<ExternalDataObject> results = new ArrayList<>();
        return results;
    }

    @Override
    public int getNumberOfResults(String query) {
        return 0;
    }

    private ExternalDataObject constructExternalDataObject(String id) {
        Context context = new Context();
        context.getCurrentUser();
        ExternalDataObject externalDataObject = new ExternalDataObject();
        String[] split = StringUtils.split(id, AuthorityValueService.SPLIT);

        if (StringUtils.isBlank(split[0])) {
            context.close();
            throw new IllegalArgumentException("Invalid UUID given");
        }

        if (StringUtils.isBlank(split[1])) {
            context.close();
            throw new IllegalArgumentException("Invalid place given");
        }

        UUID itemUUID = UUIDUtils.fromString(split[0]);
        Integer place = Integer.parseInt(split[1]);
        Item item;
        MetadataValue metadata;
        List<MetadataValue> itemMetadataList = new ArrayList<MetadataValue>();
        List<MetadataValueDTO> metadataDTOList = new ArrayList<MetadataValueDTO>();

        try {
            item = itemService.find(context, itemUUID);
        } catch (SQLException e) {
            throw new IllegalArgumentException("No record found for given UUID " + itemUUID);
        }

        itemMetadataList = itemService.getMetadataByMetadataString(item, authorityMetadata);
        try {
            metadata = itemMetadataList.get(place);
        } catch (Exception e) {
            throw new IllegalArgumentException("No metadata found for given place " + place);
        }

        AuthorityImportFiller filler = authorityImportFillerService.getAuthorityImportFillerByMetadata(metadata);
        metadataDTOList = filler.getMetadataListByRelatedItemAndMetadata(context, item, metadata);

        externalDataObject.setSource(sourceIdentifier);
        externalDataObject.setId(id);
        String title = null;
        for (MetadataValueDTO metadataValueDTO : metadataDTOList) {
            if (metadataValueDTO.getSchema().equals("dc") && metadataValueDTO.getElement().equals("title") &&
                metadataValueDTO.getQualifier() == null) {
                title = metadataValueDTO.getValue();
            }
            if (StringUtils.isNotBlank(metadataValueDTO.getValue())
                && !ItemMetadataImportFiller.isPlaceholderMetadataValue(metadataValueDTO.getValue())) {
                externalDataObject.addMetadata(metadataValueDTO);
            }
        }
        externalDataObject.setValue(title);
        externalDataObject.setDisplayValue(title);
        context.close();

        return externalDataObject;
    }
}
