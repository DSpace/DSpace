import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { Component, DebugElement, NO_ERRORS_SCHEMA, OnInit } from '@angular/core';
import { DSpaceObjectType } from '../../../core/shared/dspace-object-type.model';
import { Item } from '../../../core/shared/item.model';
import { DSOSelectorModalWrapperComponent, SelectorActionType } from './dso-selector-modal-wrapper.component';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ActivatedRoute } from '@angular/router';
import { DSpaceObject } from '../../../core/shared/dspace-object.model';
import { By } from '@angular/platform-browser';
import { DSOSelectorComponent } from '../dso-selector/dso-selector.component';
import { MockComponent } from 'ng-mocks';
import { MetadataValue } from '../../../core/shared/metadata.models';
import { createSuccessfulRemoteDataObject } from '../../remote-data.utils';

describe('DSOSelectorModalWrapperComponent', () => {
  let component: DSOSelectorModalWrapperComponent;
  let fixture: ComponentFixture<DSOSelectorModalWrapperComponent>;
  let debugElement: DebugElement;

  const item = new Item();
  item.uuid = '1234-1234-1234-1234';
  item.metadata = {
    'dc.title': [Object.assign(new MetadataValue(), {
      value: 'Item title',
      language: undefined
    })]
  };

  const itemRD = createSuccessfulRemoteDataObject(item);
  const modalStub = jasmine.createSpyObj('modalStub', ['close']);

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [TestComponent, MockComponent(DSOSelectorComponent)],
      providers: [
        { provide: NgbActiveModal, useValue: modalStub },
        {
          provide: ActivatedRoute,
          useValue: {
            root: {
              snapshot: {
                data: {
                  dso: itemRD,
                },
              },
            }
          }
        },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(TestComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
    component.ngOnInit();
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should initially set the DSO to the activated route\'s item/collection/community', () => {
    expect(component.dsoRD).toEqual(itemRD);
  });

  describe('selectObject', () => {
    beforeEach(() => {
      spyOn(component, 'navigate');
      spyOn(component, 'close');
      component.selectObject(item);
    });
    it('should call the close and navigate method on the component with the given DSO', () => {
      expect(component.close).toHaveBeenCalled();
      expect(component.navigate).toHaveBeenCalledWith(item);
    });
  });

  describe('close', () => {
    beforeEach(() => {
      component.close();
    });
    it('should call the close method on the æctive modal', () => {
      expect(modalStub.close).toHaveBeenCalled();
    });
  });

  describe('when the onSelect method emits on the child component', () => {
    beforeEach(() => {
      spyOn(component, 'selectObject');
      debugElement.query(By.css('ds-dso-selector')).componentInstance.onSelect.emit(item);
      fixture.detectChanges();
    });
    it('should call the selectObject method on the component with the correct object', () => {
      expect(component.selectObject).toHaveBeenCalledWith(item);
    });
  });

  describe('when the click method emits on close button', () => {
    beforeEach(() => {
      spyOn(component, 'close');
      debugElement.query(By.css('button.close')).triggerEventHandler('click', {});
      fixture.detectChanges();
    });
    it('should call the close method on the component', () => {
      expect(component.close).toHaveBeenCalled();
    });
  });
});

@Component({
  selector: 'ds-test-cmp',
  templateUrl: './dso-selector-modal-wrapper.component.html'
})
class TestComponent extends DSOSelectorModalWrapperComponent implements OnInit {
  objectType = DSpaceObjectType.ITEM;
  selectorTypes = [DSpaceObjectType.ITEM];
  action = SelectorActionType.EDIT;

  constructor(protected activeModal: NgbActiveModal, protected route: ActivatedRoute) {
    super(activeModal, route);
  }

  navigate(dso: DSpaceObject) {
    /* comment */
  }
}
