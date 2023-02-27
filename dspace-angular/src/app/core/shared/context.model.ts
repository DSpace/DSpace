/**
 * This enumeration represents all possible ways of representing a group of objects in the UI
 */

export enum Context {
  Any = 'undefined',
  ItemPage = 'itemPage',
  Search = 'search',
  Workflow = 'workflow',
  Workspace = 'workspace',
  SupervisedItems = 'supervisedWorkspace',
  AdminMenu = 'adminMenu',
  EntitySearchModalWithNameVariants = 'EntitySearchModalWithNameVariants',
  EntitySearchModal = 'EntitySearchModal',
  AdminSearch = 'adminSearch',
  AdminWorkflowSearch = 'adminWorkflowSearch',
  SideBarSearchModal = 'sideBarSearchModal',
  SideBarSearchModalCurrent = 'sideBarSearchModalCurrent',
}
