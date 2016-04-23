/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.ctask.arvo;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.MessagingException;

import org.apache.log4j.Logger;
import org.dspace.app.requestitem.RequestItem;
import org.dspace.authorize.AuthorizeException;
import org.dspace.content.Bitstream;
import org.dspace.content.Bundle;
import org.dspace.content.DSpaceObject;
import org.dspace.content.Item;
import org.dspace.content.Metadatum;
import org.dspace.content.RevisionToken;

import es.arvo.dspace.content.authority.AuthorityOpenaireRevisor;
import es.arvo.openaire.reputation.ReputationCalculator;

import org.dspace.core.ConfigurationManager;
import org.dspace.core.Constants;
import org.dspace.core.Context;
import org.dspace.core.Email;
import org.dspace.core.I18nUtil;
import org.dspace.core.Utils;
import org.dspace.curate.AbstractCurationTask;
import org.dspace.curate.Curator;
import org.dspace.curate.Suspendable;
import org.dspace.storage.bitstore.BitstreamStorageManager;

/**
 *
 * @author Adán Román Ruiz
 */
@Suspendable
public class RevisorTokenEnviarCurationTask extends AbstractCurationTask
{
    private static Logger log = Logger.getLogger(RevisorTokenEnviarCurationTask.class);
	private static final String PATTERN_EMAIL = "^[_A-Za-z0-9-\\+]+(\\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})$";
	 
	public static String MESSAGE_REQUEST_COPY="Revisor del item indicado por el submiter";
	// map of required fields
	private Map<String, List<String>> reqMap = new HashMap<String, List<String>>();

	@Override 
	public void init(Curator curator, String taskId) throws IOException
	{
		super.init(curator, taskId);

	}

	/**
	 * Perform the curation task upon passed DSO
	 *
	 * @param dso the DSpace object
	 * @throws IOException
	 */
	@Override
	public int perform(DSpaceObject dso) throws IOException
	{
	    // TEMP __ BORRAR
//	    try {
//		ReputationCalculator.main(new String[0]);
//	    } catch (Exception e1) {
//		e1.printStackTrace();
//	    }
	    // FIN BORRAR
		try {
			if (dso.getType() == Constants.ITEM){
				Context context=this.curator.curationContext();
				Item item = (Item)dso;
				String handle = item.getHandle();
				log.info("Enviando mails a los revisores del handle:"+handle);
				Metadatum[] revisores = item.getMetadata("oprm","revisor","mail",Item.ANY);
				Metadatum[] revisoresBBDD = item.getMetadata("oprm","revisor","bbdd",Item.ANY);
				Vector<String> revisoresMails=obtenerRevisores(revisores,revisoresBBDD);
				if (handle == null)
				{
					log.info("No se envian mais. El handle es null");
					// we are still in workflow - no hay nada que hacer
					setResult("Object skipped");
					return Curator.CURATE_SKIP;
				}else if(revisoresMails==null || revisoresMails.size()==0){
					log.info("No se envian mais. No hay revisores");
					// No hay revisores, pasamos del item
					setResult("Object skipped");
					return Curator.CURATE_SKIP;
				}else{
					ArrayList tokens=new ArrayList();
					Bundle[] bundles=item.getBundles("ORIGINAL");	
					if(bundles.length==0){
						setResult("Object dont have files in ORIGINAL bundle");
						return Curator.CURATE_ERROR;
					}
					for(int k=0;k<revisoresMails.size();k++){
						RequestItem ri=new RequestItem(item.getID(), -1, revisoresMails.get(k), revisoresMails.get(k), MESSAGE_REQUEST_COPY, true);
						log.info("Enviando mail a "+ri.getReqEmail());
						processSendDocuments(context,ri,item,getTitle(item));
						log.info("Mail enviado");
//					for (int i=0;i<bundles.length;i++){
//						Bundle bundle=bundles[i];
//						Bitstream[] bitstreams=bundle.getBitstreams();
//						for(int j=0;j<bitstreams.length;j++){
//							Bitstream bitstream=bitstreams[j];
//							
////							String token=ri.getNewToken(context);
////							//Lo aceptamos directamente
////							ri.setAccept_request(true);
////							ri.setDecision_date(new Date());
////							ri.update(context);
////							tokens.add(token);
//						}
//					}
//					if(tokens.size()==0){
//						// Hay revisores y no hay ficheros?? error
//						setResult("Object dont have files in ORIGINAL bundle");
//						return Curator.CURATE_ERROR;
//					}else{
//						Email email=Email.getEmail( "enviarTokenRevisor");
//				        email.addRecipient(revisores[k].value);
//				        email.addArgument(getLinkTokenEmail(context,ri));
//				        email.addArgument(SendIt);
//				        email.addArgument(arg);
//				        email.addArgument(arg);
//				        email.send();
//					}
					}
					context.commit();
					setResult("OK");
					return Curator.CURATE_SUCCESS;

				}
			}
			else
			{
				setResult("Object skipped");
				return Curator.CURATE_SKIP;
			}
		} catch (SQLException e) {
			log.error("Error sql "+e.getMessage());
			setResult("Excepcion Sql en tarea de curacion");
			return Curator.CURATE_ERROR;
		} catch (MessagingException e) {
			log.error("Error MessagingException "+e.getMessage());
			setResult("Excepcion enviando mail en la tarea de curacion");
			return Curator.CURATE_ERROR;
		} catch (AuthorizeException e) {
			log.error("Error AuthorizeException "+e.getMessage());
		    setResult("Excepcion de autorizacion en la tarea de curacion");
			return Curator.CURATE_ERROR;
		}
	}

