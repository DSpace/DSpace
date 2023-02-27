import { getCollectionPageRoute } from '../collection-page/collection-page-routing-paths';
import { URLCombiner } from '../core/url-combiner/url-combiner';

export const COMMUNITY_PARENT_PARAMETER = 'parent';

export const COMMUNITY_MODULE_PATH = 'communities';

export function getCommunityModuleRoute() {
  return `/${COMMUNITY_MODULE_PATH}`;
}

export function getCommunityPageRoute(communityId: string) {
  return new URLCombiner(getCommunityModuleRoute(), communityId).toString();
}

export function getCommunityEditRoute(id: string) {
  return new URLCombiner(getCommunityModuleRoute(), id, COMMUNITY_EDIT_PATH).toString();
}

export function getCommunityCreateRoute() {
  return new URLCombiner(getCommunityModuleRoute(), COMMUNITY_CREATE_PATH).toString();
}

export function getCommunityEditRolesRoute(id) {
  return new URLCombiner(getCollectionPageRoute(id), COMMUNITY_EDIT_PATH, COMMUNITY_EDIT_ROLES_PATH).toString();
}

export const COMMUNITY_CREATE_PATH = 'create';
export const COMMUNITY_EDIT_PATH = 'edit';
export const COMMUNITY_EDIT_ROLES_PATH = 'roles';
