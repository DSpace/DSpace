import { ItemVersionsNoticeComponent } from './item-versions-notice.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { VersionHistory } from '../../../core/shared/version-history.model';
import { Version } from '../../../core/shared/version.model';
import { Item } from '../../../core/shared/item.model';
import { VersionHistoryDataService } from '../../../core/data/version-history-data.service';
import { By } from '@angular/platform-browser';
import { createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { of } from 'rxjs';
import { take } from 'rxjs/operators';

describe('ItemVersionsNoticeComponent', () => {
  let component: ItemVersionsNoticeComponent;
  let fixture: ComponentFixture<ItemVersionsNoticeComponent>;
  let versionHistoryService: VersionHistoryDataService;

  const versionHistory = Object.assign(new VersionHistory(), {
    id: '1'
  });
  const firstVersion = Object.assign(new Version(), {
    id: '1',
    version: 1,
    created: new Date(2020, 1, 1),
    summary: 'first version',
    versionhistory: createSuccessfulRemoteDataObject$(versionHistory)
  });
  const latestVersion = Object.assign(new Version(), {
    id: '2',
    version: 2,
    summary: 'latest version',
    created: new Date(2020, 1, 2),
    versionhistory: createSuccessfulRemoteDataObject$(versionHistory)
  });
  const versions = [latestVersion, firstVersion];
  versionHistory.versions = createSuccessfulRemoteDataObject$(createPaginatedList(versions));
  const firstItem = Object.assign(new Item(), {
    id: 'first_item_id',
    uuid: 'first_item_id',
    handle: '123456789/1',
    version: createSuccessfulRemoteDataObject$(firstVersion)
  });
  const latestItem = Object.assign(new Item(), {
    id: 'latest_item_id',
    uuid: 'latest_item_id',
    handle: '123456789/2',
    version: createSuccessfulRemoteDataObject$(latestVersion)
  });
  firstVersion.item = createSuccessfulRemoteDataObject$(firstItem);
  latestVersion.item = createSuccessfulRemoteDataObject$(latestItem);

  const versionHistoryServiceSpy = jasmine.createSpyObj('versionHistoryService',
    ['getVersions', 'getLatestVersionFromHistory$', 'isLatest$', ]
  );

  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      declarations: [ItemVersionsNoticeComponent],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
        { provide: VersionHistoryDataService, useValue: versionHistoryServiceSpy }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    versionHistoryService = TestBed.inject(VersionHistoryDataService);

    const isLatestFcn = (version: Version) => of((version.version === latestVersion.version));

    versionHistoryServiceSpy.getVersions.and.returnValue(createSuccessfulRemoteDataObject$(createPaginatedList(versions)));
    versionHistoryServiceSpy.getLatestVersionFromHistory$.and.returnValue(of(latestVersion));
    versionHistoryServiceSpy.isLatest$.and.callFake(isLatestFcn);
  }));

  describe('when the item is the latest version', () => {
    beforeEach(() => {
      initComponentWithItem(latestItem);
    });

    it('should not display a notice', () => {
      const alert = fixture.debugElement.query(By.css('ds-alert'));
      expect(alert).toBeNull();
    });
  });

  describe('when the item is not the latest version', () => {
    beforeEach(() => {
      initComponentWithItem(firstItem);
    });

    it('should display a notice', () => {
      const alert = fixture.debugElement.query(By.css('ds-alert'));
      expect(alert).not.toBeNull();
    });
  });

  describe('isLatest', () => {
    it('firstVersion should not be the latest', () => {
      versionHistoryService.isLatest$(firstVersion).pipe(take(1)).subscribe((res) => {
        expect(res).toBeFalse();
      });
    });
    it('latestVersion should be the latest', () => {
      versionHistoryService.isLatest$(latestVersion).pipe(take(1)).subscribe((res) => {
        expect(res).toBeTrue();
      });
    });
  });

  function initComponentWithItem(item: Item) {
    fixture = TestBed.createComponent(ItemVersionsNoticeComponent);
    component = fixture.componentInstance;
    component.item = item;
    fixture.detectChanges();
  }
});
