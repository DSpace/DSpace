/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * http://www.dspace.org/license/
 */
submissionLookupIdentifiers = function(identInputs){
	var mydata = new Object();
	mydata['s_uuid'] = j('#suuid-identifier').val();
	mydata['type'] = 'identifiers';
	for (var i=0;i<identInputs.length;i++)
	{
		mydata[j(identInputs[i]).attr('id')] = j(identInputs[i]).val();
	}
	var ajaxCall = j.ajax({url: dspaceContextPath+"/json/submissionLookup", 
		type: "POST",
		dataType: "json", 
		async: true,
		contentType: "application/x-www-form-urlencoded;charset=UTF-8", 
		data: mydata, 
		error: function(info){
			j('#loading-search-result').modal("hide");
			var message = j('#jserrormessage').text();
			alert(message);
		},
		success: function(info) {
			if (info == null || info.result == null || info.result.length == 0)
			{
				j('#result-list').hide();
				j('#empty-result').show();
			}
			else
			{
				submissionLookupShowResult(info, "-identifier");
			}
			j('#loading-search-result').modal("hide");
			j('#tabs').find('a[href="#tabs-result"]').click();
		}
	});
	j('#loading-search-result').data('ajaxCall', ajaxCall);
	j('#loading-search-result').modal("show");
}

submissionLookupSearch = function(){
	var mydata = new Object();
	mydata['s_uuid'] = j('#suuid-search').val();
	mydata['type'] = 'search';
	mydata['title'] = j('#search_title').val();
	mydata['authors'] = j('#search_authors').val();
	mydata['year'] = j('#search_year').val();
	var ajaxCall = j.ajax({url: dspaceContextPath+"/json/submissionLookup", 
		type: "POST",
		dataType: "json", 
		async: true,
		contentType: "application/x-www-form-urlencoded;charset=UTF-8", 
		data: mydata, 
		error: function(info){
			j('#loading-search-result').modal('hide');
			var message = j('#jserrormessage').text();
			alert(message);
		},
		success: function(info) {
			if (info == null || info.result == null || info.result.length == 0)
			{
				j('#result-list').hide();
				j('#empty-result').show();
			}
			else
			{
				submissionLookupShowResult(info, "-search");
			}
			j('#loading-search-result').modal('hide');
			j('#tabs').find('a[href="#tabs-result"]').click();
		}
	});
	j('#loading-search-result').data('ajaxCall', ajaxCall);
	j('#loading-search-result').modal('show');
}

submissionLookupDetails = function(button, suffixID){
	var uuid = j(button).data('uuid');
	var mydata = new Object();
	var suuidID = 'suuid' + suffixID;
	mydata['s_uuid'] = j('#'+suuidID).val();
	mydata['type'] = 'details';
	mydata['i_uuid'] = uuid;
	j.ajax({url: dspaceContextPath+"/json/submissionLookup", 
		type: "POST",
		dataType: "json", 
		async: false,
		contentType: "application/x-www-form-urlencoded;charset=UTF-8", 
		data: mydata, 
		error: function(info){var message = j('#jserrormessage').text();alert(message);},
		success: function(info) {
			if (info == null || info.result == null || info.result.uuid != uuid)
			{
				var message = j('#jserrormessage').text();
				alert(message);
			}
			else
			{
				submissionLookupShowDetails(info.result);
			}
			j('#tabs').find('a[href="#tabs-result"]').click();
		}
	});
}


submissionLookupShowResult = function(info, suffixID){
	j('#result-list').show();
	j('#empty-result').hide();
	j('#result-list').html(" ");
	for (var i=0;i<info.result.length;i++)
	{
		var bt = j('<button class="btn btn-info" type="button">').append(j('#jsseedetailsbuttonmessage').text());
		var par = j('<p class="sl-result">');
		var divImg = j('<div class="submission-lookup-providers">');
		par.append(divImg);
		for (var k=0;k<info.result[i].providers.length;k++)
		{
			var prov = info.result[i].providers[k];
			divImg.append(j('<img class="img-thumbnail" src="'+dspaceContextPath+'/image/submission-lookup-small-'+prov+'.jpg">'));
		}	
		par
				.append(j('<span class="sl-result-title">').text(info.result[i].title))
				.append(j('<span class="sl-result-authors">').text(info.result[i].authors))
				.append(j('<span class="sl-result-date">').text(info.result[i].issued))
				.append(bt);
		j('#result-list').append(par);
		bt.button();
		bt.data({uuid: info.result[i].uuid});
		bt.click(function(){
			submissionLookupDetails(this, suffixID);
		});
	}
}

