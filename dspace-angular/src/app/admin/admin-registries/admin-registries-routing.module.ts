import { MetadataRegistryComponent } from './metadata-registry/metadata-registry.component';
import { RouterModule } from '@angular/router';
import { NgModule } from '@angular/core';
import { MetadataSchemaComponent } from './metadata-schema/metadata-schema.component';
import { I18nBreadcrumbResolver } from '../../core/breadcrumbs/i18n-breadcrumb.resolver';
import { BITSTREAMFORMATS_MODULE_PATH } from './admin-registries-routing-paths';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: 'metadata',
        resolve: { breadcrumb: I18nBreadcrumbResolver },
        data: {title: 'admin.registries.metadata.title', breadcrumbKey: 'admin.registries.metadata'},
        children: [
          {
            path: '',
            component: MetadataRegistryComponent
          },
          {
            path: ':schemaName',
            resolve: { breadcrumb: I18nBreadcrumbResolver },
            component: MetadataSchemaComponent,
            data: {title: 'admin.registries.schema.title', breadcrumbKey: 'admin.registries.schema'}
          }
        ]
      },
      {
        path: BITSTREAMFORMATS_MODULE_PATH,
        resolve: { breadcrumb: I18nBreadcrumbResolver },
        loadChildren: () => import('./bitstream-formats/bitstream-formats.module')
          .then((m) => m.BitstreamFormatsModule),
        data: {title: 'admin.registries.bitstream-formats.title', breadcrumbKey: 'admin.registries.bitstream-formats'}
      },
    ])
  ]
})
export class AdminRegistriesRoutingModule {

}
