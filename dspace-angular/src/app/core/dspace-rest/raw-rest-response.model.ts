import { HttpHeaders } from '@angular/common/http';

export interface RawRestResponse {
  payload: {
    [name: string]: any;
    _embedded?: any;
    _links?: any;
    page?: any;
  };
  headers?: HttpHeaders;
  statusCode: number;
  statusText: string;
}
