/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.metadataSecurity;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Resource;

import org.apache.logging.log4j.Logger;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.app.util.service.MetadataExposureService;
import org.dspace.authorize.service.AuthorizeService;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataValue;
import org.dspace.content.service.ItemService;
import org.dspace.content.service.MetadataSecurityEvaluation;
import org.dspace.core.Context;
import org.dspace.eperson.EPerson;
import org.dspace.layout.CrisLayoutBox;
import org.dspace.layout.CrisLayoutField;
import org.dspace.layout.CrisLayoutFieldMetadata;
import org.dspace.layout.CrisMetadataGroup;
import org.dspace.layout.LayoutSecurity;
import org.dspace.layout.service.CrisLayoutBoxAccessService;
import org.dspace.layout.service.CrisLayoutBoxService;
import org.dspace.services.RequestService;
import org.springframework.beans.factory.annotation.Autowired;

/**
* @author Mykhaylo Boychuk (4science.it)
*/
public class MetadataSecurityService {

    private static final Logger log = org.apache.logging.log4j.LogManager.getLogger(MetadataSecurityService.class);

    @Resource(name = "securityLevelsMap")
    private final Map<String, MetadataSecurityEvaluation> securityLevelsMap = new HashMap<>();

    @Autowired
    private ItemService itemService;

    @Autowired
    private AuthorizeService authorizeService;

    @Autowired
    private CrisLayoutBoxService crisLayoutBoxService;

    @Autowired
    private MetadataExposureService metadataExposureService;

    @Autowired
    private CrisLayoutBoxAccessService crisLayoutBoxAccessService;

    @Autowired
    private RequestService requestService;

