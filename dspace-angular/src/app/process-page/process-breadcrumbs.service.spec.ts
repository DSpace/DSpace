import { waitForAsync, TestBed } from '@angular/core/testing';
import { getTestScheduler } from 'jasmine-marbles';
import { ProcessBreadcrumbsService } from './process-breadcrumbs.service';
import { Breadcrumb } from '../breadcrumbs/breadcrumb/breadcrumb.model';
import { Process } from './processes/process.model';

describe('ProcessBreadcrumbsService', () => {
  let service: ProcessBreadcrumbsService;
  let exampleId;
  let exampleScriptName;
  let exampleProcess;
  let exampleURL;

  function init() {
    exampleId = '12345';
    exampleScriptName = 'Example Script';
    exampleProcess = Object.assign(new Process(), {processId: exampleId, scriptName: exampleScriptName});
    exampleURL = 'example.com';
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({}).compileComponents();
  }));

  beforeEach(() => {
    service = new ProcessBreadcrumbsService();
  });

  describe('getBreadcrumbs', () => {
    it('should return a breadcrumb based on a id and scriptName of the process', () => {
      const breadcrumbs = service.getBreadcrumbs(exampleProcess, exampleURL);
      getTestScheduler().expectObservable(breadcrumbs).toBe('(a|)', { a: [new Breadcrumb(exampleId + ' - ' + exampleScriptName, exampleURL)] });
    });
  });
});
