'use strict';
const assert = require('node:assert/strict');
const vm = require('node:vm');
const fs = require('node:fs');
if (process.argv.length !== 3) throw new Error('Usage: node check_player.js PRIVATE_PATCHED_PLAYER.js');
const text = fs.readFileSync(process.argv[2], 'utf8');
const matched = text.match(/\.prototype\.onEnterBreak=(function\(e\)\{[\s\S]*?\}),t\.prototype\.onBeginBreak=/);
assert(matched, 'Expected unique ad-entry function');
function check(linear, options) {
 const seen = { skip: 0, normal: 0, viewed: 0, grace: 0, badge: 0 };
 const state = {
  _state: { segment: { pod_id: 'test-pod' } },
  getStateMachine: () => ({ isLinearTime: () => linear }),
  getContextValue: () => undefined,
  startBreakGracePeriod: () => seen.grace++, markViewed: () => seen.viewed++,
  skipAdAndJumpToNextSegment: value => { seen.skip++; seen.options = value; },
  onBeginBreak: () => seen.normal++
 };
 vm.runInNewContext('(' + matched[1] + ').call(state, options)', {
  state, options, l: { START_SEGMENT: { v: 'start' } },
  window: { __vistaHuluLabSkipped: () => seen.badge++ }
 }, { timeout: 1000 });
 return { state, seen };
}
let result = check(false, { returnSegmentName: 'content', returnSegmentPosition: 42000 });
assert.equal(result.seen.skip, 1); assert.equal(result.seen.normal, 0);
assert.equal(result.seen.options.shouldSkipPod, true); assert.equal(result.seen.badge, 1);
assert.equal(result.state.returnSegmentName, 'content'); assert.equal(result.state.returnSegmentPosition, 42000);
result = check(true, { seekPosition: 1337 });
assert.equal(result.seen.skip, 0); assert.equal(result.seen.normal, 1); assert.equal(result.seen.badge, 0);
assert.equal(result.state._reportSeekOccured, 1337);
result = check(false, null); assert.equal(result.seen.skip, 1);
result = check(true, { shouldSkipPod: true }); assert.equal(result.seen.skip, 1); assert.equal(result.seen.badge, 0);
console.log('Actual player ad-entry checks passed: VOD skip, return position, live/recorded normal, null options.');
