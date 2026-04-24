package com.auction.system.ui;

import com.auction.system.manager.AuthManager;

public final class AppContext {
    private static final AuthManager AUTH_MANAGER = new AuthManager();

    private AppContext() {
    }

    public static AuthManager getAuthManager() {
        return AUTH_MANAGER;
    }
}
