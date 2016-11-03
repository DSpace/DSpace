/*
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
fnDraCallbackDedup = function(itemID, typeID, dcheck) {
	j('.fake_ws').click(function() {
		var iid = itemID;
		var did = j(this).attr('data-itemId');		
		rejectFakeDup(iid, did, typeID);
		var dataTableObj = j("#duplicateboxtable").dataTable();
		j(this).parents('tr').remove();
		if (dataTableObj.find('tbody tr').length == 0) {
			j("#duplicatebox").modal("hide");
		}
	});
	j('.fake_wf1, .fake_wf2').click(function() {
		var iid = itemID;
		var did = j(this).attr('data-itemId');
		var dedupid = duplicationData['duplicateboxtable'][did].dedupID;
		dupToFixEvent(iid, did, dedupid, dcheck, "", typeID);
		var dataTableObj = j("#duplicateboxtable").dataTable();
		j(this).parents('tr').remove();
		if (dataTableObj.find('tbody tr').length == 0) {
			j("#duplicatebox").modal("hide");
		}
	});
	j('.ignore_ws').click(function() {
		var iid = itemID;
		var did = j(this).attr('data-itemId');
		var dedupid = duplicationData['duplicateboxtable'][did].dedupID;
		j("#duplicateboxignoremsg").data({
			iid : iid,
			did : did,
			dedupid : dedupid,
			typeid : typeID,
			row : this
		});
		j("#duplicatebox").modal("hide");
		j("#duplicateboxignoremsg").modal("show");
	});
	j('.ignore_wf1, .ignore_wf2').click(function() {
		var iid = itemID;
		var did = j(this).attr('data-itemId');
		var dedupid = duplicationData['duplicateboxtable'][did].dedupID;
		ignoreDup(iid, did, "", typeID, dcheck);
//		dupVerifyEvent(iid, did, dedupid, dcheck, typeID);
		var dataTableObj = j("#duplicateboxtable").dataTable();
		j(this).parents('tr').remove();
		if (dataTableObj.find('tbody tr').length == 0) {
			j("#duplicatebox").modal("hide");
		}
	});
	
	j("#mergepopup").click(function(evt) {
		var itemId = itemID;
		var typeId = typeID;
		var data = duplicationData['duplicateboxtable'];		
		var tmp_itemid_list = itemID + ",";
		j.each(data, function( index, value ) {
			tmp_itemid_list += index;
			tmp_itemid_list += ",";
		});
		var itemid_list = tmp_itemid_list.substr(0, tmp_itemid_list.length-1);
		j("#itemid_list").val(itemid_list);		
		document.formItemIdList.submit();
	});
};

function dupToFixEvent(iid, did, dedupid, dcheck, note, typeid) {
	j.ajax({
		url : "dedup/verify",
		cache : false,
		data : ({
			dedupID : dedupid,
			toFix : true,
			itemID : iid,
			duplicateID : did,
			note : note,
			type: typeid,
			dcheck: dcheck
		})
	});

	var dataTableObj = j("#duplicateboxtable").dataTable();
	j(this).parents('tr').remove();
	if (dataTableObj.find('tbody tr').length == 0) {
		j("#duplicatebox").modal("hide");
	}
}

function rejectFakeDup(iid, did, typeid) {
	j.ajax({
		url : "dedup/reject",
		cache : false,
		data : ({
			itemID : iid,
			duplicateID : did,
			fake : true,
			type: typeid
		})
	});
}

function ignoreDup(iid, did, note, typeid, dcheck) {
	j.ajax({
		url : "dedup/reject",
		cache : false,
		data : ({
			note : note,
			itemID : iid,
			duplicateID : did,
			type: typeid,
			dcheck: dcheck
		})
	});
}

function dupVerifyEvent(iid, did, dedupid, dcheck, typeid) {
	j.ajax({
		url : "dedup/verify",
		cache : false,
		data : ({
			dedupID : dedupid,
			toFix : false,
			itemID : iid,
			duplicateID : did,
			type: typeid
		})
	});

	var dataTableObj = j("#duplicateboxtable").dataTable();
	j(this).parents('tr').remove();
	if (dataTableObj.find('tbody tr').length == 0) {
		j("#duplicatebox").modal("hide");
	}
}