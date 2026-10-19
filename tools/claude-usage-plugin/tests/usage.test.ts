import { expect, test } from 'claude-code/testing';

const NOW = Date.parse('2026-10-19T12:00:00Z');
const RESET = '2026-10-19T14:00:00Z';
const STEP = { turnId: 'private-id', index: 0, model: 'private-model', messageCount: 1 };
const RESPONSE = { turnId: 'private-id', index: 0, answer: 'unchanged', toolUses: [], stopReason: 'tool_use', usage: { input_tokens: 1, output_tokens: 1, cache_read_input_tokens: 0, cache_creation_input_tokens: 0, model: 'private-model' } };
async function finish($, input = STEP) {
  const stream = $.turn.step(input);
  let item;
  do { item = await stream.next(); } while (!item.done);
  return item.value;
}
const CONFIG = { python: '/python', collector: '/private/coding-usage.py', stateDir: '/private/state' };

test('forwards only validated short and weekly quota fields', async ($, on) => {
  let received;
  on('session.usage', () => ({ value: { context: { tokens: 999 }, cost: { secret: 'never-forward' }, rateLimits: [
    { kind: 'five_hour', percentUsed: 0, resetsAt: RESET, secret: 'never-forward' },
    { kind: 'seven_day', percentUsed: 31.5, resetsAt: RESET },
    { kind: 'spend_limit', percentUsed: 10, resetsAt: RESET },
  ] } }));
  on('clock.now', () => ({ value: NOW }));
  on('fs.read', ($, e) => {
    expect(e.path.endsWith('/claude-usage-plugin/runtime.json')).toBe(true);
    return { value: JSON.stringify(CONFIG) };
  });
  on('process.run', ($, e) => { received = e; return { value: { exitCode: 0, stdout: '', stderr: '' } }; });
  on('turn.step', async function* () { return RESPONSE; });
  expect((await finish($)).answer).toBe('unchanged');
  expect(received.argv).toEqual(['/python', '/private/coding-usage.py', 'claude-mod', '--state-dir', '/private/state']);
  expect(received.init.timeoutMs).toBe(2000);
  expect(JSON.parse(received.init.stdin)).toEqual({ rate_limits: {
    five_hour: { used_percentage: 0, resets_at: Date.parse(RESET) / 1000 },
    seven_day: { used_percentage: 31.5, resets_at: Date.parse(RESET) / 1000 },
  } });
  expect(received.init.stdin.includes('private')).toBe(false);
  expect(received.init.stdin.includes('tokens')).toBe(false);
  expect(received.init.stdin.includes('secret')).toBe(false);
});

test('missing, null, invalid and expired quotas leave saved observations alone', async ($, on) => {
  on('fs.read', () => ({ value: JSON.stringify(CONFIG) }));
  let limits: unknown = undefined;
  let writes = 0;
  on('session.usage', () => ({ value: { rateLimits: limits } }));
  on('clock.now', () => ({ value: NOW }));
  on('process.run', () => { writes++; return { value: { exitCode: 0, stdout: '', stderr: '' } }; });
  on('turn.step', async function* () { return RESPONSE; });
  for (const value of [undefined, null, {}, [], [null],
    [{ kind: 'five_hour', percentUsed: null, resetsAt: RESET }],
    ...[undefined, true, '10', -1, 101, NaN, Infinity].map(percentUsed => [{ kind: 'five_hour', percentUsed, resetsAt: RESET }]),
    ...[undefined, null, 'invalid', '2026-10-19T11:59:59Z', '2026-10-19T12:00:00.500Z', '2030-01-01T00:00:00Z'].map(resetsAt => [{ kind: 'five_hour', percentUsed: 10, resetsAt }]),
  ]) {
    limits = value;
    await finish($);
  }
  expect(writes).toBe(0);
});

test('partial quotas work and helper failures never change a turn result', async ($, on) => {
  on('session.usage', () => ({ value: { rateLimits: [{ kind: 'seven_day', percentUsed: 100, resetsAt: RESET }] } }));
  on('clock.now', () => ({ value: NOW }));
  on('fs.read', () => ({ value: JSON.stringify(CONFIG) }));
  let calls = 0;
  on('process.run', ($, e) => {
    expect(Object.keys(JSON.parse(e.init.stdin).rate_limits)).toEqual(['seven_day']);
    calls++;
    throw new Error('helper unavailable');
  });
  on('turn.step', async function* () { return RESPONSE; });
  expect((await finish($)).answer).toBe('unchanged');
  expect(calls).toBe(1);
});

test('no API response and subagent completions never freshen observations', async ($, on) => {
  on('fs.read', () => ({ value: JSON.stringify(CONFIG) }));
  on('clock.now', () => ({ value: NOW }));
  let reads = 0;
  on('session.usage', () => { reads++; return { value: {} }; });
  on('turn.step', async function* () { return { ...RESPONSE, usage: null }; });
  await finish($);
  await finish($, { ...STEP, agentId: 'subagent' });
  expect(reads).toBe(0);
});

test('unavailable usage and malformed helper config pass the event through', async ($, on) => {
  let failure = true;
  on('session.usage', () => {
    if (failure) throw new Error('usage unavailable');
    return { value: { rateLimits: [{ kind: 'five_hour', percentUsed: 10, resetsAt: RESET }] } };
  });
  on('clock.now', () => ({ value: NOW }));
  on('fs.read', () => ({ value: 'invalid json' }));
  on('turn.step', async function* () { return RESPONSE; });
  expect((await finish($)).answer).toBe('unchanged');
  failure = false;
  expect((await finish($)).answer).toBe('unchanged');
});


