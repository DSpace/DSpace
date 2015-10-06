/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Utils;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import static org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer.encodeForURL;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceMapping;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceUserAllowance;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserMetadata;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserRegistration;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * @author Amir Kamran
 */

public class UFALLicenceAgreementAgreed {
	private static final org.apache.log4j.Logger log = Logger.getLogger(UFALLicenceAgreementAgreed.class);

	/**
	 * Process the agreement form and redirect.
	 * @param context
	 * @param objectModel
	 * @param allzip
	 * @param requestedBitstreamId
	 */
	public String agree(Context context, Map objectModel,  boolean allzip, int requestedBitstreamId){
		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();

		try {
			// Loading variables through the web browser
			Request request = ObjectModelHelper.getRequest(objectModel);

			int eID = 0;
			EPerson eperson = context.getCurrentUser();
			if(eperson!=null) {
				eID = eperson.getID();
			}

			String handle = request.getParameter("item-handle");

			Item item = (Item) HandleManager.resolveToObject(context, handle);

			HashMap<String, String> seenExtra = new HashMap<String, String>();
			ArrayList<ExtraLicenseField> actions = new ArrayList<ExtraLicenseField>();

			for (Object extra : request.getParameters().keySet()) {
				String ext = extra.toString();
				if (!ext.startsWith("extra_")) {
					continue;
				}

				ExtraLicenseField exField = ExtraLicenseField.valueOf(ext.substring(6)); // ext.substring(6) will remove the prefix extra_

				//validated before we get here
				if (exField.isMetadata()) {
					String val = request.getParameter(ext);
					if (val != null && !val.isEmpty()) {
						seenExtra.put(exField.name(), val);
					}
				} else {
					actions.add(exField);
				}
			}
			
			StringBuilder ids = new StringBuilder();

			functionalityManager.openSession();

			Bitstream[] bss = null;

			if (allzip) {

				Bundle[] originals = item.getBundles("ORIGINAL");
				for (Bundle original : originals) {
					bss = original.getBitstreams();
				}
			} else {
				bss = new Bitstream[1];
				bss[0] = Bitstream.find(context, requestedBitstreamId);
			}

			String token = Utils.generateHexKey();

			for (Bitstream bitstream : bss) {

				int bitstreamID = bitstream.getID();

				List<LicenseResourceMapping> mappings = functionalityManager.getAllMappings(bitstreamID);

				UserRegistration user = new UserRegistration();
				user.setEpersonId(eID);

				for (LicenseResourceMapping mapping : mappings) {
					Set<LicenseResourceUserAllowance> allowances = mapping.getLicenseResourceUserAllowances();

					LicenseResourceUserAllowance allowance = new LicenseResourceUserAllowance();
					allowance.setLicenseResourceMapping(mapping);
					allowance.setUserRegistration(user);
					allowance.setToken(token);
					allowances.add(allowance);

					functionalityManager.saveOrUpdate(LicenseResourceMapping.class, mapping);

					for (String key : seenExtra.keySet()) {
						UserMetadata metaData = new UserMetadata();
						metaData.setMetadataKey(key);
						metaData.setMetadataValue(seenExtra.get(key));
						metaData.setLicenseResourceUserAllowance(allowance);
						metaData.setUserRegistration(user);
						functionalityManager.persist(UserMetadata.class, metaData);

						functionalityManager.update(LicenseResourceUserAllowance.class, allowance);
					}
					
					// add IP address
					UserMetadata metaData = new UserMetadata();
					metaData.setMetadataKey("IP");
					metaData.setMetadataValue(request.getRemoteAddr());
					metaData.setLicenseResourceUserAllowance(allowance);
					metaData.setUserRegistration(user);
					functionalityManager.persist(UserMetadata.class, metaData);

					functionalityManager.update(LicenseResourceUserAllowance.class, allowance);
					

				}

				ids.append(".").append(bitstreamID);
			}

			functionalityManager.closeSession();

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("context", context);
			params.put("handle", handle);
			params.put("bitstreamId", requestedBitstreamId);
			params.put("extraMetadata", seenExtra);
			params.put("allzip", allzip);
			params.put("ip", request.getRemoteAddr());
			params.put("token", token);

			for (ExtraLicenseField exField : actions) {
				exField.performAction(params);
			}

			/*
			 * AK: this is not working well with some requirements commenting
			 * this out for now.
			 * 
			 * After signing the license add the bitstream to session so that
			 * for the current session the system will not check for licenses
			 * again
			 * 
			 * HttpSession session =
			 * dspace.getSessionService().getCurrentSession(); String
			 * authorizedBistreams = (String)
			 * session.getAttribute("AuthorizedBitstreams"); if
			 * (authorizedBistreams == null) { authorizedBistreams = ""; }
			 * session.setAttribute("AuthorizedBitstreams", authorizedBistreams
			 * + ids.toString());
			 */

			String host = request.getServerName();
			int port = request.getServerPort();
			String contextPath = request.getContextPath();
			boolean forceHTTPS = ConfigurationManager.getBooleanProperty("authentication-shibboleth", "lazysession.secure", true);

			String returnURL;
			if (request.isSecure() || forceHTTPS)
				returnURL = "https://";
			else
				returnURL = "http://";

			returnURL += host;
			if (!(port == 443 || port == 80))
				returnURL += ":" + port;
			returnURL += contextPath;

			if (allzip) {
				returnURL = encodeForURL(returnURL + "/handle/" + handle
						+ "/allzip?dtoken=" + token);
			} else {
				Bitstream bitstream = bss[0];
				returnURL = encodeForURL(returnURL + "/bitstream/handle/"
						+ handle + "/" + bitstream.getName() + "?sequence="
						+ bitstream.getSequenceID() + "&dtoken=" + token);
			}

			if (!actions.contains(ExtraLicenseField.SEND_TOKEN)) {
				return contextPath + "/handle/" + handle
						+ "?download=" + returnURL;
			} else {
				return contextPath + "/handle/" + handle;
			}

		} catch (Exception e) {
			log.error(e);
			functionalityManager.close();
		}
		return null;
	}
}
