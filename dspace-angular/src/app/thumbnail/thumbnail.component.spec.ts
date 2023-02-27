import { DebugElement, Pipe, PipeTransform } from '@angular/core';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { Bitstream } from '../core/shared/bitstream.model';
import { SafeUrlPipe } from '../shared/utils/safe-url-pipe';
import { of as observableOf } from 'rxjs';

import { ThumbnailComponent } from './thumbnail.component';
import { RemoteData } from '../core/data/remote-data';
import { createFailedRemoteDataObject, createSuccessfulRemoteDataObject } from '../shared/remote-data.utils';
import { AuthService } from '../core/auth/auth.service';
import { FileService } from '../core/shared/file.service';
import { VarDirective } from '../shared/utils/var.directive';
import { AuthorizationDataService } from '../core/data/feature-authorization/authorization-data.service';

// eslint-disable-next-line @angular-eslint/pipe-prefix
@Pipe({ name: 'translate' })
class MockTranslatePipe implements PipeTransform {
  transform(key: string): string {
    return 'TRANSLATED ' + key;
  }
}

const CONTENT = 'content.url';

describe('ThumbnailComponent', () => {
  let comp: ThumbnailComponent;
  let fixture: ComponentFixture<ThumbnailComponent>;
  let de: DebugElement;
  let el: HTMLElement;
  let authService;
  let authorizationService;
  let fileService;

  beforeEach(waitForAsync(() => {
    authService = jasmine.createSpyObj('AuthService', {
      isAuthenticated: observableOf(true),
    });
    authorizationService = jasmine.createSpyObj('AuthorizationService', {
      isAuthorized: observableOf(true),
    });
    fileService = jasmine.createSpyObj('FileService', {
      retrieveFileDownloadLink: null
    });
    fileService.retrieveFileDownloadLink.and.callFake((url) => observableOf(`${url}?authentication-token=fake`));

    TestBed.configureTestingModule({
      declarations: [ThumbnailComponent, SafeUrlPipe, MockTranslatePipe, VarDirective],
      providers: [
        { provide: AuthService, useValue: authService },
        { provide: AuthorizationDataService, useValue: authorizationService },
        { provide: FileService, useValue: fileService }
      ]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ThumbnailComponent);
    fixture.detectChanges();

    authService = TestBed.inject(AuthService);

    comp = fixture.componentInstance; // ThumbnailComponent test instance
    de = fixture.debugElement.query(By.css('div.thumbnail'));
    el = de.nativeElement;
  });

  describe('loading', () => {
    it('should start out with isLoading$ true', () => {
      expect(comp.isLoading$.getValue()).toBeTrue();
    });

    it('should set isLoading$ to false once an image is successfully loaded', () => {
      comp.setSrc('http://bit.stream');
      fixture.debugElement.query(By.css('img.thumbnail-content')).triggerEventHandler('load', new Event('load'));
      expect(comp.isLoading$.getValue()).toBeFalse();
    });

    it('should set isLoading$ to false once the src is set to null', () => {
      comp.setSrc(null);
      expect(comp.isLoading$.getValue()).toBeFalse();
    });

    it('should show a loading animation while isLoading$ is true', () => {
      expect(de.query(By.css('ds-themed-loading'))).toBeTruthy();

      comp.isLoading$.next(false);
      fixture.detectChanges();
      expect(fixture.debugElement.query(By.css('ds-themed-loading'))).toBeFalsy();
    });

    describe('with a thumbnail image', () => {
      beforeEach(() => {
        comp.src$.next('https://bit.stream');
        fixture.detectChanges();
      });

      it('should render but hide the image while loading and show it once done', () => {
        let img = fixture.debugElement.query(By.css('img.thumbnail-content'));
        expect(img).toBeTruthy();
        expect(img.classes['d-none']).toBeTrue();

        comp.isLoading$.next(false);
        fixture.detectChanges();
        img = fixture.debugElement.query(By.css('img.thumbnail-content'));
        expect(img).toBeTruthy();
        expect(img.classes['d-none']).toBeFalsy();
      });

    });

    describe('without a thumbnail image', () => {
      beforeEach(() => {
        comp.src$.next(null);
        fixture.detectChanges();
      });

      it('should only show the HTML placeholder once done loading', () => {
        expect(fixture.debugElement.query(By.css('div.thumbnail-placeholder'))).toBeFalsy();

        comp.isLoading$.next(false);
        fixture.detectChanges();
        expect(fixture.debugElement.query(By.css('div.thumbnail-placeholder'))).toBeTruthy();
      });
    });

  });

  const errorHandler = () => {
    let setSrcSpy;

    beforeEach(() => {
      // disconnect error handler to be sure it's only called once
      const img = fixture.debugElement.query(By.css('img.thumbnail-content'));
      img.nativeNode.onerror = null;

      comp.ngOnChanges();
      setSrcSpy = spyOn(comp, 'setSrc').and.callThrough();
    });

    describe('retry with authentication token', () => {
      it('should remember that it already retried once', () => {
        expect(comp.retriedWithToken).toBeFalse();
        comp.errorHandler();
        expect(comp.retriedWithToken).toBeTrue();
      });

      describe('if not logged in', () => {
        beforeEach(() => {
          authService.isAuthenticated.and.returnValue(observableOf(false));
        });

        it('should fall back to default', () => {
          comp.errorHandler();
          expect(setSrcSpy).toHaveBeenCalledWith(comp.defaultImage);
        });
      });

      describe('if logged in', () => {
        beforeEach(() => {
          authService.isAuthenticated.and.returnValue(observableOf(true));
        });

        describe('and authorized to download the thumbnail', () => {
          beforeEach(() => {
            authorizationService.isAuthorized.and.returnValue(observableOf(true));
          });

          it('should add an authentication token to the thumbnail URL', () => {
            comp.errorHandler();

            if ((comp.thumbnail as RemoteData<Bitstream>)?.hasFailed) {
              // If we failed to retrieve the Bitstream in the first place, fall back to the default
              expect(setSrcSpy).toHaveBeenCalledWith(comp.defaultImage);
            } else {
              expect(setSrcSpy).toHaveBeenCalledWith(CONTENT + '?authentication-token=fake');
            }
          });
        });

        describe('but not authorized to download the thumbnail', () => {
          beforeEach(() => {
            authorizationService.isAuthorized.and.returnValue(observableOf(false));
          });

          it('should fall back to default', () => {
            comp.errorHandler();

            expect(setSrcSpy).toHaveBeenCalledWith(comp.defaultImage);

            // We don't need to check authorization if we failed to retrieve the Bitstreamin the first place
            if (!(comp.thumbnail as RemoteData<Bitstream>)?.hasFailed) {
              expect(authorizationService.isAuthorized).toHaveBeenCalled();
            }
          });
        });
      });
    });

    describe('after retrying with token', () => {
      beforeEach(() => {
        comp.retriedWithToken = true;
      });

      it('should fall back to default', () => {
        comp.errorHandler();
        expect(authService.isAuthenticated).not.toHaveBeenCalled();
        expect(fileService.retrieveFileDownloadLink).not.toHaveBeenCalled();
        expect(setSrcSpy).toHaveBeenCalledWith(comp.defaultImage);
      });
    });
  };

  describe('fallback', () => {
    describe('if there is a default image', () => {
      it('should display the default image', () => {
        comp.src$.next('http://bit.stream');
        comp.defaultImage = 'http://default.img';
        comp.errorHandler();
        expect(comp.src$.getValue()).toBe(comp.defaultImage);
      });

      it('should include the alt text', () => {
        comp.src$.next('http://bit.stream');
        comp.defaultImage = 'http://default.img';
        comp.errorHandler();

        fixture.detectChanges();
        const image: HTMLElement = fixture.debugElement.query(By.css('img')).nativeElement;
        expect(image.getAttribute('alt')).toBe('TRANSLATED ' + comp.alt);
      });
    });

    describe('if there is no default image', () => {
      it('should display the HTML placeholder', () => {
        comp.src$.next('http://default.img');
        comp.defaultImage = null;
        comp.errorHandler();
        expect(comp.src$.getValue()).toBe(null);

        fixture.detectChanges();
        const placeholder = fixture.debugElement.query(By.css('div.thumbnail-placeholder')).nativeElement;
        expect(placeholder.innerHTML).toContain('TRANSLATED ' + comp.placeholder);
      });
    });
  });

  describe('with thumbnail as Bitstream', () => {
    let thumbnail;
    beforeEach(() => {
      thumbnail = new Bitstream();
      thumbnail._links = {
        self: { href: 'self.url' },
        bundle: { href: 'bundle.url' },
        format: { href: 'format.url' },
        content: { href: CONTENT },
        thumbnail: undefined,
      };
      comp.thumbnail = thumbnail;
    });

    describe('if content can be loaded', () => {
      it('should display an image', () => {
        comp.ngOnChanges();
        fixture.detectChanges();
        const image: HTMLElement = fixture.debugElement.query(By.css('img')).nativeElement;
        expect(image.getAttribute('src')).toBe(thumbnail._links.content.href);
      });

      it('should include the alt text', () => {
        comp.ngOnChanges();
        fixture.detectChanges();
        const image: HTMLElement = fixture.debugElement.query(By.css('img')).nativeElement;
        expect(image.getAttribute('alt')).toBe('TRANSLATED ' + comp.alt);
      });
    });

    describe('if content can\'t be loaded', () => {
      errorHandler();
    });
  });

  describe('with thumbnail as RemoteData<Bitstream>', () => {
    let thumbnail: Bitstream;

    beforeEach(() => {
      thumbnail = new Bitstream();
      thumbnail._links = {
        self: { href: 'self.url' },
        bundle: { href: 'bundle.url' },
        format: { href: 'format.url' },
        content: { href: CONTENT },
        thumbnail: undefined
      };
    });

    describe('if RemoteData succeeded', () => {
      beforeEach(() => {
        comp.thumbnail = createSuccessfulRemoteDataObject(thumbnail);
      });

      describe('if content can be loaded', () => {
        it('should display an image', () => {
          comp.ngOnChanges();
          fixture.detectChanges();
          const image: HTMLElement = de.query(By.css('img')).nativeElement;
          expect(image.getAttribute('src')).toBe(thumbnail._links.content.href);
        });

        it('should display the alt text', () => {
          comp.ngOnChanges();
          fixture.detectChanges();
          const image: HTMLElement = de.query(By.css('img')).nativeElement;
          expect(image.getAttribute('alt')).toBe('TRANSLATED ' + comp.alt);
        });
      });

      describe('if content can\'t be loaded', () => {
        errorHandler();
      });
    });

    describe('if RemoteData failed', () => {
      beforeEach(() => {
        comp.thumbnail = createFailedRemoteDataObject();
      });

      it('should show the default image', () => {
        comp.defaultImage = 'default/image.jpg';
        comp.ngOnChanges();
        expect(comp.src$.getValue()).toBe('default/image.jpg');
      });
    });
  });
});
