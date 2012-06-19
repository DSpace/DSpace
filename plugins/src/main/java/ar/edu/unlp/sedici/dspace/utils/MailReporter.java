package ar.edu.unlp.sedici.dspace.utils;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.mail.MessagingException;
import javax.mail.SendFailedException;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Ariel Lira
 *
 */
public class MailReporter {

	private static final String SEDICI_MODULE = "sedici-dspace";
	private static final String PROPERTY_DEBUG_MODE = "xmlui.debug";

	private static final String EXCEPTION_MAIL_TEMPLATE = "exception_error";
	private static final String MISSING_AUTHORITY_MAIL_TEMPLATE = "missing_authority_key";
	
	protected static Logger logger = LoggerFactory.getLogger(MailReporter.class);
	
	public MailReporter() {
	}
	
	private Email getEmailTemplate(Context context,String templateName) throws MessagingException{
		String filename = I18nUtil.getEmailFilename(context.getCurrentLocale(), templateName);
		try {
			return ConfigurationManager.getEmail(filename);
		} catch (IOException e) {
			//Si es un mail de reporte diferente al de UnknownException, mando reporte de UnknownException
			if (!EXCEPTION_MAIL_TEMPLATE.equals(templateName)){
				this.reportUnknownException(context, e , null);
				//no puedo levantar el template de error ...
			}
			logger.error("No se pudo levantar el template "+templateName, e);
			throw new MessagingException("No se pudo levantar el template "+templateName, e);
		}
	}
	
	/**
	# Parameters: {0} DSpace server URL
	#             {1} Date & time
	#             {2} Session ID + IP address
	#             {3} User details
	 * @param context
	 * @param args 
	 * @param mailTemplateName 
	 * @throws MessagingException 
	 */
	private void sendMail(Context context, String recipient, String mailTemplateName, List<String> args) throws MessagingException{
	
		Email email = this.getEmailTemplate(context, mailTemplateName);
		
		email.addRecipient(recipient);

		email.addArgument(ConfigurationManager.getProperty("dspace.url"));
		email.addArgument(new Date());
		email.addArgument(context.getExtraLogInfo());
		
		if (context.getCurrentUser() != null) {
			email.addArgument(context.getCurrentUser().getFullName() + " - ID: " + context.getCurrentUser().getID() + " - email: " + context.getCurrentUser().getEmail());
		} else {
			email.addArgument("Anónimo");
		}
		
		for (String arg: args) {
			email.addArgument(arg);
		}
		
		try{
			email.send();
		}catch (SendFailedException e) {
			logger.error("Se produjo un error al intentar enviar un mail. No se pudo enviar a los destinatarios: "+Arrays.toString(e.getInvalidAddresses()), e);
			//me como el error porque es una falla de las direcciones de destino?
		}catch(MessagingException e){
			//email no es descriptivo pero no hay forma de desarmarlo desde afuera. 
			logger.error("Error al tratar de enviar el email: ", e);
			throw e;
		}
	}

	protected boolean isDebugMode() {
		return ConfigurationManager.getBooleanProperty(SEDICI_MODULE, PROPERTY_DEBUG_MODE, false);
	}

	protected String getAdminEmail(){
		String email = ConfigurationManager.getProperty("alert.recipient");
		if (email == null || "".equals(email))
			email = ConfigurationManager.getProperty("mail.from.address");
		return email;
	}
	
	protected String getFeedbackEmail(){
		String email = ConfigurationManager.getProperty("feedback.recipient");
		if (email == null || "".equals(email))
			email = this.getAdminEmail();
		return email;
	}


	/**
	 * Envia (segun la configuracion de debug) un reporte de exception por mail
	 * @param context
	 * @param thr
	 * @param url La url que se trató de acceder
	 * @throws MessagingException
	 */
	public void reportUnknownException(Context context, Throwable fullThr, String url) throws MessagingException {

		if (this.isDebugMode()) {
			logger.error("Se produjo una exception ", fullThr);
			logger.info("No mando el mail por la exception porque xmlui.debug es true");
			return;
		}

		List<String> args = new ArrayList<String>();
		
		//{4} URL + HTTP parameters, if any
		args.add(url);
		
	
		Throwable cause = fullThr;
		while (cause.getCause() != null)
			cause=cause.getCause();

		//{5} Cause
		args.add(cause.getMessage());
		
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		cause.printStackTrace(pw);
		pw.append("\n\n\nFull StackTrace :\n");
		fullThr.printStackTrace(pw);
		pw.flush();
		
		//{6} Exception stack trace
		args.add(sw.toString());
		
		this.sendMail(context,getAdminEmail(), EXCEPTION_MAIL_TEMPLATE, args);

	}
	

	/**
	 * Envia (segun la configuracion de debug) un reporte de clave inexistente por mail a la direccion de feedback
	 * @param context
	 * @param field
	 * @param url La url que se trató de acceder
	 * @throws MessagingException
	 */
	public void reportMissingAuthorityKey(Context context, String field, String key) throws MessagingException {

		//            {4} Field
		//            {5} Key
		List<String> args = new ArrayList<String>();
		args.add(field);
		args.add(key);
		this.sendMail(context,getFeedbackEmail(), MISSING_AUTHORITY_MAIL_TEMPLATE, args);

	}

}
