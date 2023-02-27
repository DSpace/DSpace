import { waitForAsync, ComponentFixture, fakeAsync, TestBed, tick } from '@angular/core/testing';

import { FormsModule } from '@angular/forms';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';
import { By } from '@angular/platform-browser';
import { ScriptsSelectComponent } from './scripts-select.component';
import { Script } from '../../scripts/script.model';
import { ScriptDataService } from '../../../core/data/processes/script-data.service';
import { ActivatedRoute, Router } from '@angular/router';
import { buildPaginatedList } from '../../../core/data/paginated-list.model';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { TranslateLoaderMock } from '../../../shared/mocks/translate-loader.mock';
import { RouterStub } from '../../../shared/testing/router.stub';
import { ActivatedRouteStub } from '../../../shared/testing/active-router.stub';

describe('ScriptsSelectComponent', () => {
  let component: ScriptsSelectComponent;
  let fixture: ComponentFixture<ScriptsSelectComponent>;
  let scriptService;
  let script1;
  let script2;

  function init() {
    script1 = new Script();
    script2 = new Script();
    scriptService = jasmine.createSpyObj('scriptService',
      {
        findAll: createSuccessfulRemoteDataObject$(buildPaginatedList(undefined, [script1, script2]))
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
      declarations: [ScriptsSelectComponent],
      providers: [
        { provide: ScriptDataService, useValue: scriptService },
        { provide: Router, useClass: RouterStub },
        { provide: ActivatedRoute, useValue: new ActivatedRouteStub() },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    })
      .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ScriptsSelectComponent);
    component = fixture.componentInstance;
    (component as any)._selectedScript = new Script();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should not show a validation error if the input field was left untouched but left empty', () => {
    const validationError = fixture.debugElement.query(By.css('.validation-error'));
    expect(validationError).toBeFalsy();
  });

  it('should show a validation error if the input field was touched but left empty', fakeAsync(() => {
    (component as any)._selectedScript.id = '';
    fixture.detectChanges();
    tick();

    const select = fixture.debugElement.query(By.css('select'));
    select.triggerEventHandler('blur', null);

    fixture.detectChanges();

    const validationError = fixture.debugElement.query(By.css('.validation-error'));
    expect(validationError).toBeTruthy();
  }));

  it('should not show a validation error if the input field was touched but not left empty', fakeAsync(() => {
    (component as any)._selectedScript.id = 'testValue';
    fixture.detectChanges();
    tick();

    const select = fixture.debugElement.query(By.css('select'));
    select.triggerEventHandler('blur', null);

    fixture.detectChanges();

    const validationError = fixture.debugElement.query(By.css('.validation-error'));
    expect(validationError).toBeFalsy();
  }));
});
