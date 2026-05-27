/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.general;

import static java.util.stream.Collectors.toList;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Collection;
import org.dspace.content.Community;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.BitstreamService;
import org.dspace.content.service.CollectionService;
import org.dspace.content.service.CommunityService;
import org.dspace.content.service.DSpaceObjectService;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataFieldService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Distributive;
import org.dspace.curate.Mutative;

/** Curation task to copy one or multiple source fields into one targed field. The source fields will not be deleted.
 * Multiple values from the same or from multiple fields will be concatenated. A String can be configured to separate
 * each of those values from another. */
@Distributive
@Mutative
public class CopyFieldsCurationTask extends AbstractCurationTask {

    protected static final String CFG_PREFIX = "curate.";
    private Logger log;
    private static final BitstreamService bitstreamService = ContentServiceFactory.getInstance().getBitstreamService();
    private static final ItemService itemService = ContentServiceFactory.getInstance().getItemService();
    private static final CollectionService collectionService =
            ContentServiceFactory.getInstance().getCollectionService();
    private static final CommunityService communityService = ContentServiceFactory.getInstance().getCommunityService();
    private static final MetadataFieldService metadataFieldService =
            ContentServiceFactory.getInstance().getMetadataFieldService();

    /** Integers as defined in org.dspace.core.Constants of DSpaaceObjectTypes on which the curration task should be
     * performed. All types not included in the list will be skipped. Currently supported types are bitstream,
     * item, collection, community and site.
     */
    private List<Integer> supportedDSpaceObjectTypes = new ArrayList<>(5);
    /** List of fields to concatenate, field values will be concatenated in the list's order. */
    private List<MetadataField> sourceFields;
    /** Shall we delete the source fields after copying them? Default set to false. */
    private boolean cleanSourceFields = false;
    /** Whether to skip objects that miss any of the fields listed as source fields. Default set to true. */
    private boolean skipOnMissingFields = true;
    /** Field to store the concatenated values. */
    private MetadataField targetField;
    /** If set true, we remove the target field, before adding it with the concatenate values. Default set to false. */
    private boolean cleanTargetField = false;
    /** String to be put between the values, e.g. ", " or " ". */
    private String concatenator = "";
    /** set to true, to add the concatenator at the end of the concatenated values. Default set to false.*/
    private boolean concatenatorAtTheEnd = false;

