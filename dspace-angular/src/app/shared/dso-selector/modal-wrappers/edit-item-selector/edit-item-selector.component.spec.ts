import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { ActivatedRoute, Router } from '@angular/router';
import { EditItemSelectorComponent } from './edit-item-selector.component';
import { Item } from '../../../../core/shared/item.model';
import { RouterStub } from '../../../testing/router.stub';
import { MetadataValue } from '../../../../core/shared/metadata.models';
import { createSuccessfulRemoteDataObject } from '../../../remote-data.utils';

describe('EditItemSelectorComponent', () => {
  let component: EditItemSelectorComponent;
  let fixture: ComponentFixture<EditItemSelectorComponent>;
  let debugElement: DebugElement;

  const item = new Item();
  item.uuid = '1234-1234-1234-1234';
  item.metadata = { 'dc.title': [Object.assign(new MetadataValue(), { value: 'Item title', language: undefined })] };
  const router = new RouterStub();
  const itemRD = createSuccessfulRemoteDataObject(item);
  const modalStub = jasmine.createSpyObj('modalStub', ['close']);
  const editPath = '/items/1234-1234-1234-1234/edit';

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [EditItemSelectorComponent],
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
          },
        },
        {
          provide: Router, useValue: router
        }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(EditItemSelectorComponent);
    component = fixture.componentInstance;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should call navigate on the router with the correct edit path when navigate is called', () => {
    component.navigate(item);
    expect(router.navigate).toHaveBeenCalledWith([editPath]);
  });

});
