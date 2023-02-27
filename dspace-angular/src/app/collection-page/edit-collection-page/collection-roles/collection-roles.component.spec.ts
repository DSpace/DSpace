import { ComponentFixture, TestBed } from '@angular/core/testing';
import { TranslateModule } from '@ngx-translate/core';
import { ActivatedRoute } from '@angular/router';
import { of as observableOf } from 'rxjs';
import { DebugElement, NO_ERRORS_SCHEMA } from '@angular/core';
import { By } from '@angular/platform-browser';
import { CollectionRolesComponent } from './collection-roles.component';
import { Collection } from '../../../core/shared/collection.model';
import { SharedModule } from '../../../shared/shared.module';
import { GroupDataService } from '../../../core/eperson/group-data.service';
import { RequestService } from '../../../core/data/request.service';
import { RouterTestingModule } from '@angular/router/testing';
import { createSuccessfulRemoteDataObject, createSuccessfulRemoteDataObject$ } from '../../../shared/remote-data.utils';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { ComcolModule } from '../../../shared/comcol/comcol.module';
import { NotificationsService } from '../../../shared/notifications/notifications.service';
import { NotificationsServiceStub } from '../../../shared/testing/notifications-service.stub';

describe('CollectionRolesComponent', () => {

  let fixture: ComponentFixture<CollectionRolesComponent>;
  let comp: CollectionRolesComponent;
  let de: DebugElement;

  beforeEach(() => {

    const route = {
      parent: {
        data: observableOf({
          dso: createSuccessfulRemoteDataObject(
            Object.assign(new Collection(), {
              _links: {
                irrelevant: {
                  href: 'irrelevant link',
                },
                adminGroup: {
                  href: 'adminGroup link',
                },
                submittersGroup: {
                  href: 'submittersGroup link',
                },
                itemReadGroup: {
                  href: 'itemReadGroup link',
                },
                bitstreamReadGroup: {
                  href: 'bitstreamReadGroup link',
                },
                workflowGroups: [
                  {
                    name: 'test',
                    href: 'test workflow group link',
                  },
                ],
              },
            })
          ),
        })
      }
    };

    const requestService = {
      hasByHref$: () => observableOf(true),
    };

    const groupDataService = {
      findByHref: () => createSuccessfulRemoteDataObject$({}),
    };

    TestBed.configureTestingModule({
      imports: [
        ComcolModule,
        SharedModule,
        RouterTestingModule.withRoutes([]),
        TranslateModule.forRoot(),
        NoopAnimationsModule
      ],
      declarations: [
        CollectionRolesComponent,
      ],
      providers: [
        { provide: ActivatedRoute, useValue: route },
        { provide: RequestService, useValue: requestService },
        { provide: GroupDataService, useValue: groupDataService },
        { provide: NotificationsService, useClass: NotificationsServiceStub }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(CollectionRolesComponent);
    comp = fixture.componentInstance;
    de = fixture.debugElement;

    fixture.detectChanges();
  });

  it('should display a collection admin role component', (done) => {
    expect(de.query(By.css('ds-comcol-role .collection-admin')))
      .toBeTruthy();
    done();
  });

  it('should display a submitters role component', (done) => {
    expect(de.query(By.css('ds-comcol-role .submitters')))
      .toBeTruthy();
    done();
  });

  it('should display a default item read role component', (done) => {
    expect(de.query(By.css('ds-comcol-role .item_read')))
      .toBeTruthy();
    done();
  });

  it('should display a default bitstream read role component', (done) => {
    expect(de.query(By.css('ds-comcol-role .bitstream_read')))
      .toBeTruthy();
    done();
  });

  it('should display a test workflow role component', (done) => {
    expect(de.query(By.css('ds-comcol-role .test')))
      .toBeTruthy();
    done();
  });
});
