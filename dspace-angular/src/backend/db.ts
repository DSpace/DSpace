// Our API for demos only
export const fakeDataBase = {
  get() {
    const res = { data: 'This fake data came from the db on the server.' };
    return Promise.resolve(res);
  }
};
