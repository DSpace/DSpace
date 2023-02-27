import { ComponentFixture, fakeAsync, TestBed, waitForAsync } from '@angular/core/testing';
import { BatchImportPageComponent } from './batch-import-page.component';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { FormsModule } from '@angular/forms';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { FileValueAccessorDirective } from '../../shared/utils/file-value-accessor.directive';
import { FileValidator } from '../../shared/utils/require-file.validator';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import {
  BATCH_IMPORT_SCRIPT_NAME,
  ScriptDataService
} from '../../core/data/processes/script-data.service';
import { Router } from '@angular/router';
import { Location } from '@angular/common';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { ProcessParameter } from '../../process-page/processes/process-parameter.model';

describe('BatchImportPageComponent', () => {
  let component: BatchImportPageComponent;
  let fixture: ComponentFixture<BatchImportPageComponent>;

  let notificationService: NotificationsServiceStub;
  let scriptService: any;
  let router;
  let locationStub;

  function init() {
    notificationService = new NotificationsServiceStub();
    scriptService = jasmine.createSpyObj('scriptService',
      {
        invoke: createSuccessfulRemoteDataObject$({ processId: '46' })
      }
    );
    router = jasmine.createSpyObj('router', {
      navigateByUrl: jasmine.createSpy('navigateByUrl')
    });
    locationStub = jasmine.createSpyObj('location', {
      back: jasmine.createSpy('back')
    });
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      imports: [
        FormsModule,
        TranslateModule.forRoot(),
        RouterTestingModule.withRoutes([])
      ],
      declarations: [BatchImportPageComponent, FileValueAccessorDirective, FileValidator],
      providers: [
        { provide: NotificationsService, useValue: notificationService },
        { provide: ScriptDataService, useValue: scriptService },
        { provide: Router, useValue: router },
        { provide: Location, useValue: locationStub },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(BatchImportPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('if back button is pressed', () => {
    beforeEach(fakeAsync(() => {
      const proceed = fixture.debugElement.query(By.css('#backButton')).nativeElement;
      proceed.click();
      fixture.detectChanges();
    }));
    it('should do location.back', () => {
      expect(locationStub.back).toHaveBeenCalled();
    });
  });

  describe('if file is set', () => {
    let fileMock: File;

    beforeEach(() => {
      fileMock = new File([''], 'filename.zip', { type: 'application/zip' });
      component.setFile(fileMock);
    });

    describe('if proceed button is pressed without validate only', () => {
      beforeEach(fakeAsync(() => {
        component.validateOnly = false;
        const proceed = fixture.debugElement.query(By.css('#proceedButton')).nativeElement;
        proceed.click();
        fixture.detectChanges();
      }));
      it('metadata-import script is invoked with --zip fileName and the mockFile', () => {
        const parameterValues: ProcessParameter[] = [
          Object.assign(new ProcessParameter(), { name: '--zip', value: 'filename.zip' }),
        ];
        parameterValues.push(Object.assign(new ProcessParameter(), { name: '--add' }));
        expect(scriptService.invoke).toHaveBeenCalledWith(BATCH_IMPORT_SCRIPT_NAME, parameterValues, [fileMock]);
      });
      it('success notification is shown', () => {
        expect(notificationService.success).toHaveBeenCalled();
      });
      it('redirected to process page', () => {
        expect(router.navigateByUrl).toHaveBeenCalledWith('/processes/46');
      });
    });

    describe('if proceed button is pressed with validate only', () => {
      beforeEach(fakeAsync(() => {
        component.validateOnly = true;
        const proceed = fixture.debugElement.query(By.css('#proceedButton')).nativeElement;
        proceed.click();
        fixture.detectChanges();
      }));
      it('metadata-import script is invoked with --zip fileName and the mockFile and -v validate-only', () => {
        const parameterValues: ProcessParameter[] = [
          Object.assign(new ProcessParameter(), { name: '--zip', value: 'filename.zip' }),
          Object.assign(new ProcessParameter(), { name: '--add' }),
          Object.assign(new ProcessParameter(), { name: '-v', value: true }),
        ];
        expect(scriptService.invoke).toHaveBeenCalledWith(BATCH_IMPORT_SCRIPT_NAME, parameterValues, [fileMock]);
      });
      it('success notification is shown', () => {
        expect(notificationService.success).toHaveBeenCalled();
      });
      it('redirected to process page', () => {
        expect(router.navigateByUrl).toHaveBeenCalledWith('/processes/46');
      });
    });

    describe('if proceed is pressed; but script invoke fails', () => {
      beforeEach(fakeAsync(() => {
        jasmine.getEnv().allowRespy(true);
        spyOn(scriptService, 'invoke').and.returnValue(createFailedRemoteDataObject$('Error', 500));
        const proceed = fixture.debugElement.query(By.css('#proceedButton')).nativeElement;
        proceed.click();
        fixture.detectChanges();
      }));
      it('error notification is shown', () => {
        expect(notificationService.error).toHaveBeenCalled();
      });
    });
  });
});
