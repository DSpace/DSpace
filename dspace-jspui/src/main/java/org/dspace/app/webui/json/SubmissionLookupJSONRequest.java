/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.json;

import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.exceptions.BadTransformationSpec;
import gr.ekt.bte.exceptions.MalformedSourceException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.services.factory.DSpaceServicesFactory;
import org.dspace.submit.lookup.MultipleSubmissionLookupDataLoader;
import org.dspace.submit.lookup.SubmissionLookupOutputGenerator;
import org.dspace.submit.lookup.SubmissionLookupService;
import org.dspace.submit.lookup.SubmissionLookupUtils;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.submit.util.SubmissionLookupDTO;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Andrea Bollini
 * @author Kostas Stamatis
 * @author Luigi Andrea Pascarelli
 * @author Panagiotis Koutsourakis
 */
public class SubmissionLookupJSONRequest extends JSONRequest
{

    private SubmissionLookupService service = DSpaceServicesFactory.getInstance().getServiceManager()
            .getServiceByName(SubmissionLookupService.class.getName(),
                    SubmissionLookupService.class);

    private static Logger log = Logger
            .getLogger(SubmissionLookupJSONRequest.class);

    @Override
    public void doJSONRequest(Context context, HttpServletRequest req,
            HttpServletResponse resp) throws AuthorizeException, IOException
    {
        Gson json = new Gson();
        String suuid = req.getParameter("s_uuid");
        SubmissionLookupDTO subDTO = service.getSubmissionLookupDTO(req, suuid);
        // Check that we have a file upload request
        boolean isMultipart = ServletFileUpload.isMultipartContent(req);
        if ("identifiers".equalsIgnoreCase(req.getParameter("type")))
        {
            Map<String, Set<String>> identifiers = new HashMap<String, Set<String>>();
            Enumeration e = req.getParameterNames();

            while (e.hasMoreElements())
            {
                String parameterName = (String) e.nextElement();
                String parameterValue = req.getParameter(parameterName);

                if (parameterName.startsWith("identifier_")
                        && StringUtils.isNotBlank(parameterValue))
                {
                    Set<String> set = new HashSet<String>();
                    set.add(parameterValue);
                    identifiers.put(
                            parameterName.substring("identifier_".length()),
                            set);
                }
            }

            List<ItemSubmissionLookupDTO> result = new ArrayList<ItemSubmissionLookupDTO>();

            TransformationEngine transformationEngine = service
                    .getPhase1TransformationEngine();
            if (transformationEngine != null)
            {
                MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader) transformationEngine
                        .getDataLoader();
                dataLoader.setIdentifiers(identifiers);

                try
                {
                    SubmissionLookupOutputGenerator outputGenerator = (SubmissionLookupOutputGenerator) transformationEngine
                            .getOutputGenerator();
                    outputGenerator.setDtoList(new ArrayList<ItemSubmissionLookupDTO>());
                    log.debug("BTE transformation is about to start!");
                    transformationEngine.transform(new TransformationSpec());
                    log.debug("BTE transformation finished!");
                    result = outputGenerator.getDtoList();
                }
                catch (BadTransformationSpec e1)
                {
                    log.error(e1.getMessage(), e1);
                }
                catch (MalformedSourceException e1)
                {
                    log.error(e1.getMessage(), e1);
                }
            }

            subDTO.setItems(result);
            service.storeDTOs(req, suuid, subDTO);
            List<Map<String, Object>> dto = getLightResultList(result);
            JsonElement tree = json.toJsonTree(dto);
            JsonObject jo = new JsonObject();
            jo.add("result", tree);
            resp.getWriter().write(jo.toString());

        }
        else if ("search".equalsIgnoreCase(req.getParameter("type")))
        {
            String title = req.getParameter("title");
            String author = req.getParameter("authors");
            int year = UIUtil.getIntParameter(req, "year");

            Map<String, Set<String>> searchTerms = new HashMap<String, Set<String>>();
            Set<String> tmp1 = new HashSet<String>();
            tmp1.add(title);
            Set<String> tmp2 = new HashSet<String>();
            tmp2.add(author);
            Set<String> tmp3 = new HashSet<String>();
            tmp3.add(String.valueOf(year));
            searchTerms.put("title", tmp1);
            searchTerms.put("authors", tmp2);
            searchTerms.put("year", tmp3);

            List<ItemSubmissionLookupDTO> result = new ArrayList<ItemSubmissionLookupDTO>();

            TransformationEngine transformationEngine = service
                    .getPhase1TransformationEngine();
            if (transformationEngine != null)
            {
                MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader) transformationEngine
                        .getDataLoader();
                dataLoader.setSearchTerms(searchTerms);

                try
                {
                    SubmissionLookupOutputGenerator outputGenerator = (SubmissionLookupOutputGenerator) transformationEngine
                            .getOutputGenerator();
                    outputGenerator.setDtoList(new ArrayList<ItemSubmissionLookupDTO>());
                    log.debug("BTE transformation is about to start!");
                    transformationEngine.transform(new TransformationSpec());
                    log.debug("BTE transformation finished!");
                    result = outputGenerator.getDtoList();
                }
                catch (BadTransformationSpec e1)
                {
                    log.error(e1.getMessage(), e1);
                }
                catch (MalformedSourceException e1)
                {
                    log.error(e1.getMessage(), e1);
                }
            }

