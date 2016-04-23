/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.artifactbrowser;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.apache.avalon.framework.parameters.Parameters;
import org.apache.cocoon.acting.AbstractAction;
import org.apache.cocoon.environment.ObjectModelHelper;
import org.apache.cocoon.environment.Redirector;
import org.apache.cocoon.environment.Request;
import org.apache.cocoon.environment.SourceResolver;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.app.xmlui.utils.ContextUtil;
import org.dspace.app.xmlui.utils.HandleUtil;
import org.dspace.authorize.AuthorizeException;
import org.dspace.authorize.AuthorizeManager;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.RevisionToken;
import org.dspace.core.ConfigurationManager;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.ctask.arvo.RevisorTokenEnviarCurationTask;
import org.dspace.eperson.EPerson;
import org.dspace.handle.HandleManager;
import org.dspace.storage.bitstore.BitstreamStorageManager;
import org.dspace.core.Constants;
/**
 * @author Scott Phillips
 */

public class SendJuzgarAction extends AbstractAction
{
    private static Logger log = Logger.getLogger(SendJuzgarAction.class);
    public static String MESSAGE_REQUEST_COPY="Juicio de revision";
    /**
     *
     */
    public Map act(Redirector redirector, SourceResolver resolver, Map objectModel,
            String source, Parameters parameters) throws Exception
    {
        Request request = ObjectModelHelper.getRequest(objectModel);
        
        String handle = parameters.getParameter("handle");
        String email = request.getParameter("email");
        String mensaje = parameters.getParameter("mensaje","");
      
        // User email from context
        Context context = ContextUtil.obtainContext(objectModel);
        EPerson loggedin = context.getCurrentUser();
        String eperson = null;
        if (loggedin != null)
        {
            eperson = loggedin.getEmail();
        }
        // Comprobar que el email es correcto
        boolean mailCorrecto=comprobarMailCorrecto(email);
        // Comprobar que ese email puede emitir juicios
        boolean mailpermitido=comprobarMailPuedeEmitirJuicios(email,handle,context);
        // Check all data is there
        if ((handle == null) || email==null || email.equals("") || !mailpermitido || !mailCorrecto)
        {
            // Either the user did not fill out the form or this is the
            // first time they are visiting the page.
            Map<String,String> map = new HashMap<String,String>();
          
                map.put("handle", handle);
                map.put("email", email);
            if(StringUtils.isNotBlank(email) && !mailCorrecto){
        	map.put("mensaje", "xmlui.ArtifactBrowser.JuzgarForm.error.mailIncorrecto");
            }else if(StringUtils.isNotBlank(email) && !mailpermitido){
        	map.put("mensaje", "xmlui.ArtifactBrowser.JuzgarForm.error.mailNoPermitido");
            }
            return map;
        }
        
        // Enviar el correo con token de juicio
        DSpaceObject dso=HandleManager.resolveToObject(context, handle);
        if(dso.getType()!=org.dspace.core.Constants.ITEM){
            //ERROR
            throw new Exception("ERROR: Deberia ser un item");
        }
        Item item=(Item) dso;
	RequestItem ri=new RequestItem(item.getID(), -1, email,email, MESSAGE_REQUEST_COPY, true);
	log.info("Enviando mail a "+ri.getReqEmail());
	processSendDocuments(context,ri,item,getTitle(item));
	log.info("Mail enviado");
	context.commit();
//
//        // All data is there, send the email
//        Email email = Email.getEmail(I18nUtil.getEmailFilename(context.getCurrentLocale(), "feedback"));
//        email.addRecipient(ConfigurationManager
//                .getProperty("feedback.recipient"));
//
//        email.addArgument(new Date()); // Date
//        email.addArgument(address);    // Email
//        email.addArgument(eperson);    // Logged in as
//        email.addArgument(page);       // Referring page
//        email.addArgument(agent);      // User agent
//        email.addArgument(session);    // Session ID
//        email.addArgument(comments);   // The feedback itself
//
//        // Replying to feedback will reply to email on form
//        email.setReplyTo(address);
//
//        // May generate MessageExceptions.
//        email.send();

        // Finished, allow to pass.
        return null;
    }

