/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.content.integration.crosswalks;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Collection;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.MetadataValue;
import org.dspace.content.crosswalk.CrosswalkException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.IConverter;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualField;
import org.dspace.content.integration.crosswalks.virtualfields.VirtualFieldMapper;
import org.dspace.content.service.ItemService;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.core.factory.CoreServiceFactory;
import org.dspace.services.ConfigurationService;
import org.springframework.beans.factory.annotation.Autowired;

public class ReferCrosswalk implements StreamDisseminationCrosswalk, FileNameDisseminator {

    private static Logger log = Logger.getLogger(ReferCrosswalk.class);

    // Patter to extract the converter name if any
    private static final Pattern converterPattern = Pattern.compile(".*\\((.*)\\)");


    @Autowired
    private ConfigurationService configurationService;

    @Autowired
    private ItemService itemService;

    @Autowired
    private VirtualFieldMapper virtualFieldMapper;


    private final String templateFileName;

    private final String mimeType;

    private final String fileName;

    private List<TemplateLine> template = new ArrayList<TemplateLine>();


    public ReferCrosswalk(String templateFileName, String mimeType, String fileName) {
        super();
        this.templateFileName = templateFileName;
        this.mimeType = mimeType;
        this.fileName = fileName;
    }

    @PostConstruct
    private void postConstruct() throws IOException {

        String parent = configurationService.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File templateFile = new File(parent, templateFileName);

        try (FileReader fileReader = new FileReader(templateFile);
            BufferedReader templateReader = new BufferedReader(fileReader)) {

            Pattern mdRepl = Pattern.compile("@[a-z0-9.*]+(\\(.*\\))?@");
            String templateLine = templateReader.readLine();
            while (templateLine != null) {
                TemplateLine line = new TemplateLine();
                Matcher matcher = mdRepl.matcher(templateLine);
                if (matcher.find()) {
                    line.beforeField = templateLine.substring(0, matcher.start());
                    line.afterField = templateLine.substring(matcher.end());

                    String mdString = templateLine.substring(matcher.start() + 1,
                        matcher.end() - 1);
                    String converterName = null;
                    Matcher converterMatcher = converterPattern.matcher(mdString);
                    if (converterMatcher.matches()) {
                        converterName = converterMatcher.group(1);
                        mdString = mdString.replaceAll("\\(" + converterName
                            + "\\)", "");
                    }

                    line.mdField = mdString;
                    line.converterName = converterName;
                    line.mdBits = line.mdField.split("\\.");

                    if (line.mdBits != null && line.mdBits[0].equalsIgnoreCase("virtual") && line.mdBits.length > 1) {
                        line.vf = virtualFieldMapper.getVirtualField(line.mdBits[1]);
                    }
                } else {
                    line.beforeField = templateLine;
                }

                template.add(line);
                templateLine = templateReader.readLine();
            }

        }
    }

    @Override
    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
        throws CrosswalkException, IOException, SQLException, AuthorizeException {

        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("ReferCrosswalk can only crosswalk an Item.");
        }

        Item item = (Item) dso;
        Map<String, String> fieldCache = new HashMap<String, String>();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
        String aliasForm;
        try {
            String formFileName = I18nUtil.getInputFormsFileName(I18nUtil.getDefaultLocale());

            Collection collection = item.getOwningCollection();

            // Read the input form file for the specific collection
            DCInputsReader inputsReader = new DCInputsReader(formFileName);

            List<DCInputSet> inputSet = inputsReader.getInputsByCollection(collection);
            DCInputSet dci = inputSet.get(0);
            aliasForm = dci.getFormName();
        } catch (Exception e) {
            throw new CrosswalkException(e.getMessage(), e);
        }
        fieldCache.put("formAlias", aliasForm);

        for (TemplateLine line : template) {
            if (line.mdField != null) {
                IConverter converter = null;
                if (StringUtils.isNotBlank(line.converterName)) {
                    converter = (IConverter) CoreServiceFactory.getInstance().getPluginService()
                        .getNamedPlugin(IConverter.class, line.converterName);
                    if (converter == null) {
                        log.error(LogManager.getHeader(null, "disseminate", "no converter plugin found with name "
                            + line.converterName + " for metadata " + line.mdField));
                    }
                }
                if (line.vf != null) {
                    String[] values = line.vf.getMetadata(item, fieldCache, line.mdField);

                    if (values != null) {
                        for (String value : values) {
                            String dvalue = null;

                            if (converter != null) {
                                dvalue = converter.makeConversion(value);
                            } else {
                                dvalue = value;
                            }

                            if (dvalue == null) {
                                continue;
                            }

                            writer.write(line.beforeField);
                            writer.write(dvalue);
                            writer.write(line.afterField);
                            writer.newLine();
                        }
                    }
                } else {
                    List<MetadataValue> dcvs = itemService.getMetadataByMetadataString(item, line.mdField);
                    if (dcvs != null) {
                        for (MetadataValue dc : dcvs) {

                            String dcValue = null;

                            if (converter != null) {
                                dcValue = converter.makeConversion(dc.getValue());
                            } else {
                                dcValue = dc.getValue();
                            }

                            if (dcValue == null) {
                                continue;
                            }

                            writer.write(line.beforeField);
                            writer.write(dcValue);
                            writer.write(line.afterField);
                            writer.newLine();
                        }
                    }
                }
            } else if (line.beforeField != null) {
                writer.write(line.beforeField);
                writer.newLine();
            }
        }

        writer.flush();

    }

    @Override
    public boolean canDisseminate(Context context, DSpaceObject dso) {
        return dso.getType() == Constants.ITEM;
    }

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getMIMEType() {
        return mimeType;
    }

    public void setConfigurationService(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }

    public void setVirtualFieldMapper(VirtualFieldMapper virtualFieldMapper) {
        this.virtualFieldMapper = virtualFieldMapper;
    }

    class TemplateLine {
        String converterName;
        String beforeField;
        String afterField;
        String mdField;
        String mdBits[];
        VirtualField vf;
    }

}
