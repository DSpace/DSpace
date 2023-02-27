import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { CommonModule } from '@angular/common';
import { RouterTestingModule } from '@angular/router/testing';
import { TranslateModule } from '@ngx-translate/core';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ActivatedRoute, Router } from '@angular/router';
import { ItemDataService } from '../../../core/data/item-data.service';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { UploadBitstreamComponent } from './upload-bitstream.component';
import { AuthService } from '../../../core/auth/auth.service';
import { Item } from '../../../core/shared/item.model';
import { of as observableOf } from 'rxjs';
import { VarDirective } from '../../../shared/utils/var.directive';
import { Bitstream } from '../../../core/shared/bitstream.model';
import { BundleDataService } from '../../../core/data/bundle-data.service';
import { Bundle } from '../../../core/shared/bundle.model';
import { RequestService } from '../../../core/data/request.service';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { RouterStub } from '../../../shared/testing/router.stub';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';
import { AuthServiceStub } from '../../../shared/testing/auth-service.stub';
import { environment } from '../../../../environments/environment';
import { buildPaginatedList } from '../../../core/data/paginated-list.model';
import { PageInfo } from '../../../core/shared/page-info.model';

describe('UploadBitstreamComponent', () => {
  let comp: UploadBitstreamComponent;
  let fixture: ComponentFixture<UploadBitstreamComponent>;

  const customName = 'customBundleName';
  const bundle = Object.assign(new Bundle(), {
    id: 'bundle',
    uuid: 'bundle',
    name: customName,
    metadata: {
      'dc.title': [
        {
          value: customName,
          language: null
        }
      ]
    },
    _links: {
      self: { href: 'bundle-selflink' }
    }
  });
  const customCreatedName = 'customCreatedBundleName';
  const createdBundle = Object.assign(new Bundle(), {
    id: 'created-bundle',
    uuid: 'created-bundle',
    name: customCreatedName,
    metadata: {
      'dc.title': [
        {
          value: customCreatedName,
          language: null
        }
      ]
    },
    _links: {
      self: { href: 'created-bundle-selflink' }
    }
  });
  const itemName = 'fake-name';
  const mockItem = Object.assign(new Item(), {
    id: 'fake-id',
    handle: 'fake/handle',
    metadata: {
      'dc.title': [
        {
          language: null,
          value: itemName
        }
      ]
    },
    bundles: createSuccessfulRemoteDataObject$(createPaginatedList([bundle]))
  });
  const standardBundleSuggestions = environment.bundle.standardBundles;
  let routeStub;
  const routerStub = new RouterStub();
  const restEndpoint = 'fake-rest-endpoint';
  const mockItemDataService = jasmine.createSpyObj('mockItemDataService', {
    getBitstreamsEndpoint: observableOf(restEndpoint),
    createBundle: createSuccessfulRemoteDataObject$(createdBundle),
    getBundles: createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [bundle])),
  });
  const bundleService = jasmine.createSpyObj('bundleService', {
    getBitstreamsEndpoint: observableOf(restEndpoint),
    findById: createSuccessfulRemoteDataObject$(bundle)
  });
  const authToken = 'fake-auth-token';
  const authServiceStub = Object.assign(new AuthServiceStub(), {
    buildAuthHeader: () => authToken
  });
  const notificationsServiceStub = new NotificationsServiceStub();
  const uploaderComponent = jasmine.createSpyObj('uploaderComponent', ['ngOnInit', 'ngAfterViewInit']);
  const requestService = jasmine.createSpyObj('requestService', {
    removeByHrefSubstring: {}
  });

  describe('when a file is uploaded', () => {
    beforeEach(waitForAsync(() => {
      createUploadBitstreamTestingModule({});
    }));

    beforeEach(() => {
      loadFixtureAndComp();
    });

    describe('and it fails, calling onUploadError', () => {
      beforeEach(() => {
        comp.onUploadError();
      });

      it('should display an error notification', () => {
        expect(notificationsServiceStub.error).toHaveBeenCalled();
      });
    });

    describe('and it succeeds, calling onCompleteItem', () => {
      const createdBitstream = Object.assign(new Bitstream(), {
        id: 'fake-bitstream'
      });

      beforeEach(() => {
        comp.onCompleteItem(createdBitstream);
      });

      it('should navigate the user to the next page', () => {
        expect(routerStub.navigate).toHaveBeenCalled();
      });
    });
  });

  describe('when a bundle url parameter is present', () => {
    beforeEach(waitForAsync(() => {
      createUploadBitstreamTestingModule({
        bundle: bundle.id
      });
    }));

    beforeEach(() => {
      loadFixtureAndComp();
    });

    it('should set the selected id to the bundle\'s id', () => {
      expect(comp.selectedBundleId).toEqual(bundle.id);
    });

    it('should set the selected name to the bundle\'s name', () => {
      expect(comp.selectedBundleName).toEqual(bundle.name);
    });

    describe('and bundle name changed', () => {
      beforeEach(waitForAsync(() => {
        jasmine.getEnv().allowRespy(true);
        mockItemDataService.getBundles.and.returnValue(createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [bundle])));
        loadFixtureAndComp();
      }));

      it('should clear out the selected id if the name doesn\'t exist', () => {
        comp.selectedBundleName = '';
        comp.bundleNameChange();

        expect(comp.selectedBundleId).toBeUndefined();
      });

      it('should select the correct id if the name exist', () => {
        comp.selectedBundleName = customName;
        comp.bundleNameChange();

        expect(comp.selectedBundleId).toEqual(bundle.id);
      });
    });
  });

  describe('when a name is filled in, but no ID is selected', () => {
    beforeEach(waitForAsync(() => {
      createUploadBitstreamTestingModule({});
    }));

    beforeEach(() => {
      loadFixtureAndComp();
      comp.selectedBundleName = customName;
    });

    describe('createBundle', () => {
      beforeEach(() => {
        comp.createBundle();
      });

      it('should create a new bundle', () => {
        expect(mockItemDataService.createBundle).toHaveBeenCalledWith(mockItem.id, customName);
      });

      it('should set the selected id to the id of the new bundle', () => {
        expect(comp.selectedBundleId).toEqual(createdBundle.id);
      });

      it('should display a success notification', () => {
        expect(notificationsServiceStub.success).toHaveBeenCalled();
      });
    });
  });

  describe('when item has no bundles yet', () => {
    beforeEach(waitForAsync(() => {
      createUploadBitstreamTestingModule({
        bundle: bundle.id
      });
      jasmine.getEnv().allowRespy(true);
      mockItemDataService.getBundles.and.returnValue(createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [])));
      loadFixtureAndComp();
    }));

    it('should display only the standard bundles in dropdown', () => {
      expect(comp.bundles.length).toEqual(standardBundleSuggestions.length);
      for (let i = 0; i < standardBundleSuggestions.length; i++) {
        // noinspection JSDeprecatedSymbols
        expect(comp.bundles[i].name).toEqual(standardBundleSuggestions[i]);
      }
    });
  });

  describe('when item has a custom bundle', () => {
    beforeEach(waitForAsync(() => {
      createUploadBitstreamTestingModule({
        bundle: bundle.id
      });
      jasmine.getEnv().allowRespy(true);
      mockItemDataService.getBundles.and.returnValue(createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [bundle])));
      loadFixtureAndComp();
    }));

    it('should still display existing bitstream bundles if the bitstream has bundles', () => {
      const expectedSuggestions = [bundle.name, ...standardBundleSuggestions];
      expect(comp.bundles.length).toEqual(expectedSuggestions.length);
      for (let i = 0; i < expectedSuggestions.length; i++) {
        // noinspection JSDeprecatedSymbols
        expect(comp.bundles[i].name).toEqual(expectedSuggestions[i]);
      }
    });
  });

  describe('when item has a standard bundle', () => {
    let clonedBundle;
    beforeEach(waitForAsync(() => {
      clonedBundle = { ...bundle };
      expect(standardBundleSuggestions.length).toBeGreaterThan(0);
      clonedBundle.name = standardBundleSuggestions[0];
      createUploadBitstreamTestingModule({
        bundle: clonedBundle.id
      });
      jasmine.getEnv().allowRespy(true);
      mockItemDataService.getBundles.and.returnValue(createSuccessfulRemoteDataObject$(buildPaginatedList(new PageInfo(), [clonedBundle])));
      loadFixtureAndComp();
    }));

    it('should not show duplicate bundle names', () => {
      const expectedSuggestions = [clonedBundle.name, ...standardBundleSuggestions.filter((standardBundleSuggestion: string) => standardBundleSuggestion !== clonedBundle.name)];
      expect(comp.bundles.length).toEqual(expectedSuggestions.length);
      for (let i = 0; i < expectedSuggestions.length; i++) {
        // noinspection JSDeprecatedSymbols
        expect(comp.bundles[i].name).toEqual(expectedSuggestions[i]);
      }
    });
  });

  /**
   * Setup an UploadBitstreamComponent testing module with custom queryParams for the route
   * @param queryParams
   */
  function createUploadBitstreamTestingModule(queryParams) {
    routeStub = {
      data: observableOf({
        dso: createSuccessfulRemoteDataObject(mockItem)
      }),
      queryParams: observableOf(queryParams),
      snapshot: {
        queryParams: queryParams,
        params: {
          id: mockItem.id
        }
      }
    };

    TestBed.configureTestingModule({
      imports: [CommonModule, RouterTestingModule.withRoutes([]), TranslateModule.forRoot(), NgbModule],
      declarations: [UploadBitstreamComponent, VarDirective],
      providers: [
        { provide: ActivatedRoute, useValue: routeStub },
        { provide: Router, useValue: routerStub },
        { provide: ItemDataService, useValue: mockItemDataService },
        { provide: NotificationsService, useValue: notificationsServiceStub },
        { provide: AuthService, useValue: authServiceStub },
        { provide: BundleDataService, useValue: bundleService },
        { provide: RequestService, useValue: requestService }
      ], schemas: [
        NO_ERRORS_SCHEMA
      ]
    }).compileComponents();
  }

  /**
   * Load the TestBed's fixture and component
   */
  function loadFixtureAndComp() {
    fixture = TestBed.createComponent(UploadBitstreamComponent);
    comp = fixture.componentInstance;
    comp.uploaderComponent = uploaderComponent;
    fixture.detectChanges();
  }

});
