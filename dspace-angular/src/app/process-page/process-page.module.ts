import { NgModule } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { ProcessPageRoutingModule } from './process-page-routing.module';
import { NewProcessComponent } from './new/new-process.component';
import { ScriptsSelectComponent } from './form/scripts-select/scripts-select.component';
import { ScriptHelpComponent } from './form/script-help/script-help.component';
import { ParameterSelectComponent } from './form/process-parameters/parameter-select/parameter-select.component';
import { ProcessParametersComponent } from './form/process-parameters/process-parameters.component';
import { StringValueInputComponent } from './form/process-parameters/parameter-value-input/string-value-input/string-value-input.component';
import { ParameterValueInputComponent } from './form/process-parameters/parameter-value-input/parameter-value-input.component';
import { FileValueInputComponent } from './form/process-parameters/parameter-value-input/file-value-input/file-value-input.component';
import { BooleanValueInputComponent } from './form/process-parameters/parameter-value-input/boolean-value-input/boolean-value-input.component';
import { DateValueInputComponent } from './form/process-parameters/parameter-value-input/date-value-input/date-value-input.component';
import { ProcessOverviewComponent } from './overview/process-overview.component';
import { ProcessDetailComponent } from './detail/process-detail.component';
import { ProcessDetailFieldComponent } from './detail/process-detail-field/process-detail-field.component';
import { ProcessBreadcrumbsService } from './process-breadcrumbs.service';
import { ProcessBreadcrumbResolver } from './process-breadcrumb.resolver';
import { ProcessFormComponent } from './form/process-form.component';

@NgModule({
  imports: [
    ProcessPageRoutingModule,
    SharedModule,
  ],
  declarations: [
    NewProcessComponent,
    ScriptsSelectComponent,
    ScriptHelpComponent,
    ParameterSelectComponent,
    ProcessParametersComponent,
    StringValueInputComponent,
    ParameterValueInputComponent,
    FileValueInputComponent,
    BooleanValueInputComponent,
    DateValueInputComponent,
    ProcessOverviewComponent,
    ProcessDetailComponent,
    ProcessDetailFieldComponent,
    ProcessFormComponent
  ],
  providers: [
    ProcessBreadcrumbResolver,
    ProcessBreadcrumbsService
  ]
})

export class ProcessPageModule {

}
