/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.webui.json;

import flexjson.JSONSerializer;
import gr.ekt.bte.core.Record;
import gr.ekt.bte.core.TransformationEngine;
import gr.ekt.bte.core.TransformationSpec;
import gr.ekt.bte.exceptions.BadTransformationSpec;
import gr.ekt.bte.exceptions.MalformedSourceException;

import java.io.IOException;
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

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.webui.util.UIUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.I18nUtil;
import org.dspace.submit.lookup.MultipleSubmissionLookupDataLoader;
import org.dspace.submit.lookup.SubmissionLookupOutputGenerator;
import org.dspace.submit.lookup.SubmissionLookupService;
import org.dspace.submit.lookup.SubmissionLookupUtils;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.submit.util.SubmissionLookupDTO;
import org.dspace.utils.DSpace;

public class SubmissionLookupJSONRequest extends JSONRequest {

	private SubmissionLookupService service = new DSpace().getServiceManager()
			.getServiceByName(SubmissionLookupService.class.getName(),
					SubmissionLookupService.class);

	private static Logger log = Logger
			.getLogger(SubmissionLookupJSONRequest.class);

	@Override
	public void doJSONRequest(Context context, HttpServletRequest req,
			HttpServletResponse resp) throws AuthorizeException, IOException 
			{
		String suuid = req.getParameter("s_uuid");
		SubmissionLookupDTO subDTO = service.getSubmissionLookupDTO(req, suuid);
		if ("identifiers".equalsIgnoreCase(req.getParameter("type"))) {
			Map<String, Set<String>> identifiers = new HashMap<String, Set<String>>();
			Enumeration e = req.getParameterNames();

			while (e.hasMoreElements()) {
				String parameterName = (String) e.nextElement();
				String parameterValue = req.getParameter(parameterName);

				if (parameterName.startsWith("identifier_")
						&& StringUtils.isNotBlank(parameterValue)) {
					Set<String> set = new HashSet<String>();
					set.add(parameterValue);
					identifiers.put(
							parameterName.substring("identifier_".length()),
							set);
				}
			}

			List<ItemSubmissionLookupDTO> result = new ArrayList<ItemSubmissionLookupDTO>();

			TransformationEngine transformationEngine = service.getPhase1TransformationEngine();
			if (transformationEngine != null){
				MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader)transformationEngine.getDataLoader();
				dataLoader.setIdentifiers(identifiers);

				try {
					transformationEngine.transform(new TransformationSpec());
					
					SubmissionLookupOutputGenerator outputGenerator = (SubmissionLookupOutputGenerator)transformationEngine.getOutputGenerator();
					result = outputGenerator.getDtoList();
				} catch (BadTransformationSpec e1) {
					e1.printStackTrace();
				} catch (MalformedSourceException e1) {
					e1.printStackTrace();
				}
			}

			subDTO.setItems(result);
			service.storeDTOs(req, suuid, subDTO);
			List<Map<String, Object>> dto = getLightResultList(result);
			JSONSerializer serializer = new JSONSerializer();
			serializer.rootName("result");
			serializer.deepSerialize(dto, resp.getWriter());
		} else if ("search".equalsIgnoreCase(req.getParameter("type"))) {
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

			TransformationEngine transformationEngine = service.getPhase1TransformationEngine();
			if (transformationEngine != null){
				MultipleSubmissionLookupDataLoader dataLoader = (MultipleSubmissionLookupDataLoader)transformationEngine.getDataLoader();
				dataLoader.setSearchTerms(searchTerms);

				SubmissionLookupOutputGenerator outputGenerator = (SubmissionLookupOutputGenerator)transformationEngine.getOutputGenerator();
				result = outputGenerator.getDtoList();
				try {
					transformationEngine.transform(new TransformationSpec());
				} catch (BadTransformationSpec e1) {
					e1.printStackTrace();
				} catch (MalformedSourceException e1) {
					e1.printStackTrace();
				}
			}

			subDTO.setItems(result);
			service.storeDTOs(req, suuid, subDTO);
			List<Map<String, Object>> dto = getLightResultList(result);
			JSONSerializer serializer = new JSONSerializer();
			serializer.rootName("result");
			serializer.deepSerialize(dto, resp.getWriter());
		} else if ("details".equalsIgnoreCase(req.getParameter("type"))) {
			String i_uuid = req.getParameter("i_uuid");
			JSONSerializer serializer = new JSONSerializer();
			serializer.rootName("result");
			Map<String, Object> dto = getDetails(subDTO.getLookupItem(i_uuid),
					context);
			serializer.deepSerialize(dto, resp.getWriter());
		}
	}

	private Map<String, Object> getDetails(ItemSubmissionLookupDTO item,
			Context context) {
		List<String> fieldOrder = getFieldOrderFromConfiguration();
        Record totalData = item.getTotalPublication(service.getProviders());
		Set<String> availableFields = totalData.getFields();
		List<String[]> fieldsLabels = new ArrayList<String[]>();
		for (String f : fieldOrder) {
			if (availableFields.contains(f)) {
				try {
					fieldsLabels.add(new String[] {
							f,
							I18nUtil.getMessage("jsp.submission-lookup.detail."
									+ f, context) });
				} catch (MissingResourceException e) {
					fieldsLabels.add(new String[] { f, f });
				}
			}
		}
		Map<String, Object> data = new HashMap<String, Object>();
		String uuid = item.getUUID();
        Record pub = item.getTotalPublication(service.getProviders());
		data.put("uuid", uuid);
		data.put("providers", item.getProviders());
		data.put("publication", pub);
		data.put("fieldsLabels", fieldsLabels);
		return data;
	}

	private List<String> getFieldOrderFromConfiguration() {
		String config = ConfigurationManager
				.getProperty("submission-lookup.detail.fields");
		if (config == null) {
			config = "title,authors,editors,years,doi,pmid,eid,arxiv,journal,jissn,jeissn,volume,issue,serie,sissn,seissn,abstract,mesh,keywords,subtype";
		}
		List<String> result = new ArrayList<String>();
		String[] split = config.split(",");
		for (String s : split) {
			if (StringUtils.isNotBlank(s)) {
				result.add(s.trim());
			}
		}
		return result;
	}

	private List<Map<String, Object>> getLightResultList(
			List<ItemSubmissionLookupDTO> result) {
		List<Map<String, Object>> publications = new ArrayList<Map<String, Object>>();
		if (result != null && result.size()>0) {
			for (ItemSubmissionLookupDTO item : result) {
				String uuid = item.getUUID();
				Record pub = item.getTotalPublication(service.getProviders());
				Map<String, Object> data = new HashMap<String, Object>();
				data.put("uuid", uuid);
				data.put("providers", item.getProviders());
				data.put("title", SubmissionLookupUtils.getFirstValue(pub, "title"));
				data.put("authors",pub.getValues("authors")!=null?
						StringUtils.join(SubmissionLookupUtils.getValues(pub, "authors").iterator(), ", "):"");
				data.put("issued", SubmissionLookupUtils.getFirstValue(pub, "issued"));
				publications.add(data);
			}
		}
		return publications;
	}
}
