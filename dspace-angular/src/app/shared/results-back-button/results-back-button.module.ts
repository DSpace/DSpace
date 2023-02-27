import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TranslateModule } from '@ngx-translate/core';
import { ResultsBackButtonComponent } from './results-back-button.component';
import { ThemedResultsBackButtonComponent } from './themed-results-back-button.component';

@NgModule({
  declarations: [
    ResultsBackButtonComponent,
    ThemedResultsBackButtonComponent
  ],
  imports: [
    CommonModule,
    TranslateModule
  ],
  exports: [
    ThemedResultsBackButtonComponent
  ]
})
export class ResultsBackButtonModule {

}
