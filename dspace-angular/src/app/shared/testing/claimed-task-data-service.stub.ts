import { Observable, EMPTY } from 'rxjs';
import { ProcessTaskResponse } from '../../core/tasks/models/process-task-response';
import { ClaimedTask } from '../../core/tasks/models/claimed-task-object.model';
import { RemoteData } from '../../core/data/remote-data';

export class ClaimedTaskDataServiceStub {

  public submitTask(_scopeId: string, _body: any): Observable<ProcessTaskResponse> {
    return EMPTY;
  }

  public findByItem(_uuid: string): Observable<RemoteData<ClaimedTask>> {
    return EMPTY;
  }

}
