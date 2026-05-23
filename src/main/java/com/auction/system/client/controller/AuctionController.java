package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.google.gson.Gson;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Base64;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AuctionController {
    private static final Gson GSON = GsonProvider.get();
    private static final DecimalFormat VND_FORMAT =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(new Locale("vi", "VN")));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private Timeline countdownTimer;

    @FXML
    private ListView<Item> itemListView;

    @FXML
    private Label nameValue;

    @FXML
    private Label priceValue;

    @FXML
    private Label statusValue;

    @FXML
    private Label sellerValue;

    @FXML
    private Label leaderValue;

    @FXML
    private Label scheduleValue;

    @FXML
    private Label countdownValue;

    @FXML
    private TextArea descriptionArea;

    @FXML
    private TextArea bidHistoryArea;

    @FXML
    private ImageView itemImageView;

    @FXML
    protected void initialize() {
        itemListView.setItems(items);
        itemListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.getName() + " | " + formatCurrency(item.getCurrentPrice()) + " | " + displayStatus(item));
            }
        });
        itemListView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> showItemDetails(newItem));

        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        bidHistoryArea.setEditable(false);
        bidHistoryArea.setWrapText(true);
        initializeAuctionActions();
        startCountdownTimer();
        startRealtimeListener();
        loadItems();
    }

    protected void initializeAuctionActions() {
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        stopTimers();
        try {
            AppContext.logout();
        } catch (IOException ignored) {
            // The local session is closed even if the server disconnect acknowledgement fails.
        }

        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loginView));
            stage.setTitle("Dang nhap");
            stage.show();
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Không thể mở màn hình đăng nhập.");
        }
    }

    protected void stopTimers() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    protected void showItemDetails(Item item) {
        if (item == null) {
            nameValue.setText("-");
            priceValue.setText("-");
            statusValue.setText("-");
            sellerValue.setText("-");
            leaderValue.setText("-");
            scheduleValue.setText("-");
            countdownValue.setText("-");
            descriptionArea.clear();
            bidHistoryArea.clear();
            setItemImage(null);
            updateAuctionActions(null);
            return;
        }

        nameValue.setText(item.getName());
        priceValue.setText(formatCurrency(item.getCurrentPrice()));
        statusValue.setText(displayStatus(item).name());
        sellerValue.setText(formatUsername(item.getSellerUsername(), item.getSellerId(), "-"));
        leaderValue.setText(formatUsername(item.getHighestBidderUsername(), item.getHighestBidderId(), "Chưa có"));

        scheduleValue.setText(formatSchedule(item));
        updateCountdown(item);
        descriptionArea.setText(item.getDescription());
        bidHistoryArea.setText(formatBidHistory(item));
        setItemImage(item);
        updateAuctionActions(item);
    }

    private void setItemImage(Item item) {
        if (itemImageView == null) {
            return;
        }

        String imageBase64 = item == null ? null : item.getImageBase64();
        if (imageBase64 == null || imageBase64.isBlank()) {
            itemImageView.setImage(null);
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            itemImageView.setImage(new Image(new ByteArrayInputStream(bytes), 54, 54, true, true));
        } catch (IllegalArgumentException exception) {
            itemImageView.setImage(null);
        }
    }

    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            Item selectedItem = itemListView.getSelectionModel().getSelectedItem();
            updateCountdown(selectedItem);
            updateAuctionActions(selectedItem);
            // re-render list cells so an expired RUNNING item shows FINISHED before the server confirms
            itemListView.refresh();
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdown(Item item) {
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            countdownValue.setText("-");
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime())) {
            countdownValue.setText("Sắp bắt đầu  •  " + formatRemainingTime(java.time.Duration.between(now, item.getStartTime())));
            return;
        }
        if (now.isBefore(item.getEndTime())) {
            countdownValue.setText("Đang diễn ra  •  còn " + formatRemainingTime(java.time.Duration.between(now, item.getEndTime())));
            return;
        }

        countdownValue.setText("Đã kết thúc");
        // Server chưa kịp gửi AUCTION_FINISHED — cập nhật status trên UI ngay để tránh hiển thị sai
        statusValue.setText(displayStatus(item).name());
    }

    // Khi RUNNING nhưng đã quá end time, hiển thị FINISHED ngay trên UI trong lúc chờ server xác nhận
    private AuctionStatus displayStatus(Item item) {
        if (item.getStatus() == AuctionStatus.RUNNING
                && item.getEndTime() != null
                && LocalDateTime.now().isAfter(item.getEndTime())) {
            return AuctionStatus.FINISHED;
        }
        return item.getStatus();
    }

    private String formatRemainingTime(java.time.Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatSchedule(Item item) {
        if (item.getStartTime() == null || item.getEndTime() == null) {
            return "-";
        }
        return DATE_TIME_FORMATTER.format(item.getStartTime()) + " -> " + DATE_TIME_FORMATTER.format(item.getEndTime());
    }

    private String formatBidHistory(Item item) {
        if (item.getBidHistory().isEmpty()) {
            return "Chưa có lượt đặt giá nào.";
        }

        LocalDateTime now = LocalDateTime.now();
        StringBuilder builder = new StringBuilder();
        List<Bid> history = item.getBidHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            Bid bid = history.get(i);
            String bidderName = bid.getBidder() != null ? bid.getBidder().getFullName() : bid.getBidderId();
            builder.append("[")
                    .append(bid.getBidType())
                    .append("] ")
                    .append(bidderName)
                    .append("  —  đã đặt ")
                    .append(formatCurrency(bid.getAmount()))
                    .append("  •  ")
                    .append(formatRelativeTime(bid.getTimestamp(), now))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    @FXML
    protected void showBidHistoryDialog() {
        Item selectedItem = getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Bạn chưa chọn sản phẩm.");
            return;
        }

        TextArea historyView = new TextArea(formatBidHistory(selectedItem));
        historyView.setEditable(false);
        historyView.setWrapText(true);
        historyView.setPrefSize(760, 460);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Lịch sử đấu giá");
        dialog.setHeaderText("Lịch sử đấu giá - " + selectedItem.getName());
        dialog.getDialogPane().setContent(historyView);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private String formatRelativeTime(LocalDateTime time, LocalDateTime now) {
        if (time == null) return "vừa xong";
        long totalSeconds = java.time.Duration.between(time, now).getSeconds();
        if (totalSeconds < 60) return "vừa xong";
        if (totalSeconds < 3600) return (totalSeconds / 60) + " phút trước";
        if (totalSeconds < 86_400) return (totalSeconds / 3600) + " tiếng trước";
        return (totalSeconds / 86_400) + " ngày trước";
    }

    protected void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected void loadItems() {
        try {
            AuctionClient client = AppContext.getAuctionClient();
            client.send(new Payload(PayloadType.LIST_ITEMS));
            ResponsePayload response = readResponse(client);
            if (!response.isSuccess()) {
                showAlert(Alert.AlertType.ERROR, response.getMessage());
                return;
            }

            Object rawItems = response.getBody().get("items");
            if (rawItems instanceof List<?> itemList) {
                items.setAll(itemList.stream()
                        .map(this::toItem)
                        .filter(item -> item != null)
                        .toList());
                itemListView.getSelectionModel().selectFirst();
                showItemDetails(itemListView.getSelectionModel().getSelectedItem());
                return;
            }

            showAlert(Alert.AlertType.ERROR, "Dữ liệu sản phẩm từ server không hợp lệ.");
        } catch (IOException exception) {
            Stage stage = (Stage) itemListView.getScene().getWindow();
            stopTimers();
            AppContext.goToServerDown(stage);
        }
    }

    protected ResponsePayload readResponse(AuctionClient client) throws IOException {
        Payload raw = client.read();
        if (raw == null) {
            throw new IOException("Server returned null");
        }

        ResponsePayload response = new ResponsePayload();
        response.setType(raw.getType());
        raw.getBody().forEach(response::put);
        return response;
    }

    protected Item findItemById(String itemId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElse(null);
    }

    protected Item getSelectedItem() {
        return itemListView.getSelectionModel().getSelectedItem();
    }

    protected void selectItem(Item item) {
        itemListView.getSelectionModel().select(item);
    }

    private void startRealtimeListener() {
        try {
            AuctionClient client = AppContext.getAuctionClient();
            client.startListening(
                    payload -> {
                        if (payload != null && payload.getType() == PayloadType.UPDATE_AUCTION) {
                            ResponsePayload update = new ResponsePayload();
                            update.setType(payload.getType());
                            payload.getBody().forEach(update::put);
                            Platform.runLater(() -> handleAuctionUpdate(update));
                        }
                    },
                    error -> Platform.runLater(() -> {
                        stopTimers();
                        Stage stage = (Stage) itemListView.getScene().getWindow();
                        AppContext.goToServerDown(stage);
                    })
            );
        } catch (IOException e) {
            Stage stage = (Stage) itemListView.getScene().getWindow();
            stopTimers();
            AppContext.goToServerDown(stage);
        }
    }

    private void handleAuctionUpdate(ResponsePayload update) {
        String eventType = String.valueOf(update.getBody().get("eventType"));
        if ("BALANCE_UPDATED".equals(eventType)) {
            String userId = String.valueOf(update.getBody().get("userId"));
            Object rawBalance = update.getBody().get("newBalance");
            double newBalance = rawBalance instanceof Number n ? n.doubleValue() : 0;
            onBalanceUpdated(userId, newBalance);
            return;
        }
        Object rawItem = update.getBody().get("item");
        Item updatedItem = toItem(rawItem);
        if (updatedItem == null) {
            return;
        }

        if ("ITEM_ADDED".equals(eventType)) {
            boolean exists = items.stream().anyMatch(i -> Objects.equals(i.getId(), updatedItem.getId()));
            if (!exists) items.add(updatedItem);
            return;
        }

        if ("ITEM_REMOVED".equals(eventType)) {
            items.removeIf(i -> Objects.equals(i.getId(), updatedItem.getId()));
            Item selected = itemListView.getSelectionModel().getSelectedItem();
            if (selected != null && Objects.equals(selected.getId(), updatedItem.getId())) {
                showItemDetails(null);
            }
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            if (Objects.equals(items.get(i).getId(), updatedItem.getId())) {
                Item previousItem = items.get(i);
                if ((updatedItem.getImageBase64() == null || updatedItem.getImageBase64().isBlank())
                        && previousItem.getImageBase64() != null) {
                    updatedItem.setImageBase64(previousItem.getImageBase64());
                }
                items.set(i, updatedItem);

                Item selected = itemListView.getSelectionModel().getSelectedItem();
                if (selected != null && Objects.equals(selected.getId(), updatedItem.getId())) {
                    showItemDetails(updatedItem);
                } else {
                    updateAuctionActions(itemListView.getSelectionModel().getSelectedItem());
                }
                break;
            }
        }

        if (findItemById(updatedItem.getId()) == null) {
            items.add(updatedItem);
            if (itemListView.getSelectionModel().getSelectedItem() == null) {
                itemListView.getSelectionModel().select(updatedItem);
            }
        }

        onAuctionEvent(updatedItem, eventType);
    }

    protected void onAuctionEvent(Item item, String eventType) {
    }
    protected void onBalanceUpdated(String userId, double newBalance) {
    }

    private Item toItem(Object rawItem) {
        if (rawItem == null) {
            return null;
        }
        if (rawItem instanceof Item item) {
            return item;
        }
        try {
            return GSON.fromJson(GSON.toJson(rawItem), Item.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    //Nếu có sellerUsername -> hiện @username
    //Nếu chưa có sellerUsername nhưng có sellerId -> tạm hiện id
    //Nếu cả hai đều không có -> hiện "-"
    private String formatUsername(String username, String fallbackId, String emptyText) {
        if (username != null && !username.isBlank()) {
            return "@" + username;
        }

        if (fallbackId != null && !fallbackId.isBlank()) {
            return fallbackId;
        }

        return emptyText;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    protected void updateAuctionActions(Item item) {
    }

    protected boolean isWithinBiddingWindow(Item item) {
        if (item.getStartTime() == null || item.getEndTime() == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(item.getStartTime()) && now.isBefore(item.getEndTime());
    }

    protected String formatCurrency(double amount) {
        return VND_FORMAT.format(amount) + " VND";
    }
}
