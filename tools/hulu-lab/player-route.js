'use strict';

const PLAYER_URL = 'https://firetv-neutron.app.hulu.com/build_files/firetv-neutron/1/cb36f58029bb.0f69ce3983c2/george.js';
function isPlayerUrl(url) { return url === PLAYER_URL; }
function isPlayerRequest(header) { return /^GET \/player\.js HTTP\/1\.[01]\r\n/.test(header); }

if (typeof module !== 'undefined') module.exports = { isPlayerUrl, isPlayerRequest, installPlayerRoute };

function installPlayerRoute(report) {
  let replacement = null;
  let active = 0;
  const location = Process.getModuleByName('libneutron.so').path;
  const playerPath = location.slice(0, location.lastIndexOf('/') + 1) + 'libvista_hulu_player.js.so';
  const bytes = text => Memory.allocUtf8String(text).readByteArray(text.length);
  const readHeader = data => Array.from(new Uint8Array(data), value => String.fromCharCode(value)).join('');

  async function serve(connection) {
    const deadline = setTimeout(() => { connection.close().catch(() => {}); }, 5000);
    try {
      let header = '';
      while (!header.includes('\r\n\r\n') && header.length < 4096) {
        const chunk = await connection.input.read(4096 - header.length);
        if (!chunk.byteLength) return;
        header += readHeader(chunk);
      }
      if (!header.endsWith('\r\n\r\n') || !isPlayerRequest(header)) {
        await connection.output.writeAll(bytes('HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\nConnection: close\r\n\r\n'));
        return;
      }
      const body = File.readAllBytes(playerPath);
      await connection.output.writeAll(bytes('HTTP/1.1 200 OK\r\nContent-Type: application/javascript\r\nContent-Length: ' + body.byteLength + '\r\nCache-Control: no-store\r\nConnection: close\r\n\r\n'));
      await connection.output.writeAll(body);
      report('patched player served locally');
    } catch (_) {
      report('local player transfer failed');
    } finally {
      clearTimeout(deadline);
      await connection.close().catch(() => {});
    }
  }

  rpc.exports = {
    async init() {
      let listener = null;
      try {
        const file = new File(playerPath, 'rb');
        file.close();
        listener = await Socket.listen({ family: 'ipv4', host: '127.0.0.1', port: 0, backlog: 2 });
        replacement = 'http://127.0.0.1:' + listener.port + '/player.js';
        Interceptor.attach(Process.getModuleByName('libcurl.so').getExportByName('curl_easy_setopt'), {
          onEnter(args) {
            if (!replacement || args[1].toInt32() !== 10002 || args[2].isNull()) return;
            try {
              if (!isPlayerUrl(args[2].readUtf8String())) return;
              this.localUrl = Memory.allocUtf8String(replacement);
              args[2] = this.localUrl;
              report('version-pinned player routed locally');
            } catch (_) { /* Leave unmatched requests alone. */ }
          }
        });
        (async () => {
          try {
            while (true) {
              const connection = await listener.accept();
              if (active >= 2) { await connection.close(); continue; }
              active++;
              serve(connection).finally(() => { active--; });
            }
          } catch (_) {
            replacement = null;
            report('local player listener stopped');
          }
        })();
        report('local player route ready');
      } catch (_) {
        replacement = null;
        if (listener) await listener.close().catch(() => {});
        report('local player route unavailable; original player retained');
      }
    }
  };
  report('local player initializer registered');
}
