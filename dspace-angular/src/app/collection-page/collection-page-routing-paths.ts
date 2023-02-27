import { URLCombiner } from '../core/url-combiner/url-combiner';

export const COLLECTION_PARENT_PARAMETER = 'parent';

export const COLLECTION_MODULE_PATH = 'collections';

export function getCollectionModuleRoute() {
  return `/${COLLECTION_MODULE_PATH}`;
}

export function getCollectionPageRoute(collectionId: string) {
  return new URLCombiner(getCollectionModuleRoute(), collectionId).toString();
}

export function getCollectionEditRoute(id: string) {
  return new URLCombiner(getCollectionModuleRoute(), id, COLLECTION_EDIT_PATH).toString();
}

export function getCollectionCreateRoute() {
  return new URLCombiner(getCollectionModuleRoute(), COLLECTION_CREATE_PATH).toString();
}

export function getCollectionEditRolesRoute(id) {
  return new URLCombiner(getCollectionPageRoute(id), COLLECTION_EDIT_PATH, COLLECTION_EDIT_ROLES_PATH).toString();
}

export function getCollectionItemTemplateRoute(id) {
  return new URLCombiner(getCollectionPageRoute(id), ITEMTEMPLATE_PATH).toString();
}

export const COLLECTION_CREATE_PATH = 'create';
export const COLLECTION_EDIT_PATH = 'edit';
export const COLLECTION_EDIT_ROLES_PATH = 'roles';
export const ITEMTEMPLATE_PATH = 'itemtemplate';