 // Copia de ItemRequestResponseAction
    private void processSendDocuments(Context context, RequestItem requestItem,Item item,String title) throws SQLException, MessagingException, IOException, AuthorizeException {
	String token=Utils.generateHexKey();
	String url=generateUrl(token);
    	Email email=Email.getEmail( I18nUtil.getEmailFilename(context.getCurrentLocale(), "enviarTokenJuicio"));
       	email.addRecipient(requestItem.getReqEmail());
        email.addArgument(getTitle(item));//{1}
        email.addArgument(getAuthors(item));//{2}
        email.addArgument(item.getHandle());//{3}
        email.addArgument(url);//{4}
        email.addArgument("");//{5}
        email.addArgument(ConfigurationManager.getProperty("mail.feedback.recipient"));//{6}
        if (requestItem.isAllfiles()){
            Bundle[] bundles = item.getBundles("ORIGINAL");
            for (int i = 0; i < bundles.length; i++){
                Bitstream[] bitstreams = bundles[i].getBitstreams();
                for (int k = 0; k < bitstreams.length; k++){
                    if (!bitstreams[k].getFormat().isInternal() /*&& RequestItemManager.isRestricted(context, bitstreams[k])*/){
                        email.addAttachment(BitstreamStorageManager.retrieve(context, bitstreams[k].getID()), bitstreams[k].getName(), bitstreams[k].getFormat().getMIMEType());
                    }
                }
            }
        } else {
            Bitstream bit = Bitstream.find(context,requestItem.getBitstreamId());
            email.addAttachment(BitstreamStorageManager.retrieve(context, requestItem.getBitstreamId()), bit.getName(), bit.getFormat().getMIMEType());
        }     
        
        email.send();
        
        RevisionToken rt=new RevisionToken(requestItem.getReqEmail(),"J",token,item.getHandle());
        rt.create(context);

        requestItem.setDecision_date(new Date());
        requestItem.setAccept_request(true);
        requestItem.update(context);
	}
    
    // Solo pueden hacer juicios los que tengan revisiones de ese mismo item (y no tengan ya un juicio)
    private boolean comprobarMailPuedeEmitirJuicios(String email, String handle, Context context) throws IOException, SQLException, AuthorizeException {
	if(StringUtils.isNotBlank(handle) && StringUtils.isNotBlank(email)){
	    //Si ya tiene uno no puede hacer mas
	    ArrayList<RevisionToken> juiciosToken=RevisionToken.findJuiciosOfHandle(context, handle);
	    for(int i=0;i<juiciosToken.size();i++){
		if(juiciosToken.get(i).getEmail().equalsIgnoreCase(email)){
		    return false;
		}
	    }
	    
	    //Si tiene revision puede
	    DSpaceObject dso=HandleManager.resolveToObject(context, handle);
	    RevisionToken revisionToken=RevisionToken.findItemOfRevision(context, dso.getID());
	    if(revisionToken!=null){
        	    ArrayList<RevisionToken> revisiones=RevisionToken.findRevisionsOfHandle(context, revisionToken.getHandleRevisado());
        	    for(int i=0;i<revisiones.size();i++){
        		if(revisiones.get(i).getEmail().equalsIgnoreCase(email)){
        		    return true;
        		}
        	    }
	    }
	    
	    //Si es admin puede a cualquiera
	    if(AuthorizeManager.isAdmin(context)){
		return true;
	    }
	    
	  //Si el autor autenticado es uno de los autores del item puede
	    EPerson eperson=context.getCurrentUser();
	    //Si el email es el del autenticado o admin
	    if(eperson!=null && email.equalsIgnoreCase(eperson.getEmail())){
		//y el autenticado es autor
		Item item=null;
		if(dso.getType()==Constants.ITEM){
		    item=((Item)dso);
		}
		if(eperson!=null && item!=null){
		    //autor macheando nombre
		    Metadatum[] autores = item.getMetadata("dc","contributor","author",Item.ANY);
		    for(int i=0;i<autores.length;i++){
			if(autores[i].value.equalsIgnoreCase(eperson.getFullName())){
			    //esta, asi que se permiten envios a 
			    return true;
			}
		    }
		}
	    }
	}
	return false;
    }

    private String getAuthors(Item item) {
    	Metadatum[] author = item.getMetadata("dc","contributor","author",Item.ANY);
    	if(author!=null && author.length>0){
    		StringBuffer sb=new StringBuffer();
    		for(int i=0;i<author.length;i++){
    			if(i!=0){
    				sb.append("; ");
    			}
    			sb.append(author[i].value);
    		}
    		return sb.toString();
    	}
    	return "Sin autor indicado";
	}
    
    private String generateUrl(String token) {
   	StringBuffer sb=new StringBuffer();
   	sb.append(ConfigurationManager.getProperty("dspace.url"));
//   	sb.append("/handle/");
//   	sb.append(ConfigurationManager.getProperty("openaire.coleccion.evaluaciones"));
//   	sb.append("/submit?tokenEvaluacion=");
   	sb.append("/submit/");
   	sb.append(token);
   	sb.append("/juicio");
   	return sb.toString();
       }
    
    private String getTitle(Item item) {
    	Metadatum[] title = item.getMetadata("dc","title",null,Item.ANY);
    	if(title!=null && title.length>0){
    		return title[0].value;
    	}
    	return "Sin titulo";
    }
    
    private static boolean comprobarMailCorrecto(String email) {
	//Vacio es correcto
	if(StringUtils.isEmpty(email)){
	    return true;
	}
	String PATTERN_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	Pattern pattern = Pattern.compile(PATTERN_EMAIL);
	Matcher matcher = pattern.matcher(email);
	if(matcher.matches()){
	    return true;
	}
	return false;
    }

}
