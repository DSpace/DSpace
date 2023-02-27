import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthenticatedGuard } from '../core/auth/authenticated.guard';
import { ThemedSubmissionImportExternalComponent } from '../submission/import-external/themed-submission-import-external.component';

@NgModule({
  imports: [
    RouterModule.forChild([
      {
        canActivate: [ AuthenticatedGuard ],
        path: '',
        component: ThemedSubmissionImportExternalComponent,
        pathMatch: 'full',
        data: {
          title: 'submission.import-external.page.title'
        }
      }
    ])
  ],
  providers: [ ]
})
export class ImportExternalRoutingModule {

}