    /** Set the list of fields to take as the source of this curation task. Field values will be concatenated in the
     *  list's order. */
    protected void setSourceFields(List<String> sourceFields) {
        this.sourceFields = new ArrayList<>(sourceFields.size());
        for (String field : sourceFields) {
            try {
                MetadataField mf = metadataFieldService.findByString(Curator.curationContext(), field, '.');
                if (mf != null) {
                    log.debug("Config: add " + field + " to the list of source fields.");
                    this.sourceFields.add(mf);
                } else {
                    log.warn("Unable to find metadatafield " + field);
                    throw new IllegalArgumentException("Cannot load metadata field " + field + ".");
                }
            } catch (SQLException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /** Set the field to store the concatenated values. **/
    protected void setTargetField(String targetField) {
        try {
            this.targetField = metadataFieldService.findByString(Curator.curationContext(), targetField, '.');
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        if (this.targetField == null) {
            log.warn("unable to find field " + targetField + ".");
            throw new IllegalArgumentException("Cannot load metadata field " + targetField + ".");
        }
    }

    @Override
    public void init(Curator curator, String taskId) throws IOException {
        log = LogManager.getLogger("org.dspace.ctask.general.CopyFieldsCurationTask:" + taskId);
        super.init(curator, taskId);
        // reset types before adding types
        this.supportedDSpaceObjectTypes = new ArrayList<>(5);
        String[] types = configurationService.getArrayProperty(CFG_PREFIX + taskId + ".types",
                new String[] {"Bitstream", "Item", "Collection", "Community", "Site"});
        for (String type : types) {
            if (StringUtils.equalsIgnoreCase(type, "Site") &&
                    !this.supportedDSpaceObjectTypes.contains(Constants.SITE)) {
                this.supportedDSpaceObjectTypes.add(Constants.SITE);
            }
            if (StringUtils.equalsIgnoreCase(type, "Community") &&
                    !this.supportedDSpaceObjectTypes.contains(Constants.COMMUNITY)) {
                this.supportedDSpaceObjectTypes.add(Constants.COMMUNITY);
            }
            if (StringUtils.equalsIgnoreCase(type, "Collection") &&
                    !this.supportedDSpaceObjectTypes.contains(Constants.COLLECTION)) {
                this.supportedDSpaceObjectTypes.add(Constants.COLLECTION);
            }
            if (StringUtils.equalsIgnoreCase(type, "Item") &&
                    !this.supportedDSpaceObjectTypes.contains(Constants.ITEM)) {
                this.supportedDSpaceObjectTypes.add(Constants.ITEM);
            }
            if (StringUtils.equalsIgnoreCase(type, "Bitstream") &&
                    !this.supportedDSpaceObjectTypes.contains(Constants.BITSTREAM)) {
                this.supportedDSpaceObjectTypes.add(Constants.BITSTREAM);
            }
        }
        this.setSourceFields(Arrays.asList(configurationService.getArrayProperty(
                CFG_PREFIX + taskId + ".sourceFields")));
        this.cleanSourceFields = configurationService.getBooleanProperty(CFG_PREFIX + taskId +
                ".cleanSourceFields", false);
        this.skipOnMissingFields = configurationService.getBooleanProperty(CFG_PREFIX + taskId +
                ".skipOnMissingFields", true);
        String key = CFG_PREFIX + taskId + ".targetField";
        String target = configurationService.getProperty(key);
        if (StringUtils.isBlank(target)) {
            throw new RuntimeException("Key: " + key + ", not found in config.");
        }
        this.setTargetField(target);
        this.cleanTargetField = configurationService.getBooleanProperty(CFG_PREFIX + taskId +
                ".cleanTargetField", false);
        if (configurationService.hasProperty(CFG_PREFIX + taskId + ".concatenator")) {
            // the concatenator might be set to a space, get the raw value, don't trim it.
            // remove the first and last character, as we expect this configuration to use double or single quotes.
            // using quotes around configuration values is not normal in DSpace, but this was the only workaround
            // to be able to set one space as concatenator.
            // Please remind yourself to escape commas for Apache commons config not interpreting it as an array.
            // Often used values are "", " ", or "\, ".
            Object concatenatorProperty =
                    configurationService.getPropertyValue(CFG_PREFIX + taskId + ".concatenator");
            log.debug("Concatenator raw value: |" + concatenatorProperty + "|");
            if (concatenatorProperty instanceof ArrayList) {
                log.warn("It looks like " + CFG_PREFIX + taskId + ".concatenator contains not escaped commas. " +
                        "Please escape commas like '\\,'.");
            }
            this.concatenator = StringUtils.substring((String) concatenatorProperty, 1, -1);
            log.debug("concatenator set to |" + this.concatenator + "|");
        }
        this.concatenatorAtTheEnd = configurationService.getBooleanProperty(CFG_PREFIX + taskId +
                ".cocatenatorAtTheEnd", false);

        if (this.sourceFields == null || this.sourceFields.isEmpty()) {
            throw new IllegalStateException("List of source fields is not set.");
        }
        if (this.targetField == null) {
            throw new IllegalStateException("Target field is not set.");
        }
    }

    @Override
    protected void distribute(DSpaceObject dso) throws IOException {
        Context context = null;
        try {
            context = Curator.curationContext();
        } catch (SQLException ex) {
            throw new RuntimeException(ex);
        }
        // if this is an item, and we should run also on bitstreams, tread them first, as
        // AbstractCurationTask.distribute will ignore them.
        if (dso instanceof Item && this.supportedDSpaceObjectTypes.contains(Constants.BITSTREAM)) {
            List<Bitstream> bitstreams = ((Item) dso).getBundles(Constants.CONTENT_BUNDLE_NAME).stream()
                    .flatMap(b -> b.getBitstreams().stream()).collect(toList());
            for (Bitstream bitstream : bitstreams) {
                log.debug("Found bitstream " + bitstream.getName() + " (" + bitstream.getID().toString() +
                        ") on item " + dso.getHandle() + " (" + dso.getID().toString() + ").");
                try {
                    performObject(bitstream);
                    context.uncacheEntity(bitstream);
                } catch (SQLException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
        // then run AbstractCurationTask.distribute on the dspace object, to copy fields on any other DSpaceObject,#
        // including the item itself.
        super.distribute(dso);
    }

    @Override
    public int perform(DSpaceObject dso) throws IOException {
        // run recursively
        this.distribute(dso);
        return Curator.CURATE_SUCCESS;
    }

    @Override
    protected void performObject(DSpaceObject dso) throws SQLException, IOException {
        if (!this.supportedDSpaceObjectTypes.contains(dso.getType())) {
            return;
        }
        Context context = Curator.curationContext();
        DSpaceObjectService dSpaceObjectService = null;
        if (dso instanceof Bitstream) {
            dSpaceObjectService = this.bitstreamService;
        }
        if (dso instanceof Item) {
            dSpaceObjectService = this.itemService;
        }
        if (dso instanceof Collection) {
            dSpaceObjectService = this.collectionService;
        }
        if (dso instanceof Community) {
            dSpaceObjectService = this.communityService;
        }
        assert dSpaceObjectService != null;

        StringBuilder concatenatedValue = new StringBuilder();
        for (MetadataField field : this.sourceFields) {
            List<MetadataValue> values = dSpaceObjectService.getMetadata(dso,
                    field.getMetadataSchema().getName(), field.getElement(), field.getQualifier(), Item.ANY);
            if (values == null || values.isEmpty()) {
                if (this.skipOnMissingFields) {
                    log.debug("Skipping " + Constants.typeText[dso.getType()] + " " + dso.getHandle() + " (" +
                            dso.getID().toString() + ") as " + field.toString('.') +
                            " doesn't exist.");
                    return;
                } else {
                    log.debug("Skipping field " + field.toString('.') + " on " +
                            Constants.typeText[dso.getType()] + " " + dso.getHandle() + " (" +
                            dso.getID().toString() + ").");
                    continue;
                }
            }
            assert values != null && !(values.isEmpty());

            for (MetadataValue value : values) {
                if (!concatenatedValue.isEmpty()) {
                    concatenatedValue.append(this.concatenator);
                }
                concatenatedValue.append(value.getValue());
            }
        }
        if (concatenatedValue.isEmpty()) {
            log.debug("No values found to copy.");
            return;
        }
        if (this.concatenatorAtTheEnd) {
            concatenatedValue.append(concatenator);
        }
        try {
            log.debug("Going to add '" + concatenatedValue.toString() + "' to  " +
                    this.targetField.toString('.') + ".");
            // Check if the value exists.
            List<MetadataValue> existingValues = dSpaceObjectService.getMetadata(dso,
                    this.targetField.getMetadataSchema().getName(), this.targetField.getElement(),
                    this.targetField.getQualifier(), Item.ANY);
            for (MetadataValue existingValue : existingValues) {
                if (existingValue.getValue().equals(concatenatedValue.toString())) {
                    log.debug("This value already exists, skipping.");
                    return;
                }
            }
            if (this.cleanTargetField) {
                log.debug("deleting " + targetField.toString('.') + " on " + dso.getID().toString() + ".");
                dSpaceObjectService.clearMetadata(context, dso, targetField.getMetadataSchema().getName(),
                        targetField.getElement(), targetField.getQualifier(), Item.ANY);
            }
            if (this.cleanSourceFields) {
                for (MetadataField field : this.sourceFields) {
                    dSpaceObjectService.clearMetadata(context, dso, field.getMetadataSchema().getName(),
                            field.getElement(), field.getQualifier(), Item.ANY);
                }
            }
            log.debug("Adding " + this.targetField.toString('.') + " = '"
                    + concatenatedValue.toString() + " to " + Constants.typeText[dso.getType()] + " "
                    + dso.getHandle() + " (" + dso.getID().toString() + ").");
            dSpaceObjectService.addMetadata(context, dso, this.targetField, null,
                    concatenatedValue.toString());
            dSpaceObjectService.update(context, dso);
        } catch (SQLException | AuthorizeException ex) {
            throw new RuntimeException(ex);
        }
    }
}
