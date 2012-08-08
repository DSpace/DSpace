/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */

package gr.ekt.repositories.dspace.utils;

import java.sql.SQLException;

import java.util.ArrayList;
import java.util.List;
import org.dspace.app.util.DCInput;
import org.dspace.app.util.DCInputSet;
import org.dspace.app.util.DCInputsReader;
import org.dspace.app.util.DCInputsReaderException;
import org.dspace.content.Collection;
import org.dspace.content.DCValue;
import org.dspace.content.Item;
import java.util.Locale;
import org.dspace.core.I18nUtil;

/**
 *
 * @author iostath
 */
public class Utilities {
 
    
public static List getControlledVocabulariesDisplayValue(Item item, DCValue[] values, String schema, String element, String qualifier) throws SQLException, DCInputsReaderException{
    
            List toReturn=new ArrayList<String>();
            DCInput myInputs = null;
            boolean myInputsFound=false;
   
            DCInputSet inputSet = null;
           
            //Read the input form file for the specific collection
            DCInputsReader inputsReader = null;
	    String col_handle = "";
		
            Collection collection = item.getOwningCollection();

		if (collection==null)
                {
			col_handle = "db-id/" + item.getID();
		}
		else {
			col_handle = collection.getHandle(); 
              	}
		if (inputsReader == null) {
                    // read configurable submissions forms data
                    inputsReader = new DCInputsReader();

		}
               inputSet = inputsReader.getInputs(col_handle);

               
               	//Replace the values of DCValue[] with the correct ones in case of controlled vocabularies
		String currentField = schema+"."+element+(qualifier==null?"":"."+qualifier);
				
		if (inputSet!=null){
                   
                   int pageNums= inputSet.getNumberPages();

                   for (int p=0;p<pageNums;p++){
                    DCInput[] inputs = inputSet.getPageRows(p, false, false);
                   

                    if (inputs!=null){
			
                        for (int i=0; i<inputs.length;i++){
                            String inputField=inputs[i].getSchema()+"."+inputs[i].getElement()+(inputs[i].getQualifier()==null?"":"."+inputs[i].getQualifier());
                            if (currentField.equals(inputField)){
                                
                                   myInputs = inputs[i];
                                   myInputsFound=true;
                                   break;
                                   
                                }
                            }
                        }
                    if (myInputsFound) break;
                    }        
                 }   
                    if (myInputsFound) {    
                     
                              for (int j = 0; j < values.length; j++){
                              
                                    String pairsName = myInputs.getPairsType();
                                    String stored_value = values[j].value;
                                    String displayVal = myInputs.getDisplayString(pairsName,stored_value);
                                    
                                    if (displayVal!=null && !"".equals(displayVal)){
                                    
                                        toReturn.add(displayVal);
                                     }
                                   
                                }
                             }
                        
			
		
		return toReturn;
	}

public static String getControlledVocabulariesDisplayValueLocalized(Item item, DCValue[] values, String schema, String element, String qualifier,  Locale locale) throws SQLException, DCInputsReaderException{
    
            String toReturn="";
            DCInput myInputs = null;
            boolean myInputsFound=false;
            String formFileName = I18nUtil.getInputFormsFileName(locale);
            DCInputSet inputSet = null;
          
            //Read the input form file for the specific collection
            DCInputsReader inputsReader = null;
	    String col_handle = "";
		
            Collection collection = item.getOwningCollection();

		if (collection==null)
                {
			col_handle = "db-id/" + item.getID();
		}
		else {
			col_handle = collection.getHandle(); 
              	}
		if (inputsReader == null) {
                    // read configurable submissions forms data
                    inputsReader = new DCInputsReader(formFileName);

		}
               inputSet = inputsReader.getInputs(col_handle);

               
               	//Replace the values of DCValue[] with the correct ones in case of controlled vocabularies
		String currentField = schema+"."+element+(qualifier==null?"":"."+qualifier);
				
		if (inputSet!=null){
                    
                  int pageNums= inputSet.getNumberPages();

                   for (int p=0;p<pageNums;p++){
                    
                        DCInput[] inputs = inputSet.getPageRows(p, false, false);
                    
                    
                    if (inputs!=null){
                        
			
                        for (int i=0; i<inputs.length;i++){
                            String inputField=inputs[i].getSchema()+"."+inputs[i].getElement()+(inputs[i].getQualifier()==null?"":"."+inputs[i].getQualifier());
                            if (currentField.equals(inputField)){
                                
                                   myInputs = inputs[i];
                                   myInputsFound=true;
                                   break;
                                   
                                }
                            }
                        }
                        if (myInputsFound) break;
                      }
                    }           
                    
                    if (myInputsFound) {    
                     
                              for (int j = 0; j < values.length; j++){
                              
                                    String pairsName = myInputs.getPairsType();
                                    String stored_value = values[j].value;
                                    String displayVal = myInputs.getDisplayString(pairsName,stored_value);
                                    
                                    if (displayVal!=null && !"".equals(displayVal)){
                                    
                                        return displayVal;
                                     }
                                   
                                }
                             }
                        
			
		
            return toReturn;
	}

public static List getControlledVocabulariesMultilingualValues(Item item, DCValue[] values, String schema, String element, String qualifier, Locale[] supportedLanguages) throws SQLException, DCInputsReaderException{
    
            List toReturn=new ArrayList<String>();
            DCInput myInputs = null;
            boolean myInputsFound=false;
   
            DCInputSet inputSet = null;
           
            //Read the input form file for the specific collection
            DCInputsReader inputsReader = null;
	    String col_handle = "";
		
            Collection collection = item.getOwningCollection();

		if (collection==null)
                {
			col_handle = "db-id/" + item.getID();
		}
		else {
			col_handle = collection.getHandle(); 
              	}
		if (inputsReader == null) {
                    // read configurable submissions forms data
                    inputsReader = new DCInputsReader();

		}
               inputSet = inputsReader.getInputs(col_handle);

               
               	//Replace the values of DCValue[] with the correct ones in case of controlled vocabularies
		String currentField = schema+"."+element+(qualifier==null?"":"."+qualifier);
				
		if (inputSet!=null){
                    
                    int pageNums= inputSet.getNumberPages();
                    
                    for (int p=0;p<pageNums;p++){
                      
                        DCInput[] inputs = inputSet.getPageRows(p, false, false);

                        if (inputs!=null){
			
                        for (int i=0; i<inputs.length;i++){
                            String inputField=inputs[i].getSchema()+"."+inputs[i].getElement()+(inputs[i].getQualifier()==null?"":"."+inputs[i].getQualifier());
                            if (currentField.equals(inputField)){
                                
                                   myInputs = inputs[i];
                                   myInputsFound=true;
                                   break;
                                   
                                }
                              }
                            }
                     if (myInputsFound) break;
                        }
                    }           
                    
                    if (myInputsFound) {    
                     
                              for (int j = 0; j < values.length; j++){
                              
                                    String pairsName = myInputs.getPairsType();
                                    String stored_value = values[j].value;
                                    String displayVal = myInputs.getDisplayString(pairsName,stored_value);
                                    
                                    if (displayVal!=null && !"".equals(displayVal)){
                                    
                                        
                                        for (int i=0;i<supportedLanguages.length;i++){
                                                Locale locale= (Locale) supportedLanguages[i];
                                                toReturn.add(I18nUtil.getMessage(displayVal,locale));
                                        }
                                   
                                }
                             }
                        
                    }
		
		return toReturn;
	}
 }
    
    

