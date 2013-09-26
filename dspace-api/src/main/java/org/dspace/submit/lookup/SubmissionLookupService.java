/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.submit.lookup;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Item;
import org.dspace.content.MetadataField;
import org.dspace.content.MetadataSchema;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.submit.util.EnhancedSubmissionLookupPublication;
import org.dspace.submit.util.ItemSubmissionLookupDTO;
import org.dspace.submit.util.SubmissionLookupDTO;
import org.dspace.submit.util.SubmissionLookupPublication;

import edu.emory.mathcs.backport.java.util.Arrays;

public class SubmissionLookupService {

	public static final String SL_NAMESPACE_PREFIX = "http://www.dspace.org/sl/"; 

	public static final String MANUAL_USER_INPUT = "manual";

	private static Logger log = Logger.getLogger(SubmissionLookupUtils.class);

	// Patter to extract the converter name if any
	private static final Pattern converterPattern = Pattern
			.compile(".*\\((.*)\\)");

	// attenzione inizializzato dal metodo init
	private Properties configuration;

	private static final String NOT_FOUND_DOI = "NOT-FOUND-DOI";

	public static final String SEPARATOR_VALUE = "#######";

	private static final String SEPARATOR_VALUE_REGEX = SEPARATOR_VALUE;

	private List<String> extraMetadataToKeep;

	private Map<String, EnhancerSubmissionLookup> enhancedMetadata;

	private List<SubmissionLookupProvider> providers;

	private Map<String, List<SubmissionLookupProvider>> idents2provs;

	private List<SubmissionLookupProvider> searchProviders;

