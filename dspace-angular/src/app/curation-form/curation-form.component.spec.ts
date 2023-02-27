import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { CurationFormComponent } from './curation-form.component';
import { ScriptDataService } from '../core/data/processes/script-data.service';
import { ProcessDataService } from '../core/data/processes/process-data.service';
import { Process } from '../process-page/processes/process.model';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../shared/remote-data.utils';
import { NotificationsServiceStub } from '../shared/testing/notifications-service.stub';
import { RouterStub } from '../shared/testing/router.stub';
import { NotificationsService } from '../shared/notifications/notifications.service';
import { Router } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { By } from '@angular/platform-browser';
import { ConfigurationDataService } from '../core/data/configuration-data.service';
import { ConfigurationProperty } from '../core/shared/configuration-property.model';
import { getProcessDetailRoute } from '../process-page/process-page-routing.paths';
import { HandleService } from '../shared/handle.service';

describe('CurationFormComponent', () => {
  let comp: CurationFormComponent;
  let fixture: ComponentFixture<CurationFormComponent>;

  let scriptDataService: ScriptDataService;
  let processDataService: ProcessDataService;
  let configurationDataService: ConfigurationDataService;
  let handleService: HandleService;
  let notificationsService;
  let router;

  const process = Object.assign(new Process(), {processId: 'process-id'});

  beforeEach(waitForAsync(() => {

    scriptDataService = jasmine.createSpyObj('scriptDataService', {
      invoke: createSuccessfulRemoteDataObject$(process)
    });

    processDataService = jasmine.createSpyObj('processDataService', {
      findByHref: createSuccessfulRemoteDataObject$(process)
    });

    configurationDataService = jasmine.createSpyObj('configurationDataService', {
      findByPropertyName: createSuccessfulRemoteDataObject$(Object.assign(new ConfigurationProperty(), {
        name: 'plugin.named.org.dspace.curate.CurationTask',
        values: [
          'org.dspace.ctask.general.ProfileFormats = profileformats',
          '',
          'org.dspace.ctask.general.RequiredMetadata = requiredmetadata',
          'org.dspace.ctask.general.MetadataValueLinkChecker = checklinks',
          'value-to-be-skipped'
        ]
      }))
    });

    handleService = {
      normalizeHandle: (a) => a
    } as any;

    notificationsService = new NotificationsServiceStub();
    router = new RouterStub();

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), FormsModule, ReactiveFormsModule],
      declarations: [CurationFormComponent],
      providers: [
        { provide: ScriptDataService, useValue: scriptDataService },
        { provide: ProcessDataService, useValue: processDataService },
        { provide: NotificationsService, useValue: notificationsService },
        { provide: HandleService, useValue: handleService },
        { provide: Router, useValue: router},
        { provide: ConfigurationDataService, useValue: configurationDataService },
      ],
      schemas: [CUSTOM_ELEMENTS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(CurationFormComponent);
    comp = fixture.componentInstance;

    fixture.detectChanges();
    spyOn(router, 'navigateByUrl').and.callThrough();
  });
  describe('init', () => {
    it('should initialise the comp and contain the different tasks', () => {
      expect(comp).toBeDefined();

      const elements = fixture.debugElement.queryAll(By.css('option'));
      expect(elements.length).toEqual(3);
      expect(elements[0].nativeElement.innerHTML).toContain('curation-task.task.profileformats.label');
      expect(elements[1].nativeElement.innerHTML).toContain('curation-task.task.requiredmetadata.label');
      expect(elements[2].nativeElement.innerHTML).toContain('curation-task.task.checklinks.label');
    });
  });
  describe('hasHandleValue', () => {
    it('should return true when a dsoHandle value was provided', () => {
      comp.dsoHandle = 'some-handle';
      fixture.detectChanges();

      expect(comp.hasHandleValue()).toBeTrue();
    });
    it('should return false when no dsoHandle value was provided', () => {
      expect(comp.hasHandleValue()).toBeFalse();
    });
  });
  describe('submit', () => {
    it('should submit the selected process and handle to the scriptservice and navigate to the corresponding process page', () => {
      comp.dsoHandle = 'test-handle';
      comp.submit();

      expect(scriptDataService.invoke).toHaveBeenCalledWith('curate', [
        {name: '-t', value: 'profileformats'},
        {name: '-i', value: 'test-handle'},
      ], []);
      expect(notificationsService.success).toHaveBeenCalled();
      expect(router.navigateByUrl).toHaveBeenCalledWith(getProcessDetailRoute('process-id'));
    });
    it('should the selected process and handle to the scriptservice and stay on the page on error', () => {
      (scriptDataService.invoke as jasmine.Spy).and.returnValue(createFailedRemoteDataObject$('Error', 500));

      comp.dsoHandle = 'test-handle';
      comp.submit();

      expect(scriptDataService.invoke).toHaveBeenCalledWith('curate', [
        {name: '-t', value: 'profileformats'},
        {name: '-i', value: 'test-handle'},
      ], []);
      expect(notificationsService.error).toHaveBeenCalled();
      expect(processDataService.findByHref).not.toHaveBeenCalled();
      expect(router.navigateByUrl).not.toHaveBeenCalled();
    });
  });
  it('should use the handle provided by the form when no dsoHandle is provided', () => {
    comp.form.get('handle').patchValue('form-handle');

    comp.submit();

    expect(scriptDataService.invoke).toHaveBeenCalledWith('curate', [
      {name: '-t', value: 'profileformats'},
      {name: '-i', value: 'form-handle'},
    ], []);
  });
  it('should use "all" when the handle provided by the form is empty and when no dsoHandle is provided', () => {

    comp.submit();

    expect(scriptDataService.invoke).toHaveBeenCalledWith('curate', [
      {name: '-t', value: 'profileformats'},
      {name: '-i', value: 'all'},
    ], []);
  });

  it(`should show an error notification and return when an invalid dsoHandle is provided`, () => {
    comp.dsoHandle = 'test-handle';
    spyOn(handleService, 'normalizeHandle').and.returnValue(null);
    comp.submit();

    expect(notificationsService.error).toHaveBeenCalled();
    expect(scriptDataService.invoke).not.toHaveBeenCalled();
  });
});
