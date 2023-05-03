/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.rest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.BufferedReader;
import java.io.FileReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.dspace.app.rest.converter.ClarinLicenseConverter;
import org.dspace.app.rest.converter.ClarinLicenseLabelConverter;
import org.dspace.app.rest.test.AbstractControllerIntegrationTest;
import org.dspace.authorize.AuthorizeException;
import org.dspace.builder.ClarinUserRegistrationBuilder;
import org.dspace.content.clarin.ClarinLicense;
import org.dspace.content.clarin.ClarinLicenseLabel;
import org.dspace.content.clarin.ClarinUserRegistration;
import org.dspace.content.service.clarin.ClarinLicenseLabelService;
import org.dspace.content.service.clarin.ClarinLicenseService;
import org.dspace.core.Context;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;


/**
 * Tests for import license controller.
 *
 * @author Michaela Paurikova (michaela.paurikova at dataquest.sk)
 */
public class ClarinLicenseImportControllerIT extends AbstractControllerIntegrationTest {

    private final static String LICENSE_LABELS = "test/json_data_import_licenses/jm.license_label.json";
    private final static String EXTENDED_MAPPINGS =
            "test/json_data_import_licenses/jm.license_label_extended_mapping.json";
    private final static String LICENSES = "test/json_data_import_licenses/jm.license_definition.json";
    private final static String LICENSE_LABELS_TEST = "test/json_data_import_licenses/jm.license_label_test.json";
    private final static String EXTENDED_MAPPINGS_TEST =
            "test/json_data_import_licenses/jm.license_label_extended_mapping_test.json";
    private final static String LICENSES_TEST = "test/json_data_import_licenses/jm.license_definition_test.json";

    private final static String URL_LICENSE_LABEL = "/api/licenses/import/labels";
    private final static String URL_EXTENDED_MAPPING = "/api/licenses/import/extendedMapping";
    private final static String URL_LICENSE = "/api/licenses/import/licenses";
    private JsonNodeFactory jsonNodeFactory = new JsonNodeFactory(true);

    @Autowired
    private ClarinLicenseLabelService clarinLicenseLabelService;

    @Autowired
    private ClarinLicenseService clarinLicenseService;

    @Autowired
    private ClarinLicenseConverter clarinLicenseConverter;

    @Autowired
    private ClarinLicenseLabelConverter clarinLicenseLabelConverter;

    private Dictionary<String, ClarinLicenseLabel> licenseLabelDictionary = new Hashtable<>();
    private Dictionary<Integer, ClarinLicenseLabel> licenseLabelIDDictionary = new Hashtable<>();
    private Dictionary<String, ClarinLicense> licenseDictionary = new Hashtable<>();
    private Dictionary<Integer, Set<Integer>> extendedMappingDictionary = new Hashtable<>();

