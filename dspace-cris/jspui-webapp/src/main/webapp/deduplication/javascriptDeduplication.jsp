<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    https://github.com/CILEA/dspace-cris/wiki/License

--%>
<%@ page import="org.dspace.core.ConfigurationManager" %>
<!-- javascript deduplication -->
		<% if (ConfigurationManager.getBooleanProperty("deduplication","deduplication.submission.feature",false)) {%>
			
			j("#duplicatebox").modal({show: false});
			
			j("#duplicateboxignoremsg").modal({show:false});
		
			j(".modal").click(function(e){  
				if(j(".btn-group").hasClass('open')) {
		        	e.stopPropagation();  
		        }
		        if(j(".btn-group").hasClass('open')) {
		        	j(".btn-group").removeClass('open');
		        }
		    });
			
			j("#cancelpopup").click(function() {
				j('#submit_cancel').click();
				j('#cancelButton').click();
			});
			
			j("#ignoreduppopup").click(function(evt) {
				var mynote = j("#duplicateboxignoremsgTextarea").val();
				var maxLength = 256;
	           	var mynoteLength = mynote.length;
				if (mynoteLength < maxLength && (mynote != null && mynote.replace(/\s/g,"") != ""))
				{
					var iid = j('#duplicateboxignoremsg').data().iid;
					var did = j('#duplicateboxignoremsg').data().did;
					var dedupid = j('#duplicateboxignoremsg').data().dedupid;
					var typeid = j('#duplicateboxignoremsg').data().typeid;
					var row = j('#duplicateboxignoremsg').data().row;
					
					/* ignoreDup(iid, did, mynote); */
					dupToFixEvent(iid, did, dedupid, null, mynote, typeid); 
					j(row).parents('tr').remove();
					
					var rowCount = j('#duplicateboxtable tr').length;
					if(rowCount>1) {
						j("#duplicatebox").modal("show");
					}
				} else {
					j('#duplicateboxignoremsg .modal-body .alert-danger').removeClass("hidden");
					evt.stopPropagation();		
				}
			});
			
			j("#ignoreduppopupcancel").click(function() {
				j('#duplicateboxignoremsg').data(null);				
				j("#duplicatebox").modal("show");
				j('#duplicateboxignoremsg .modal-body .alert-danger').addClass("hidden");
			});
			
			checkDuplicates(<%= item.getID() %>, <%= item.getType() %>, <%= si.isInWorkflow() || si.isEditing() %>);
			 
		<% } %>