    // TODO this method id duplicate of @ItemConverter#getPermissionFilteredMetadata(Context context, Item obj)
    public List<MetadataValue> getPermissionFilteredMetadata(Context context, Item obj, List<MetadataValue> fullList) {

        List<MetadataValue> returnList = new LinkedList<>();
        String entityType = itemService.getMetadataFirstValue(obj, "dspace", "entity", "type", Item.ANY);
        try {
            if (obj.isWithdrawn() && (Objects.isNull(context) ||
                Objects.isNull(context.getCurrentUser()) || !authorizeService.isAdmin(context))) {
                return new ArrayList<MetadataValue>();
            }
            List<CrisLayoutBox> boxes;
            if (context != null) {
                boxes = crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0);
            } else {
                // the context could be null if the converter is used to prepare test data or in a batch script
                boxes = new ArrayList<CrisLayoutBox>();
            }

            Optional<List<DCInputSet>> submissionDefinitionInputs = submissionDefinitionInputs();
            if (submissionDefinitionInputs.isPresent()) {
                return fromSubmissionDefinition(context, boxes, obj, submissionDefinitionInputs.get(), fullList);
            }

            for (MetadataValue metadataValue : fullList) {
                MetadataField metadataField = metadataValue.getMetadataField();
                if (checkMetadataFieldVisibility(context, boxes, obj, metadataField)) {
                    if (metadataValue.getSecurityLevel() != null) {
                        MetadataSecurityEvaluation metadataSecurityEvaluation =
                            mapBetweenSecurityLevelAndClassSecurityLevel( metadataValue.getSecurityLevel());
                        if (metadataSecurityEvaluation.allowMetadataFieldReturn(context, obj ,metadataField)) {
                            returnList.add(metadataValue);
                        }
                    } else {
                        returnList.add(metadataValue);
                    }
                }
            }
        } catch (SQLException e ) {
            log.error("Error filtering item metadata based on permissions", e);
        }
        return returnList;
    }

    // TODO this method id duplicate of @ItemConverter#checkMetadataFieldVisibility(context, item, metadataField)
    public boolean checkMetadataFieldVisibility(Context context, Item item,
            MetadataField metadataField) throws SQLException {
        String entityType = itemService.getMetadataFirstValue(item, "dspace", "entity", "type", Item.ANY);
        List<CrisLayoutBox> boxes = crisLayoutBoxService.findEntityBoxes(context, entityType, 1000, 0);
        return checkMetadataFieldVisibility(context, boxes, item, metadataField);
    }

    private boolean checkMetadataFieldVisibility(Context context, List<CrisLayoutBox> boxes, Item item,
            MetadataField metadataField) throws SQLException {
        if (boxes.size() == 0) {
            if (context != null && authorizeService.isAdmin(context)) {
                return true;
            } else {
                if (!metadataExposureService
                        .isHidden(context, metadataField.getMetadataSchema().getName(),
                                  metadataField.getElement(),
                                  metadataField.getQualifier())) {
                    return true;
                }
            }
        } else {
            return checkMetadataFieldVisibilityByBoxes(context, boxes, item, metadataField);
        }
        return false;
    }

    private boolean checkMetadataFieldVisibilityByBoxes(Context context, List<CrisLayoutBox> boxes, Item item,
            MetadataField metadataField) throws SQLException {
        List<MetadataField> allPublicMetadata = getPublicMetadata(boxes);
        List<CrisLayoutBox> boxesWithMetadataFieldExcludedPublic = getBoxesWithMetadataFieldExcludedPublic(
                metadataField, boxes);
        EPerson currentUser = context.getCurrentUser();
        if (isPublicMetadataField(metadataField, allPublicMetadata)) {
            return true;
        } else if (currentUser != null) {
            for (CrisLayoutBox box : boxesWithMetadataFieldExcludedPublic) {
                if (crisLayoutBoxAccessService.hasAccess(context, currentUser, box, item)) {
                    return true;
                }
            }
        }
        // the metadata is not included in any box so use the default dspace security
        if (boxesWithMetadataFieldExcludedPublic.size() == 0) {
            if (!metadataExposureService
                    .isHidden(context, metadataField.getMetadataSchema().getName(),
                              metadataField.getElement(),
                              metadataField.getQualifier())) {
                return true;
            }
        }

        return false;
    }

    private List<MetadataField> getPublicMetadata(List<CrisLayoutBox> boxes) {
        List<MetadataField> publicMetadata = new ArrayList<MetadataField>();
        for (CrisLayoutBox box : boxes) {
            if (box.getSecurity() == LayoutSecurity.PUBLIC.getValue()) {
                List<CrisLayoutField> crisLayoutFields = box.getLayoutFields();
                for (CrisLayoutField field : crisLayoutFields) {
                    if (field instanceof CrisLayoutFieldMetadata) {
                        publicMetadata.add(field.getMetadataField());
                    }
                }
            }
        }
        return publicMetadata;
    }

    private List<CrisLayoutBox> getBoxesWithMetadataFieldExcludedPublic(MetadataField metadataField,
            List<CrisLayoutBox> boxes) {
        List<CrisLayoutBox> boxesWithMetadataField = new LinkedList<CrisLayoutBox>();
        for (CrisLayoutBox box : boxes) {
            List<CrisLayoutField> crisLayoutFields = box.getLayoutFields();
            for (CrisLayoutField field : crisLayoutFields) {
                if (field instanceof CrisLayoutFieldMetadata) {
                    checkField(metadataField, boxesWithMetadataField, box, field.getMetadataField());
                    for (CrisMetadataGroup metadataGroup : field.getCrisMetadataGroupList()) {
                        checkField(metadataField, boxesWithMetadataField, box, metadataGroup.getMetadataField());
                    }
                }
            }
        }
        return boxesWithMetadataField;
    }

    private void checkField(MetadataField metadataField, List<CrisLayoutBox> boxesWithMetadataField, CrisLayoutBox box,
            MetadataField field) {
        if (field.equals(metadataField) && box.getSecurity() != LayoutSecurity.PUBLIC.getValue()) {
            boxesWithMetadataField.add(box);
        }
    }

    private boolean isPublicMetadataField(MetadataField metadataField, List<MetadataField> allPublicMetadata) {
        for (MetadataField publicField : allPublicMetadata) {
            if (publicField.equals(metadataField)) {
                return true;
            }
        }
        return false;
    }

    private Optional<List<DCInputSet>> submissionDefinitionInputs() {
        return Optional.ofNullable(requestService.getCurrentRequest())
                .map(rq -> (String )rq.getAttribute("submission-name"))
                .map(this::dcInputsSet);
    }

    // private method to catch checked exception that might occur during a lambda call
    private List<DCInputSet> dcInputsSet(final String sd) {
        final DCInputsReader dcInputsReader;
        try {
            dcInputsReader = new DCInputsReader();
            return dcInputsReader.getInputsBySubmissionName(sd);

        } catch (DCInputsReaderException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private List<MetadataValue> fromSubmissionDefinition(Context context, List<CrisLayoutBox> boxes, Item item,
            final List<DCInputSet> dcInputSets, final List<MetadataValue> fullList) {
        Predicate<MetadataValue> inDcInputs = mv -> dcInputSets.stream().anyMatch((dc) -> {
            try {
                return dc.isFieldPresent(mv.getMetadataField().toString('.'))
                        || checkMetadataFieldVisibilityByBoxes(context, boxes, item, mv.getMetadataField());
            } catch (SQLException e) {
                return false;
            }
        });
        return fullList.stream()
                       .filter(inDcInputs)
                       .collect(Collectors.toList());
    }

    public MetadataSecurityEvaluation mapBetweenSecurityLevelAndClassSecurityLevel(int securityValue) {
        return securityLevelsMap.get(securityValue + "");
    }

}