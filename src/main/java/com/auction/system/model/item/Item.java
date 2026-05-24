package com.auction.system.model.item;

import com.auction.system.model.auction.Bid;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.auction.system.model.auction.AuctionStatus;

public class Item implements Serializable {
    public static final String CATEGORY_ELECTRONICS = "ELECTRONICS";
    public static final String CATEGORY_VEHICLE = "VEHICLE";
    public static final String CATEGORY_ART = "ART";
    public static final String CATEGORY_FASHION = "FASHION";
    public static final String CATEGORY_BOOK = "BOOK";
    public static final String CATEGORY_HOME = "HOME";
    public static final String CATEGORY_COLLECTIBLE = "COLLECTIBLE";
    public static final String DEFAULT_CATEGORY = "OTHER";

    private static final List<String> SUPPORTED_CATEGORIES = List.of(
            CATEGORY_ELECTRONICS,
            CATEGORY_VEHICLE,
            CATEGORY_ART,
            CATEGORY_FASHION,
            CATEGORY_BOOK,
            CATEGORY_HOME,
            CATEGORY_COLLECTIBLE,
            DEFAULT_CATEGORY
    );

    private String id;
    private String name, description;
    private String category = DEFAULT_CATEGORY;
    private String imagePath;
    private String imageBase64;
    private double startPrice, currentPrice;
    private AuctionStatus status;
    private String sellerId;
    private String sellerUsername;
    private String highestBidderId;
    private String highestBidderUsername;
    private boolean auctionApproved;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private boolean currentUserAutoBidActive;
    private double currentUserAutoBidMaxBid;
    private double currentUserAutoBidIncrementAmount;
    private final List<Bid> bidHistory = new ArrayList<>();

    public Item() {
        setCategory(DEFAULT_CATEGORY);
    }

    public Item(String id, String name, String description, double startingPrice, String sellerId) {
        this.id = id;
        this.name = name; 
        this.description = description;
        this.startPrice = startingPrice;
        this.sellerId = sellerId;
        setCategory(DEFAULT_CATEGORY);
    }

    public Item(String id, String name, String description, double startingPrice, String sellerId, String category) {
        this(id, name, description, startingPrice, sellerId);
        setCategory(category);
    }

    public Item(String id, String name, String description, double startPrice, double currentPrice, AuctionStatus status)  {
        this.id = id;
        this.name = name;
        this.description = description;
        this.startPrice = startPrice;
        this.currentPrice = currentPrice > 0 ? currentPrice : startPrice;
        this.status = status != null ? status : AuctionStatus.OPEN;
        setCategory(DEFAULT_CATEGORY);
    }

    public Item(String id, String name, String description, double startPrice, double currentPrice,
                AuctionStatus status, String category)  {
        this(id, name, description, startPrice, currentPrice, status);
        setCategory(category);
    }

    public static List<String> getSupportedCategories() {
        return SUPPORTED_CATEGORIES;
    }

    public static boolean isSupportedCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return false;
        }
        return SUPPORTED_CATEGORIES.contains(category.trim().toUpperCase(Locale.ROOT));
    }

    public static String normalizeCategory(String category) {
        if (category == null || category.trim().isEmpty()) {
            return DEFAULT_CATEGORY;
        }

        String normalized = category.trim().toUpperCase(Locale.ROOT);
        return SUPPORTED_CATEGORIES.contains(normalized) ? normalized : DEFAULT_CATEGORY;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return normalizeCategory(category);
    }

    public void setCategory(String category) {
        this.category = normalizeCategory(category);
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImageBase64() {
        return imageBase64;
    }

    public void setImageBase64(String imageBase64) {
        this.imageBase64 = imageBase64;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getHighestBidderId() {
        return highestBidderId;
    }

    public void setHighestBidderId(String highestBidderId) {
        this.highestBidderId = highestBidderId;
    }

    public boolean isAuctionApproved() {
        return auctionApproved;
    }

    public void setAuctionApproved(boolean auctionApproved) {
        this.auctionApproved = auctionApproved;
    }

    public LocalDateTime   getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<Bid> getBidHistory() {
        return Collections.unmodifiableList(bidHistory);
    }

    public void addBid(Bid bid) {
        bidHistory.add(bid);
    }

    public boolean isCurrentUserAutoBidActive() {
        return currentUserAutoBidActive;
    }

    public void setCurrentUserAutoBidActive(boolean currentUserAutoBidActive) {
        this.currentUserAutoBidActive = currentUserAutoBidActive;
    }

    public double getCurrentUserAutoBidMaxBid() {
        return currentUserAutoBidMaxBid;
    }

    public void setCurrentUserAutoBidMaxBid(double currentUserAutoBidMaxBid) {
        this.currentUserAutoBidMaxBid = currentUserAutoBidMaxBid;
    }

    public double getCurrentUserAutoBidIncrementAmount() {
        return currentUserAutoBidIncrementAmount;
    }

    public void setCurrentUserAutoBidIncrementAmount(double currentUserAutoBidIncrementAmount) {
        this.currentUserAutoBidIncrementAmount = currentUserAutoBidIncrementAmount;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public String getHighestBidderUsername() {
        return highestBidderUsername;
    }

    public void setHighestBidderUsername(String highestBidderUsername) {
        this.highestBidderUsername = highestBidderUsername;
    }
}
