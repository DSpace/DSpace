import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { SystemWideAlertDataService } from '../../core/data/system-wide-alert-data.service';
import { SystemWideAlert } from '../system-wide-alert.model';
import { utcToZonedTime, zonedTimeToUtc } from 'date-fns-tz';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { createPaginatedList } from '../../shared/testing/utils.test';
import { TranslateModule } from '@ngx-translate/core';
import { SystemWideAlertFormComponent } from './system-wide-alert-form.component';
import { RequestService } from '../../core/data/request.service';
import { NotificationsServiceStub } from '../../shared/testing/notifications-service.stub';
import { RouterStub } from '../../shared/testing/router.stub';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { UiSwitchModule } from 'ngx-ui-switch';
import { SystemWideAlertModule } from '../system-wide-alert.module';

describe('SystemWideAlertFormComponent', () => {
  let comp: SystemWideAlertFormComponent;
  let fixture: ComponentFixture<SystemWideAlertFormComponent>;
  let systemWideAlertDataService: SystemWideAlertDataService;

  let systemWideAlert: SystemWideAlert;
  let requestService: RequestService;
  let notificationsService;
  let router;


  beforeEach(waitForAsync(() => {

    const countDownDate = new Date();
    countDownDate.setDate(countDownDate.getDate() + 1);
    countDownDate.setHours(countDownDate.getHours() + 1);
    countDownDate.setMinutes(countDownDate.getMinutes() + 1);

    systemWideAlert = Object.assign(new SystemWideAlert(), {
      alertId: 1,
      message: 'Test alert message',
      active: true,
      countdownTo: utcToZonedTime(countDownDate, 'UTC').toISOString()
    });

    systemWideAlertDataService = jasmine.createSpyObj('systemWideAlertDataService', {
      findAll: createSuccessfulRemoteDataObject$(createPaginatedList([systemWideAlert])),
      put: createSuccessfulRemoteDataObject$(systemWideAlert),
      create: createSuccessfulRemoteDataObject$(systemWideAlert)
    });

    requestService = jasmine.createSpyObj('requestService', ['setStaleByHrefSubstring']);

    notificationsService = new NotificationsServiceStub();
    router = new RouterStub();

    TestBed.configureTestingModule({
      imports: [FormsModule, SystemWideAlertModule, UiSwitchModule, TranslateModule.forRoot()],
      declarations: [SystemWideAlertFormComponent],
      providers: [
        {provide: SystemWideAlertDataService, useValue: systemWideAlertDataService},
        {provide: NotificationsService, useValue: notificationsService},
        {provide: Router, useValue: router},
        {provide: RequestService, useValue: requestService},
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SystemWideAlertFormComponent);
    comp = fixture.componentInstance;

    spyOn(comp, 'createForm').and.callThrough();
    spyOn(comp, 'initFormValues').and.callThrough();

    fixture.detectChanges();
  });

  describe('init', () => {
    it('should init the comp', () => {
      expect(comp).toBeTruthy();
    });
    it('should create the form and init the values based on an existing alert', () => {
      expect(comp.createForm).toHaveBeenCalled();
      expect(comp.initFormValues).toHaveBeenCalledWith(systemWideAlert);
    });
  });

  describe('createForm', () => {
    it('should create the form', () => {
      const now = new Date();

      comp.createForm();
      expect(comp.formMessage.value).toEqual('');
      expect(comp.formActive.value).toEqual(false);
      expect(comp.time).toEqual({hour: now.getHours(), minute: now.getMinutes()});
      expect(comp.date).toEqual({year: now.getFullYear(), month: now.getMonth() + 1, day: now.getDate()});
    });
  });

  describe('initFormValues', () => {
    it('should fill in the form based on the provided system-wide alert', () => {
      comp.initFormValues(systemWideAlert);

      const countDownTo = zonedTimeToUtc(systemWideAlert.countdownTo, 'UTC');

      expect(comp.formMessage.value).toEqual(systemWideAlert.message);
      expect(comp.formActive.value).toEqual(true);
      expect(comp.time).toEqual({hour: countDownTo.getHours(), minute: countDownTo.getMinutes()});
      expect(comp.date).toEqual({
        year: countDownTo.getFullYear(),
        month: countDownTo.getMonth() + 1,
        day: countDownTo.getDate()
      });
    });
  });
  describe('setCounterEnabled', () => {
    it('should set the preview time on enable and update the behaviour subject', () => {
      spyOn(comp, 'updatePreviewTime');
      comp.setCounterEnabled(true);

      expect(comp.updatePreviewTime).toHaveBeenCalled();
      expect(comp.counterEnabled$.value).toBeTrue();
    });
    it('should reset the preview time on disable and update the behaviour subject', () => {
      spyOn(comp, 'updatePreviewTime');
      comp.setCounterEnabled(false);

      expect(comp.updatePreviewTime).not.toHaveBeenCalled();
      expect(comp.previewDays).toEqual(0);
      expect(comp.previewHours).toEqual(0);
      expect(comp.previewMinutes).toEqual(0);
      expect(comp.counterEnabled$.value).toBeFalse();
    });
  });

  describe('updatePreviewTime', () => {
    it('should calculate the difference between the current date and the date configured in the form', () => {
      const countDownDate = new Date();
      countDownDate.setDate(countDownDate.getDate() + 1);
      countDownDate.setHours(countDownDate.getHours() + 1);
      countDownDate.setMinutes(countDownDate.getMinutes() + 1);

      comp.time = {hour: countDownDate.getHours(), minute: countDownDate.getMinutes()};
      comp.date = {year: countDownDate.getFullYear(), month: countDownDate.getMonth() + 1, day: countDownDate.getDate()};

      comp.updatePreviewTime();

      expect(comp.previewDays).toEqual(1);
      expect(comp.previewHours).toEqual(1);
      expect(comp.previewDays).toEqual(1);
    });
  });

  describe('setActive', () => {
    it('should set whether the alert is active and save the current alert', () => {
      spyOn(comp, 'save');
      spyOn(comp.formActive, 'patchValue');
      comp.setActive(true);

      expect(comp.formActive.patchValue).toHaveBeenCalledWith(true);
      expect(comp.save).toHaveBeenCalledWith(false);
    });
  });

  describe('save', () => {
    it('should update the exising alert with the form values and show a success notification on success and navigate back', () => {
      spyOn(comp, 'back');
      comp.currentAlert = systemWideAlert;

      comp.formMessage.patchValue('New message');
      comp.formActive.patchValue(true);
      comp.time = {hour: 4, minute: 26};
      comp.date = {year: 2023, month: 1, day: 25};

      const expectedAlert = new SystemWideAlert();
      expectedAlert.alertId = systemWideAlert.alertId;
      expectedAlert.message = 'New message';
      expectedAlert.active = true;
      const countDownTo = new Date(2023, 0, 25, 4, 26);
      expectedAlert.countdownTo = utcToZonedTime(countDownTo, 'UTC').toUTCString();

      comp.save();

      expect(systemWideAlertDataService.put).toHaveBeenCalledWith(expectedAlert);
      expect(notificationsService.success).toHaveBeenCalled();
      expect(requestService.setStaleByHrefSubstring).toHaveBeenCalledWith('systemwidealerts');
      expect(comp.back).toHaveBeenCalled();
    });
    it('should update the exising alert with the form values and show a success notification on success and not navigate back when false is provided to the save method', () => {
      spyOn(comp, 'back');
      comp.currentAlert = systemWideAlert;

      comp.formMessage.patchValue('New message');
      comp.formActive.patchValue(true);
      comp.time = {hour: 4, minute: 26};
      comp.date = {year: 2023, month: 1, day: 25};

      const expectedAlert = new SystemWideAlert();
      expectedAlert.alertId = systemWideAlert.alertId;
      expectedAlert.message = 'New message';
      expectedAlert.active = true;
      const countDownTo = new Date(2023, 0, 25, 4, 26);
      expectedAlert.countdownTo = utcToZonedTime(countDownTo, 'UTC').toUTCString();

      comp.save(false);

      expect(systemWideAlertDataService.put).toHaveBeenCalledWith(expectedAlert);
      expect(notificationsService.success).toHaveBeenCalled();
      expect(requestService.setStaleByHrefSubstring).toHaveBeenCalledWith('systemwidealerts');
      expect(comp.back).not.toHaveBeenCalled();
    });
    it('should update the exising alert with the form values but add an empty countdown date when disabled and show a success notification on success', () => {
      spyOn(comp, 'back');
      comp.currentAlert = systemWideAlert;

      comp.formMessage.patchValue('New message');
      comp.formActive.patchValue(true);
      comp.time = {hour: 4, minute: 26};
      comp.date = {year: 2023, month: 1, day: 25};
      comp.counterEnabled$.next(false);

      const expectedAlert = new SystemWideAlert();
      expectedAlert.alertId = systemWideAlert.alertId;
      expectedAlert.message = 'New message';
      expectedAlert.active = true;
      expectedAlert.countdownTo = null;

      comp.save();

      expect(systemWideAlertDataService.put).toHaveBeenCalledWith(expectedAlert);
      expect(notificationsService.success).toHaveBeenCalled();
      expect(requestService.setStaleByHrefSubstring).toHaveBeenCalledWith('systemwidealerts');
      expect(comp.back).toHaveBeenCalled();
    });
    it('should update the exising alert with the form values and show a error notification on error', () => {
      spyOn(comp, 'back');
      (systemWideAlertDataService.put as jasmine.Spy).and.returnValue(createFailedRemoteDataObject$());
      comp.currentAlert = systemWideAlert;

      comp.formMessage.patchValue('New message');
      comp.formActive.patchValue(true);
      comp.time = {hour: 4, minute: 26};
      comp.date = {year: 2023, month: 1, day: 25};

      const expectedAlert = new SystemWideAlert();
      expectedAlert.alertId = systemWideAlert.alertId;
      expectedAlert.message = 'New message';
      expectedAlert.active = true;
      const countDownTo = new Date(2023, 0, 25, 4, 26);
      expectedAlert.countdownTo = utcToZonedTime(countDownTo, 'UTC').toUTCString();

      comp.save();

      expect(systemWideAlertDataService.put).toHaveBeenCalledWith(expectedAlert);
      expect(notificationsService.error).toHaveBeenCalled();
      expect(requestService.setStaleByHrefSubstring).not.toHaveBeenCalledWith('systemwidealerts');
      expect(comp.back).not.toHaveBeenCalled();
    });
    it('should create a new alert with the form values and show a success notification on success', () => {
      spyOn(comp, 'back');
      comp.currentAlert = undefined;

      comp.formMessage.patchValue('New message');
      comp.formActive.patchValue(true);
      comp.time = {hour: 4, minute: 26};
      comp.date = {year: 2023, month: 1, day: 25};

      const expectedAlert = new SystemWideAlert();
      expectedAlert.message = 'New message';
      expectedAlert.active = true;
      const countDownTo = new Date(2023, 0, 25, 4, 26);
      expectedAlert.countdownTo = utcToZonedTime(countDownTo, 'UTC').toUTCString();

      comp.save();

      expect(systemWideAlertDataService.create).toHaveBeenCalledWith(expectedAlert);
      expect(notificationsService.success).toHaveBeenCalled();
      expect(requestService.setStaleByHrefSubstring).toHaveBeenCalledWith('systemwidealerts');
      expect(comp.back).toHaveBeenCalled();

    });
    it('should create a new alert with the form values and show a error notification on error', () => {
      spyOn(comp, 'back');
      (systemWideAlertDataService.create as jasmine.Spy).and.returnValue(createFailedRemoteDataObject$());

      comp.currentAlert = undefined;

      comp.formMessage.patchValue('New message');
      comp.formActive.patchValue(true);
      comp.time = {hour: 4, minute: 26};
      comp.date = {year: 2023, month: 1, day: 25};

      const expectedAlert = new SystemWideAlert();
      expectedAlert.message = 'New message';
      expectedAlert.active = true;
      const countDownTo = new Date(2023, 0, 25, 4, 26);
      expectedAlert.countdownTo = utcToZonedTime(countDownTo, 'UTC').toUTCString();

      comp.save();

      expect(systemWideAlertDataService.create).toHaveBeenCalledWith(expectedAlert);
      expect(notificationsService.error).toHaveBeenCalled();
      expect(requestService.setStaleByHrefSubstring).not.toHaveBeenCalledWith('systemwidealerts');
      expect(comp.back).not.toHaveBeenCalled();

    });
  });
  describe('back', () => {
    it('should navigate back to the home page', () => {
      comp.back();
      expect(router.navigate).toHaveBeenCalledWith(['/home']);
    });
  });


});
