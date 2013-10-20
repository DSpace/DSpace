/*
 * ReqEmail.java
 *
 * Created on 27 de Marco de 2006, 14:56 by Arnaldo Dantas
 *
* This class serve as a util class for mail options
* Use this class to define mail properties.
* 
* use the function to set the fileds you want and then call sendMessage
* function.
 */

package org.dspace.app.webui.util;

import java.io.InputStream;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
* 
* @author Arnaldo Dantas
*/


public class ReqEmail { 
  
  //fields for message
  private Multipart g_multiPart=new MimeMultipart();     //"Container" for diffrent message parts
  
  /** The arguments to fill out */
  private List arguments;
  
  /* message formated */
  private boolean formated;
    
  //fields for addresses
  private String g_host                    ="localhost";     //mail server
  private String g_message               =null;     //the body message
  private String g_subject               =null;     //the body message
  private InternetAddress[] g_from     =null;     //from adress
  private InternetAddress[] g_to          =null;     //array for To adresses
  private InternetAddress[] g_cc         =null;     //array for CC adresses
  private InternetAddress[] g_bcc          =null;     //array for BCC adresses
  //delimeter between adresses
  private static String ADDRESS_DELIMETERS = " ,;";
  
  /** mail host server field identifier*/
  public static final int FIELD_HOST          =1000;
  /** from address field identifier */
  public static final int FIELD_FROM          =1001;
  /** TO address field identifier */
  public static final int FIELD_TO          =1002;
  /** CC address field identifier */
  public static final int FIELD_CC          =1003;
  /** BCC address field identifier */
  public static final int FIELD_BCC          =1004;
  /** MEssage field identifier */
  public static final int FIELD_MESSAGE     =1005;
  /** Subject field identifier */
  public static final int FIELD_SUBJECT     =1006;
  /** DELIMETER field identifier */
  public static final int FIELD_DELIMETER     =1007;


  public ReqEmail()
  {
    arguments = new ArrayList();
    g_host = "localhost";
    g_message = null;
    g_subject = null;
    g_from = null;
    g_to = null;
    g_cc = null;
    g_bcc = null;
    formated = false;
  }
  
  
  /**
   * Set field 
   
   * @param p_fields          field in message
   * @param p_text          string which conatins values
   */
  public void setField(int p_fields , String p_text){
      
      try{
          switch(p_fields){
              
              case FIELD_HOST:
                  g_host = p_text;     
                  break;
                  
              case FIELD_MESSAGE:
                  g_message = p_text;     
                  break;
                  
              case FIELD_FROM:
                  g_from = convertToAddress(p_text);     
                  break;
                  
              case FIELD_SUBJECT:
                  g_subject = p_text;
                  break;
                  
              case FIELD_TO:
                  g_to = convertToAddress(p_text);
                  break;
                  
              case FIELD_CC:
                  g_cc = convertToAddress(p_text);
                  break;
                  
              case FIELD_BCC:
                  g_bcc = convertToAddress(p_text);
                  break;
                  
              case FIELD_DELIMETER:
                  ADDRESS_DELIMETERS = p_text;
                  break;
              }
          }catch(Exception e){
          System.out.println("Exception while trying to set field "+p_fields);
          }
      }
 
   /**
    * Fill out the next argument in the template
    * 
    * @param arg
    *            the value for the next argument
    */
   public void addArgument(Object arg)
   {
       arguments.add(arg);
   }

   /**
    * Format the mail message for givem Arguments
    *
    **/
   public boolean formatMessage ()
   {
       boolean format = false;
       if (!formated && g_message != null)
       {
        Object[] args = arguments.toArray();
        g_message = KeepMessageFormat.format(g_message, args);
        formated = format = true;
       }
       return format;
   }
  
  /**
   * Get field by its identifier
   *
   * @param p_identifier      identifier of field which we need
   * @return                    field which we asked for
   */
  public InternetAddress[] getAddresses(int p_identifier){
      
      switch(p_identifier){
          
          case FIELD_TO:
              return g_to;     
              
          case FIELD_CC:
              return g_cc;     
              
          case FIELD_BCC:
              return g_bcc;     
          }
      return null;
      }
  
