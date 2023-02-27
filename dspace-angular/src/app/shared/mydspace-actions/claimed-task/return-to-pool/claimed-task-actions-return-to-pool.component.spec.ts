import { ChangeDetectionStrategy, Injector, NO_ERRORS_SCHEMA } from '@angular/core';
import { ComponentFixture, fakeAsync, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { TranslateLoader, TranslateModule } from '@ngx-translate/core';

import { ClaimedTaskActionsReturnToPoolComponent } from './claimed-task-actions-return-to-pool.component';
import { ClaimedTask } from '../../../../core/tasks/models/claimed-task-object.model';
import { ProcessTaskResponse } from '../../../../core/tasks/models/process-task-response';
import { ClaimedTaskDataService } from '../../../../core/tasks/claimed-task-data.service';
import { TranslateLoaderMock } from '../../../mocks/translate-loader.mock';
import { NotificationsService } from '../../../notifications/notifications.service';
import { NotificationsServiceStub } from '../../../testing/notifications-service.stub';
import { Router } from '@angular/router';
import { RouterStub } from '../../../testing/router.stub';
import { PoolTaskDataService } from '../../../../core/tasks/pool-task-data.service';
import { SearchService } from '../../../../core/shared/search/search.service';
import { RequestService } from '../../../../core/data/request.service';
import { getMockSearchService } from '../../../mocks/search-service.mock';
import { getMockRequestService } from '../../../mocks/request.service.mock';
import { of } from 'rxjs';

let component: ClaimedTaskActionsReturnToPoolComponent;
let fixture: ComponentFixture<ClaimedTaskActionsReturnToPoolComponent>;

const searchService = getMockSearchService();

const requestService = getMockRequestService();

let mockPoolTaskDataService: PoolTaskDataService;

describe('ClaimedTaskActionsReturnToPoolComponent', () => {
  const object = Object.assign(new ClaimedTask(), { id: 'claimed-task-1' });
  const claimedTaskService = jasmine.createSpyObj('claimedTaskService', {
    returnToPoolTask: of(new ProcessTaskResponse(true))
  });

  beforeEach(waitForAsync(() => {
    mockPoolTaskDataService = new PoolTaskDataService(null, null, null, null);
    TestBed.configureTestingModule({
      imports: [
        TranslateModule.forRoot({
          loader: {
            provide: TranslateLoader,
            useClass: TranslateLoaderMock,
          },
        }),
      ],
      providers: [
        { provide: ClaimedTaskDataService, useValue: claimedTaskService },
        { provide: Injector, useValue: {} },
        { provide: NotificationsService, useValue: new NotificationsServiceStub() },
        { provide: Router, useValue: new RouterStub() },
        { provide: SearchService, useValue: searchService },
        { provide: RequestService, useValue: requestService },
        { provide: PoolTaskDataService, useValue: mockPoolTaskDataService },
      ],
      declarations: [ClaimedTaskActionsReturnToPoolComponent],
      schemas: [NO_ERRORS_SCHEMA]
    }).overrideComponent(ClaimedTaskActionsReturnToPoolComponent, {
      set: { changeDetection: ChangeDetectionStrategy.Default }
    }).compileComponents();
  }));

  beforeEach(fakeAsync(() => {
    fixture = TestBed.createComponent(ClaimedTaskActionsReturnToPoolComponent);
    component = fixture.componentInstance;
    component.object = object;
    spyOn(component, 'initReloadAnchor').and.returnValue(undefined);
    fixture.detectChanges();
  }));

  it('should display return to pool button', () => {
    const btn = fixture.debugElement.query(By.css('.btn-secondary'));

    expect(btn).toBeDefined();
  });

  it('should display spin icon when return to pool action is pending', () => {
    component.processing$.next(true);
    fixture.detectChanges();

    const span = fixture.debugElement.query(By.css('.btn-secondary .fa-spin'));

    expect(span).toBeDefined();
  });

  describe('actionExecution', () => {
    beforeEach(() => {
      component.actionExecution().subscribe();
      fixture.detectChanges();
    });

    it('should call claimedTaskService\'s returnToPoolTask', () => {
      expect(claimedTaskService.returnToPoolTask).toHaveBeenCalledWith(object.id);
    });

  });

  describe('reloadObjectExecution', () => {
    beforeEach(() => {
      spyOn(mockPoolTaskDataService, 'findByItem').and.returnValue(of(null));

      component.itemUuid = 'uuid';
      component.reloadObjectExecution().subscribe();
      fixture.detectChanges();
    });

    it('should call poolTaskDataService findItem with itemUuid', () => {
      expect(mockPoolTaskDataService.findByItem).toHaveBeenCalledWith('uuid');
    });
  });

});
