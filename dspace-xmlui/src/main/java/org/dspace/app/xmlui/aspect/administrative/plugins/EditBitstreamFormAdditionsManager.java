/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
package org.dspace.app.xmlui.aspect.administrative.plugins;

import org.apache.log4j.Logger;
import org.dspace.utils.DSpace;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Created by Philip Vissenaekens (philip at atmire dot com)
 * Date: 26/10/15
 * Time: 16:54
 */

@Component
public class EditBitstreamFormAdditionsManager {

    private static Logger log = Logger.getLogger(EditBitstreamFormAdditionsManager.class);

    private Map<Integer,List<EditBitstreamFormAddition>> editBitstreamFormMap;

    public static EditBitstreamFormAdditionsManager getInstance(){
        List<EditBitstreamFormAdditionsManager> servicesByType = new DSpace().getServiceManager().getServicesByType(EditBitstreamFormAdditionsManager.class);
        if(servicesByType.size()>0){
            return servicesByType.get(0);
        }else{
            log.error("EditBitstreamFormAdditionsManager service not found.");
        }
        return null;
    }

    @Autowired(required=false)
    public void seteditBitstreamFormAdditions(List<EditBitstreamFormAddition> editBitstreamForms) {
        Map<Integer, List<EditBitstreamFormAddition>> editBitstreamFormMap = new HashMap<>();
        for (EditBitstreamFormAddition editBitstreamForm : editBitstreamForms){
            int dsoType = editBitstreamForm.getDsoType();
            List<EditBitstreamFormAddition> editBitstreamFormList = editBitstreamFormMap.get(dsoType);
            if (editBitstreamFormList == null) {
                editBitstreamFormList = new ArrayList<>();
            }
            editBitstreamFormList.add(editBitstreamForm);
            editBitstreamFormMap.put(dsoType, editBitstreamFormList);

        }
        this.editBitstreamFormMap = editBitstreamFormMap;
    }

    public EditBitstreamFormAddition findeditBitstreamFormAddition(int dsoType, String key){
        if(editBitstreamFormMap!=null && editBitstreamFormMap.containsKey(dsoType)) {
            List<EditBitstreamFormAddition> editBitstreamForms = editBitstreamFormMap.get(dsoType);
            for (EditBitstreamFormAddition editBitstreamForm : editBitstreamForms) {
                if (editBitstreamForm.getKey().equals(key)) {
                    return editBitstreamForm;
                }
            }
        }
        return null;
    }
}
