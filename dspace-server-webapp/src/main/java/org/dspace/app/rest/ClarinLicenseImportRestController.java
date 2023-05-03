/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.dspace.app.rest.utils.ContextUtil.obtainContext;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.BadRequestException;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.logging.log4j.Logger;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.content.service.clarin.ClarinUserRegistrationService;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for import licenses into database.
 * Endpoint: /api/licenses/import/{value}
 * This controller can:
 *     - import labels in json format into database (POST /api/licenses/import/labels)
 *     - import extended mapping in json format - create mapped dictionary (POST /api/licenses/import/extendedMapping)
 *     - import licenses in json format into database (POST /api/licenses/import/licenses)
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
@RestController
@RequestMapping("/api/licenses/import")
public class ClarinLicenseImportRestController {
    private static final Logger log = org.apache.logging.log4j.LogManager
            .getLogger(ClarinLicenseImportRestController.class);
    private Dictionary<Integer, Integer> licenseLabelsIds = new Hashtable<>();
    private Dictionary<Integer, Set<ClarinLicenseLabel>> licenseToLicenseLabel = new Hashtable<>();
    @Autowired
    private ClarinLicenseLabelService clarinLicenseLabelService;
    @Autowired
    private ClarinLicenseService clarinLicenseService;
    @Autowired
    private ClarinUserRegistrationService clarinUserRegistrationService;

