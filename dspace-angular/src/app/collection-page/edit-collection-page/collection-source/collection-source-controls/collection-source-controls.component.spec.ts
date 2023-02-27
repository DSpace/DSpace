import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { ContentSource } from '../../../../core/shared/content-source.model';
import { Collection } from '../../../../core/shared/collection.model';
import { createSuccessfulRemoteDataObject$ } from '../../../../shared/remote-data.utils';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NotificationsService } from '../../../../shared/notifications/notifications.service';
import { CollectionDataService } from '../../../../core/data/collection-data.service';
import { RequestService } from '../../../../core/data/request.service';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ProcessDataService } from '../../../../core/data/processes/process-data.service';
import { ScriptDataService } from '../../../../core/data/processes/script-data.service';
import { HttpClient } from '@angular/common/http';
import { BitstreamDataService } from '../../../../core/data/bitstream-data.service';
import { NotificationsServiceStub } from '../../../../shared/testing/notifications-service.stub';
import { Process } from '../../../../process-page/processes/process.model';
import { of as observableOf } from 'rxjs';
import { CollectionSourceControlsComponent } from './collection-source-controls.component';
import { Bitstream } from '../../../../core/shared/bitstream.model';
import { getTestScheduler } from 'jasmine-marbles';
import { TestScheduler } from 'rxjs/testing';
import { By } from '@angular/platform-browser';
import { VarDirective } from '../../../../shared/utils/var.directive';
import { ContentSourceSetSerializer } from '../../../../core/shared/content-source-set-serializer';

