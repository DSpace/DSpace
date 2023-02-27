import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FileDownloadLinkComponent } from './file-download-link.component';
import { Bitstream } from '../../core/shared/bitstream.model';
import { By } from '@angular/platform-browser';
import { URLCombiner } from '../../core/url-combiner/url-combiner';
import { getBitstreamModuleRoute } from '../../app-routing-paths';
import { AuthorizationDataService } from '../../core/data/feature-authorization/authorization-data.service';
import { cold, getTestScheduler } from 'jasmine-marbles';
import { FeatureID } from '../../core/data/feature-authorization/feature-id';
import { Item } from '../../core/shared/item.model';
import { getItemModuleRoute } from '../../item-page/item-page-routing-paths';
import { RouterLinkDirectiveStub } from '../testing/router-link-directive.stub';

describe('FileDownloadLinkComponent', () => {
  let component: FileDownloadLinkComponent;
  let fixture: ComponentFixture<FileDownloadLinkComponent>;

  let scheduler;
  let authorizationService: AuthorizationDataService;

  let bitstream: Bitstream;
  let item: Item;

  function init() {
    authorizationService = jasmine.createSpyObj('authorizationService', {
      isAuthorized: cold('-a', {a: true})
    });
    bitstream = Object.assign(new Bitstream(), {
      uuid: 'bitstreamUuid',
      _links: {
        self: {href: 'obj-selflink'}
      }
    });
    item = Object.assign(new Item(), {
      uuid: 'itemUuid',
      _links: {
        self: {href: 'obj-selflink'}
      }
    });
  }

  function initTestbed() {
    TestBed.configureTestingModule({
      declarations: [FileDownloadLinkComponent, RouterLinkDirectiveStub],
      providers: [
        {provide: AuthorizationDataService, useValue: authorizationService},
      ]
    })
      .compileComponents();
  }

  describe('init', () => {
    describe('getBitstreamPath', () => {
      describe('when the user has download rights', () => {
        beforeEach(waitForAsync(() => {
          scheduler = getTestScheduler();
          init();
          initTestbed();
        }));

        beforeEach(() => {
          fixture = TestBed.createComponent(FileDownloadLinkComponent);
          component = fixture.componentInstance;
          component.bitstream = bitstream;
          component.item = item;
          fixture.detectChanges();
        });
        it('should return the bitstreamPath based on the input bitstream', () => {
          expect(component.bitstreamPath$).toBeObservable(cold('-a', {a: { routerLink: new URLCombiner(getBitstreamModuleRoute(), bitstream.uuid, 'download').toString(), queryParams: {} }}));
          expect(component.canDownload$).toBeObservable(cold('--a', {a: true}));

        });
        it('should init the component', () => {
          scheduler.flush();
          fixture.detectChanges();
          const link = fixture.debugElement.query(By.css('a'));
          expect(link.injector.get(RouterLinkDirectiveStub).routerLink).toContain(new URLCombiner(getBitstreamModuleRoute(), bitstream.uuid, 'download').toString());
          const lock = fixture.debugElement.query(By.css('.fa-lock'));
          expect(lock).toBeNull();
        });
      });
      describe('when the user has no download rights but has the right to request a copy', () => {
        beforeEach(waitForAsync(() => {
          scheduler = getTestScheduler();
          init();
          (authorizationService.isAuthorized as jasmine.Spy).and.callFake((featureId, object) => {
            if (featureId === FeatureID.CanDownload) {
              return cold('-a', {a: false});
            }
            return cold('-a', {a: true});
          });
          initTestbed();
        }));
        beforeEach(() => {
          fixture = TestBed.createComponent(FileDownloadLinkComponent);
          component = fixture.componentInstance;
          component.item = item;
          component.bitstream = bitstream;
          fixture.detectChanges();
        });
        it('should return the bitstreamPath based on the input bitstream', () => {
          expect(component.bitstreamPath$).toBeObservable(cold('-a', {a: { routerLink: new URLCombiner(getItemModuleRoute(), item.uuid, 'request-a-copy').toString(), queryParams: { bitstream: bitstream.uuid } }}));
          expect(component.canDownload$).toBeObservable(cold('--a', {a: false}));

        });
        it('should init the component', () => {
          scheduler.flush();
          fixture.detectChanges();
          const link = fixture.debugElement.query(By.css('a'));
          expect(link.injector.get(RouterLinkDirectiveStub).routerLink).toContain(new URLCombiner(getItemModuleRoute(), item.uuid, 'request-a-copy').toString());
          const lock = fixture.debugElement.query(By.css('.fa-lock')).nativeElement;
          expect(lock).toBeTruthy();
        });
      });
      describe('when the user has no download rights and no request a copy rights', () => {
        beforeEach(waitForAsync(() => {
          scheduler = getTestScheduler();
          init();
          (authorizationService.isAuthorized as jasmine.Spy).and.returnValue(cold('-a', {a: false}));
          initTestbed();
        }));
        beforeEach(() => {
          fixture = TestBed.createComponent(FileDownloadLinkComponent);
          component = fixture.componentInstance;
          component.bitstream = bitstream;
          component.item = item;
          fixture.detectChanges();
        });
        it('should return the bitstreamPath based on the input bitstream', () => {
          expect(component.bitstreamPath$).toBeObservable(cold('-a', {a: { routerLink: new URLCombiner(getBitstreamModuleRoute(), bitstream.uuid, 'download').toString(), queryParams: {} }}));
          expect(component.canDownload$).toBeObservable(cold('--a', {a: false}));

        });
        it('should init the component', () => {
          scheduler.flush();
          fixture.detectChanges();
          const link = fixture.debugElement.query(By.css('a'));
          expect(link.injector.get(RouterLinkDirectiveStub).routerLink).toContain(new URLCombiner(getBitstreamModuleRoute(), bitstream.uuid, 'download').toString());
          const lock = fixture.debugElement.query(By.css('.fa-lock')).nativeElement;
          expect(lock).toBeTruthy();
        });
      });
    });
  });
});
