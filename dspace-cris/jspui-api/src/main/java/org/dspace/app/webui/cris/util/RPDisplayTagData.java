/**
 * The contents of this file are subject to the license and copyright
 * detailed in the LICENSE and NOTICE files at the root of the source
 * tree and available online at
 *
 * https://github.com/CILEA/dspace-cris/wiki/License
 */
package org.dspace.app.webui.cris.util;


import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import org.displaytag.pagination.PaginatedList;
import org.displaytag.properties.SortOrderEnum;
import org.dspace.app.cris.model.dto.ResearcherPageDTO;

public class RPDisplayTagData implements PaginatedList, Serializable {

	public static final int PAGE_SIZE_DEFAULT = 20;

	private long totalCount;

	private List<ResearcherPageDTO> pageItems;

	private int pageSize;

	private int page;

	private String sort;

	private String dir;

	public RPDisplayTagData() {
		this(0, Collections.EMPTY_LIST, "id", "asc", 1, PAGE_SIZE_DEFAULT);
	}

	public RPDisplayTagData(long count, List pageItems, String sort, String dir,
			int page, int pageSize) {
		this.totalCount = count;
		this.pageItems = pageItems;
		this.sort = sort;
		this.dir = dir;
		this.page = page;
		this.pageSize = pageSize;
	}

	public int getPage() {
		return page;
	}

	public void setPage(int page) {
		this.page = page;
	}

	public String getSort() {
		return sort;
	}

	public void setSort(String sort) {
		this.sort = sort;
	}

	public String getDir() {
		return dir;
	}

	public void setDir(String dir) {
		this.dir = dir;
	}

	public void setPageItems(List<ResearcherPageDTO> pageItems) {
		this.pageItems = pageItems;
	}
	
	public void setTotalCount(int totalCount) {
		this.totalCount = totalCount;
	}
	
	// PaginatedList

	public int getFullListSize() {
		return new Long(totalCount).intValue();
	}

	public List<ResearcherPageDTO> getList() {
		return pageItems;
	}

	public int getObjectsPerPage() {
		return pageSize;
	}

	public int getPageNumber() {
		return page;
	}

	public String getSearchId() {
		return null;
	}

	public String getSortCriterion() {
		return sort;
	}

	public SortOrderEnum getSortDirection() {
		return "asc".equals(dir) ? SortOrderEnum.ASCENDING : ("desc"
				.equals(dir) ? SortOrderEnum.DESCENDING : null);
	}

}
