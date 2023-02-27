import { TranslateService } from '@ngx-translate/core';

export function getMockTranslateService(): TranslateService {
  return jasmine.createSpyObj('translateService', {
    get: jasmine.createSpy('get'),
    use: jasmine.createSpy('use'),
    instant: jasmine.createSpy('instant'),
    setDefaultLang: jasmine.createSpy('setDefaultLang')
  });
}
