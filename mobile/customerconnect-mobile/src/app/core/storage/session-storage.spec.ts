import { WebSessionStorage } from './session-storage';

describe('WebSessionStorage', () => {
  it('keeps mobile persistence behind the storage adapter', () => {
    const storage = new WebSessionStorage();
    storage.write('session', { accessToken: 'token' });
    expect(storage.read<{ accessToken: string }>('session')?.accessToken).toBe('token');
    storage.remove('session');
    expect(storage.read('session')).toBeNull();
  });
});
