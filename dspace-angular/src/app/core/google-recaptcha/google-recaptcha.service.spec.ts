import { GoogleRecaptchaService } from './google-recaptcha.service';
import { of as observableOf } from 'rxjs';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { NativeWindowRef } from '../services/window.service';

describe('GoogleRecaptchaService', () => {
  let service: GoogleRecaptchaService;

  let rendererFactory2;
  let configurationDataService;
  let spy: jasmine.Spy;
  let scriptElementMock: any;
  let cookieService;
  let window;
  const innerHTMLTestValue = 'mock-script-inner-html';
  const document = { documentElement: { lang: 'en' } } as Document;
  scriptElementMock = {
    set innerHTML(newVal) { /* noop */ },
    get innerHTML() { return innerHTMLTestValue; }
  };

  function init() {
    window = new NativeWindowRef();
    rendererFactory2 = jasmine.createSpyObj('rendererFactory2', {
      createRenderer: observableOf('googleRecaptchaToken'),
      createElement: scriptElementMock
    });
    configurationDataService = jasmine.createSpyObj('configurationDataService', {
      findByPropertyName: createSuccessfulRemoteDataObject$({ values: ['googleRecaptchaToken'] })
    });
    cookieService = jasmine.createSpyObj('cookieService', {
      get: '{%22token_item%22:true%2C%22impersonation%22:true%2C%22redirect%22:true%2C%22language%22:true%2C%22klaro%22:true%2C%22has_agreed_end_user%22:true%2C%22google-analytics%22:true}',
      set: () => {
        /* empty */
      }
    });
    service = new GoogleRecaptchaService(cookieService, document, window, rendererFactory2, configurationDataService);
  }

  beforeEach(() => {
    init();
  });

  describe('getRecaptchaToken', () => {
    let result;

    beforeEach(() => {
      spy = spyOn(service, 'getRecaptchaToken').and.stub();
    });

    it('should send a Request with action', () => {
      service.getRecaptchaToken('test');
      expect(spy).toHaveBeenCalledWith('test');
    });

  });
});