    @Test
    public void importLicensesTest() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration userRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSE_LABELS)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        ClarinLicenseLabel clarinLicenseLabel;
        List<JsonNode> nodes = new ArrayList<>();
        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("label_id", jsonNodeFactory.textNode(jsonObject.get("label_id").toString()));
            node.set("label", jsonNodeFactory.textNode(jsonObject.get("label").toString()));
            node.set("title", jsonNodeFactory.textNode(jsonObject.get("title").toString()));
            node.set("is_extended", jsonNodeFactory.textNode(jsonObject.get("is_extended").toString()));
            nodes.add(node);

            //for test control
            clarinLicenseLabel = new ClarinLicenseLabel();
            clarinLicenseLabel.setId(Integer.parseInt(jsonObject.get("label_id").toString()));
            clarinLicenseLabel.setLabel(jsonObject.get("label").toString());
            clarinLicenseLabel.setTitle(jsonObject.get("title").toString());
            clarinLicenseLabel.setExtended(Boolean.parseBoolean(jsonObject.get("is_extended").toString()));
            licenseLabelDictionary.put(clarinLicenseLabel.getLabel(), clarinLicenseLabel);
            licenseLabelIDDictionary.put(clarinLicenseLabel.getID(), clarinLicenseLabel);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_LICENSE_LABEL)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().isOk());

        //extendedMapping
        bufferReader = new BufferedReader(new FileReader(getClass().getResource(EXTENDED_MAPPINGS).getFile()));
        nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("mapping_id", jsonNodeFactory.textNode(jsonObject.get("mapping_id").toString()));
            node.set("license_id", jsonNodeFactory.textNode(jsonObject.get("license_id").toString()));
            node.set("label_id", jsonNodeFactory.textNode(jsonObject.get("label_id").toString()));
            if (Objects.isNull(extendedMappingDictionary.get(Integer.parseInt(jsonObject.get("license_id")
                    .toString())))) {
                extendedMappingDictionary.put(Integer.parseInt(jsonObject.get("license_id").toString()),
                        new HashSet<>());
            }
            extendedMappingDictionary.get(Integer.parseInt(jsonObject.get("license_id").toString())).add(
                    Integer.parseInt(jsonObject.get("label_id").toString()));
            nodes.add(node);
        }

        getClient(adminToken).perform(post(URL_EXTENDED_MAPPING)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().isOk());

        //licenses
        bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSES).getFile()));
        ClarinLicense license;
        nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("license_id", jsonNodeFactory.textNode(jsonObject.get("license_id").toString()));
            node.set("name", jsonNodeFactory.textNode(jsonObject.get("name").toString()));
            node.set("definition", jsonNodeFactory.textNode(jsonObject.get("definition").toString()));
            node.set("eperson_id", jsonNodeFactory.textNode(admin.getID().toString()));
            node.set("label_id", jsonNodeFactory.textNode(jsonObject.get("label_id").toString()));
            node.set("confirmation", jsonNodeFactory.textNode(jsonObject.get("confirmation").toString()));
            node.set("required_info", jsonNodeFactory.textNode(Objects.isNull(jsonObject.get("required_info")) ?
                    null : jsonObject.get("required_info").toString()));
            nodes.add(node);

            //for test control
            license = new ClarinLicense();
            license.setId(Integer.parseInt(jsonObject.get("license_id").toString()));
            license.setName(jsonObject.get("name").toString());
            license.setDefinition(jsonObject.get("definition").toString());
            license.setEperson(userRegistration);
            Set<ClarinLicenseLabel> labels = new HashSet<>();
            labels.add(this.licenseLabelIDDictionary.get(Integer.parseInt(jsonObject.get("label_id").toString())));
            license.setLicenseLabels(labels);
            license.setConfirmation(Integer.parseInt(jsonObject.get("confirmation").toString()));
            license.setRequiredInfo(Objects.isNull(jsonObject.get("required_info")) ?
                    null : jsonObject.get("required_info").toString());
            licenseDictionary.put(license.getName(), license);
            if (Objects.isNull(extendedMappingDictionary.get(license.getID()))) {
                extendedMappingDictionary.put(license.getID(), new HashSet<>());
            }
            extendedMappingDictionary.get(license.getID()).add(license.getLicenseLabels().get(0).getID());
        }

        getClient(adminToken).perform(post(URL_LICENSE)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().isOk());

        //check
        context.turnOffAuthorisationSystem();
        List<ClarinLicense> clarinLicenses = this.clarinLicenseService.findAll(context);
        Assert.assertEquals(clarinLicenses.size(), licenseDictionary.size());
        //control of the license mapping
        for (ClarinLicense clarinLicense: clarinLicenses) {
            ClarinLicense oldLicense = licenseDictionary.get(clarinLicense.getName());
            Assert.assertNotNull(oldLicense);
            Assert.assertEquals(clarinLicense.getConfirmation(), oldLicense.getConfirmation());
            Assert.assertEquals(clarinLicense.getDefinition(), oldLicense.getDefinition());
            Assert.assertEquals(clarinLicense.getRequiredInfo(), oldLicense.getRequiredInfo());
            Assert.assertEquals(clarinLicense.getLicenseLabels().size(), extendedMappingDictionary.get(
                    oldLicense.getID()).size());
            Assert.assertEquals(clarinLicense.getEperson().getID(), userRegistration.getID());
            List<ClarinLicenseLabel> clarinLicenseLabels = clarinLicense.getLicenseLabels();
            for (ClarinLicenseLabel label: clarinLicenseLabels) {
                ClarinLicenseLabel oldLabel = licenseLabelDictionary.get(label.getLabel());
                Assert.assertEquals(label.getLabel(), oldLabel.getLabel());
                Assert.assertEquals(label.getTitle(), oldLabel.getTitle());
                Assert.assertEquals(label.isExtended(), oldLabel.isExtended());
            }
        }

        //control of the license label mapping
        List<ClarinLicenseLabel> clarinLicenseLabels = this.clarinLicenseLabelService.findAll(context);
        Assert.assertEquals(clarinLicenseLabels.size(), licenseLabelDictionary.size());
        for (ClarinLicenseLabel label: clarinLicenseLabels) {
            ClarinLicenseLabel oldLabel = licenseLabelDictionary.get(label.getLabel());
            Assert.assertNotNull(oldLabel);
            Assert.assertEquals(label.getTitle(), oldLabel.getTitle());
            Assert.assertEquals(label.getIcon(), oldLabel.getIcon());
        }
        context.restoreAuthSystemState();
        this.cleanAll(context);
        System.out.println();
    }

    @Test
    public void labelIDoesntExist() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSE_LABELS_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("label", jsonNodeFactory.textNode(jsonObject.get("label").toString()));
            node.set("title", jsonNodeFactory.textNode(jsonObject.get("title").toString()));
            node.set("is_extended", jsonNodeFactory.textNode(jsonObject.get("is_extended").toString()));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_LICENSE_LABEL)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(422));
        this.cleanAll(context);
    }

    @Test
    public void labelIDisNull() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSE_LABELS_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("license_id", jsonNodeFactory.textNode(null));
            node.set("label", jsonNodeFactory.textNode(jsonObject.get("label").toString()));
            node.set("title", jsonNodeFactory.textNode(jsonObject.get("title").toString()));
            node.set("is_extended", jsonNodeFactory.textNode(jsonObject.get("is_extended").toString()));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_LICENSE_LABEL)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(422));
        this.cleanAll(context);
    }

    @Test
    public void labelIncorrectArgument() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSE_LABELS_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while (Objects.nonNull((line = bufferReader.readLine()))) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("label_id", jsonNodeFactory.textNode(jsonObject.get("label_id").toString()));
            node.set("title", jsonNodeFactory.textNode(jsonObject.get("title").toString()));
            node.set("is_extended", jsonNodeFactory.textNode(jsonObject.get("is_extended").toString()));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_LICENSE_LABEL)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(409));
        this.cleanAll(context);
    }

    @Test
    public void extendedMappingDoesntExist() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(EXTENDED_MAPPINGS_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("mapping_id", jsonNodeFactory.textNode(jsonObject.get("mapping_id").toString()));
            node.set("label_id", jsonNodeFactory.textNode(jsonObject.get("label_id").toString()));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_EXTENDED_MAPPING)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(422));
        this.cleanAll(context);
    }

    @Test
    public void extendedMappingLabelDoesntExist() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(EXTENDED_MAPPINGS_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("mapping_id", jsonNodeFactory.textNode(jsonObject.get("mapping_id").toString()));
            node.set("license_id", jsonNodeFactory.textNode(jsonObject.get("license_id").toString()));
            node.set("label_id", jsonNodeFactory.textNode("1000"));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_EXTENDED_MAPPING)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(409));
        this.cleanAll(context);
    }

    @Test
    public void licenseIDDoesntExist() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration userRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSES_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("name", jsonNodeFactory.textNode(jsonObject.get("name").toString()));
            node.set("definition", jsonNodeFactory.textNode(jsonObject.get("definition").toString()));
            node.set("eperson_id", jsonNodeFactory.textNode(jsonObject.get("eperson_id").toString()));
            node.set("label_id", jsonNodeFactory.textNode(jsonObject.get("label_id").toString()));
            node.set("eperson_id", jsonNodeFactory.textNode(admin.getID().toString()));
            node.set("confirmation", jsonNodeFactory.textNode(jsonObject.get("confirmation").toString()));
            node.set("required_info", jsonNodeFactory.textNode(Objects.isNull(jsonObject.get("required_info")) ?
                    null : jsonObject.get("required_info").toString()));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_LICENSE)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(422));
        this.cleanAll(context);
    }

    @Test
    public void licenseIDisNull() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration userRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSES_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("name", jsonNodeFactory.textNode(jsonObject.get("name").toString()));
            node.set("license_id", jsonNodeFactory.textNode(null));
            node.set("definition", jsonNodeFactory.textNode(jsonObject.get("definition").toString()));
            node.set("eperson_id", jsonNodeFactory.textNode(jsonObject.get("eperson_id").toString()));
            node.set("label_id", jsonNodeFactory.textNode(jsonObject.get("label_id").toString()));
            node.set("eperson_id", jsonNodeFactory.textNode(admin.getID().toString()));
            node.set("confirmation", jsonNodeFactory.textNode(jsonObject.get("confirmation").toString()));
            node.set("required_info", jsonNodeFactory.textNode(Objects.isNull(jsonObject.get("required_info")) ?
                    null : jsonObject.get("required_info").toString()));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_LICENSE)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(422));
        this.cleanAll(context);
    }

    @Test
    public void licenseIncorrectArgument() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration userRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSES_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while ((line = bufferReader.readLine()) != null) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("license_id", jsonNodeFactory.textNode(jsonObject.get("license_id").toString()));
            node.set("eperson_id", jsonNodeFactory.textNode(jsonObject.get("eperson_id").toString()));
            node.set("label_id", jsonNodeFactory.textNode(jsonObject.get("label_id").toString()));
            node.set("eperson_id", jsonNodeFactory.textNode(admin.getID().toString()));
            node.set("confirmation", jsonNodeFactory.textNode(jsonObject.get("confirmation").toString()));
            node.set("required_info", jsonNodeFactory.textNode(Objects.isNull(jsonObject.get("required_info")) ?
                    null : jsonObject.get("required_info").toString()));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_LICENSE)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(409));
        this.cleanAll(context);
    }

    public void licenseLabelDoesntExist() throws Exception {
        context.turnOffAuthorisationSystem();
        ClarinUserRegistration userRegistration = ClarinUserRegistrationBuilder
                .createClarinUserRegistration(context).withEPersonID(admin.getID()).build();
        context.restoreAuthSystemState();
        ObjectMapper mapper = new ObjectMapper();
        JSONParser parser = new JSONParser();
        BufferedReader bufferReader = new BufferedReader(new FileReader(getClass().getResource(LICENSES_TEST)
                .getFile()));
        Object obj;
        String line;
        ObjectNode node;
        JSONObject jsonObject;
        List<JsonNode> nodes = new ArrayList<>();

        while (Objects.nonNull(line = bufferReader.readLine())) {
            obj = parser.parse(line);
            jsonObject = (JSONObject)obj;
            node = jsonNodeFactory.objectNode();
            node.set("name", jsonNodeFactory.textNode(jsonObject.get("name").toString()));
            node.set("license_id", jsonNodeFactory.textNode(jsonObject.get("license_id").toString()));
            node.set("eperson_id", jsonNodeFactory.textNode(jsonObject.get("eperson_id").toString()));
            node.set("label_id", jsonNodeFactory.textNode("1000"));
            node.set("eperson_id", jsonNodeFactory.textNode(admin.getID().toString()));
            node.set("confirmation", jsonNodeFactory.textNode(jsonObject.get("confirmation").toString()));
            node.set("required_info", jsonNodeFactory.textNode(Objects.isNull(jsonObject.get("required_info")) ?
                    null : jsonObject.get("required_info").toString()));
            nodes.add(node);
        }

        String adminToken = getAuthToken(admin.getEmail(), password);
        getClient(adminToken).perform(post(URL_LICENSE)
                        .content(mapper.writeValueAsBytes(nodes))
                        .contentType(contentType))
                .andExpect(status().is(409));
        this.cleanAll(context);
    }

    private void cleanAll(Context context) throws SQLException, AuthorizeException {
        context.turnOffAuthorisationSystem();
        List<ClarinLicense> licenses = clarinLicenseService.findAll(context);
        List<ClarinLicenseLabel> labels = clarinLicenseLabelService.findAll(context);
        for (ClarinLicense license: licenses) {
            clarinLicenseService.delete(context, license);
        }
        for (ClarinLicenseLabel label: labels) {
            clarinLicenseLabelService.delete(context, label);
        }
        context.restoreAuthSystemState();
    }
}