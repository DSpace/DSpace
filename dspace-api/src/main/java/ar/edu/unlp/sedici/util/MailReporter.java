/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package ar.edu.unlp.sedici.util;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.text.DateFormat;
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
	
	protected static Logger log = LoggerFactory.getLogger(MailReporter.class);
	
	public MailReporter() {
	}
	
	private static Email getEmailTemplate(Context context,String templateName) throws MessagingException{
		String filename = I18nUtil.getEmailFilename(context.getCurrentLocale(), templateName);
		try {
			return Email.getEmail(filename);
		} catch (IOException e) {
			//Si es un mail de reporte diferente al de UnknownException, mando reporte de UnknownException
			if (!EXCEPTION_MAIL_TEMPLATE.equals(templateName)){
				reportUnknownException(context,"ups, no puedo levantar el template de error", e , null, null);
			}
			log.error("No se pudo levantar el template "+templateName, e);
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
	private static void sendMail(Context context, String recipient, String mailTemplateName, List<String> args) throws MessagingException{
		if (ConfigurationManager.getBooleanProperty("mail.template."+mailTemplateName+".disabled", false))
		{
			log.info("Se generó un correo basado en el template "+mailTemplateName+" pero no se envio porque estos correos están deshabilitados");
			return;
		}
		Email email = getEmailTemplate(context, mailTemplateName);
		
		email.addRecipient(recipient);

		email.addArgument(ConfigurationManager.getProperty("dspace.url"));
		email.addArgument(DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(new Date()));
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
			log.error("Se produjo un error al intentar enviar un mail. No se pudo enviar a los destinatarios: "+Arrays.toString(e.getInvalidAddresses()), e);
			//me como el error porque es una falla de las direcciones de destino?
		}catch(MessagingException e){
			//email no es descriptivo pero no hay forma de desarmarlo desde afuera. 
			log.error("Error al tratar de enviar el email: ", e);
			throw e;
		} catch (IOException e) {
			//email no es descriptivo pero no hay forma de desarmarlo desde afuera. 
			log.error("Error al tratar de enviar el email: ", e);
			throw new MessagingException("Error al tratar de enviar el email", e);
		}
	}

	protected static boolean isDebugMode() {
		return ConfigurationManager.getBooleanProperty(SEDICI_MODULE, PROPERTY_DEBUG_MODE, false);
	}

	protected static String getAdminEmail(){
		String email = ConfigurationManager.getProperty("alert.recipient");
		if (email == null || "".equals(email))
			email = ConfigurationManager.getProperty("mail.from.address");
		return email;
	}
	
	protected static String getFeedbackEmail(){
		String email = ConfigurationManager.getProperty("feedback.recipient");
		if (email == null || "".equals(email))
			email = getAdminEmail();
		return email;
	}


	/**
	 * Envia (segun la configuracion de debug) un reporte de exception por mail
	 * @param context
	 * @param message 
	 * @param thr
	 * @param url La url que se trató de acceder
	 * @param parameters 
	 * @throws MessagingException
	 */
	public static void reportUnknownException(Context context, String message, Throwable fullThr, String url, String parameters) {

		if (isDebugMode()) {
			log.error("Se produjo una exception ", fullThr);
			log.info("No mando el mail por la exception porque xmlui.debug es true");
			return;
		}

		List<String> args = new ArrayList<String>();
		
		//{4} URL + HTTP parameters, if any
		args.add(url);
		
	
		Throwable cause = fullThr;
		while (cause.getCause() != null)
			cause=cause.getCause();

		//{5} Cause
		if (message == null)
			message = "";
		else
			message += ", ";
		message += cause.getMessage();
		args.add(message);
		
		//{6} Parameters
		if (parameters == null)
			parameters = "";
		args.add(parameters);

		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		cause.printStackTrace(pw);
		pw.append("\n\n\nFull StackTrace :\n");
		fullThr.printStackTrace(pw);
		pw.flush();
		
		//{7} Exception stack trace
		args.add(sw.toString());
		try{
			sendMail(context,getAdminEmail(), EXCEPTION_MAIL_TEMPLATE, args);
		}  catch (MessagingException me) {
			log.error("Se produjo un error al intentar enviar un mail de reportUnknownException", me);
			//Silenciosamente dejo pasar el error porque la operacion en curson no tiene nada que ver con el envio de email
		}
	}
	

	/**
	 * Envia (segun la configuracion de debug) un reporte de clave inexistente por mail a la direccion de feedback
	 * @param context
	 * @param field
	 * @param url La url que se trató de acceder
	 * @throws MessagingException
	 */
	public static void reportMissingAuthorityKey(String field, String key) {
		Context context;
		try {
			context = new Context();
		} catch (SQLException e2) {
			log.error("No se pudo instanciar el Context ... algo raro pasa", e2);
			throw new RuntimeException(e2);
		} 

		try {
			//            {4} Field
			//            {5} Key
			List<String> args = new ArrayList<String>();
			args.add(field);
			args.add(key);
			try{
				sendMail(context,getFeedbackEmail(), MISSING_AUTHORITY_MAIL_TEMPLATE, args);
			}  catch (MessagingException e) {
				log.error("Se produjo un error al intentar enviar un mail de reportMissingAuthorityKey", e);
				//Silenciosamente dejo pasar el error porque la operacion en curso no tiene nada que ver con el envio de email
				//throw new RuntimeException(e);
			}
		}finally{
			try {
				context.complete();	
			} catch (SQLException e3) {
				log.error("No se pudo cerrar el Context ... algo raro pasa", e3);
				throw new RuntimeException(e3);
			} 
		}
	}

	public static void reportUnknownException(String message, Throwable e, String url) {
		reportUnknownException( message, e, url, null);
	}
	
	public static void reportUnknownException(String message, Throwable e, String url, String parameters) {
		Context context;
		try {
			context = new Context();
		} catch (SQLException e2) {
			log.error("No se pudo instanciar el Context ... algo raro pasa", e2);
			throw new RuntimeException(e2);
		} 

		try {
			MailReporter.reportUnknownException(context, message, e, url, parameters);
		}finally{
			try {
				context.complete();	
			} catch (SQLException e3) {
				log.error("No se pudo cerrar el Context ... algo raro pasa", e3);
				throw new RuntimeException(e3);
			} 
		}

	}

}
