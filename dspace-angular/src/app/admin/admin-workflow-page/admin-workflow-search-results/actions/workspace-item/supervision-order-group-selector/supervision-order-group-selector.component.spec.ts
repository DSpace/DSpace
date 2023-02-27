import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { NgbActiveModal } from '@ng-bootstrap/ng-bootstrap';
import { SupervisionOrderGroupSelectorComponent } from './supervision-order-group-selector.component';
import { SupervisionOrderDataService } from '../../../../../../core/supervision-order/supervision-order-data.service';
import { NotificationsService } from '../../../../../../shared/notifications/notifications.service';
import { Group } from '../../../../../../core/eperson/models/group.model';
import { SupervisionOrder } from '../../../../../../core/supervision-order/models/supervision-order.model';
import { of } from 'rxjs';

describe('SupervisionOrderGroupSelectorComponent', () => {
  let component: SupervisionOrderGroupSelectorComponent;
  let fixture: ComponentFixture<SupervisionOrderGroupSelectorComponent>;
  let debugElement: DebugElement;

  const modalStub = jasmine.createSpyObj('modalStub', ['close']);

  const supervisionOrderDataService: any = jasmine.createSpyObj('supervisionOrderDataService', {
    create: of(new SupervisionOrder())
  });

  const selectedOrderType = 'NONE';
  const itemUUID = 'itemUUID1234';

  const selectedGroup = new Group();
  selectedGroup.uuid = 'GroupUUID1234';

  const supervisionDataObject = new SupervisionOrder();
  supervisionDataObject.ordertype = selectedOrderType;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      imports: [TranslateModule.forRoot()],
      declarations: [SupervisionOrderGroupSelectorComponent],
      providers: [
        { provide: NgbActiveModal, useValue: modalStub },
        { provide: SupervisionOrderDataService, useValue: supervisionOrderDataService },
        { provide: NotificationsService, useValue: {} },
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

  }));

  beforeEach(waitForAsync(() => {
    fixture = TestBed.createComponent(SupervisionOrderGroupSelectorComponent);
    component = fixture.componentInstance;

  }));

  beforeEach(() => {
    component.itemUUID = itemUUID;
    component.selectedGroup = selectedGroup;
    component.selectedOrderType = selectedOrderType;
    debugElement = fixture.debugElement;
    fixture.detectChanges();
  });

  it('should create component', () => {
    expect(component).toBeTruthy();
  });

  it('should call create for supervision order', () => {
    component.save();
    fixture.detectChanges();
    expect(supervisionOrderDataService.create).toHaveBeenCalledWith(supervisionDataObject, itemUUID, selectedGroup.uuid, selectedOrderType);
  });

});
