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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.dspace.content.crosswalk.CrosswalkInternalException;
import org.dspace.content.crosswalk.CrosswalkObjectNotSupported;
import org.dspace.content.crosswalk.IConverter;
import org.dspace.content.crosswalk.StreamDisseminationCrosswalk;
import org.dspace.content.crosswalk.StreamIngestionCrosswalk;
import org.dspace.content.factory.ContentServiceFactory;
import org.dspace.content.service.ItemService;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.LogManager;
import org.dspace.core.SelfNamedPlugin;
import org.dspace.core.factory.CoreServiceFactory;

/**
 * This class has been initially developed by Graham Triggs, we have moved to a
 * CILEA package to make more clear that it is not included in the org.dspace
 * sourcecode.
 * ADDED support to UTF-8 and FileNameDisseminator (CILEA improvements)
 * 
 * @author grahamt
 * 
 */
public class ReferCrosswalk extends SelfNamedPlugin
        implements StreamDisseminationCrosswalk, StreamIngestionCrosswalk, FileNameDisseminator {
    protected static final String CONFIG_PREFIX = "crosswalk.refer";

    private static Logger log = Logger.getLogger(ReferCrosswalk.class);

    private List<TemplateLine> template = new ArrayList<TemplateLine>();

    private boolean initialized = false;

    private ItemService itemService = ContentServiceFactory.getInstance().getItemService();

    // Patter to extract the converter name if any
    private static final Pattern converterPattern = Pattern.compile(".*\\((.*)\\)");

    private static String aliases[] = null;
    static {
        List<String> aliasList = new ArrayList<String>();
        Enumeration<?> pe = ConfigurationManager.propertyNames();
        String propname = CONFIG_PREFIX + ".template.";
        while (pe.hasMoreElements()) {
            String key = (String) pe.nextElement();
            if (key.startsWith(propname)) {
                aliasList.add(key.substring(propname.length()));
            }
        }
        aliases = (String[]) aliasList.toArray(new String[aliasList.size()]);
    }

    public static String[] getPluginNames() {
        return aliases;
    }

    public boolean canDisseminate(Context context, DSpaceObject dso) {
        if (dso.getType() == Constants.ITEM) {
            return true;
        }
        return false;
    }

    public void disseminate(Context context, DSpaceObject dso, OutputStream out)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {
        init();
        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("ReferCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item) dso;
        Map<String, String> fieldCache = new HashMap<String, String>();

        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"));
        String aliasForm;
        try {
            String formFileName = I18nUtil.getInputFormsFileName(I18nUtil.getDefaultLocale());
            String col_handle = "";

            Collection collection = item.getOwningCollection();

            if (collection == null) {
                // set an empty handle so to get the default input set
                col_handle = "";
            } else {
                col_handle = collection.getHandle();
            }

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
                if (line.vfDissem != null) {
                    String[] values = line.vfDissem.getMetadata(item, fieldCache, line.mdField);

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

    public String getMIMEType() {
        return ConfigurationManager.getProperty(CONFIG_PREFIX + ".mimetype." + getPluginInstanceName().split("-")[0]);
    }

    public void ingest(Context context, DSpaceObject dso, InputStream in, String MIMEType)
            throws CrosswalkException, IOException, SQLException, AuthorizeException {
        init();
        if (dso.getType() != Constants.ITEM) {
            throw new CrosswalkObjectNotSupported("ReferCrosswalk can only crosswalk an Item.");
        }
        Item item = (Item) dso;

        Set<VirtualFieldIngester> ingesters = new HashSet<VirtualFieldIngester>();
        Map<String, String> fields = new HashMap<String, String>();

        BufferedReader reader = new BufferedReader(new InputStreamReader(in));

        String line = reader.readLine();
        while (line != null) {
            for (TemplateLine templateLine : template) {
                if (line.startsWith(templateLine.beforeField) && line.endsWith(templateLine.afterField)) {
                    String value = line.substring(templateLine.beforeField.length(),
                            line.length() - templateLine.afterField.length());
                    fields.put(templateLine.mdField, value);

                    if (templateLine.vfIngest != null) {
                        if (!ingesters.contains(templateLine.vfIngest)) {
                            ingesters.add(templateLine.vfIngest);
                        }
                        if (templateLine.mdBits.length == 3) {
                            templateLine.vfIngest.addMetadata(item, fields, templateLine.mdBits[3], value);
                        } else {
                            throw new CrosswalkInternalException(
                                    "Incorrect virtual field specification in template"
                                    + " - must be virtual.processor.fieldname");
                        }
                    }

                    if (templateLine.mdBits.length == 2) {
                        itemService.addMetadata(context, item, templateLine.mdBits[0], templateLine.mdBits[1],
                                    null, null, value);
                    } else if (templateLine.mdBits.length == 3) {
                        itemService.addMetadata(context, item, templateLine.mdBits[0], templateLine.mdBits[1],
                                    templateLine.mdBits[2], null, value);
                    } else {
                        throw new CrosswalkInternalException(
                                "Incorrect field specification in template - must be schema.element[.qualifier]");
                    }

                    break;
                }
            }

            line = reader.readLine();
        }

        reader.close();

        if (ingesters.size() > 0) {
            for (VirtualFieldIngester ingester : ingesters) {
                ingester.finalizeItem(item, fields);
            }
        }
    }

    private synchronized void init() throws CrosswalkInternalException, IOException {
        if (initialized) {
            return;
        }
        initialized = true;

        String myName = getPluginInstanceName();
        if (myName == null) {
            throw new CrosswalkInternalException("Cannot determine plugin name, "
                    + "You must use PluginManager to instantiate ReferCrosswalk so the instance knows its name.");
        }
        String templatePropName = CONFIG_PREFIX + ".template." + myName;
        String templateFileName = ConfigurationManager.getProperty(templatePropName);

        if (templateFileName == null) {
            throw new CrosswalkInternalException("Configuration error: "
                    + "No template file configured for Refer crosswalk named \"" + myName + "\"");
        }
        String parent = ConfigurationManager.getProperty("dspace.dir") + File.separator + "config" + File.separator;
        File templateFile = new File(parent, templateFileName);
        BufferedReader templateReader = new BufferedReader(new FileReader(templateFile));

        Pattern mdRepl = Pattern.compile("@[a-z0-9.*]+(\\(.*\\))?@");
        String templateLine = templateReader.readLine();
        while (templateLine != null) {
            TemplateLine line = new TemplateLine();
            Matcher matcher = mdRepl.matcher(templateLine);
            if (matcher.find()) {
                line.beforeField = templateLine.substring(0, matcher.start());
                line.afterField = templateLine.substring(matcher.end());

                String mdString = templateLine.substring(matcher.start() + 1, matcher.end() - 1);
                String converterName = null;
                Matcher converterMatcher = converterPattern.matcher(mdString);
                if (converterMatcher.matches()) {
                    converterName = converterMatcher.group(1);
                    mdString = mdString.replaceAll("\\(" + converterName + "\\)", "");
                }

                line.mdField = mdString;
                line.converterName = converterName;
                line.mdBits = line.mdField.split("\\.");

                if (line.mdBits != null && line.mdBits[0].equalsIgnoreCase("virtual") && line.mdBits.length > 1) {
                    line.vfDissem = (VirtualFieldDisseminator) CoreServiceFactory.getInstance().getPluginService()
                     .getNamedPlugin(VirtualFieldDisseminator.class, line.mdBits[1]);
                    line.vfIngest = (VirtualFieldIngester) CoreServiceFactory.getInstance().getPluginService()
                     .getNamedPlugin(VirtualFieldIngester.class, line.mdBits[1]);
                }
            } else {
                line.beforeField = templateLine;
            }

            template.add(line);
            templateLine = templateReader.readLine();
        }
        templateReader.close();
    }

    @Override
    public String getFileName() {
        String filename = ConfigurationManager.getProperty(CONFIG_PREFIX + "." + getPluginInstanceName() + ".filename");
        return filename == null ? "references" : filename;

    }
}

class TemplateLine {
    String converterName;
    String beforeField;
    String afterField;
    String mdField;
    String mdBits[];
    VirtualFieldDisseminator vfDissem;
    VirtualFieldIngester vfIngest;
}