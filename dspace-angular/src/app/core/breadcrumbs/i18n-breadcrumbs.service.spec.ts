import { TestBed, waitForAsync } from '@angular/core/testing';
import { Breadcrumb } from '../../breadcrumbs/breadcrumb/breadcrumb.model';
import { getTestScheduler } from 'jasmine-marbles';
import { BREADCRUMB_MESSAGE_POSTFIX, I18nBreadcrumbsService } from './i18n-breadcrumbs.service';

describe('I18nBreadcrumbsService', () => {
  let service: I18nBreadcrumbsService;
  let exampleString;
  let exampleURL;

  function init() {
    exampleString = 'example.string';
    exampleURL = 'example.com';
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({}).compileComponents();
  }));

  beforeEach(() => {
    service = new I18nBreadcrumbsService();
  });

  describe('getBreadcrumbs', () => {
    it('should return a breadcrumb based on a string by adding the postfix', () => {
      const breadcrumbs = service.getBreadcrumbs(exampleString, exampleURL);
      getTestScheduler().expectObservable(breadcrumbs).toBe('(a|)', { a: [new Breadcrumb(exampleString + BREADCRUMB_MESSAGE_POSTFIX, exampleURL)] });
    });
  });
});
