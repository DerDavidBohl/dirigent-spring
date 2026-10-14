import {stateTone} from './state-tone';

describe('stateTone', () => {
  it('maps the state vocabulary to four tones', () => {
    expect(stateTone('RUNNING')).toBe('running');
    expect(stateTone('STARTING')).toBe('transitional');
    expect(stateTone('STOPPING')).toBe('transitional');
    expect(stateTone('UPDATED')).toBe('transitional');
    expect(stateTone('STOPPED')).toBe('stopped');
    expect(stateTone('REMOVED')).toBe('stopped');
    expect(stateTone('UNKNOWN')).toBe('stopped');
    expect(stateTone('FAILED')).toBe('failed');
  });

  it('treats unexpected values as stopped', () => {
    expect(stateTone('SOMETHING_NEW')).toBe('stopped');
  });
});