test('startup and diagnostic command prove loading without changing quota data', async ($, on) => {
  const records = [];
  let registered;
  on('command.register', ($, e) => { registered = e; return { value: { command: 'vista-usage-check' } }; });
  on('fs.read', () => ({ value: JSON.stringify(CONFIG) }));
  on('clock.now', () => ({ value: NOW }));
  on('fs.write', ($, e) => { records.push(e); return { value: undefined }; });
  on('session.start', () => ({ cwd: '/project', surface: 'desktop' }));
  on('process.run', ($, e) => {
    expect(e.argv).toEqual(['/python', '/private/coding-usage.py', '--help']);
    expect(e.init.stdin).toBe(undefined);
    return { value: { exitCode: 0, stdout: 'private text must not be forwarded', stderr: '' } };
  });
  await $.session.start({ cwd: '/project', surface: 'desktop' });
  expect(registered.name).toBe('vista-usage-check');
  expect(records.length).toBe(1);
  expect(records[0].path).toBe('/private/state/claude-plugin-health.json');
  expect(JSON.parse(records[0].text)).toEqual({ stage: 'loaded', at: NOW });
  const result = await $.command.run({ command: 'vista-usage-check', args: '' });
  expect(result.text.includes('helper-ok')).toBe(true);
  expect(result.text.includes('private text')).toBe(false);
  expect(records.length).toBe(1);
});

test('health distinguishes missing turn usage, quota validation and helper failure', async ($, on) => {
  let record;
  let limits = [];
  let exitCode = 0;
  let responseUsage = null;
  on('fs.read', () => ({ value: JSON.stringify(CONFIG) }));
  on('clock.now', () => ({ value: NOW }));
  on('fs.write', ($, e) => { record = JSON.parse(e.text); return { value: undefined }; });
  on('session.usage', () => ({ value: { rateLimits: limits } }));
  on('process.run', () => ({ value: { exitCode, stdout: '', stderr: '' } }));
  on('turn.step', async function* () { return { ...RESPONSE, usage: responseUsage }; });
  await finish($);
  expect(record).toEqual({ stage: 'no-response-usage', hasUsage: false, at: NOW });
  responseUsage = RESPONSE.usage;
  await finish($);
  expect(record.stage).toBe('limits-unavailable');
  limits = [{ kind: 'five_hour', percentUsed: 10, resetsAt: 1792418400 }, { kind: 'seven_day', percentUsed: NaN, resetsAt: RESET }];
  await finish($);
  expect(record.stage).toBe('no-valid-limits');
  expect(record.invalidReset).toBe(1);
  expect(record.invalidPercent).toBe(1);
  limits = [{ kind: 'five_hour', percentUsed: 10, resetsAt: RESET }];
  exitCode = 1;
  await finish($);
  expect(record.stage).toBe('helper-exit-error');
  exitCode = 0;
  await finish($);
  expect(record.stage).toBe('saved');
  expect(Object.keys(record).sort()).toEqual(['at','hasUsage','invalidPercent','invalidReset','stage','windowsAccepted','windowsSeen'].sort());
});

test('health strips arbitrary error messages and write failures never block turns', async ($, on) => {
  let record;
  on('fs.read', () => ({ value: JSON.stringify(CONFIG) }));
  on('clock.now', () => ({ value: NOW }));
  on('session.usage', () => { throw new TypeError('private prompt or credential must not be forwarded'); });
  on('fs.write', ($, e) => { record = JSON.parse(e.text); throw new Error('private write failure'); });
  on('turn.step', async function* () { return RESPONSE; });
  expect((await finish($)).answer).toBe('unchanged');
  expect(record).toEqual({ stage: 'usage-read', hasUsage: true, errorClass: 'Other', at: NOW });
});


test('each completed API response is observed while stream and result pass unchanged', async ($, on) => {
  const chunks = [{ kind: 'text', index: 0, text: 'private streamed answer' }, { kind: 'stop', stopReason: 'tool_use', usage: RESPONSE.usage }];
  let saves = 0;
  on('fs.read', () => ({ value: JSON.stringify(CONFIG) }));
  on('clock.now', () => ({ value: NOW }));
  on('fs.write', () => ({ value: undefined }));
  on('session.usage', () => ({ value: { rateLimits: [{ kind: 'five_hour', percentUsed: 10, resetsAt: RESET }] } }));
  on('process.run', ($, e) => { saves++; expect(e.init.stdin.includes('private')).toBe(false); return { value: { exitCode: 0, stdout: '', stderr: '' } }; });
  on('turn.step', async function* ($, e) { yield* chunks; return { ...RESPONSE, turnId: e.turnId, index: e.index }; });
  const stream = $.turn.step(STEP);
  expect((await stream.next()).value).toEqual(chunks[0]);
  expect(saves).toBe(0);
  expect((await stream.next()).value).toEqual(chunks[1]);
  const end = await stream.next();
  expect(end.done).toBe(true);
  expect(end.value).toEqual(RESPONSE);
  expect(saves).toBe(1);
  await finish($, { ...STEP, index: 1 });
  expect(saves).toBe(2);
});
