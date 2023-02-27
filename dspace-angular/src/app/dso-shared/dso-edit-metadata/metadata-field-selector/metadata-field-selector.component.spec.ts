import { MetadataFieldSelectorComponent } from './metadata-field-selector.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../../shared/utils/var.directive';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { RegistryService } from '../../../core/registry/registry.service';
import { MetadataField } from '../../../core/metadata/metadata-field.model';
import { MetadataSchema } from '../../../core/metadata/metadata-schema.model';
import { createFailedRemoteDataObject$, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { createPaginatedList } from '../../../shared/testing/utils.test';
import { followLink } from '../../../shared/utils/follow-link-config.model';
import { By } from '@angular/platform-browser';
import { NotificationsService } from '../../../shared/notifications/notifications.service';

describe('MetadataFieldSelectorComponent', () => {
  let component: MetadataFieldSelectorComponent;
  let fixture: ComponentFixture<MetadataFieldSelectorComponent>;

  let registryService: RegistryService;
  let notificationsService: NotificationsService;

  let metadataSchema: MetadataSchema;
  let metadataFields: MetadataField[];

  beforeEach(waitForAsync(() => {
    metadataSchema = Object.assign(new MetadataSchema(), {
      id: 0,
      prefix: 'dc',
      namespace: 'http://dublincore.org/documents/dcmi-terms/',
    });
    metadataFields = [
      Object.assign(new MetadataField(), {
        id: 0,
        element: 'description',
        qualifier: undefined,
        schema: createSuccessfulRemoteDataObject$(metadataSchema),
      }),
      Object.assign(new MetadataField(), {
        id: 1,
        element: 'description',
        qualifier: 'abstract',
        schema: createSuccessfulRemoteDataObject$(metadataSchema),
      }),
    ];

    registryService = jasmine.createSpyObj('registryService', {
      queryMetadataFields: createSuccessfulRemoteDataObject$(createPaginatedList(metadataFields)),
    });
    notificationsService = jasmine.createSpyObj('notificationsService', ['error', 'success']);

    TestBed.configureTestingModule({
      declarations: [MetadataFieldSelectorComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
        { provide: RegistryService, useValue: registryService },
        { provide: NotificationsService, useValue: notificationsService },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(MetadataFieldSelectorComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  describe('when a query is entered', () => {
    const query = 'test query';

    beforeEach(() => {
      component.showInvalid = true;
      component.query$.next(query);
    });

    it('should reset showInvalid', () => {
      expect(component.showInvalid).toBeFalse();
    });

    it('should query the registry service for metadata fields and include the schema', () => {
      expect(registryService.queryMetadataFields).toHaveBeenCalledWith(query, null, true, false, followLink('schema'));
    });
  });

  describe('validate', () => {
    it('should return an observable true and show no feedback if the current mdField exists in registry', (done) => {
      component.mdField = 'dc.description.abstract';
      component.validate().subscribe((result) => {
        expect(result).toBeTrue();
        fixture.detectChanges();
        expect(fixture.debugElement.query(By.css('.invalid-feedback'))).toBeNull();
        done();
      });
    });

    it('should return an observable false and show invalid feedback if the current mdField is missing in registry', (done) => {
      component.mdField = 'dc.fake.field';
      component.validate().subscribe((result) => {
        expect(result).toBeFalse();
        fixture.detectChanges();
        expect(fixture.debugElement.query(By.css('.invalid-feedback'))).toBeTruthy();
        done();
      });
    });

    describe('when querying the metadata fields returns an error response', () => {
      beforeEach(() => {
        (registryService.queryMetadataFields as jasmine.Spy).and.returnValue(createFailedRemoteDataObject$('Failed'));
      });

      it('should return an observable false and show a notification', (done) => {
        component.mdField = 'dc.description.abstract';
        component.validate().subscribe((result) => {
          expect(result).toBeFalse();
          expect(notificationsService.error).toHaveBeenCalled();
          done();
        });
      });
    });
  });
});
