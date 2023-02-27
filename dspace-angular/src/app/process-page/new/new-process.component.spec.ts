import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';

import { FormsModule } from '@angular/forms';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { NewProcessComponent } from './new-process.component';
import { ScriptDataService } from '../../core/data/processes/script-data.service';
import { ScriptParameter } from '../scripts/script-parameter.model';
import { Script } from '../scripts/script.model';
import { ProcessParameter } from '../processes/process-parameter.model';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { of as observableOf } from 'rxjs';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { TranslateLoaderMock } from '../../shared/mocks/translate-loader.mock';
import { RequestService } from '../../core/data/request.service';
import { ActivatedRoute } from '@angular/router';
import { LinkService } from '../../core/cache/builders/link.service';
import { VarDirective } from '../../shared/utils/var.directive';
import { ProcessDataService } from '../../core/data/processes/process-data.service';

describe('NewProcessComponent', () => {
  let component: NewProcessComponent;
  let fixture: ComponentFixture<NewProcessComponent>;
  let scriptService;
  let parameterValues;
  let script;

  function init() {
    const param1 = new ScriptParameter();
    const param2 = new ScriptParameter();
    script = Object.assign(new Script(), { parameters: [param1, param2] });
    parameterValues = [
      Object.assign(new ProcessParameter(), { name: '-a', value: 'bla' }),
      Object.assign(new ProcessParameter(), { name: '-b', value: '123' }),
      Object.assign(new ProcessParameter(), { name: '-c', value: 'value' }),
    ];
    scriptService = jasmine.createSpyObj(
      'scriptService',
      {
        invoke: observableOf({
          response:
            {
              isSuccessful: true
            }
        })
      }
    );
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock
          }
        })],
      declarations: [NewProcessComponent, VarDirective],
      providers: [
        { provide: ScriptDataService, useValue: scriptService },
        { provide: NotificationsService, useClass: NotificationsServiceStub },
        { provide: RequestService, useValue: {} },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParams: {} } } },
        { provide: LinkService, useValue: {} },
        { provide: ProcessDataService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(NewProcessComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
