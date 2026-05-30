package com.auction.system.server.manager;

import com.auction.system.common.payload.UserProfilePayload;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.model.user.User;
import com.auction.system.server.dao.ProfileDAO;
import com.auction.system.server.dao.UserDAO;

public class ProfileManager {
    private final UserDAO userDAO = new UserDAO();
    private final ProfileDAO profileDAO = new ProfileDAO();

    public UserProfilePayload getProfile(User authenticatedUser) {
        if (authenticatedUser == null) {
            throw new IllegalStateException("Please login before viewing profile");
        }

        User latestUser = userDAO.findById(authenticatedUser.getId());
        if (latestUser == null) {
            throw new IllegalStateException("Current user does not exist");
        }

        UserProfilePayload profile = new UserProfilePayload();
        profile.setUserId(latestUser.getId());
        profile.setFullName(latestUser.getFullName());
        profile.setUsername(latestUser.getUserName());
        profile.setEmail(latestUser.getEmail());
        profile.setRole(latestUser.getRole());
        profile.setApproved(latestUser.isApproved());
        profile.setBalance(latestUser.getBalance());

        if (latestUser instanceof Bidder) {
            profile.setBidderStats(profileDAO.findBidderStats(latestUser.getId()));
            profile.setBidHistory(profileDAO.findBidHistory(latestUser.getId()));
            profile.setWonItems(profileDAO.findWonItems(latestUser.getId()));
        } else if (latestUser instanceof Seller) {
            profile.setSellerStats(profileDAO.findSellerStats(latestUser.getId()));
            profile.setSellerResults(profileDAO.findSellerResults(latestUser.getId()));
        }

        return profile;
    }
}