describe('CollectionSourceControlsComponent', () => {
  let comp: CollectionSourceControlsComponent;
  let fixture: ComponentFixture<CollectionSourceControlsComponent>;

  const uuid = '29481ed7-ae6b-409a-8c51-34dd347a0ce4';
  let contentSource: ContentSource;
  let collection: Collection;
  let process: Process;
  let bitstream: Bitstream;

  let scriptDataService: ScriptDataService;
  let processDataService: ProcessDataService;
  let requestService: RequestService;
  let notificationsService;
  let collectionService: CollectionDataService;
  let httpClient: HttpClient;
  let bitstreamService: BitstreamDataService;
  let scheduler: TestScheduler;


  beforeEach(waitForAsync(() => {
    scheduler = getTestScheduler();
    contentSource = Object.assign(new ContentSource(), {
      uuid: uuid,
      metadataConfigs: [
        {
          id: 'dc',
          label: 'Simple Dublin Core',
          nameSpace: 'http://www.openarchives.org/OAI/2.0/oai_dc/'
        },
        {
          id: 'qdc',
          label: 'Qualified Dublin Core',
          nameSpace: 'http://purl.org/dc/terms/'
        },
        {
          id: 'dim',
          label: 'DSpace Intermediate Metadata',
          nameSpace: 'http://www.dspace.org/xmlns/dspace/dim'
        }
      ],
      oaiSource: 'oai-harvest-source',
      oaiSetId: 'oai-set-id',
      _links: {self: {href: 'contentsource-selflink'}}
    });
    process = Object.assign(new Process(), {
      processId: 'process-id', processStatus: 'COMPLETED',
      _links: {output: {href: 'output-href'}}
    });

    bitstream = Object.assign(new Bitstream(), {_links: {content: {href: 'content-href'}}});

    collection = Object.assign(new Collection(), {
      uuid: 'fake-collection-id',
      _links: {self: {href: 'collection-selflink'}}
    });
    notificationsService = new NotificationsServiceStub();
    collectionService = jasmine.createSpyObj('collectionService', {
      getContentSource: createSuccessfulRemoteDataObject$(contentSource),
      findByHref: createSuccessfulRemoteDataObject$(collection)
    });
    scriptDataService = jasmine.createSpyObj('scriptDataService', {
      invoke: createSuccessfulRemoteDataObject$(process),
    });
    processDataService = jasmine.createSpyObj('processDataService', {
      findById: createSuccessfulRemoteDataObject$(process),
    });
    bitstreamService = jasmine.createSpyObj('bitstreamService', {
      findByHref: createSuccessfulRemoteDataObject$(bitstream),
    });
    httpClient = jasmine.createSpyObj('httpClient', {
      get: observableOf('Script text'),
    });
    requestService = jasmine.createSpyObj('requestService', ['removeByHrefSubstring', 'setStaleByHrefSubstring']);

    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot(), RouterTestingModule],
      declarations: [CollectionSourceControlsComponent, VarDirective],
      providers: [
        {provide: ScriptDataService, useValue: scriptDataService},
        {provide: ProcessDataService, useValue: processDataService},
        {provide: RequestService, useValue: requestService},
        {provide: NotificationsService, useValue: notificationsService},
        {provide: CollectionDataService, useValue: collectionService},
        {provide: HttpClient, useValue: httpClient},
        {provide: BitstreamDataService, useValue: bitstreamService}
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));
  beforeEach(() => {
    fixture = TestBed.createComponent(CollectionSourceControlsComponent);
    comp = fixture.componentInstance;
    comp.isEnabled = true;
    comp.collection = collection;
    comp.shouldShow = true;
    fixture.detectChanges();
  });
  describe('init', () => {
    it('should', () => {
      expect(comp).toBeTruthy();
    });
  });
  describe('testConfiguration', () => {
    it('should invoke a script and ping the resulting process until completed and show the resulting info', () => {
      comp.testConfiguration(contentSource);
      scheduler.flush();

      expect(scriptDataService.invoke).toHaveBeenCalledWith('harvest', [
        {name: '-g', value: null},
        {name: '-a', value: contentSource.oaiSource},
        {name: '-i', value: new ContentSourceSetSerializer().Serialize(contentSource.oaiSetId)},
      ], []);

      expect(processDataService.findById).toHaveBeenCalledWith(process.processId, false);
      expect(bitstreamService.findByHref).toHaveBeenCalledWith(process._links.output.href);
      expect(notificationsService.info).toHaveBeenCalledWith(jasmine.anything() as any, 'Script text');
    });
  });
  describe('importNow', () => {
    it('should invoke a script that will start the harvest', () => {
      comp.importNow();
      scheduler.flush();

      expect(scriptDataService.invoke).toHaveBeenCalledWith('harvest', [
        {name: '-r', value: null},
        {name: '-c', value: collection.uuid},
      ], []);
      expect(processDataService.findById).toHaveBeenCalledWith(process.processId, false);
      expect(notificationsService.success).toHaveBeenCalled();
    });
  });
  describe('resetAndReimport', () => {
    it('should invoke a script that will start the harvest', () => {
      comp.resetAndReimport();
      scheduler.flush();

      expect(scriptDataService.invoke).toHaveBeenCalledWith('harvest', [
        {name: '-o', value: null},
        {name: '-c', value: collection.uuid},
      ], []);
      expect(processDataService.findById).toHaveBeenCalledWith(process.processId, false);
      expect(notificationsService.success).toHaveBeenCalled();
    });
  });
  describe('the controls', () => {
    it('should be shown when shouldShow is true', () => {
      comp.shouldShow = true;
      fixture.detectChanges();
      const buttons = fixture.debugElement.queryAll(By.css('button'));
      expect(buttons.length).toEqual(3);
    });
    it('should be shown when shouldShow is false', () => {
      comp.shouldShow = false;
      fixture.detectChanges();
      const buttons = fixture.debugElement.queryAll(By.css('button'));
      expect(buttons.length).toEqual(0);
    });
    it('should be disabled when isEnabled is false', () => {
      comp.shouldShow = true;
      comp.isEnabled = false;

      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('button'));

      expect(buttons[0].nativeElement.disabled).toBeTrue();
      expect(buttons[1].nativeElement.disabled).toBeTrue();
      expect(buttons[2].nativeElement.disabled).toBeTrue();
    });
    it('should be enabled when isEnabled is true', () => {
      comp.shouldShow = true;
      comp.isEnabled = true;

      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('button'));

      expect(buttons[0].nativeElement.disabled).toBeFalse();
      expect(buttons[1].nativeElement.disabled).toBeFalse();
      expect(buttons[2].nativeElement.disabled).toBeFalse();
    });
    it('should call the corresponding button when clicked', () => {
      spyOn(comp, 'testConfiguration');
      spyOn(comp, 'importNow');
      spyOn(comp, 'resetAndReimport');

      comp.shouldShow = true;
      comp.isEnabled = true;

      fixture.detectChanges();

      const buttons = fixture.debugElement.queryAll(By.css('button'));

      buttons[0].triggerEventHandler('click', null);
      expect(comp.testConfiguration).toHaveBeenCalled();

      buttons[1].triggerEventHandler('click', null);
      expect(comp.importNow).toHaveBeenCalled();

      buttons[2].triggerEventHandler('click', null);
      expect(comp.resetAndReimport).toHaveBeenCalled();
    });
  });


});