    /**
     * This method import labels in json format into database.
     *
     * @param licenseLabels Array of json nodes
     * @param request The response object
     * @param request The request object
     * @return Response entity with status
     * @throws SQLException
     * @throws AuthorizeException
     */
    @RequestMapping(method = RequestMethod.POST, value = "/labels")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity importLicenseLabels(@RequestBody(required = false) List<JsonNode> licenseLabels,
                                              HttpServletRequest request, HttpServletResponse response)
            throws SQLException, AuthorizeException {

        if (Objects.isNull(licenseLabels) || CollectionUtils.isEmpty(licenseLabels)) {
            throw new BadRequestException("The new license labels should be included as " +
                    "json in the body of this request");
        }

        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            return new ResponseEntity<>("Context is null", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<Integer> errors = new ArrayList<>();
        ClarinLicenseLabel licenseLabel;

        for (JsonNode jsonLicenseLabel : licenseLabels) {
            if (jsonLicenseLabel.has("label_id") && !jsonLicenseLabel.get("label_id").isNull()) {
                if (jsonLicenseLabel.has("label") && jsonLicenseLabel.has("title")
                    && jsonLicenseLabel.has("is_extended")) {

                    Integer id = jsonLicenseLabel.get("label_id").asInt();
                    String label = jsonLicenseLabel.get("label").isNull() ?
                            null : jsonLicenseLabel.get("label").asText();
                    String title = jsonLicenseLabel.get("title").isNull() ?
                            null : jsonLicenseLabel.get("title").asText();
                    boolean is_extended = jsonLicenseLabel.get("is_extended").asBoolean();
                    // create
                    licenseLabel = clarinLicenseLabelService.create(context);
                    licenseLabel.setLabel(label);
                    licenseLabel.setTitle(title);
                    licenseLabel.setExtended(is_extended);

                    clarinLicenseLabelService.update(context, licenseLabel);
                    this.licenseLabelsIds.put(id, licenseLabel.getID());
                } else {
                    //if any argument is missing, we create log and move to the next label
                    errors.add(jsonLicenseLabel.get("label_id").asInt());
                }
            } else {
                return new ResponseEntity<>("Label id has to be entered and it cannot be null!",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        context.commit();

        if (errors.isEmpty()) {
            return new ResponseEntity<>("Import License labels were successful", HttpStatus.OK);
        }

        for (Integer id: errors) {
            log.warn("The license label with id: " + id + " had incorrect inputted arguments!");
        }
        return new ResponseEntity<>("License label extended mappings were imported partially!",
                HttpStatus.CONFLICT);
    }

    /**
     * This method mapping extended mappings into dictionary.
     *
     * @param licenseLabelExtendedMappings Array of json nodes
     * @param request The response object
     * @param request The request object
     * @return Response entity with status
     */
    @RequestMapping(method = RequestMethod.POST, value = "/extendedMapping")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity importLicenseLabelExtendedMapping(@RequestBody(required = false) List<JsonNode>
                                                                        licenseLabelExtendedMappings,
                                          HttpServletRequest request, HttpServletResponse response) {

        if (Objects.isNull(licenseLabelExtendedMappings) || CollectionUtils.isEmpty(licenseLabelExtendedMappings)) {
            throw new BadRequestException("The new license label extended mappings should be included as " +
                    "json in the body of this request");
        }

        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            return new ResponseEntity<>("Context is null", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<Integer> errors = new ArrayList<>();

        for (JsonNode jsonLicenseLabelExtendedMapping : licenseLabelExtendedMappings) {
            if (jsonLicenseLabelExtendedMapping.has("license_id") &&
                    !jsonLicenseLabelExtendedMapping.get("license_id").isNull() &&
                    jsonLicenseLabelExtendedMapping.has("label_id") &&
                    !jsonLicenseLabelExtendedMapping.get("label_id").isNull()) {

                Set<ClarinLicenseLabel> licenseLabels = this.licenseToLicenseLabel.get(
                        jsonLicenseLabelExtendedMapping.get("license_id").asInt());
                if (Objects.isNull(licenseLabels)) {
                    licenseLabels = new HashSet<>();
                    this.licenseToLicenseLabel.put(jsonLicenseLabelExtendedMapping.get(
                            "license_id").asInt(), licenseLabels);
                }
                ClarinLicenseLabel clarinLicenseLabel;
                try {
                    Integer licenseLabelID = this.licenseLabelsIds.get(jsonLicenseLabelExtendedMapping.get(
                            "label_id").asInt());
                    if (Objects.isNull(licenseLabelID)) {
                        //if label_id doesn't exist, we create log and move to the next extended mapping
                        errors.add(jsonLicenseLabelExtendedMapping.get("label_id").asInt());
                        continue;
                    }
                    clarinLicenseLabel = clarinLicenseLabelService.find(context,licenseLabelID);
                    if (Objects.isNull(clarinLicenseLabel)) {
                        //if label_id doesn't exist, we create log and move to the next extended mapping
                        errors.add(licenseLabelID);
                        continue;
                    }
                    licenseLabels.add(clarinLicenseLabel);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            } else {
                return new ResponseEntity<>("Label id and license id have to be entered and it cannot be null!",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        if (errors.isEmpty()) {
            return new ResponseEntity<>("Import License label extended mappings were successful!", HttpStatus.OK);
        }

        for (Integer id: errors) {
            log.warn("The extended mapping with label id: " + id + " was not imported! There was not find " +
                    "corresponded label in database!");
        }
        return new ResponseEntity<>("License label extended mappings were imported partially!",
                HttpStatus.CONFLICT);
    }

    /**
     * This method import licenses into database.
     *
     * @param licenses Array of json nodes
     * @param request The response object
     * @param request The request object
     * @return Response entity with status
     * @throws SQLException
     * @throws AuthorizeException
     */
    @RequestMapping(method = RequestMethod.POST, value = "/licenses")
    @PreAuthorize("hasAuthority('ADMIN')")
    public ResponseEntity importLicenses(@RequestBody(required = false) List<JsonNode> licenses,
                                               HttpServletRequest request, HttpServletResponse response)
            throws SQLException, AuthorizeException {

        if (Objects.isNull(licenses) || CollectionUtils.isEmpty(licenses)) {
            throw new BadRequestException("The new licenses should be included as json in the body of this request");
        }

        Context context = obtainContext(request);
        if (Objects.isNull(context)) {
            return new ResponseEntity<>("Context is null", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        ClarinLicense license;
        List<Integer> errors = new ArrayList<>();

        for (JsonNode jsonLicense : licenses) {
            if (jsonLicense.has("license_id") && !jsonLicense.get("license_id").isNull()) {
                if (jsonLicense.has("name") && jsonLicense.has("definition")
                        //&& jsonLicense.has("eperson_id")
                        && jsonLicense.has("label_id") &&
                        jsonLicense.has("confirmation") && jsonLicense.has("required_info")) {

                    Integer id = jsonLicense.get("license_id").asInt();
                    //the name has to be unique too
                    String name = jsonLicense.get("name").isNull() ? null : jsonLicense.get("name").asText();
                    String definition = jsonLicense.get("definition").isNull() ?
                            null : jsonLicense.get("definition").asText();
                    Integer label_id = jsonLicense.get("label_id").isNull() ?
                            null : jsonLicense.get("label_id").asInt();
                    Integer confirmation = jsonLicense.get("confirmation").isNull() ?
                            null : jsonLicense.get("confirmation").asInt();
                    String required_info = jsonLicense.get("required_info").isNull() ?
                            null : jsonLicense.get("required_info").asText();
                    //eperson_id is only require if the license iswas reated by an andmin and not imported
                    String epersonIdString = jsonLicense.get("eperson_id").asText();
                    UUID epersonId = UUID.fromString(epersonIdString);
                    List<ClarinUserRegistration> userRegistrations = clarinUserRegistrationService.findByEPersonUUID(
                            context, epersonId);
                    ClarinUserRegistration userRegistration = userRegistrations.size() > 0 ?
                            userRegistrations.get(0) : null;
                    //TODO
                    //String createdOnString = jsonLicense.get("created_on").asText();

                    if (Objects.nonNull(clarinLicenseService.findByName(context, name))) {
                        errors.add(label_id);
                        continue;
                    }

                    ClarinLicenseLabel label = null;
                    if (Objects.nonNull(label_id) && Objects.nonNull(this.licenseLabelsIds.get(label_id))) {
                        label = this.clarinLicenseLabelService.find(context, this.licenseLabelsIds.get(label_id));
                        label_id = this.licenseLabelsIds.get(label_id);
                    }

                    if (Objects.isNull(label_id) || Objects.isNull(label)) {
                        //if label_id doesn't exist, we create log and move to the next extended mapping
                        errors.add(label_id);
                        continue;
                    }

                    Set<ClarinLicenseLabel> licenseLabels = this.licenseToLicenseLabel.get(id);
                    if (Objects.isNull(licenseLabels)) {
                        licenseLabels = new HashSet<>();
                    }
                    licenseLabels.add(label);

                    license = clarinLicenseService.create(context);
                    license.setName(name);
                    license.setLicenseLabels(licenseLabels);
                    license.setDefinition(definition);
                    license.setEperson(userRegistration);
                    license.setConfirmation(confirmation);
                    license.setRequiredInfo(required_info);

                    clarinLicenseService.update(context, license);
                } else {
                    //if any argument is missing, we create log and move to the next label
                    errors.add(jsonLicense.get("license_id").asInt());
                }
            } else {
                return new ResponseEntity<>("License id has to be entered and it cannot be null!" +
                        " Name has to be unique!",
                        HttpStatus.UNPROCESSABLE_ENTITY);
            }
        }

        context.commit();

        if (errors.isEmpty()) {
            return new ResponseEntity<>("Import licenses were successful", HttpStatus.OK);
        }
        for (Integer id: errors) {
            log.warn("The license with label, which is mapping to label id: " + id + " was not imported! " +
                    "There is not corresponded label in database!");
        }
        return new ResponseEntity<>("License label extended mappings were imported partially!",
                HttpStatus.CONFLICT);
    }
}