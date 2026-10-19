let lastHealth = { stage: 'registered' };

function errorClass(error) {
  return ['Error', 'TypeError', 'SyntaxError', 'RangeError'].includes(error?.name) ? error.name : 'Other';
}

async function runtime($) {
  const config = JSON.parse(await $.fs.read($.plugin.root + '/runtime.json'));
  if (![config.python, config.collector, config.stateDir].every(value => typeof value === 'string' && value.startsWith('/'))) throw new TypeError();
  return config;
}

async function health($, config, detail) {
  lastHealth = { ...detail, at: await $.clock.now() };
  // shortcut: a diagnostic write can be partial; the quota file still uses Python's atomic save.
  try { await $.fs.write(config.stateDir + '/claude-plugin-health.json', JSON.stringify(lastHealth)); } catch {}
}

export function register(on) {
  on('session.start', async ($, e, next) => {
    try { await $.command.register({ name: 'vista-usage-check', description: 'Check Vista quota mod and local helper without inference or changing quota readings' }); } catch {}
    try { await health($, await runtime($), { stage: 'loaded' }); } catch (error) { lastHealth = { stage: 'runtime-error', errorClass: errorClass(error) }; }
    return next(e);
  });

  on('command.run', { command: 'vista-usage-check' }, async $ => {
    let probe = 'runtime-error';
    try {
      const config = await runtime($);
      probe = 'helper-error';
      const result = await $.process.run([config.python, config.collector, '--help'], { timeoutMs: 2000 });
      probe = result.exitCode === 0 ? 'helper-ok' : 'helper-exit-error';
    } catch (error) { probe += ':' + errorClass(error); }
    return { text: 'Vista usage mod active; ' + probe + '; last observation: ' + JSON.stringify(lastHealth) };
  });

  on('turn.step', async function* ($, e, next) {
    // Forward every stream chunk unread; inspect only the response's usage presence.
    const response = yield* next(e);
    if (!e.agentId) await observe($, Boolean(response.usage));
    return response;
  });
}

async function observe($, hasUsage) {
  let config;
  const detail = { stage: 'runtime', hasUsage };
  try {
    config = await runtime($);
    detail.stage = 'no-response-usage';
    // Missing response usage cannot prove freshness; keep the previous quota timestamp.
    if (hasUsage) {
      detail.stage = 'usage-read';
      const usage = await $.session.usage();
      const now = await $.clock.now();
      const limits = Array.isArray(usage?.rateLimits) ? usage.rateLimits : [];
      detail.windowsSeen = Math.min(limits.length, 100);
      detail.invalidPercent = 0;
      detail.invalidReset = 0;
      const rate_limits = {};
      for (const raw of limits.slice(0, 100)) {
        if (!raw || (raw.kind !== 'five_hour' && raw.kind !== 'seven_day')) continue;
        const reset = typeof raw.resetsAt === 'string' ? Math.floor(Date.parse(raw.resetsAt) / 1000) : NaN;
        if (typeof raw.percentUsed !== 'number' || !Number.isFinite(raw.percentUsed) || raw.percentUsed < 0 || raw.percentUsed > 100) { detail.invalidPercent++; continue; }
        if (!Number.isFinite(reset) || reset * 1000 <= now || reset * 1000 > now + 400 * 86400000) { detail.invalidReset++; continue; }
        rate_limits[raw.kind] = { used_percentage: raw.percentUsed, resets_at: reset };
      }
      detail.windowsAccepted = Object.keys(rate_limits).length;
      detail.stage = limits.length ? 'no-valid-limits' : 'limits-unavailable';
      if (detail.windowsAccepted) {
        detail.stage = 'helper-run';
        const result = await $.process.run([config.python, config.collector, 'claude-mod', '--state-dir', config.stateDir], {
          stdin: JSON.stringify({ rate_limits }), timeoutMs: 2000,
        });
        detail.stage = result.exitCode === 0 ? 'saved' : 'helper-exit-error';
      }
    }
  } catch (error) { detail.errorClass = errorClass(error); }
  // Diagnostics contain only fixed stage names, booleans, bounded counters and error classes.
  try { if (config) await health($, config, detail); else lastHealth = detail; } catch {}
}