            subDTO.setItems(result);
            service.storeDTOs(req, suuid, subDTO);
            List<Map<String, Object>> dto = getLightResultList(result);
            JsonElement tree = json.toJsonTree(dto);
            JsonObject jo = new JsonObject();
            jo.add("result", tree);
            resp.getWriter().write(jo.toString());
        }
        else if ("details".equalsIgnoreCase(req.getParameter("type")))
        {
            String i_uuid = req.getParameter("i_uuid");
            Map<String, Object> dto = getDetails(subDTO.getLookupItem(i_uuid),
                    context);
            JsonElement tree = json.toJsonTree(dto);
            JsonObject jo = new JsonObject();
            jo.add("result", tree);
            resp.getWriter().write(jo.toString());
        }
        else if (isMultipart)
        {

            // Create a factory for disk-based file items
            FileItemFactory factory = new DiskFileItemFactory();

            // Create a new file upload handler
            ServletFileUpload upload = new ServletFileUpload(factory);
            // Parse the request
            Map<String, String> valueMap = new HashMap<String, String>();
            InputStream io = null;

            // Parse the request
            List<FileItem> iter;
            String filename = null;
            try
            {
                iter = upload.parseRequest(req);
                for (FileItem item : iter)
                {
                    String name = item.getFieldName();
                    InputStream stream = item.getInputStream();
                    if (item.isFormField())
                    {
                        String value = Streams.asString(stream);
                        valueMap.put(name, value);
                    }
                    else
                    {
                        io = stream;
                    }
                }
            }
            catch (FileUploadException e)
            {
                throw new IOException(e);
            }

            suuid = valueMap.get("s_uuid");
            subDTO = service.getSubmissionLookupDTO(req, suuid);

            List<ItemSubmissionLookupDTO> result = new ArrayList<ItemSubmissionLookupDTO>();

            TransformationEngine transformationEngine = service
                    .getPhase1TransformationEngine();
            if (transformationEngine != null)
            {
                MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader) transformationEngine
                        .getDataLoader();

                String tempDir = (ConfigurationManager
                        .getProperty("upload.temp.dir") != null) ? ConfigurationManager
                        .getProperty("upload.temp.dir") : System
                        .getProperty("java.io.tmpdir");
                File uploadDir = new File(tempDir);
                if (!uploadDir.exists()) {
                    if (!uploadDir.mkdir()) {
                        uploadDir = null;
                    }
                }
                File file = File.createTempFile("submissionlookup-loader",
                                                ".temp",
                                                uploadDir);
                BufferedOutputStream out = new BufferedOutputStream(
                        new FileOutputStream(file));
                Utils.bufferedCopy(io, out);
                dataLoader.setFile(file.getAbsolutePath(),
                        valueMap.get("provider_loader"));

                try
                {
                    SubmissionLookupOutputGenerator outputGenerator = (SubmissionLookupOutputGenerator) transformationEngine
                            .getOutputGenerator();
                    outputGenerator.setDtoList(new ArrayList<ItemSubmissionLookupDTO>());
                    log.debug("BTE transformation is about to start!");
                    transformationEngine.transform(new TransformationSpec());
                    log.debug("BTE transformation finished!");
                    result = outputGenerator.getDtoList();
                }
                catch (BadTransformationSpec e1)
                {
                    log.error(e1.getMessage(), e1);
                }
                catch (MalformedSourceException e1)
                {
                    log.error(e1.getMessage(), e1);
                }
                finally
                {
                    file.delete();
                }
            }
            subDTO.setItems(result);
            service.storeDTOs(req, suuid, subDTO);
            List<Map<String, Object>> dto = getLightResultList(result);
            if (valueMap.containsKey("skip_loader"))
            {
                if (valueMap.get("skip_loader").equals("true"))
                {
                    Map<String, Object> skip = new HashMap<String, Object>();
                    skip.put("skip", Boolean.TRUE);
                    skip.put("uuid", valueMap.containsKey("s_uuid") ? suuid
                            : -1);
                    skip.put(
                            "collectionid",
                            valueMap.containsKey("select-collection-file") ? valueMap
                                    .get("select-collection-file") : -1);
                    dto.add(skip);
                }
            }
            JsonElement tree = json.toJsonTree(dto);
            JsonObject jo = new JsonObject();
            jo.add("result", tree);
            resp.setContentType("text/plain");
