# Phone companion

The primary phone client is the [native SwiftUI iPhone app](../companion/ios/README.md), built in Xcode. An optional lightweight browser client remains for testing/fallback. Both connect directly to the TV; no laptop needs to stay on.

Enable **Settings → Phone companion** on Vista. On trusted home Wi-Fi, scan the TV QR using the iPhone app (or enter its address manually), then enter the six-digit TV code. Safari/Chrome can also use the displayed HTTP address as a fallback. No cloud account is required. Pairing automatically closes the TV dialog after connection. Done closes it manually.

The native app and fallback page provide:

- D-pad/OK/Back within the visible Vista Home and Explore Activities; a Vista Home shortcut from Explore or the Activity's ambient view.
- Typed movie/TV searches sent to Explore. Its optional TMDB catalog credential must still be configured on the TV for actual catalog results. Phone search does not read service passwords, sign-ins, viewing histories or provider search results.
- Six installed-app shortcuts while Vista is visible.
- Spotify previous/play-pause/next through an available local media session and the existing user-enabled notification-access gate. The companion does not grant this permission. If no transport is available, it reports that rather than claiming playback changed.
- Automatic/season preview selection, wildlife on/off, and opening the Activity ambient scene. Preview selections persist until Automatic is restored. Real clock/weather stay live; an already running system dream reads scene preferences within one minute.

**Fire OS limitation:** an ordinary background app cannot bring Vista or another Activity in front of a streaming app on this tested firmware. Use the physical Home button to return first. The companion reports this restriction. It does not use root, input injection, a shell broker, accessibility access or an overlay permission. TV dialogs, playback-app arrows, volume, power and wake from the system dream use the physical remote. The physical Home bridge remains separate and unchanged.

## Network and access boundary

The opt-in, unexported, non-sticky foreground service binds one private IPv4 interface on TCP 8765. A persistent Android notification offers Turn off; Vista settings also stops it. It does not start at boot or acquire a wake lock. TV network changes can require turning it off and enabling it again. Guest-network isolation can prevent the phone reaching the TV.

HTTP is **not encrypted**. Use a trusted home network, without port forwarding or public exposure. The QR contains only the local address, not a credential. This is a local convenience controller, not a hardened public server. A hostile same-network observer could intercept HTTP traffic; pairing and browser-origin checks do not provide transport encryption.

A cryptographically generated six-digit code lasts two minutes, allows at most five incorrect attempts and is consumed by successful pairing. Requesting another code revokes the prior phone immediately. The session credential has 256 random bits, lives only in server memory and the native app's memory or the fallback browser tab's sessionStorage, and is sent in an Authorization header. It never appears in the URL/QR, logs or preferences. Unpair invalidates it. Service shutdown, process death/restart, 30 minutes without authenticated use, or a twelve-hour absolute limit ends the session. Public page requests do not keep it active. The server does not maintain browsing/listening history or client logs.

The transport accepts a fixed command set, limits request/body/header sizes, rate limits API traffic and uses one socket worker with read deadlines and a small accept backlog. Commands pass to the Android main thread with a bounded wait and cancellation when still queued. No filesystem routes, arbitrary intents/packages, shell commands, external assets, CORS or notification contents are exposed. Requests must use the bound numeric Host and matching browser Origin. State-changing requests require POST plus explicit content type, and authenticated requests require the token. These checks reduce cross-site command requests and DNS rebinding; they are not substitutes for TLS or network trust.

The service handles one phone pairing at a time; copying its bearer credential would allow another client until revoked. Spotify callbacks may remain subscribed while this explicitly enabled companion is on after a Spotify control is requested. Shutdown removes them. Normal popout callbacks still close with the panel.

## QR source

TV QR encoding uses Project Nayuki's [MIT-licensed Java QR library](https://www.nayuki.io/page/qr-code-generator-library), version 1.8.0, source commit `720f62bddb7226106071d4728c292cb1df519ceb`. `QrCode.java`, `QrSegment.java`, `BitBuffer.java` and `DataTooLongException.java` retain their full upstream license headers. The package declaration changes to fit the existing flat Java build; trailing whitespace is normalized. QR generation occurs when the pairing dialog opens, not on a scene frame.