	private Vector obtenerRevisores(Metadatum[] revisores,Metadatum[] revisoresBBDD) {
		 Pattern pattern = Pattern.compile(PATTERN_EMAIL);
		 		 
		Vector mails=new Vector();
		if(revisores!=null && revisores.length>0){
			for(int i=0;i<revisores.length;i++){
				 Matcher matcher = pattern.matcher(revisores[i].value);
				 if(matcher.matches()){
					 mails.add(revisores[i].value);
				 }
			}
		}
		if(revisoresBBDD!=null && revisoresBBDD.length>0){
			AuthorityOpenaireRevisor authority=new AuthorityOpenaireRevisor();
			for(int i=0;i<revisoresBBDD.length;i++){
				 String mail=authority.getMail(revisoresBBDD[i].authority);
				 if(mail!=null){
					 Matcher matcher = pattern.matcher(mail);
					 if(matcher.matches()){
						 mails.add(mail);
					 }
				 }
			}
		}
		return mails;
	}

	/**
	 * Copia de SendItemRequestAction
	 * @param context
	 * @param requestItem
	 * @return
	 * @throws SQLException
	 */
    public String getLinkTokenEmail(Context context, RequestItem requestItem)
            throws SQLException
    {
        String base = ConfigurationManager.getProperty("dspace.url");

        String specialLink = (new StringBuffer()).append(base).append(
                base.endsWith("/") ? "" : "/").append(
                "itemRequestResponse/").append(requestItem.getNewToken(context))
                .toString()+"/";

        return specialLink;
    }
    
    // Copia de ItemRequestResponseAction
    private void processSendDocuments(Context context, RequestItem requestItem,Item item,String title) throws SQLException, MessagingException, IOException, AuthorizeException {
	String token=Utils.generateHexKey();
	String url=generateUrl(token);
    	Email email=Email.getEmail( I18nUtil.getEmailFilename(context.getCurrentLocale(), "enviarTokenRevisor"));
       	email.addRecipient(requestItem.getReqEmail());
        email.addArgument(getTitle(item));//{0}
        email.addArgument(getAuthors(item));//{1}
        email.addArgument(item.getHandle());//{2}
        email.addArgument(url);//{3}
        email.addArgument(ConfigurationManager.getProperty("dspace.name"));//{4}
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
        
        RevisionToken rt=new RevisionToken(requestItem.getReqEmail(),"R",token,item.getHandle());
        rt.create(context);

        requestItem.setDecision_date(new Date());
        requestItem.setAccept_request(true);
        requestItem.update(context);
	}

    private String generateUrl(String token) {
	StringBuffer sb=new StringBuffer();
	sb.append(ConfigurationManager.getProperty("dspace.url"));
//	sb.append("/handle/");
//	sb.append(ConfigurationManager.getProperty("openaire.coleccion.evaluaciones"));
//	sb.append("/submit?tokenEvaluacion=");
	sb.append("/submit/");
	sb.append(token);
	sb.append("/revision");
	return sb.toString();
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

	private String getTitle(Item item) {
    	Metadatum[] title = item.getMetadata("dc","title",null,Item.ANY);
    	if(title!=null && title.length>0){
    		return title[0].value;
    	}
    	return "Sin titulo";
    }
}
