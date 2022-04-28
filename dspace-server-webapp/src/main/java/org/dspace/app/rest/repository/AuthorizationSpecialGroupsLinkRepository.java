package org.dspace.app.rest.repository;

import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.servlet.http.HttpServletRequest;

import org.dspace.app.rest.converter.ConverterService;
import org.dspace.app.rest.model.AuthorizationRest;
import org.dspace.app.rest.model.GroupRest;
import org.dspace.app.rest.projection.Projection;
import org.dspace.app.rest.utils.ContextUtil;
import org.dspace.app.rest.utils.Utils;
import org.dspace.core.Context;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

/**
 * Link repository for "specialGroups" subresource of an individual authorization.
 */
@Component(AuthorizationRest.CATEGORY + "." + AuthorizationRest.NAME + "." + AuthorizationRest.SPECIALGROUPS)
public class AuthorizationSpecialGroupsLinkRepository {

	@Autowired
	private Utils utils;

	@Autowired
	private ConverterService converter;

	public Page<GroupRest> getSpecialGroups(@Nullable HttpServletRequest request, @Nullable Pageable optionalPageable,
			Projection projection) throws SQLException {
		Context context = ContextUtil.obtainContext(request);

		context.setSpecialGroup(java.util.UUID.fromString("3c5e9fa5-c829-4a5f-b2f1-094281f7e38d"));
		List<GroupRest> groupList = context.getSpecialGroups().stream()
				.map(g -> (GroupRest) converter.toRest(g, projection)).collect(Collectors.toList());
		Page<GroupRest> groupPage = (Page<GroupRest>) utils.getPage(groupList, optionalPageable);

		return groupPage;
	}
}
