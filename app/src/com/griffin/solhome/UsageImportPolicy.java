package com.griffin.solhome;

/** Defense in depth for ContentProvider.call, whose permission enforcement is method-dependent. */
final class UsageImportPolicy {
    static boolean allowed(int caller,int own){return caller==2000||caller==own;}
}