  /**
   * Get field 
   * @return          mail server host
   */
  public String getHost(){
      return g_host;     
      }
  
  /**
   * Get message 
   * @return          the body message 
   */
  public String getMessage(){
      return g_message;     
      }
  
  /**
   * Get Subject of message
   * @return          the body message 
   */
  public String getSubject(){
      return g_subject;     
      }
  
  
  /**
   * Sets a new Delimeter between addresses
   * Default is ","
   * 
   * @param p_del          new delimeter
   */
  public void setDelimeter(String p_del){
      ADDRESS_DELIMETERS = p_del;
      }
  
  /**
   * Convert string to InternetAddress[].
   * the function gets a string of addresses seperated by delimeter
   * and parse the addresses from it
   * 
   * @param  p_string          string with new data
   * @return array 
   */
  public InternetAddress[] convertToAddress(String p_string){
      StringTokenizer l_strToken=null;
      InternetAddress[] l_reply=null;
      
      l_strToken = new StringTokenizer(p_string , ADDRESS_DELIMETERS , false);
      l_reply = new InternetAddress[l_strToken.countTokens()];
      
      int ind=0;
      try{
          while(l_strToken.hasMoreElements())     
              l_reply[ind++] = new InternetAddress(l_strToken.nextToken());
          
          return l_reply;
          }catch(Exception e){
          System.out.println("Exception while trying to generate adresses "+e);
          }
      return null;
      }
  
  
  //////////////////////////////////////////////////////////
  //                      ATTACHMENTS SECTION                         //
  //////////////////////////////////////////////////////////
  
  /** 
   * add file attachment to the message
   * 
   * @param p_fileName     file name to attach to message
   */
  public void addAttachment(InputStream inputStream, String name, String contentType){
      //create attachment      
      try{
          MimeBodyPart mbp = new MimeBodyPart();
          DataSource ds = new InputStreamDataSource(name, contentType, inputStream);
          mbp.setDataHandler(new DataHandler(ds));
          mbp.setFileName(name);
          g_multiPart.addBodyPart(mbp);
          
          }catch(Exception e){
          System.out.println("Exception in: addAttachment");
          }
      }
  
  /** 
   * set the body text of this message
   * 
   * 
   */
  private void setBodyText(){
      //create attachment      
      try{
          MimeBodyPart mbp = new MimeBodyPart();          
          mbp.addHeader("charset","UTF-8");
          mbp.setText(g_message,"UTF-8");
          g_multiPart.addBodyPart(mbp);
          
          }catch(Exception e){
          System.out.println("Exception inset body text");
          }
      }
  
  
  //////////////////////////////////////////////////////////
  /**
   * This function will send an e-mail message 
   */
  public void sendMessage() throws MessagingException 
  {
      //do some check before mailing:
      if (g_host == null){
          System.out.println("Please set a mail server first");
          return;
          }
      
      if (g_to == null){
          System.out.println("Field 'TO' was not filled");
          return;
          }
      
      Properties l_props = System.getProperties();
      Session l_session;
      Message l_message;
      
          //set Session & msg
          l_props.put("mail.smtp.host" , getHost() );
          l_session=Session.getDefaultInstance( l_props , null);              
          l_message = new MimeMessage(l_session);

          //g_from=convertToAddress(FROM_FIELD);
          //add addresses fields to the message
          if (g_from != null)
              l_message.addFrom(g_from);

          if (g_to != null)
              l_message.addRecipients(Message.RecipientType.TO  , g_to);
          
          if (g_cc != null)
              l_message.addRecipients(Message.RecipientType.CC  , g_cc);

          if (g_bcc != null)
              l_message.addRecipients(Message.RecipientType.BCC , g_bcc);
          
          if (g_message != null)
              setBodyText();
          
          if (g_subject != null)
              l_message.setSubject(g_subject);
          
          l_message.setContent( g_multiPart);
          //l_message.setContent(g_message , "text/plain");
          Date now = new Date();
          l_message.setSentDate(now);
          Transport.send(l_message);
          
      }
  }