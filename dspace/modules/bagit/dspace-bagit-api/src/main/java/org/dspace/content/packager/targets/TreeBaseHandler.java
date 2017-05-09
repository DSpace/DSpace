package org.dspace.content.packager.targets;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

import org.apache.log4j.Logger;

import javax.mail.MessagingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.dspace.content.packager.BagItDisseminatorException;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.eperson.EPerson;

import org.glassfish.jersey.client.JerseyClientBuilder;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;

public class TreeBaseHandler implements RemoteRepoHandler {

	private static final Logger LOGGER = Logger
			.getLogger(TreeBaseHandler.class);

	private static final String DEV_BASE_URL = "http://treebase-stage.nescent.org/treebase-web";
	private static final String BASE_URL = "http://treebase.org/treebase-web";
	private static final String SERVICE_PATH = "/handshaking/dryadImport";
	private static final String CONTENT_TYPE = "multipart/mixed";
	private static final String HANDLER_NAME = "TreeBase";

	public void send(File aBagItFile, EPerson aPerson)
			throws BagItDisseminatorException {
		String testing = ConfigurationManager.getProperty("bagit.testing.mode");
		String url;

		if (testing != null && testing.equalsIgnoreCase("false")) {
			url = BASE_URL;
		}
		else {
			url = DEV_BASE_URL;
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.info("Sending bagit " + aBagItFile.getName()
					+ " to TreeBASE service at " + url);
		}

//		Client c = Client.create();
//		WebResource service = c.resource(url);
		Client client = JerseyClientBuilder.newClient();
		WebTarget target = client.target(url).path("myresource/{param}");

		try {
			ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
			InputStream inStream = new FileInputStream(aBagItFile);
			byte[] buffer = new byte[1024];
			int bytesRead = 0;

			while (true) {
				bytesRead = inStream.read(buffer);
				if (bytesRead == -1) break;
				byteStream.write(buffer, 0, bytesRead);
			};

			MultiPart multiPart = new MultiPart();
			multiPart.bodyPart(new BodyPart(byteStream.toByteArray(),
					MediaType.APPLICATION_OCTET_STREAM_TYPE));

//			WebResource webResource = service.path(SERVICE_PATH);
//			Builder builder = webResource.type(CONTENT_TYPE);
//			ClientResponse response = (ClientResponse) builder.put(ClientResponse.class, multiPart);
			WebTarget webTarget = target.path(SERVICE_PATH);
			Response response = webTarget.request().put(Entity.entity(multiPart, multiPart.getMediaType()));

			switch (response.getStatus()) {
			case 200:
			case 201:
			case 202:
				String treebaseURL = response.getEntity().toString();

				if (LOGGER.isInfoEnabled()) {
					LOGGER.info("TreeBASE reponded with: " + treebaseURL);
				}

				Locale locale = I18nUtil.getDefaultLocale();
				Email email = ConfigurationManager.getEmail(I18nUtil
						.getEmailFilename(locale, "bagit_notify"));

				email.addRecipient(aPerson.getEmail());
				email.addArgument(HANDLER_NAME);
				email.addArgument(treebaseURL);
				
				try {
					email.send();
				}
				catch (MessagingException details) {
					throw new BagItDisseminatorException(details);
				}

				break;
			default:
				throw new BagItDisseminatorException(
						"TreeBASE reports failure: " + response.getStatus());
			}
		}
		catch (IOException details) {
			throw new BagItDisseminatorException(details);
		}
	}
	
	public String getHandlerName() {
		return HANDLER_NAME;
	}
}
