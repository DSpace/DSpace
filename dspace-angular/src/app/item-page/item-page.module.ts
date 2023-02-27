import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';

import { SharedModule } from '../shared/shared.module';

import { ItemPageComponent } from './simple/item-page.component';
import { ItemPageRoutingModule } from './item-page-routing.module';
import { MetadataUriValuesComponent } from './field-components/metadata-uri-values/metadata-uri-values.component';
import {
  ItemPageAuthorFieldComponent
} from './simple/field-components/specific-field/author/item-page-author-field.component';
import {
  ItemPageDateFieldComponent
} from './simple/field-components/specific-field/date/item-page-date-field.component';
import {
  ItemPageAbstractFieldComponent
} from './simple/field-components/specific-field/abstract/item-page-abstract-field.component';
import { ItemPageUriFieldComponent } from './simple/field-components/specific-field/uri/item-page-uri-field.component';
import { ItemPageFieldComponent } from './simple/field-components/specific-field/item-page-field.component';
import { CollectionsComponent } from './field-components/collections/collections.component';
import { FullItemPageComponent } from './full/full-item-page.component';
import { FullFileSectionComponent } from './full/field-components/file-section/full-file-section.component';
import { PublicationComponent } from './simple/item-types/publication/publication.component';
import { ItemComponent } from './simple/item-types/shared/item.component';
import { EditItemPageModule } from './edit-item-page/edit-item-page.module';
import { UploadBitstreamComponent } from './bitstreams/upload/upload-bitstream.component';
import { StatisticsModule } from '../statistics/statistics.module';
import {
  AbstractIncrementalListComponent
} from './simple/abstract-incremental-list/abstract-incremental-list.component';
import { UntypedItemComponent } from './simple/item-types/untyped-item/untyped-item.component';
import { JournalEntitiesModule } from '../entity-groups/journal-entities/journal-entities.module';
import { ResearchEntitiesModule } from '../entity-groups/research-entities/research-entities.module';
import { ThemedItemPageComponent } from './simple/themed-item-page.component';
import { ThemedFullItemPageComponent } from './full/themed-full-item-page.component';
import { MediaViewerComponent } from './media-viewer/media-viewer.component';
import { MediaViewerVideoComponent } from './media-viewer/media-viewer-video/media-viewer-video.component';
import { MediaViewerImageComponent } from './media-viewer/media-viewer-image/media-viewer-image.component';
import { NgxGalleryModule } from '@kolkov/ngx-gallery';
import { MiradorViewerComponent } from './mirador-viewer/mirador-viewer.component';
import { VersionPageComponent } from './version-page/version-page/version-page.component';
import { ThemedFileSectionComponent } from './simple/field-components/file-section/themed-file-section.component';
import { OrcidAuthComponent } from './orcid-page/orcid-auth/orcid-auth.component';
import { OrcidPageComponent } from './orcid-page/orcid-page.component';
import { NgbAccordionModule } from '@ng-bootstrap/ng-bootstrap';
import { OrcidSyncSettingsComponent } from './orcid-page/orcid-sync-settings/orcid-sync-settings.component';
import { OrcidQueueComponent } from './orcid-page/orcid-queue/orcid-queue.component';
import { UploadModule } from '../shared/upload/upload.module';
import { ResultsBackButtonModule } from '../shared/results-back-button/results-back-button.module';
import { ItemAlertsComponent } from './alerts/item-alerts.component';
import { ItemVersionsModule } from './versions/item-versions.module';
import { BitstreamRequestACopyPageComponent } from './bitstreams/request-a-copy/bitstream-request-a-copy-page.component';
import { FileSectionComponent } from './simple/field-components/file-section/file-section.component';
import { ItemSharedModule } from './item-shared.module';
import { DsoPageModule } from '../shared/dso-page/dso-page.module';


const ENTRY_COMPONENTS = [
  // put only entry components that use custom decorator
  PublicationComponent,
  UntypedItemComponent
];

const DECLARATIONS = [
  FileSectionComponent,
  ThemedFileSectionComponent,
  ItemPageComponent,
  ThemedItemPageComponent,
  FullItemPageComponent,
  ThemedFullItemPageComponent,
  MetadataUriValuesComponent,
  ItemPageAuthorFieldComponent,
  ItemPageDateFieldComponent,
  ItemPageAbstractFieldComponent,
  ItemPageUriFieldComponent,
  ItemPageFieldComponent,
  CollectionsComponent,
  FullFileSectionComponent,
  PublicationComponent,
  UntypedItemComponent,
  ItemComponent,
  UploadBitstreamComponent,
  AbstractIncrementalListComponent,
  MediaViewerComponent,
  MediaViewerVideoComponent,
  MediaViewerImageComponent,
  MiradorViewerComponent,
  VersionPageComponent,
  OrcidPageComponent,
  OrcidAuthComponent,
  OrcidSyncSettingsComponent,
  OrcidQueueComponent,
  ItemAlertsComponent,
  BitstreamRequestACopyPageComponent,
];

@NgModule({
  imports: [
    CommonModule,
    SharedModule.withEntryComponents(),
    ItemPageRoutingModule,
    EditItemPageModule,
    ItemVersionsModule,
    ItemSharedModule,
    StatisticsModule.forRoot(),
    JournalEntitiesModule.withEntryComponents(),
    ResearchEntitiesModule.withEntryComponents(),
    NgxGalleryModule,
    NgbAccordionModule,
    ResultsBackButtonModule,
    UploadModule,
    DsoPageModule,
  ],
  declarations: [
    ...DECLARATIONS,

  ],
  exports: [
    ...DECLARATIONS,
  ]
})
export class ItemPageModule {
  /**
   * NOTE: this method allows to resolve issue with components that using a custom decorator
   * which are not loaded during SSR otherwise
   */
  static withEntryComponents() {
    return {
      ngModule: ItemPageModule,
      providers: ENTRY_COMPONENTS.map((component) => ({provide: component}))
    };
  }

}
