/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
DUPLICATION_COL = [];
I18N_LOADERROR = "Error";
duplicationData = new Object();
var dupTable = null;

fnServerObjectToArrayDedup = function (tableid, typeID, check)
{
    return function ( sSource, aoData, fnCallback ) {
    	j.ajax( {
            "dataType": 'json',
            "type": "POST",
            "url": sSource,
            "data": aoData,
            "error": function(data){
            	if (!duplicationData.error)
            	{
            		duplicationData.error = true;
            		alert(I18N_LOADERROR);
            	}
            },
            "success": function (json) {
            	if (json == null || json.error)
            	{
            		if (!duplicationData.error)
                	{
            			duplicationData.error = true;
            			alert(I18N_LOADERROR);
                	}		
            		json = new Object();
            		json.aaData = [];
            		return;
            	}

            	duplicationData[tableid] = new Object();
            	duplicationData["dedupnotecontainer"] = new Object();
    			var duplicates = json.potentialDuplicates;
    			if (duplicates!=null && duplicates.length > 0)
    			{
						for (var i = 0, iLen = duplicates.length; i < iLen; i++) {
							duplicationData[tableid][''
									+ duplicates[i].duplicateItem['entityID']] = duplicates[i];
							if(duplicates[i].note) {
								duplicationData["dedupnotecontainer"][''
													+ duplicates[i].duplicateItem['entityID']] = duplicates[i].note;
							}
						}
						fnCallback(json);
						j("#duplicatebox").modal("show");
    			}
    			else {
    				json.aaData = [];
    				j("#duplicatebox").modal("hide");
    				return;
    			}                
            }
        } );
    };
};



function checkDuplicates(itemID, typeID, check)
{
		dupTable  = j('#duplicateboxtable').dataTable( {
			"bAutoWidth" : false,
			"bJQueryUI": false,
			"bPaginate": false,
			"bLengthChange": false,
			"sDom":	
				"<'row'<'col-sm-5'l><'col-sm-6'f>>"+
				"<'row'<'col-sm-12'r>>"+
				"<'row'<'col-sm-12't>>",
			"bFilter": false,
			"bSort": false,
			"bInfo": true,
			"bServerSide": true,
			"bProcessing": true,			
			"fnDrawCallback": function( oSettings ) {				
				fnDraCallbackDedup(itemID, typeID, check);
			 },
			"sAjaxSource": dspaceContextPath+"/json/duplicate?itemid="+itemID+"&check="+check+"&typeid="+typeID,
			"fnServerData": fnServerObjectToArrayDedup("duplicateboxtable", typeID, check),
			"aoColumns": DUPLICATION_COL
		} );
		
}