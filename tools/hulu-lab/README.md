# Hulu Lab diagnostic

Private experimental Hulu testing is isolated in a separate application ID. The original installed app and its data are retained. This directory contains only original diagnostic JavaScript and tests; it contains no Hulu APK, decompiled source, native library, account data or signing material.

`observe.js` is a Frida Gadget script for the private Lab copy. It opens no debug listener and changes no request, trust policy, account entitlement or video DRM behavior. It reports only HLS/DASH/other and allowlisted public Hulu host names. Paths, query strings, headers, payloads and unknown host names are omitted. It also reports whether a large script passes through the exported JavaScriptCore evaluation API, without reading script text. Distinct output is bounded.

Run the URL-redaction checks:

```sh
node tools/hulu-lab/observe.test.js
```

The diagnostic is not part of Vista's Android build. It does not block or skip ads. Actual playback in the separate copy requires official Hulu activation. Native hooks and modified clients may fail after Hulu updates; the original app is the fallback.

Instrumentation reference: [Frida Gadget autonomous scripts](https://frida.re/docs/gadget/#script).
