/* Created for LINDAT/CLARIN */
package cz.cuni.mff.ufal;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.http.HttpEnvironment;
import org.dspace.app.xmlui.cocoon.AbstractDSpaceTransformer;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.app.xmlui.wing.Message;
import org.dspace.app.xmlui.wing.WingException;
import org.dspace.app.xmlui.wing.element.Body;
import org.dspace.app.xmlui.wing.element.Division;
import org.dspace.app.xmlui.wing.element.PageMeta;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Utils;
import org.dspace.handle.HandleManager;
import org.dspace.utils.DSpace;

import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceMapping;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.LicenseResourceUserAllowance;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserMetadata;
import cz.cuni.mff.ufal.lindat.utilities.hibernate.UserRegistration;
import cz.cuni.mff.ufal.lindat.utilities.interfaces.IFunctionalities;

/**
 * Display to the user that the license is successfully signed.
 * 
 * @author Amir Kamran
 */

public class UFALLicenceAgreementAgreed extends AbstractDSpaceTransformer {

	private static final Message T_title = message("xmlui.ufal.UFALLicenceAgreementAgreed.title");

	private static final Message T_dspace_home = message("xmlui.general.dspace_home");

	private static final Message T_trail_item = message("xmlui.ufal.UFALLicenceAgreementAgreed.trail_item");

	private static final Message T_trail_license_agreed = message("xmlui.ufal.UFALLicenceAgreementAgreed.trail_license_agreed");

	private static final Message T_head = message("xmlui.ufal.UFALLicenceAgreementAgreed.head");

	public void addPageMeta(PageMeta pageMeta) throws WingException,
			SQLException {
		// Set the page title
		pageMeta.addMetadata("title").addContent(T_title);

		DSpaceObject dso = HandleUtil.obtainHandle(objectModel);
		if (!(dso instanceof Item)) {
			return;
		}

		Item item = (Item) dso;

		pageMeta.addTrailLink(contextPath + "/", T_dspace_home);
		HandleUtil.buildHandleTrail(item, pageMeta, contextPath);
		pageMeta.addTrailLink(contextPath + "/handle/" + item.getHandle(),
				T_trail_item);
		pageMeta.addTrail().addContent(T_trail_license_agreed);
	}

	public void addBody(Body body) throws WingException {
		Division licenceAgreementAgreed = null;
		IFunctionalities functionalityManager = DSpaceApi.getFunctionalityManager();

		try {

			licenceAgreementAgreed = body.addDivision("ufal-licence-agreement-agreed", "primary");
			licenceAgreementAgreed.setHead(T_head);

			// First check the availibility of the plugin
			if (functionalityManager.isFunctionalityEnabled("lr.license.agreement") == false) {
				functionalityManager.setErrorMessage(message("xmlui.ufal.UFALLicenceAgreement.functionality_blocked").toString());
				DSpaceXmluiApi.app_xmlui_aspect_eperson_postError(licenceAgreementAgreed);
				return;
			}

			// If we have the user that is not logged in, we terminate
			if (context.getCurrentUser() == null) {
				functionalityManager.setErrorMessage(message("xmlui.ufal.UFALLicenceAgreementAgreed.user_login_error").toString());
				DSpaceXmluiApi.app_xmlui_aspect_eperson_postError(licenceAgreementAgreed);
				return;
			}

			// Loading variables through the web browser
			Request request = ObjectModelHelper.getRequest(objectModel);
			HttpServletResponse response = (HttpServletResponse)objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);
			HttpSession session = request.getSession();

			String handle = request.getParameter("handle");

			boolean allzip = Boolean.parseBoolean(request.getParameter("allzip"));

			int requestedBitstreamId = -1;
			String bitid = request.getParameter("bitstreamId");

			if (bitid != null && !bitid.isEmpty()) {
				requestedBitstreamId = Integer.parseInt(bitid);
			}

			Item item = (Item) HandleManager.resolveToObject(context, handle);

			HashMap<String, String> seenExtra = new HashMap<String, String>();
			ArrayList<ExtraLicenseField> actions = new ArrayList<ExtraLicenseField>();
			ArrayList<ExtraLicenseField> errors = new ArrayList<ExtraLicenseField>();

			for (Object extra : request.getParameters().keySet()) {
				String ext = extra.toString();
				if (!ext.startsWith("extra_")) {
					continue;
				}

				ExtraLicenseField exField = ExtraLicenseField.valueOf(ext.substring(6)); // ext.substring(6) will remove the prefix extra_

				if (exField.isMetadata()) {
					String val = request.getParameter(ext);

					if (!exField.validate(val)) {
						errors.add(exField);
					}

					if (val != null && !val.isEmpty()) {
						session.setAttribute("extra_" + exField.name(), val);
						seenExtra.put(exField.name(), val);
					}
				} else {
					actions.add(exField);
				}
			}
			
			if(!errors.isEmpty()) {
				this.errorRedirect(allzip, requestedBitstreamId, item, errors);
				return;
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
				user.setEpersonId(eperson.getID());

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
				response.sendRedirect(contextPath + "/handle/" + handle
						+ "?download=" + returnURL);
			} else {
				response.sendRedirect(contextPath + "/handle/" + handle);
			}

		} catch (Exception e) {
			try {
				DSpaceXmluiApi
						.app_xmlui_aspect_eperson_postError(licenceAgreementAgreed);
			} catch (Exception f) {
			}

		}
	}

	private void errorRedirect(boolean allzip, int bitstreamID, Item item, ArrayList<ExtraLicenseField> errors) throws IOException {
		Request request = ObjectModelHelper.getRequest(objectModel);
		HttpServletResponse response = (HttpServletResponse)objectModel.get(HttpEnvironment.HTTP_RESPONSE_OBJECT);		
		String redirectURL = request.getContextPath() + "/handle/"+ item.getHandle();
		if (allzip) {
			redirectURL += "/ufal-licence-agreement?allzip=true";
		} else {
			redirectURL += "/ufal-licence-agreement?bitstreamId=" + bitstreamID;
		}
		if(!errors.isEmpty()) {
			redirectURL += "&err=";
			for(ExtraLicenseField errField : errors) {
				redirectURL += errField.name() + ",";
			}
		}
		response.sendRedirect(redirectURL);
	}

}
