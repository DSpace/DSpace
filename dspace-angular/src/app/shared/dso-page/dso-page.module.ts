import { NgModule } from '@angular/core';
import { TranslateModule } from '@ngx-translate/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { DsoEditMenuComponent } from '../dso-page/dso-edit-menu/dso-edit-menu.component';
import {
  DsoEditMenuSectionComponent
} from '../dso-page/dso-edit-menu/dso-edit-menu-section/dso-edit-menu-section.component';
import {
  DsoEditMenuExpandableSectionComponent
} from '../dso-page/dso-edit-menu/dso-edit-expandable-menu-section/dso-edit-menu-expandable-section.component';
import { NgbTooltipModule } from '@ng-bootstrap/ng-bootstrap';

const COMPONENTS = [
  DsoEditMenuComponent,
  DsoEditMenuSectionComponent,
  DsoEditMenuExpandableSectionComponent,
];

const ENTRY_COMPONENTS = [
];

const MODULES = [
  TranslateModule,
  RouterModule,
  CommonModule,
  NgbTooltipModule,
];
const PROVIDERS = [

];

@NgModule({
  imports: [
    ...MODULES
  ],
  declarations: [
    ...COMPONENTS,
    ...ENTRY_COMPONENTS,
  ],
  providers: [
    ...PROVIDERS,
    ...ENTRY_COMPONENTS,
  ],
  exports: [
    ...COMPONENTS
  ]
})

/**
 * This module handles all components, providers and modules that are needed for the menu
 */
export class DsoPageModule {

}
