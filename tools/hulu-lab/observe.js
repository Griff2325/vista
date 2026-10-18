'use strict';

// Only request category and allowlisted public host names leave this function.
function describeRequest(value) {
  if (typeof value !== 'string' || value.length > 32768) return null;
  const match = /^https?:\/\/([a-z0-9.-]+)(?::\d+)?(\/[^?#]*)?(?:[?#].*)?$/i.exec(value);
  if (!match) return null;
  const host = match[1].toLowerCase();
  const allowed = ['hulu.com', 'hulustream.com', 'huluim.com'];
  const safeHost = allowed.some(root => host === root || host.endsWith('.' + root)) ? host : 'other';
  const path = (match[2] || '/').toLowerCase();
  const type = path.endsWith('.m3u8') ? 'HLS' : path.endsWith('.mpd') ? 'DASH' : 'other';
  return type + ' ' + safeHost;
}

function publicAsset(value) {
  if (typeof value !== 'string' || value.length > 32768) return null;
  const match = /^https:\/\/(firetv(?:-neutron)?\.app\.hulu\.com)(\/(?:semver|build_files)\/firetv(?:-neutron)?\/[a-z0-9_./-]{1,200}\.(?:js|html))(?:\?[^#]*)?$/i.exec(value);
  if (!match || match[2].split('/').some(part => part === '.' || part === '..')) return null;
  return 'https://' + match[1].toLowerCase() + match[2];
}

if (typeof module !== 'undefined') module.exports = { describeRequest, publicAsset };

if (typeof Process !== 'undefined' && typeof Interceptor !== 'undefined') {
  const seen = new Set();
  const writeLog = new NativeFunction(Process.getModuleByName('liblog.so').getExportByName('__android_log_write'), 'int', ['int', 'pointer', 'pointer']);
  const tag = Memory.allocUtf8String('HuluLab');
  const report = message => writeLog(4, tag, Memory.allocUtf8String(message));
  Process.attachModuleObserver({
    onAdded(library) {
      if (library.name === 'libjsc.so') {
        try {
          const length = new NativeFunction(library.getExportByName('JSStringGetLength'), 'uint', ['pointer']);
          Interceptor.attach(library.getExportByName('JSEvaluateScript'), {
            onEnter(args) {
              if (args[1].isNull()) return;
              const size = length(args[1]);
              if (size > 500000 && !seen.has('large-script')) {
                seen.add('large-script');
                report('large script through JSC API');
              }
            }
          });
          report('script observer ready');
        } catch (_) {
          report('script observer unavailable');
        }
      }
      if (library.name !== 'libcurl.so') return;
      try {
        Interceptor.attach(library.getExportByName('curl_easy_setopt'), {
          onEnter(args) {
            if (args[1].toInt32() !== 10002 || args[2].isNull()) return;
            try {
              const url = args[2].readUtf8String();
              const asset = publicAsset(url);
              if (asset && !seen.has(asset) && seen.size < 64) {
                seen.add(asset);
                report('public asset ' + asset);
              }
              const description = describeRequest(url);
              if (description && !seen.has(description) && seen.size < 64) {
                seen.add(description);
                report('request ' + description);
              }
            } catch (_) { /* A diagnostic must never interrupt playback. */ }
          }
        });
        report('curl observer ready');
      } catch (_) {
        report('curl observer unavailable');
      }
    }
  });
  report('observer loaded');
  if (typeof installPlayerRoute === 'function') {
    try { installPlayerRoute(report); }
    catch (_) { report('local player install failed; original player retained'); }
  }
}
