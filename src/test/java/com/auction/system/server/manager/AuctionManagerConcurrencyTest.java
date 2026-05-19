package com.auction.system.server.manager;

import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.Seller;
import com.auction.system.server.dao.BidDAO;
import com.auction.system.server.database.Database;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.IntToDoubleFunction;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionManagerConcurrencyTest {

    private static final String TEST_PREFIX = "TEST-CONC-";
    private static final double START_PRICE = 500.0;
    private static final int WAIT_TIMEOUT_SECONDS = 30;

    private AuctionManager manager;
    private BidDAO bidDAO;
    private Seller seller;
    private Item item;
    private String itemId;

    @BeforeEach
    void setUp() {
        Database.getInstance().initializeDatabase();
        cleanUpTestData();

        manager = AuctionManager.getInstance();
        manager.resetForTest();
        bidDAO = new BidDAO();

        String sellerToken = shortToken();
        itemId = TEST_PREFIX + "ITEM-" + shortToken();

        seller = new Seller(
                TEST_PREFIX + "SELLER-" + sellerToken,
                "Concurrency Seller",
                "seller_conc_" + sellerToken,
                "seller_conc_" + sellerToken + "@example.com",
                "secret"
        );

        item = new Item(
                itemId,
                "Concurrency Test Item",
                "Item used for concurrent bidding tests",
                START_PRICE,
                START_PRICE,
                AuctionStatus.OPEN
        );

        manager.registerUser(seller);
        manager.addItem(item, seller);
        manager.startAuction(
                itemId,
                LocalDateTime.now().minusSeconds(5),
                LocalDateTime.now().plusMinutes(5)
        );
    }

    @AfterEach
    void tearDown() {
        if (manager != null) {
            manager.resetForTest();
        }
        cleanUpTestData();
    }

    @Test // test 30 bid đặt cùng 1 giá
    //kì vọng là chỉ 1 bid thành công
    void multipleBiddersSubmittingSamePriceOnlyOneBidIsAccepted() throws Exception {
        int bidderCount = 30;
        double bidPrice = 600.0;

        List<Bidder> bidders = registerBidders(bidderCount, "same");
        List<BidResult> results = placeBidsAtSameTime(bidders, index -> bidPrice);

        assertNoUnexpectedFailures(results);

        List<BidResult> successfulResults = successfulResults(results);
        assertEquals(1, successfulResults.size(), describeResults(results));

        BidResult winner = successfulResults.get(0);
        Item storedItem = manager.findItemById(itemId).orElseThrow();
        List<Bid> persistedBids = bidDAO.findByItemId(itemId);

        assertEquals(bidPrice, storedItem.getCurrentPrice(), 0.001);
        assertEquals(winner.bidder().getId(), storedItem.getHighestBidderId());
        assertEquals(1, storedItem.getBidHistory().size());
        assertEquals(1, persistedBids.size());
        assertEquals(winner.bidder().getId(), persistedBids.get(0).getBidderId());
        assertEquals(bidPrice, persistedBids.get(0).getAmount(), 0.001);
    }

    @Test /*tạo 50 bidder đặt nhiều giá khác nhau
    kì vọng bid có giá cao nhất chắc chắn thành công
    current_price = gia cao nhat
    highestBidderId là bidder đặt giá cao nhất.
    Số bid trong RAM khớp số bid đã lưu trong DB.
    BidDAO.findHighestBidByItemId(...) trả về đúng bidder thắng.
    */
    void multipleBiddersSubmittingDifferentPricesFinalWinnerIsHighestBid() throws Exception {
        int bidderCount = 50;
        List<Bidder> bidders = registerBidders(bidderCount, "diff");
        List<Double> prices = shuffledPricesAboveStartPrice(bidderCount);

        List<BidResult> results = placeBidsAtSameTime(bidders, prices::get);

        assertNoUnexpectedFailures(results);

        BidResult expectedWinner = results.stream()
                .max(Comparator.comparingDouble(BidResult::amount))
                .orElseThrow();

        Item storedItem = manager.findItemById(itemId).orElseThrow();
        Bid persistedHighestBid = bidDAO.findHighestBidByItemId(itemId);
        List<Bid> persistedBids = bidDAO.findByItemId(itemId);
        List<BidResult> successfulResults = successfulResults(results);

        assertTrue(successfulResults.size() >= 1, describeResults(results));
        assertTrue(expectedWinner.success(), "Highest bid must be accepted. " + describeResults(results));
        assertEquals(expectedWinner.amount(), storedItem.getCurrentPrice(), 0.001);
        assertEquals(expectedWinner.bidder().getId(), storedItem.getHighestBidderId());
        assertEquals(successfulResults.size(), storedItem.getBidHistory().size());
        assertEquals(successfulResults.size(), persistedBids.size());

        assertNotNull(persistedHighestBid);
        assertEquals(expectedWinner.amount(), persistedHighestBid.getAmount(), 0.001);
        assertEquals(expectedWinner.bidder().getId(), persistedHighestBid.getBidderId());
    }

    private List<Bidder> registerBidders(int count, String scenario) {
        List<Bidder> bidders = new ArrayList<>(count);

        for (int index = 0; index < count; index++) {
            String token = scenario + "_" + index + "_" + shortToken();
            Bidder bidder = new Bidder(
                    TEST_PREFIX + "BIDDER-" + scenario.toUpperCase() + "-" + index + "-" + shortToken(),
                    "Bidder " + index,
                    "bidder_" + token,
                    "bidder_" + token + "@example.com",
                    "secret"
            );

            manager.registerUser(bidder);
            bidders.add(bidder);
        }

        return bidders;
    }

    private List<Double> shuffledPricesAboveStartPrice(int count) {
        List<Double> prices = IntStream.rangeClosed(1, count)
                .mapToObj(index -> START_PRICE + (index * 10.0))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        Collections.shuffle(prices, new java.util.Random(20260519));
        return prices;
    }

    private List<BidResult> placeBidsAtSameTime(
            List<Bidder> bidders,
            IntToDoubleFunction amountByIndex
    ) throws InterruptedException, ExecutionException {
        int bidderCount = bidders.size();
        ExecutorService executor = Executors.newFixedThreadPool(bidderCount);
        CountDownLatch readyLatch = new CountDownLatch(bidderCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        List<Future<BidResult>> futures = new ArrayList<>(bidderCount);

        try {
            for (int index = 0; index < bidderCount; index++) {
                int bidderIndex = index;
                Bidder bidder = bidders.get(bidderIndex);
                double amount = amountByIndex.applyAsDouble(bidderIndex);

                futures.add(executor.submit(() -> {
                    readyLatch.countDown();

                    if (!startLatch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                        return BidResult.failed(
                                bidderIndex,
                                bidder,
                                amount,
                                new AssertionError("Timed out waiting for start signal")
                        );
                    }

                    try {
                        return BidResult.success(
                                bidderIndex,
                                bidder,
                                amount,
                                manager.placeBid(itemId, bidder, amount)
                        );
                    } catch (Throwable error) {
                        return BidResult.failed(bidderIndex, bidder, amount, error);
                    }
                }));
            }

            assertTrue(
                    readyLatch.await(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "Timed out waiting for bidder tasks to be ready"
            );

            startLatch.countDown();
            executor.shutdown();

            assertTrue(
                    executor.awaitTermination(WAIT_TIMEOUT_SECONDS, TimeUnit.SECONDS),
                    "Timed out waiting for bidder tasks to finish"
            );

            List<BidResult> results = new ArrayList<>(bidderCount);
            for (Future<BidResult> future : futures) {
                results.add(future.get());
            }
            return results;
        } finally {
            executor.shutdownNow();
        }
    }

    private List<BidResult> successfulResults(List<BidResult> results) {
        return results.stream()
                .filter(BidResult::success)
                .toList();
    }

    private void assertNoUnexpectedFailures(List<BidResult> results) {
        List<BidResult> unexpectedFailures = results.stream()
                .filter(result -> !result.success())
                .filter(result -> !isExpectedRejectedBid(result.error()))
                .toList();

        assertTrue(
                unexpectedFailures.isEmpty(),
                "Unexpected bidder errors:" + System.lineSeparator() + describeResults(unexpectedFailures)
        );
    }

    private boolean isExpectedRejectedBid(Throwable error) {
        if (error == null || error.getMessage() == null) {
            return false;
        }

        String message = error.getMessage();
        return (error instanceof IllegalArgumentException
                && message.contains("Bid amount must be greater than current price"))
                || (error instanceof IllegalStateException
                && message.contains("another bidder placed a higher or equal bid first"));
    }

    private String describeResults(List<BidResult> results) {
        StringBuilder builder = new StringBuilder();

        for (BidResult result : results) {
            builder.append(System.lineSeparator())
                    .append("[")
                    .append(result.index())
                    .append("] bidder=")
                    .append(result.bidder().getId())
                    .append(", amount=")
                    .append(result.amount())
                    .append(", success=")
                    .append(result.success());

            if (!result.success()) {
                builder.append(", error=")
                        .append(result.error().getClass().getSimpleName())
                        .append(": ")
                        .append(result.error().getMessage());
            }
        }

        return builder.toString();
    }

    private void cleanUpTestData() {
        String deleteBids = """
                DELETE FROM bids
                WHERE item_id LIKE 'TEST-CONC-%'
                   OR bidder_id LIKE 'TEST-CONC-%'
                """;

        String deleteAuctions = """
                DELETE FROM auctions
                WHERE item_id LIKE 'TEST-CONC-%'
                """;

        String deleteItems = """
                DELETE FROM items
                WHERE id LIKE 'TEST-CONC-%'
                   OR seller_id LIKE 'TEST-CONC-%'
                """;

        String deleteUsers = """
                DELETE FROM users
                WHERE id LIKE 'TEST-CONC-%'
                """;

        try (Connection conn = Database.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try (
                    PreparedStatement deleteBidsStmt = conn.prepareStatement(deleteBids);
                    PreparedStatement deleteAuctionsStmt = conn.prepareStatement(deleteAuctions);
                    PreparedStatement deleteItemsStmt = conn.prepareStatement(deleteItems);
                    PreparedStatement deleteUsersStmt = conn.prepareStatement(deleteUsers)
            ) {
                deleteBidsStmt.executeUpdate();
                deleteAuctionsStmt.executeUpdate();
                deleteItemsStmt.executeUpdate();
                deleteUsersStmt.executeUpdate();

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Cannot clean concurrent bid test data", e);
        }
    }

    private String shortToken() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private record BidResult(
            int index,
            Bidder bidder,
            double amount,
            Bid bid,
            Throwable error
    ) {

        static BidResult success(int index, Bidder bidder, double amount, Bid bid) {
            return new BidResult(index, bidder, amount, bid, null);
        }

        static BidResult failed(int index, Bidder bidder, double amount, Throwable error) {
            return new BidResult(index, bidder, amount, null, error);
        }

        boolean success() {
            return bid != null;
        }
    }
}
