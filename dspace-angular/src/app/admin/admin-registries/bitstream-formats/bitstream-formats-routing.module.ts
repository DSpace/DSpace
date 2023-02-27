import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { BitstreamFormatsResolver } from './bitstream-formats.resolver';
import { EditBitstreamFormatComponent } from './edit-bitstream-format/edit-bitstream-format.component';
import { BitstreamFormatsComponent } from './bitstream-formats.component';
import { AddBitstreamFormatComponent } from './add-bitstream-format/add-bitstream-format.component';
import { I18nBreadcrumbResolver } from '../../../core/breadcrumbs/i18n-breadcrumb.resolver';

const BITSTREAMFORMAT_EDIT_PATH = ':id/edit';
const BITSTREAMFORMAT_ADD_PATH = 'add';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        path: '',
        component: BitstreamFormatsComponent
      },
      {
        path: BITSTREAMFORMAT_ADD_PATH,
        resolve: { breadcrumb: I18nBreadcrumbResolver },
        component: AddBitstreamFormatComponent,
        data: {breadcrumbKey: 'admin.registries.bitstream-formats.create'}
      },
      {
        path: BITSTREAMFORMAT_EDIT_PATH,
        component: EditBitstreamFormatComponent,
        resolve: {
          bitstreamFormat: BitstreamFormatsResolver,
          breadcrumb: I18nBreadcrumbResolver
        },
        data: {breadcrumbKey: 'admin.registries.bitstream-formats.edit'}
      },
    ])
  ],
  providers: [
    BitstreamFormatsResolver,
  ]
})
export class BitstreamFormatsRoutingModule {

}
