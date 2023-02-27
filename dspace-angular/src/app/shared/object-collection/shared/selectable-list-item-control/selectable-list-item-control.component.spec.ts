import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { SelectableListService } from '../../../object-list/selectable-list/selectable-list.service';
import { SelectableListItemControlComponent } from './selectable-list-item-control.component';
import { Item } from '../../../../core/shared/item.model';
import { FormsModule } from '@angular/forms';
import { VarDirective } from '../../../utils/var.directive';
import { of as observableOf } from 'rxjs';
import { ListableObject } from '../listable-object.model';

describe('SelectableListItemControlComponent', () => {
  let comp: SelectableListItemControlComponent;
  let fixture: ComponentFixture<SelectableListItemControlComponent>;
  let de: DebugElement;
  let el: HTMLElement;
  let object;
  let otherObject;
  let selectionConfig;
  let listId;
  let index;
  let selectionService;
  let selection: ListableObject[];
  let uuid1: string;
  let uuid2: string;

  function init() {
    uuid1 = '0beb44f8-d2ed-459a-a1e7-ffbe059089a9';
    uuid2 = 'e1dc80aa-c269-4aa5-b6bd-008d98056247';
    listId = 'Test List ID';
    object = Object.assign(new Item(), { uuid: uuid1 });
    otherObject = Object.assign(new Item(), { uuid: uuid2 });
    selectionConfig = { repeatable: false, listId };
    index = 0;
    selection = [otherObject];
    selectionService = jasmine.createSpyObj('selectionService', {
        selectSingle: jasmine.createSpy('selectSingle'),
        deselectSingle: jasmine.createSpy('deselectSingle'),
        isObjectSelected: observableOf(true),
        getSelectableList: observableOf({ selection })
      }
    );
  }

  beforeEach(waitForAsync(() => {
    init();
    TestBed.configureTestingModule({
      declarations: [SelectableListItemControlComponent, VarDirective],
      imports: [FormsModule],
      providers: [
        {
          provide: SelectableListService,
          useValue: selectionService
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SelectableListItemControlComponent);
    comp = fixture.componentInstance; // SelectableListItemControlComponent test instance
    de = fixture.debugElement;
    el = de.nativeElement;
    comp.object = object;
    comp.selectionConfig = selectionConfig;
    comp.index = index;
    fixture.detectChanges();
  });

  it('should call deselectSingle on the service when the object when selectCheckbox is called with value false', () => {
    comp.selectCheckbox(false);
    expect(selectionService.deselectSingle).toHaveBeenCalledWith(listId, object);
  });

  it('should call selectSingle on the service when the object when selectCheckbox is called with value false', () => {
    comp.selectCheckbox(true);
    expect(selectionService.selectSingle).toHaveBeenCalledWith(listId, object);
  });

  it('should call selectSingle on the service when the object when selectRadio is called with value true and deselect all others in the selection', () => {
    comp.selectRadio(true);
    expect(selectionService.deselectSingle).toHaveBeenCalledWith(listId, selection[0]);
    expect(selectionService.selectSingle).toHaveBeenCalledWith(listId, object);
  });

  it('should not call selectSingle on the service when the object when selectRadio is called with value false and not deselect all others in the selection', () => {
    comp.selectRadio(false);
    expect(selectionService.deselectSingle).not.toHaveBeenCalledWith(listId, selection[0]);
    expect(selectionService.selectSingle).not.toHaveBeenCalledWith(listId, object);
  });
});
