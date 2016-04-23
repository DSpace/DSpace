/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.authorprofile.administrative.configuration;

import org.dspace.app.xmlui.wing.Message;

import java.util.*;

/**
 *
 * @author Roeland Dillen (roeland at atmire dot com)
 * @author Kevin Van de Velde (kevin at atmire dot com)
 * @author Ben Bosman (ben at atmire dot com)
 * @author Mark Diggory (markd at atmire dot com)
 */
public class ValidationReport {

    private Map<Validator,Set<String>> validators=new HashMap<Validator,Set<String>>();


    public static ValidationReport create(Validator validator,String value){
            return new ValidationReport(validator,new HashSet<String>(Collections.singleton(value)));
    }

    private ValidationReport(Validator validator, Set<String> value) {
         validators.put(validator,value);
    }

    public void add(Validator validator,String value){
         if(validators.containsKey(validator)){
             validators.get(validator).add(value);
         } else {
             validators.put(validator,new HashSet<String>(Collections.singleton(value)));
         }


    }


    public List<Message> getErrorMessage(String prefix,String suffix){
        LinkedList<Message> messages=new LinkedList<Message>();
        for(Validator validator:validators.keySet()){
            messages.add(validator.getErrorMessage(prefix,suffix).parameterize(validators.get(validator)));
        }
        return messages;
    }

    public List<Message> getErrorMessage(String prefix,String suffix,String value){
        LinkedList<Message> messages=new LinkedList<Message>();
        for(Validator validator:validators.keySet()){
            if(validators.get(validator).contains(value))
                messages.add(validator.getErrorMessage(prefix,suffix).parameterize(validators.get(validator)));
        }
        return messages;
    }
}
