export class RequestError extends Error {
  statusCode: number;
  statusText: string;
}