//            if you works in localhost mode and use IE10 to debug the feature uncomment the follow line
//            resp.setHeader("Access-Control-Allow-Origin","*");
            resp.getWriter().write(jo.toString());
        }
    }

    private Map<String, Object> getDetails(ItemSubmissionLookupDTO item,
            Context context)
    {
        List<String> fieldOrder = getFieldOrder();
        Record totalData = item.getTotalPublication(service.getProviders());
        Set<String> availableFields = totalData.getFields();
        List<String[]> fieldsLabels = new ArrayList<String[]>();
        for (String f : fieldOrder)
        {
            if (availableFields.contains(f))
            {
                try
                {
                	if (totalData.getValues(f)!=null && totalData.getValues(f).size()>0)
                		fieldsLabels.add(new String[] {f, I18nUtil.getMessage("jsp.submission-lookup.detail."+ f, context) });
                }
                catch (MissingResourceException e)
                {
                    fieldsLabels.add(new String[] { f, f });
                }
            }
        }
        Map<String, Object> data = new HashMap<String, Object>();
        String uuid = item.getUUID();

        Record pub = item.getTotalPublication(service.getProviders());
        Map<String, List<String>> publication1 = new HashMap<String, List<String>>();
        for (String field : pub.getFields())
        {
            publication1
                    .put(field, SubmissionLookupUtils.getValues(pub, field));
        }

        data.put("uuid", uuid);
        data.put("providers", item.getProviders());
        data.put("publication", publication1);
        data.put("fieldsLabels", fieldsLabels);
        return data;
    }

    private List<String> getFieldOrder()
    {
    	if (service.getDetailFields()!=null){
    		return service.getDetailFields();
    	}
    	
    	//Default values, in case the property is not set
    	List<String> defaultValues = new ArrayList<String>();
    	defaultValues.add("title");
    	defaultValues.add("authors");
    	defaultValues.add("editors");
    	defaultValues.add("translators");
    	defaultValues.add("chairs");
    	defaultValues.add("issued");
    	defaultValues.add("abstract");
    	defaultValues.add("doi");
    	defaultValues.add("journal");
    	defaultValues.add("volume");
    	defaultValues.add("issue");
    	defaultValues.add("publisher");
    	defaultValues.add("jissn");
    	defaultValues.add("pisbn");
    	defaultValues.add("eisbn");
    	defaultValues.add("arxivCategory");
    	defaultValues.add("keywords");
    	defaultValues.add("mesh");
    	defaultValues.add("language");
    	defaultValues.add("subtype");
    	defaultValues.add("translators");
        
    	return defaultValues;
    }

    private List<Map<String, Object>> getLightResultList(
            List<ItemSubmissionLookupDTO> result)
    {
        List<Map<String, Object>> publications = new ArrayList<Map<String, Object>>();
        if (result != null && result.size() > 0)
        {
            for (ItemSubmissionLookupDTO item : result)
            {
                String uuid = item.getUUID();
                Record pub = item.getTotalPublication(service.getProviders());
                Map<String, Object> data = new HashMap<String, Object>();
                data.put("uuid", uuid);
                data.put("providers", item.getProviders());
                data.put("title",
                        SubmissionLookupUtils.getFirstValue(pub, "title"));
                data.put(
                        "authors",
                        pub.getValues("authors") != null ? StringUtils.join(
                                SubmissionLookupUtils.getValues(pub, "authors")
                                        .iterator(), ", ") : "");
                data.put("issued",
                        SubmissionLookupUtils.getFirstValue(pub, "issued"));

                publications.add(data);
            }
        }
        return publications;
    }
}