submissionLookupShowDetails = function(info){
	
	var modalbody = j('#loading-details .modal-body');
	var divImg = j('<div class="submission-lookup-providers">');
	
	for (var k=0;k<info.providers.length;k++)
	{
		var prov = info.providers[k];
		divImg.append(j('<img class="img-thumbnail" src="'+dspaceContextPath+'/image/submission-lookup-small-'+prov+'.jpg">'));
	}
	modalbody.append(divImg);
	var detailsDiv = j('<div class="submission-lookup-details">');
	var details = j('<table class="table">');
	detailsDiv.append(details);
	
	for (var i=0;i<info.fieldsLabels.length;i++)
	{
		var fieldName = info.fieldsLabels[i][0];
		var fieldLabel = info.fieldsLabels[i][1]; 
		var values = info.publication[fieldName];
		var tr = j('<tr>');
		tr.append(j('<td class="submission-lookup-label">').append(fieldLabel));
		var td = j('<td>');
		tr.append(td);
		for (var k=0;k<values.length;k++)
		{
			td.append(j('<span>').text(values[k]));
			if (k != values.length-1)
			{
				td.append('<br>');
			}
		}
		details.append(tr);
	}
	modalbody.append(detailsDiv);
	
	modalbody.append(j('#select-collection-div'));

	var modalfooter = j('#loading-details .modal-footer');
	var start = j('<button class="btn btn-success" type="button">');
	start.append(j('#jsfilldatabuttonmessage').text());
	start.button();
	start.click(function(){
		var selcolid = j('#select-collection').val();
		if (selcolid != null && selcolid != -1)
		{
			j('#collectionid').val(selcolid);
			j('#iuuid').val(info.uuid);
			j('#form-submission').submit();
		}
		else
		{
			j('#no-collection-warn').modal('show');
			j('#loading-details').modal('hide');
		}
	});
	modalfooter.append(start);
	j('#loading-details').modal('show');
};

submissionLookupFile = function(form){

	var suuidVal = j('#suuid-loader').val();
	var suuid = j('<input type="hidden" name="s_uuid" value="'+suuidVal+'">');
	var collectionidVal = j('#select-collection-file').val();
	var collectionid = j('<input type="hidden" name="collectionid" value="'+collectionidVal+'">');
	var preview_loader = "";
	if(j('#preview_loader').is (':checked')) {
		preview_loader = j('<input type="hidden" name="skip_loader" value="false">');
	}
	else {
		preview_loader = j('<input type="hidden" name="skip_loader" value="true">');
	}
	
	var provider_loaderVal = j('#provider_loader').val();
	var provider_loader = j('<input type="hidden" name="provider_loader" value="'+provider_loaderVal+'">');
	
	    // Create the iframe...
	    var iframe = j('<iframe name="upload_iframe" id="upload_iframe" style="display: none" />');
	    // Add to document...
	    j("body").append(iframe);
	 
	    // Add event...
	    var eventHandler = function () {
	    		
	    		j('#upload_iframe').off();
	    	
	             var clickResultTab = true;
	             var index = 0;
	             var iindex = new Array();
	             // Message from server...
	             var json = j.parseJSON(j('#upload_iframe').contents().find('body').text());
	            	 if (json == null || json.result == null || json.result.length == 0)
	       			{
	       				j('#result-list').hide();
	       				j('#empty-result').show();
	       			}
	       			else
	       			{
						for (var i = 0; i < json.result.length; i++) {
							if (json.result[i].skip == true) {
								clickResultTab = false;
								index = i;
								break;
							}
							iindex[i] = json.result[i].uuid;
						}
	       			}	            	
					if (clickResultTab) {
						submissionLookupShowResult(json, "-loader");
						j('#loading-file-result').modal("hide");
						j('#tabs').find('a[href="#tabs-result"]').click();
					} else {
						// skip details
						j('#collectionid').val(json.result[index].collectionid);
						j('#suuid').val(json.result[index].uuid);
						j('#fuuid').val(iindex);
						j('#form-submission').submit();						
						return false;
					}
	 			// Del the iframe...
		        j('upload_iframe').empty();
	        };
	 
	    j('#upload_iframe').on("load", eventHandler);
	    
	    // Set properties of form...
	    form.attr("target", "upload_iframe");
	    form.attr("action", dspaceContextPath+"/json/submissionLookup");
	    form.attr("method", "post");
	    form.attr("enctype", "multipart/form-data");
	    form.attr("encoding", "multipart/form-data");
	    form.attr("target", "upload_iframe");
	    form.attr("file", j('#file_upload').val());
	    
	    j(form).append(suuid);
        j(form).append(collectionid);
        j(form).append(preview_loader);
        j(form).append(provider_loader);
	    // Submit the form...
	    form.submit();
	 
	    j('#loading-file-result').modal("show");
	    
	 
};


