import { RelatedEntitiesSearchComponent } from './simple/related-entities/related-entities-search/related-entities-search.component';
import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { SearchModule } from '../shared/search/search.module';
import { SharedModule } from '../shared/shared.module';
import { TranslateModule } from '@ngx-translate/core';
import { DYNAMIC_FORM_CONTROL_MAP_FN } from '@ng-dynamic-forms/core';
import { dsDynamicFormControlMapFn } from '../shared/form/builder/ds-dynamic-form-ui/ds-dynamic-form-control-container.component';
import { TabbedRelatedEntitiesSearchComponent } from './simple/related-entities/tabbed-related-entities-search/tabbed-related-entities-search.component';
import { ItemVersionsDeleteModalComponent } from './versions/item-versions-delete-modal/item-versions-delete-modal.component';
import { ItemVersionsSummaryModalComponent } from './versions/item-versions-summary-modal/item-versions-summary-modal.component';
import { MetadataValuesComponent } from './field-components/metadata-values/metadata-values.component';
import { GenericItemPageFieldComponent } from './simple/field-components/specific-field/generic/generic-item-page-field.component';
import { MetadataRepresentationListComponent } from './simple/metadata-representation-list/metadata-representation-list.component';
import { RelatedItemsComponent } from './simple/related-items/related-items-component';

const ENTRY_COMPONENTS = [
  ItemVersionsDeleteModalComponent,
  ItemVersionsSummaryModalComponent,

];

const COMPONENTS = [
  ...ENTRY_COMPONENTS,
  RelatedEntitiesSearchComponent,
  TabbedRelatedEntitiesSearchComponent,
  MetadataValuesComponent,
  GenericItemPageFieldComponent,
  MetadataRepresentationListComponent,
  RelatedItemsComponent,
];

@NgModule({
  declarations: [
    ...COMPONENTS
  ],
  imports: [
    CommonModule,
    SearchModule,
    SharedModule,
    TranslateModule
  ],
  exports: [
    ...COMPONENTS
  ],
  providers: [
    {
      provide: DYNAMIC_FORM_CONTROL_MAP_FN,
      useValue: dsDynamicFormControlMapFn
    },
    ...ENTRY_COMPONENTS,
  ]
})
export class ItemSharedModule { }
