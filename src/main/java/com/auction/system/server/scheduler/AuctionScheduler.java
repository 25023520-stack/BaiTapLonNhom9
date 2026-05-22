package com.auction.system.server.scheduler;

import com.auction.system.server.dao.AuctionDAO;
import com.auction.system.server.manager.AuctionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionScheduler.class);
    private static final int CHECK_INTERVAL_SECONDS = 10;

    private final AuctionManager auctionManager;
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "auction-scheduler");
        t.setDaemon(true);
        return t;
    });

    public AuctionScheduler(AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    public void start() {
        // run immediately to recover auctions that expired while server was offline,
        // then repeat every CHECK_INTERVAL_SECONDS
        scheduler.scheduleAtFixedRate(
                this::closeExpiredAuctions,
                0,
                CHECK_INTERVAL_SECONDS,
                TimeUnit.SECONDS
        );
        LOGGER.info("AuctionScheduler started — checking every {}s", CHECK_INTERVAL_SECONDS);
    }

    public void stop() {
        scheduler.shutdownNow();
    }

    private void closeExpiredAuctions() {
        try {
            List<String> expiredItemIds = auctionDAO.findExpiredRunningItemIds();
            for (String itemId : expiredItemIds) {
                try {
                    auctionManager.finishAuction(itemId);
                    LOGGER.info("Scheduler auto-closed expired auction for item: {}", itemId);
                } catch (Exception e) {
                    LOGGER.warn("Failed to auto-close auction for item {}: {}", itemId, e.getMessage());
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Scheduler failed to query expired auctions: {}", e.getMessage());
        }
    }
}
