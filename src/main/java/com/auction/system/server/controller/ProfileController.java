package com.auction.system.server.controller;

import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.user.User;
import com.auction.system.server.manager.ProfileManager;

public class ProfileController {
    private final ProfileManager profileManager = new ProfileManager();

    public ResponsePayload profile(User authenticatedUser) {
        try {
            ResponsePayload response = ResponsePayload.ok("Profile retrieved");
            response.put("profile", profileManager.getProfile(authenticatedUser));
            return response;
        } catch (RuntimeException exception) {
            return ResponsePayload.error(exception.getMessage());
        }
    }
}
