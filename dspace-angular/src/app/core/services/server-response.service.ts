import { RESPONSE } from '@nguniversal/express-engine/tokens';
import { Inject, Injectable, Optional } from '@angular/core';
import { Response } from 'express';

@Injectable()
export class ServerResponseService {
  private response: Response;

  constructor(@Optional() @Inject(RESPONSE) response: any) {
    this.response = response;
  }

  setStatus(code: number, message?: string): this {
    if (this.response) {
      this.response.statusCode = code;
      if (message) {
        this.response.statusMessage = message;
      }
    }
    return this;
  }

  setUnauthorized(message = 'Unauthorized'): this {
    return this.setStatus(401, message);
  }

  setForbidden(message = 'Forbidden'): this {
    return this.setStatus(403, message);
  }

  setNotFound(message = 'Not found'): this {
    return this.setStatus(404, message);
  }

  setInternalServerError(message = 'Internal Server Error'): this {
    return this.setStatus(500, message);
  }
}
