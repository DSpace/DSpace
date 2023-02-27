import { NgModule } from '@angular/core';
import { BrowseByRoutingModule } from './browse-by-routing.module';
import { BrowseByModule } from './browse-by.module';
import { ItemDataService } from '../core/data/item-data.service';
import { BrowseService } from '../core/browse/browse.service';
import { BrowseByGuard } from './browse-by-guard';
import { SharedBrowseByModule } from '../shared/browse-by/shared-browse-by.module';

@NgModule({
  imports: [
    SharedBrowseByModule,
    BrowseByRoutingModule,
    BrowseByModule.withEntryComponents(),
  ],
  providers: [
    ItemDataService,
    BrowseService,
    BrowseByGuard,
  ],
  declarations: [

  ]
})
export class BrowseByPageModule {

}