	private synchronized void init() {
		if (configuration != null)
			return;
		String configFilePath = ConfigurationManager.getProperty("dspace.dir")
				+ File.separator + "config" + File.separator + "crosswalks"
				+ File.separator + "submission-lookup-mapping.properties";
		configuration = new Properties();
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(configFilePath);
			configuration.load(fis);
		} catch (Exception notfound) {
			throw new IllegalArgumentException(
					"Impossibile leggere la configurazione per il SubmissionLookupService (database esterno -> SURplus)",
					notfound);
		} finally {
			if (fis != null) {
				try {
					fis.close();
				} catch (IOException ioe) {
					log.error(ioe.getMessage(), ioe);
				}
			}
		}
	}

	public void setExtraMetadataToKeep(List<String> extraMetadataToKeep) {
		this.extraMetadataToKeep = extraMetadataToKeep;
	}

	public List<String> getExtraMetadataToKeep() {
		return extraMetadataToKeep;
	}

	public void setEnhancedMetadata(
			Map<String, EnhancerSubmissionLookup> enhancedMetadata) {
		this.enhancedMetadata = enhancedMetadata;
	}

	public Map<String, EnhancerSubmissionLookup> getEnhancedMetadata() {
		return enhancedMetadata;
	}

	public void setProviders(List<SubmissionLookupProvider> providers) {
		this.providers = providers;
		this.idents2provs = new HashMap<String, List<SubmissionLookupProvider>>();
		this.searchProviders = new ArrayList<SubmissionLookupProvider>();

		if (providers != null) {
			for (SubmissionLookupProvider p : providers) {
				if (p.isSearchProvider()) {
					searchProviders.add(p);
				}
				List<String> suppIdentifiers = p.getSupportedIdentifiers();
				if (suppIdentifiers != null) {
					for (String ident : suppIdentifiers) {
						List<SubmissionLookupProvider> tmp = idents2provs
								.get(ident);
						if (tmp == null) {
							tmp = new ArrayList<SubmissionLookupProvider>();
							idents2provs.put(ident, tmp);
						}
						tmp.add(p);
					}
				}
			}
		}
	}

	public List<String> getIdentifiers() {
		List<String> identifiers = new ArrayList<String>();
		identifiers.add("doi");
		identifiers.add("pubmed");
		identifiers.add("arxiv");
		return identifiers;
	}

	public Map<String, List<SubmissionLookupProvider>> getProvidersIdentifiersMap() {
		return idents2provs;
	}

	public void merge(String formName, Item item, ItemSubmissionLookupDTO dto) {
		init();
		SubmissionLookupPublication lookupPub = dto
				.getTotalPublication(providers);
		EnhancedSubmissionLookupPublication itemLookup = new EnhancedSubmissionLookupPublication(
				enhancedMetadata, lookupPub);
		Set<String> addedMetadata = new HashSet<String>();
		for (String field : itemLookup.getFields()) {
			String metadata = getMetadata(formName, itemLookup, field);
			if (StringUtils.isBlank(metadata)) {
				continue;
			}
			if (item.getMetadata(metadata).length == 0
					|| addedMetadata.contains(metadata)) {
				addedMetadata.add(metadata);
				String[] md = splitMetadata(metadata);
				if (isValidMetadata(formName, md)) {
					if (isRepeatableMetadata(formName, md)) {
						for (String value : itemLookup.getValues(field)) {
							String[] splitValue = splitValue(value);
							if (splitValue[3] != null) {
								item.addMetadata(md[0], md[1], md[2], md[3],
										splitValue[0], splitValue[1],
										Integer.parseInt(splitValue[2]));
							} else {
								item.addMetadata(md[0], md[1], md[2], md[3],
										value);
							}
						}
					} else {
						String value = itemLookup.getFirstValue(field);
						String[] splitValue = splitValue(value);
						if (splitValue[3] != null) {
							item.addMetadata(md[0], md[1], md[2], md[3],
									splitValue[0], splitValue[1],
									Integer.parseInt(splitValue[2]));
						} else {
							item.addMetadata(md[0], md[1], md[2], md[3], value);
						}
					}
				}
			}
		}

		// creo un nuovo context per il check di esistenza dei metadata di cache
		Context context = null;
		try {
			context = new Context();
			for (SubmissionLookupPublication pub : dto.getPublications()) {
				String providerName = pub.getProviderName();
				if (providerName != MANUAL_USER_INPUT) {
					for (String field : pub.getFields()) {
						String metadata = getMetadata(formName, pub, field);
						if (StringUtils.isBlank(metadata)) {
							continue;
						}

						String[] md = splitMetadata(metadata);
						if (isValidMetadata(formName, md)) {
							makeSureMetadataExist(context, providerName, md[1],
									md[2]);
							if (isRepeatableMetadata(formName, md)) {
								for (String value : pub.getValues(field)) {
									String[] splitValue = splitValue(value);
									item.addMetadata(providerName, md[1],
											md[2], md[3], splitValue[0],
											splitValue[1],
											Integer.parseInt(splitValue[2]));
								}
							} else {
								String[] splitValue = splitValue(pub
										.getFirstValue(field));
								item.addMetadata(providerName, md[1], md[2],
										md[3], splitValue[0], splitValue[1],
										Integer.parseInt(splitValue[2]));
							}
						}
					}
				}
			}
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		} finally {
			if (context != null && context.isValid()) {
				context.abort();
			}
		}
	}

	private void makeSureMetadataExist(Context context, String schema,
			String element, String qualifier) {
		try {
			context.turnOffAuthorisationSystem();
			boolean create = false;
			MetadataSchema mdschema = MetadataSchema.find(context, schema);
			MetadataField mdfield = null;
			if (mdschema == null) {
				mdschema = new MetadataSchema(SL_NAMESPACE_PREFIX + schema,
						schema);
				mdschema.create(context);
				create = true;
			} else {
				mdfield = MetadataField.findByElement(context,
						mdschema.getSchemaID(), element, qualifier);
			}

			if (mdfield == null) {
				mdfield = new MetadataField(mdschema, element, qualifier,
						"Campo utilizzato per la cache del provider submission-lookup: "
								+ schema);
				mdfield.create(context);
				create = true;
			}
			if (create) {
				context.commit();
			}
			context.restoreAuthSystemState();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private boolean isRepeatableMetadata(String formName, String[] md) {
		try {
			DCInput dcinput = getDCInput(formName, md[0], md[1], md[2]);
			if (dcinput != null) {
				return dcinput.isRepeatable();
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	private boolean isValidMetadata(String formName, String[] md) {
		try {
			if (extraMetadataToKeep != null
					&& extraMetadataToKeep.contains(StringUtils.join(
							Arrays.copyOfRange(md, 0, 3), "."))) {
				return true;
			}
            return getDCInput(formName, md[0], md[1], md[2])!=null;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

    private DCInput getDCInput(String formName, String schema,
            String element, String qualifier) throws DCInputsReaderException
    {
        DCInputSet dcinputset = new DCInputsReader().getInputs(formName);
        for (int idx = 0; idx < dcinputset.getNumberPages(); idx++)
        {
            for (DCInput dcinput : dcinputset.getPageRows(idx, true, true))
            {
                if (dcinput.getSchema().equals(schema)
                        && dcinput.getElement().equals(element)
                        && (dcinput.getQualifier() != null && dcinput
                                .getQualifier().equals(qualifier))
                        || (dcinput.getQualifier() == null && qualifier == null))
                {
                    return dcinput;
                }
            }
        }
        return null;
    }

	private String[] splitValue(String value) {
		String[] splitted = value.split(SEPARATOR_VALUE_REGEX);
		String[] result = new String[6];
		result[0] = splitted[0];
		result[2] = "-1";
		result[3] = "-1";
		result[4] = "-1";
		if (splitted.length > 1) {
			result[5] = "splitted";
			if (StringUtils.isNotBlank(splitted[1])) {
				result[1] = splitted[1];
			}
			if (splitted.length > 2) {
				result[2] = String.valueOf(Integer.parseInt(splitted[2]));
				if (splitted.length > 3) {
					result[3] = String.valueOf(Integer.parseInt(splitted[3]));
					if (splitted.length > 4) {
						result[4] = String.valueOf(Integer
								.parseInt(splitted[4]));
					}
				}
			}
		}
		return result;
	}

	private String[] splitMetadata(String metadata) {
		String[] mdSplit = new String[3];
		if (StringUtils.isNotBlank(metadata)) {
			String tmpSplit[] = metadata.split("\\.");
			if (tmpSplit.length == 4) {
				mdSplit = new String[4];
				mdSplit[0] = tmpSplit[0];
				mdSplit[1] = tmpSplit[1];
				mdSplit[2] = tmpSplit[2];
				mdSplit[3] = tmpSplit[3];
			} else if (tmpSplit.length == 3) {
				mdSplit = new String[4];
				mdSplit[0] = tmpSplit[0];
				mdSplit[1] = tmpSplit[1];
				mdSplit[2] = tmpSplit[2];
				mdSplit[3] = null;
			} else if (tmpSplit.length == 2) {
				mdSplit = new String[4];
				mdSplit[0] = tmpSplit[0];
				mdSplit[1] = tmpSplit[1];
				mdSplit[2] = null;
				mdSplit[3] = null;
			}
		}
		return mdSplit;
	}

	private String getMetadata(String formName,
			SubmissionLookupPublication itemLookup, String name) {
		String type = itemLookup.getType();
		String md = configuration.getProperty(
				type + "." + name,
				configuration.getProperty(formName + "." + name,
						configuration.getProperty(name)));
		if (md != null && md.contains("|")) {
			String[] cond = md.trim().split("\\|");
			for (int idx = 1; idx < cond.length; idx++) {
				boolean temp = itemLookup.getFields().contains(cond[idx]);
				if (temp) {
					return null;
				}
			}
			return cond[0];
		}
		return md;
	}

	public SubmissionLookupDTO getSubmissionLookupDTO(
			HttpServletRequest request, String uuidSubmission) {
		SubmissionLookupDTO dto = (SubmissionLookupDTO) request.getSession()
				.getAttribute("submission_lookup_" + uuidSubmission);
		if (dto == null) {
			dto = new SubmissionLookupDTO();
			storeDTOs(request, uuidSubmission, dto);
		}
		return dto;
	}

	public void invalidateDTOs(HttpServletRequest request, String uuidSubmission) {
		request.getSession().removeAttribute(
				"submission_lookup_" + uuidSubmission);
	}

	public void storeDTOs(HttpServletRequest request, String uuidSubmission,
			SubmissionLookupDTO dto) {
		request.getSession().setAttribute(
				"submission_lookup_" + uuidSubmission, dto);
	}

	public List<SubmissionLookupProvider> getSearchProviders() {
		return searchProviders;
	}

	public List<ItemSubmissionLookupDTO> searchByTerms(Context context,
			String title, String author, int year) {
		List<SubmissionLookupPublication> publications = new ArrayList<SubmissionLookupPublication>();
		List<String> timeoutProviders = new ArrayList<String>();
		for (SubmissionLookupProvider provider : searchProviders) {
			List<SubmissionLookupPublication> pPublications = null;
			try {
				pPublications = provider.search(context, title, author, year);
			} catch (SocketTimeoutException st) {
				timeoutProviders.add(provider.getShortName());
			} catch (Exception e) {
				e.printStackTrace();
			}
			if (pPublications != null) {
				publications.addAll(pPublications);
			}
		}
		return buildItemSubmissionLookupDTO(context, publications, true,
				timeoutProviders);
	}

	public List<ItemSubmissionLookupDTO> searchByIdentifiers(Context context,
			Map<String, String> keys) {
		Set<String> ids = keys.keySet();
		List<SubmissionLookupPublication> publications = new ArrayList<SubmissionLookupPublication>();
		List<String> timeoutProviders = new ArrayList<String>();
		for (SubmissionLookupProvider provider : providers) {
			for (String id : ids) {
				if (provider.getSupportedIdentifiers().contains(id)) {
					List<SubmissionLookupPublication> pPublications = null;
					try {
						pPublications = provider.getByIdentifier(context, keys);
					} catch (SocketTimeoutException st) {
						timeoutProviders.add(provider.getShortName());
					} catch (Exception e) {
						e.printStackTrace();
					}
					if (pPublications != null) {
						publications.addAll(pPublications);
					}
					break;
				}
			}
		}
		return buildItemSubmissionLookupDTO(context, publications,
				!ids.contains(SubmissionLookupProvider.DOI), timeoutProviders);
	}

	private List<ItemSubmissionLookupDTO> buildItemSubmissionLookupDTO(
			Context context, List<SubmissionLookupPublication> publications,
			boolean extend, List<String> evictProviders) {
		Map<String, List<SubmissionLookupPublication>> doi2publications = new HashMap<String, List<SubmissionLookupPublication>>();
		Map<String, Set<String>> provider2foundDOIs = new HashMap<String, Set<String>>();
		List<String> foundDOIs = new ArrayList<String>();
		List<SubmissionLookupPublication> allPublications = new ArrayList<SubmissionLookupPublication>();
		allPublications.addAll(publications);

		for (SubmissionLookupPublication publication : publications) {
			String doi = publication
					.getFirstValue(SubmissionLookupProvider.DOI);
			if (doi == null) {
				doi = NOT_FOUND_DOI;
			} else {
				doi = SubmissionLookupUtils.normalizeDOI(doi);
				if (!foundDOIs.contains(doi))
				{
				    foundDOIs.add(doi);
				}
				Set<String> tmp = provider2foundDOIs.get(publication
						.getProviderName());
				if (tmp == null) {
					tmp = new HashSet<String>();
					provider2foundDOIs.put(publication.getProviderName(), tmp);
				}
				tmp.add(doi);
			}

			List<SubmissionLookupPublication> tmp = doi2publications.get(doi);
			if (tmp == null) {
				tmp = new ArrayList<SubmissionLookupPublication>();
				doi2publications.put(doi, tmp);
			}
			tmp.add(publication);
		}

		if (extend) {
			for (SubmissionLookupProvider provider : idents2provs
					.get(SubmissionLookupProvider.DOI)) {
				if (evictProviders != null
						&& evictProviders.contains(provider.getShortName())) {
					continue;
				}
				Set<String> doiToSearch = new HashSet<String>();
				Set<String> alreadyFoundDOIs = provider2foundDOIs.get(provider
						.getShortName());
				for (String doi : foundDOIs) {
					if (alreadyFoundDOIs == null
							|| !alreadyFoundDOIs.contains(doi)) {
						doiToSearch.add(doi);
					}
				}
				List<SubmissionLookupPublication> pPublications = null;
				try {
					if (doiToSearch.size() > 0) {
						pPublications = provider
								.getByDOIs(context, doiToSearch);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
				if (pPublications != null) {
					allPublications.addAll(pPublications);
					for (SubmissionLookupPublication publication : pPublications) {
						String doi = publication
								.getFirstValue(SubmissionLookupProvider.DOI);
						List<SubmissionLookupPublication> tmp = doi2publications
								.get(doi);
						if (tmp == null) {
							tmp = new ArrayList<SubmissionLookupPublication>();
							doi2publications.put(doi, tmp);
						}
						tmp.add(publication);
					}
				}
			}
		}

		List<ItemSubmissionLookupDTO> result = new ArrayList<ItemSubmissionLookupDTO>();

		for (String doi : foundDOIs) {
			ItemSubmissionLookupDTO dto = new ItemSubmissionLookupDTO(
					doi2publications.get(doi));
			result.add(dto);
		}
		
		List<SubmissionLookupPublication> noDOIs = doi2publications
                .get(NOT_FOUND_DOI);
        if (noDOIs != null) {
            for (SubmissionLookupPublication p : noDOIs) {
                List<SubmissionLookupPublication> single = new ArrayList<SubmissionLookupPublication>();
                single.add(p);
                ItemSubmissionLookupDTO dto = new ItemSubmissionLookupDTO(
                        single);
                result.add(dto);
            }
        }
		return result;
	}

	public List<SubmissionLookupProvider> getProviders() {
		return providers;
	}
}
