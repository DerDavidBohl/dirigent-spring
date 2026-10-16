export type StateTone = 'running' | 'transitional' | 'stopped' | 'failed';

// UX-008: deployment states collapse into four tones; REQ-002 defines the vocabulary.
export const DEPLOYMENT_STATES = ['RUNNING', 'STARTING', 'STOPPING', 'UPDATED', 'STOPPED', 'REMOVED', 'UNKNOWN', 'FAILED'] as const;

export function stateTone(state: string): StateTone {
  switch (state) {
    case 'RUNNING':
      return 'running';
    case 'STARTING':
    case 'STOPPING':
    case 'UPDATED':
      return 'transitional';
    case 'FAILED':
      return 'failed';
    default:
      return 'stopped';
  }
}
