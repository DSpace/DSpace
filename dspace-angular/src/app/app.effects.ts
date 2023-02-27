import { StoreEffects } from './store.effects';
import { NotificationsEffects } from './shared/notifications/notifications.effects';
import { NavbarEffects } from './navbar/navbar.effects';
import { SidebarEffects } from './shared/sidebar/sidebar-effects.service';
import { RelationshipEffects } from './shared/form/builder/ds-dynamic-form-ui/relation-lookup-modal/relationship.effects';
import { ThemeEffects } from './shared/theme-support/theme.effects';

export const appEffects = [
  StoreEffects,
  NavbarEffects,
  NotificationsEffects,
  SidebarEffects,
  ThemeEffects,
  RelationshipEffects
];
