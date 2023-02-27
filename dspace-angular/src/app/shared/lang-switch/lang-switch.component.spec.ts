import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';

import { Observable, of } from 'rxjs';
import { TranslateLoader, TranslateModule, TranslateService } from '@ngx-translate/core';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';

import { LangSwitchComponent } from './lang-switch.component';
import { LangConfig } from '../../../config/lang-config.interface';
import { LocaleService } from '../../core/locale/locale.service';

// This test is completely independent from any message catalogs or keys in the codebase
// The translation module is instantiated with these bogus messages that we aren't using anyway.

// Double quotes are mandatory in JSON, so de-activating the tslint rule checking for single quotes here.
/* eslint-disable @typescript-eslint/quotes */
// JSON for the language files has double quotes around all literals
/* eslint-disable quote-props */
class CustomLoader implements TranslateLoader {
  getTranslation(lang: string): Observable<any> {
    return of({
      'footer': {
        'copyright': 'copyright © 2002-{{ year }}',
        'link.dspace': 'DSpace software',
        'link.lyrasis': 'LYRASIS'
      }
    });
  }
}

/* eslint-enable @typescript-eslint/quotes */
/* eslint-enable quote-props */

let localService: any;

describe('LangSwitchComponent', () => {

  function getMockLocaleService(): LocaleService {
    return jasmine.createSpyObj('LocaleService', {
      setCurrentLanguageCode: jasmine.createSpy('setCurrentLanguageCode'),
      refreshAfterChangeLanguage: jasmine.createSpy('refreshAfterChangeLanguage')
    });
  }

  describe('with English and Deutsch activated, English as default', () => {
    let component: LangSwitchComponent;
    let fixture: ComponentFixture<LangSwitchComponent>;
    let de: DebugElement;
    let langSwitchElement: HTMLElement;

    let translate: TranslateService;
    let http: HttpTestingController;

    beforeEach(waitForAsync(() => {

      const mockConfig = {
        languages: [{
          code: 'en',
          label: 'English',
          active: true,
        }, {
          code: 'de',
          label: 'Deutsch',
          active: true,
        }]
      };

      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule, TranslateModule.forRoot(
          {
            loader: { provide: TranslateLoader, useClass: CustomLoader }
          }
        )],
        declarations: [LangSwitchComponent],
        schemas: [NO_ERRORS_SCHEMA],
        providers: [
          TranslateService,
          { provide: LocaleService, useValue: getMockLocaleService() },
        ]
      }).compileComponents()
        .then(() => {
          translate = TestBed.inject(TranslateService);
          translate.addLangs(mockConfig.languages.filter((langConfig: LangConfig) => langConfig.active === true).map((a) => a.code));
          translate.setDefaultLang('en');
          translate.use('en');
          http = TestBed.inject(HttpTestingController);
          fixture = TestBed.createComponent(LangSwitchComponent);
          localService = TestBed.inject(LocaleService);
          component = fixture.componentInstance;
          de = fixture.debugElement;
          langSwitchElement = de.nativeElement;
          fixture.detectChanges();
        });
    }));

    it('should create', () => {
      expect(component).toBeDefined();
    });

    it('should identify English as the label for the current active language in the component', waitForAsync(() => {
      fixture.detectChanges();
      expect(component.currentLangLabel()).toEqual('English');
    }));

    it('should be initialized with more than one language active', waitForAsync(() => {
      fixture.detectChanges();
      expect(component.moreThanOneLanguage).toBeTruthy();
    }));

    it('should define the main A HREF in the UI', (() => {
      expect(langSwitchElement.querySelector('a')).toBeDefined();
    }));

    describe('when selecting a language', () => {
      beforeEach(() => {
        spyOn(translate, 'use');
        const langItem = fixture.debugElement.query(By.css('.dropdown-item')).nativeElement;
        langItem.click();
        fixture.detectChanges();
      });

      it('should translate the app and set the client\'s language cookie', () => {
        expect(localService.setCurrentLanguageCode).toHaveBeenCalled();
        expect(localService.refreshAfterChangeLanguage).toHaveBeenCalled();
      });

    });
  });

  describe('with English as the only active and also default language', () => {

    let component: LangSwitchComponent;
    let fixture: ComponentFixture<LangSwitchComponent>;
    let de: DebugElement;
    let langSwitchElement: HTMLElement;

    let translate: TranslateService;
    let http: HttpTestingController;

    beforeEach(waitForAsync(() => {

      const mockConfig = {
        languages: [{
          code: 'en',
          label: 'English',
          active: true,
        }, {
          code: 'de',
          label: 'Deutsch',
          active: false
        }]
      };

      TestBed.configureTestingModule({
        imports: [HttpClientTestingModule, TranslateModule.forRoot(
          {
            loader: { provide: TranslateLoader, useClass: CustomLoader }
          }
        )],
        declarations: [LangSwitchComponent],
        schemas: [NO_ERRORS_SCHEMA],
        providers: [
          TranslateService,
          { provide: LocaleService, useValue: getMockLocaleService() }
        ]
      }).compileComponents();
      translate = TestBed.inject(TranslateService);
      translate.addLangs(mockConfig.languages.filter((MyLangConfig) => MyLangConfig.active === true).map((a) => a.code));
      translate.setDefaultLang('en');
      translate.use('en');
      http = TestBed.inject(HttpTestingController);
    }));

    beforeEach(() => {
      fixture = TestBed.createComponent(LangSwitchComponent);
      component = fixture.componentInstance;
      de = fixture.debugElement;
      langSwitchElement = de.nativeElement;
    });

    it('should create', () => {
      expect(component).toBeDefined();
    });

    it('should not define the main header for the language switch, as it should be invisible', (() => {
      expect(langSwitchElement.querySelector('a')).toBeNull();
    }));

  });

});
