import { Component, Input, OnInit } from '@angular/core';
import { ScriptDataService } from '../core/data/processes/script-data.service';
import { FormControl, FormGroup } from '@angular/forms';
import { getFirstCompletedRemoteData } from '../core/shared/operators';
import { find, map } from 'rxjs/operators';
import { NotificationsService } from '../shared/notifications/notifications.service';
import { TranslateService } from '@ngx-translate/core';
import { hasValue, isEmpty, isNotEmpty } from '../shared/empty.util';
import { RemoteData } from '../core/data/remote-data';
import { Router } from '@angular/router';
import { ProcessDataService } from '../core/data/processes/process-data.service';
import { Process } from '../process-page/processes/process.model';
import { ConfigurationDataService } from '../core/data/configuration-data.service';
import { ConfigurationProperty } from '../core/shared/configuration-property.model';
import { Observable } from 'rxjs';
import { getProcessDetailRoute } from '../process-page/process-page-routing.paths';
import { HandleService } from '../shared/handle.service';

export const CURATION_CFG = 'plugin.named.org.dspace.curate.CurationTask';
/**
 * Component responsible for rendering the Curation Task form
 */
@Component({
  selector: 'ds-curation-form',
  templateUrl: './curation-form.component.html'
})
export class CurationFormComponent implements OnInit {

  config: Observable<RemoteData<ConfigurationProperty>>;
  tasks: string[];
  form: FormGroup;

  @Input()
  dsoHandle: string;

  constructor(
    private scriptDataService: ScriptDataService,
    private configurationDataService: ConfigurationDataService,
    private processDataService: ProcessDataService,
    private notificationsService: NotificationsService,
    private translateService: TranslateService,
    private handleService: HandleService,
    private router: Router
  ) {
  }

  ngOnInit(): void {
    this.form = new FormGroup({
      task: new FormControl(''),
      handle: new FormControl('')
    });

    this.config = this.configurationDataService.findByPropertyName(CURATION_CFG);
    this.config.pipe(
      find((rd: RemoteData<ConfigurationProperty>) => rd.hasSucceeded),
      map((rd: RemoteData<ConfigurationProperty>) => rd.payload)
    ).subscribe((configProperties) => {
      this.tasks = configProperties.values
        .filter((value) => isNotEmpty(value) && value.includes('='))
        .map((value) => value.split('=')[1].trim());
      this.form.get('task').patchValue(this.tasks[0]);
    });
  }

  /**
   * Determines whether the inputted dsoHandle has a value
   */
  hasHandleValue() {
    return hasValue(this.dsoHandle);
  }

  /**
   * Submit the selected taskName and handle to the script data service to run the corresponding curation script
   * Navigate to the process page on success
   */
  submit() {
    const taskName = this.form.get('task').value;
    let handle;
    if (this.hasHandleValue()) {
      handle = this.handleService.normalizeHandle(this.dsoHandle);
      if (isEmpty(handle)) {
        this.notificationsService.error(this.translateService.get('curation.form.submit.error.head'),
          this.translateService.get('curation.form.submit.error.invalid-handle'));
        return;
      }
    } else {
      handle = this.handleService.normalizeHandle(this.form.get('handle').value);
      if (isEmpty(handle)) {
        handle = 'all';
      }
    }

    this.scriptDataService.invoke('curate', [
      { name: '-t', value: taskName },
      { name: '-i', value: handle },
    ], []).pipe(getFirstCompletedRemoteData()).subscribe((rd: RemoteData<Process>) => {
      if (rd.hasSucceeded) {
        this.notificationsService.success(this.translateService.get('curation.form.submit.success.head'),
          this.translateService.get('curation.form.submit.success.content'));
        this.router.navigateByUrl(getProcessDetailRoute(rd.payload.processId));
      } else {
        this.notificationsService.error(this.translateService.get('curation.form.submit.error.head'),
          this.translateService.get('curation.form.submit.error.content'));
      }
    });
  }
}
