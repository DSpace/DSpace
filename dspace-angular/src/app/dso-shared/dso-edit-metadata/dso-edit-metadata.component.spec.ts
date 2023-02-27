import { DsoEditMetadataComponent } from './dso-edit-metadata.component';
import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { VarDirective } from '../../shared/utils/var.directive';
import { TranslateModule } from '@ngx-translate/core';
import { RouterTestingModule } from '@angular/router/testing';
import { DebugElement, Injectable, NO_ERRORS_SCHEMA } from '@angular/core';
import { DSpaceObject } from '../../core/shared/dspace-object.model';
import { Item } from '../../core/shared/item.model';
import { MetadataValue } from '../../core/shared/metadata.models';
import { createSuccessfulRemoteDataObject$ } from '../../shared/remote-data.utils';
import { By } from '@angular/platform-browser';
import { NotificationsService } from '../../shared/notifications/notifications.service';
import { ArrayMoveChangeAnalyzer } from '../../core/data/array-move-change-analyzer.service';
import { ITEM } from '../../core/shared/item.resource-type';
import { DATA_SERVICE_FACTORY } from '../../core/data/base/data-service.decorator';
import { Operation } from 'fast-json-patch';
import { RemoteData } from '../../core/data/remote-data';
import { Observable } from 'rxjs/internal/Observable';

const ADD_BTN = 'add';
const REINSTATE_BTN = 'reinstate';
const SAVE_BTN = 'save';
const DISCARD_BTN = 'discard';

@Injectable()
class TestDataService {
  patch(object: Item, operations: Operation[]): Observable<RemoteData<Item>> {
    return createSuccessfulRemoteDataObject$(object);
  }
}

describe('DsoEditMetadataComponent', () => {
  let component: DsoEditMetadataComponent;
  let fixture: ComponentFixture<DsoEditMetadataComponent>;

  let notificationsService: NotificationsService;

  let dso: DSpaceObject;

  beforeEach(waitForAsync(() => {
    dso = Object.assign(new Item(), {
      type: ITEM,
      metadata: {
        'dc.title': [
          Object.assign(new MetadataValue(), {
            value: 'Test Title',
            language: 'en',
            place: 0,
          }),
        ],
        'dc.subject': [
          Object.assign(new MetadataValue(), {
            value: 'Subject One',
            language: 'en',
            place: 0,
          }),
          Object.assign(new MetadataValue(), {
            value: 'Subject Two',
            language: 'en',
            place: 1,
          }),
          Object.assign(new MetadataValue(), {
            value: 'Subject Three',
            language: 'en',
            place: 2,
          }),
        ],
      },
    });

    notificationsService = jasmine.createSpyObj('notificationsService', ['error', 'success']);

    TestBed.configureTestingModule({
      declarations: [DsoEditMetadataComponent, VarDirective],
      imports: [TranslateModule.forRoot(), RouterTestingModule.withRoutes([])],
      providers: [
        TestDataService,
        { provide: DATA_SERVICE_FACTORY, useValue: jasmine.createSpy('getDataServiceFor').and.returnValue(TestDataService) },
        { provide: NotificationsService, useValue: notificationsService },
        ArrayMoveChangeAnalyzer,
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(DsoEditMetadataComponent);
    component = fixture.componentInstance;
    component.dso = dso;
    fixture.detectChanges();
  });

  describe('when no changes have been made', () => {
    assertButton(ADD_BTN, true, false);
    assertButton(REINSTATE_BTN, false);
    assertButton(SAVE_BTN, true, true);
    assertButton(DISCARD_BTN, true, true);
  });

  describe('when the form contains changes', () => {
    beforeEach(() => {
      component.form.fields['dc.title'][0].newValue.value = 'Updated Title Once';
      component.form.fields['dc.title'][0].confirmChanges();
      component.form.resetReinstatable();
      component.onValueSaved();
      fixture.detectChanges();
    });

    assertButton(SAVE_BTN, true, false);
    assertButton(DISCARD_BTN, true, false);

    describe('and they were discarded', () => {
      beforeEach(() => {
        component.discard();
        fixture.detectChanges();
      });

      assertButton(REINSTATE_BTN, true, false);
      assertButton(SAVE_BTN, true, true);
      assertButton(DISCARD_BTN, false);

      describe('and a new change is made', () => {
        beforeEach(() => {
          component.form.fields['dc.title'][0].newValue.value = 'Updated Title Twice';
          component.form.fields['dc.title'][0].confirmChanges();
          component.form.resetReinstatable();
          component.onValueSaved();
          fixture.detectChanges();
        });

        assertButton(REINSTATE_BTN, false);
        assertButton(SAVE_BTN, true, false);
        assertButton(DISCARD_BTN, true, false);
      });
    });
  });

  describe('when a new value is present', () => {
    beforeEach(() => {
      component.add();
      fixture.detectChanges();
    });

    assertButton(ADD_BTN, true, true);

    it('should display a row with a field selector and metadata value', () => {
      expect(fixture.debugElement.query(By.css('ds-metadata-field-selector'))).toBeTruthy();
      expect(fixture.debugElement.query(By.css('ds-dso-edit-metadata-value'))).toBeTruthy();
    });

    describe('and gets assigned to a metadata field', () => {
      beforeEach(() => {
        component.form.newValue.newValue.value = 'New Subject';
        component.form.setMetadataField('dc.subject');
        component.form.resetReinstatable();
        component.onValueSaved();
        fixture.detectChanges();
      });

      assertButton(ADD_BTN, true, false);

      it('should not display the separate row with field selector and metadata value anymore', () => {
        expect(fixture.debugElement.query(By.css('ds-metadata-field-selector'))).toBeNull();
        expect(fixture.debugElement.query(By.css('ds-dso-edit-metadata-value'))).toBeNull();
      });
    });
  });

  function assertButton(name: string, exists: boolean, disabled: boolean = false): void {
    describe(`${name} button`, () => {
      let btn: DebugElement;

      beforeEach(() => {
        btn = fixture.debugElement.query(By.css(`#dso-${name}-btn`));
      });

      if (exists) {
        it('should exist', () => {
          expect(btn).toBeTruthy();
        });

        it(`should${disabled ? ' ' : ' not '}be disabled`, () => {
          expect(btn.nativeElement.disabled).toBe(disabled);
        });
      } else {
        it('should not exist', () => {
          expect(btn).toBeNull();
        });
      }
    });
  }

});
