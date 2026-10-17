package com.griffin.solhome;

import android.service.notification.NotificationListenerService;

/**
 * User-enabled Android access gate for MediaSessionManager. Intentionally does not
 * request, inspect, store, or forward notification objects or notification content.
 */
public final class SpotifyMediaListener extends NotificationListenerService {}
