'use strict';
const assert = require('node:assert/strict');
const { describeRequest } = require('./observe');
assert.equal(describeRequest('https://dynamic-manifest.hulustream.com/private/title/playlist.m3u8?token=secret'), 'HLS dynamic-manifest.hulustream.com');
assert.equal(describeRequest('https://video.hulustream.com/file.MPD#secret'), 'DASH video.hulustream.com');
assert.equal(describeRequest('https://auth.hulu.com/private/account?password=secret'), 'other auth.hulu.com');
assert.equal(describeRequest('https://hulu.com.evil.test/title.m3u8?secret'), 'HLS other');
assert.equal(describeRequest('https://secret@hulu.com/private.mpd'), null);
assert.equal(describeRequest('https://hulu.com:443/file.mpd'), 'DASH hulu.com');
assert.equal(describeRequest('file:///private/secret'), null);
assert.equal(describeRequest(null), null);
assert.equal(describeRequest('https://hulu.com/' + 'x'.repeat(32768)), null);

// Exercise the native-hook boundary without Frida or real account traffic.
const logs = [];
const hooks = new Map();
const library = name => ({ name, getExportByName: symbol => symbol });
require('node:vm').runInNewContext(require('node:fs').readFileSync(__dirname + '/observe.js', 'utf8'), {
  Process: {
    getModuleByName: library,
    attachModuleObserver: observer => ['libjsc.so', 'libcurl.so'].forEach(name => observer.onAdded(library(name)))
  },
  Interceptor: { attach: (symbol, handler) => hooks.set(symbol, handler) },
  Memory: { allocUtf8String: value => value },
  NativeFunction: function (symbol) {
    if (symbol === '__android_log_write') return (_, tag, text) => { assert.equal(tag, 'HuluLab'); logs.push(text); };
    if (symbol === 'JSStringGetLength') return value => value.size;
    throw new Error('Unexpected native call');
  }
});
const pointer = value => ({ isNull: () => false, readUtf8String: () => value });
const request = [null, { toInt32: () => 10002 }, pointer('https://dynamic-manifest.hulustream.com/private-title/playlist.m3u8?token=secret')];
hooks.get('curl_easy_setopt').onEnter(request);
hooks.get('curl_easy_setopt').onEnter(request);
hooks.get('curl_easy_setopt').onEnter([null, { toInt32: () => 10001 }, pointer('secret')]);
hooks.get('curl_easy_setopt').onEnter([null, { toInt32: () => 10002 }, { isNull: () => false, readUtf8String: () => { throw new Error('Unreadable'); } }]);
hooks.get('JSEvaluateScript').onEnter([null, { isNull: () => false, size: 600000 }]);
assert(logs.includes('curl observer ready'));
assert(logs.includes('script observer ready'));
assert(logs.includes('large script through JSC API'));
assert.equal(logs.filter(line => line.startsWith('request ')).length, 1);
assert(!JSON.stringify(logs).includes('secret'));
assert(!JSON.stringify(logs).includes('private-title'));
console.log('Hulu Lab request-redaction tests passed');
