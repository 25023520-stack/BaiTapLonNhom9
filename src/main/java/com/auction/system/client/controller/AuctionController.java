package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.User;
import com.google.gson.Gson;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AuctionController {
    private static final Gson GSON = GsonProvider.get();
    private static final DecimalFormat VND_FORMAT =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(new Locale("vi", "VN")));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private final ObservableList<Bidder> bidders = FXCollections.observableArrayList();
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
    private ComboBox<Bidder> bidderSelector;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Button bidButton;

    @FXML
    void initialize() {
        itemListView.setItems(items);
        itemListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item.getName() + " | " + formatCurrency(item.getCurrentPrice()) + " | " + item.getStatus());
            }
        });
        itemListView.getSelectionModel().selectedItemProperty().addListener((obs, oldItem, newItem) -> showItemDetails(newItem));

        bidderSelector.setItems(bidders);
        bidderSelector.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Bidder bidder, boolean empty) {
                super.updateItem(bidder, empty);
                setText(empty || bidder == null ? null : bidder.getFullName());
            }
        });
        bidderSelector.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Bidder bidder, boolean empty) {
                super.updateItem(bidder, empty);
                setText(empty || bidder == null ? "Tai khoan dau gia" : bidder.getFullName());
            }
        });

        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        bidHistoryArea.setEditable(false);
        bidHistoryArea.setWrapText(true);
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setDisable(true);
        configureCurrentUser();
        startCountdownTimer();
        startRealtimeListener();
        loadItems();
    }

    private void configureCurrentUser() {
        User currentUser = AppContext.getCurrentUser();
        if (currentUser instanceof Bidder bidder) {
            bidders.setAll(bidder);
            bidderSelector.getSelectionModel().selectFirst();
            bidderSelector.setDisable(true);
            bidAmountField.setDisable(false);
            return;
        }

        bidders.clear();
        bidderSelector.setDisable(true);
        bidAmountField.setDisable(true);
        bidButton.setDisable(true);
    }

    @FXML
    void submitBid() {
        Item selectedItem = itemListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Ban chua chon san pham.");
            return;
        }

        User currentUser = AppContext.getCurrentUser();
        if (!(currentUser instanceof Bidder bidder)) {
            showAlert(Alert.AlertType.WARNING, "Tai khoan hien tai khong co quyen dat gia.");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountField.getText().trim());
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.ERROR, "Gia dat khong hop le.");
            return;
        }
        if (bidAmount <= 0) {
            showAlert(Alert.AlertType.ERROR, "Gia dat phai lon hon 0.");
            return;
        }
        if (selectedItem.getStatus() != AuctionStatus.RUNNING) {
            showAlert(Alert.AlertType.WARNING, "Phien dau gia hien khong mo de dat gia.");
            return;
        }
        if (bidAmount <= selectedItem.getCurrentPrice()) {
            showAlert(Alert.AlertType.WARNING,
                    "Gia dat phai lon hon gia hien tai " + formatCurrency(selectedItem.getCurrentPrice()) + ".");
            return;
        }

        try {
            AuctionClient client = AppContext.getAuctionClient();
            bidButton.setDisable(true);
            client.send(new BidPayload(selectedItem.getId(), bidAmount));
            ResponsePayload response = readResponse(client);
            if (!response.isSuccess()) {
                updateBidControls(selectedItem);
                showAlert(Alert.AlertType.ERROR, response.getMessage());
                return;
            }

            loadItems();
            itemListView.getSelectionModel().select(findItemById(selectedItem.getId()));
            showItemDetails(findItemById(selectedItem.getId()));
            bidAmountField.clear();
            showAlert(Alert.AlertType.INFORMATION, "Dat gia thanh cong.");
        } catch (IOException exception) {
            updateBidControls(selectedItem);
            showAlert(Alert.AlertType.ERROR, "Khong the ket noi toi server.");
        } catch (RuntimeException exception) {
            updateBidControls(selectedItem);
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    private void showItemDetails(Item item) {
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
            updateBidControls(null);
            return;
        }

        nameValue.setText(item.getName());
        priceValue.setText(formatCurrency(item.getCurrentPrice()));
        statusValue.setText(item.getStatus().name());
        sellerValue.setText(isBlank(item.getSellerId()) ? "-" : item.getSellerId());
        leaderValue.setText(isBlank(item.getHighestBidderId()) ? "Chua co" : item.getHighestBidderId());
        scheduleValue.setText(formatSchedule(item));
        updateCountdown(item);
        descriptionArea.setText(item.getDescription());
        bidHistoryArea.setText(formatBidHistory(item));
        updateBidControls(item);
    }

    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            Item selectedItem = itemListView.getSelectionModel().getSelectedItem();
            updateCountdown(selectedItem);
            updateBidControls(selectedItem);
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
            countdownValue.setText("Bat dau sau " + formatRemainingTime(java.time.Duration.between(now, item.getStartTime())));
            return;
        }
        if (now.isBefore(item.getEndTime())) {
            countdownValue.setText("Ket thuc sau " + formatRemainingTime(java.time.Duration.between(now, item.getEndTime())));
            return;
        }

        countdownValue.setText("Da ket thuc");
    }

    private String formatRemainingTime(java.time.Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%d ngay %02d:%02d:%02d", days, hours, minutes, seconds);
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
            return "Chua co luot dat gia nao.";
        }

        StringBuilder builder = new StringBuilder();
        for (Bid bid : item.getBidHistory()) {
            builder.append(bid.getTimestamp())
                    .append(" | ")
                    .append(bid.getBidder().getId())
                    .append(" | ")
                    .append(formatCurrency(bid.getAmount()))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void loadItems() {
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

            showAlert(Alert.AlertType.ERROR, "Du lieu san pham tu server khong hop le.");
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong the tai danh sach san pham tu server.");
        }
    }

    private ResponsePayload readResponse(AuctionClient client) throws IOException {
        Payload raw = client.read();
        if (raw == null) {
            throw new IOException("Server returned null");
        }

        ResponsePayload response = new ResponsePayload();
        response.setType(raw.getType());
        raw.getBody().forEach(response::put);
        return response;
    }

    private Item findItemById(String itemId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElse(null);
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
                    error -> Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Mat ket noi toi server"))
            );
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Khong the bat dau lang nghe server.");
        }
    }

    private void handleAuctionUpdate(ResponsePayload update) {
        Object rawItem = update.getBody().get("item");
        Item updatedItem = toItem(rawItem);
        if (updatedItem == null) {
            return;
        }

        for (int i = 0; i < items.size(); i++) {
            if (Objects.equals(items.get(i).getId(), updatedItem.getId())) {
                items.set(i, updatedItem);

                Item selected = itemListView.getSelectionModel().getSelectedItem();
                if (selected != null && Objects.equals(selected.getId(), updatedItem.getId())) {
                    showItemDetails(updatedItem);
                } else {
                    updateBidControls(itemListView.getSelectionModel().getSelectedItem());
                }
                break;
            }
        }
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private void updateBidControls(Item item) {
        User currentUser = AppContext.getCurrentUser();
        boolean canBid = currentUser instanceof Bidder
                && item != null
                && item.getStatus() == AuctionStatus.RUNNING
                && isWithinBiddingWindow(item);
        bidButton.setDisable(!canBid);
        bidAmountField.setDisable(!(currentUser instanceof Bidder));
        if (canBid) {
            bidAmountField.setPromptText("Gia phai lon hon " + formatCurrency(item.getCurrentPrice()));
        } else if (currentUser instanceof Bidder && item != null && item.getStartTime() != null
                && LocalDateTime.now().isBefore(item.getStartTime())) {
            bidAmountField.setPromptText("Phien dau gia chua bat dau");
        } else if (currentUser instanceof Bidder && item != null && item.getEndTime() != null
                && !LocalDateTime.now().isBefore(item.getEndTime())) {
            bidAmountField.setPromptText("Phien dau gia da ket thuc");
        } else if (currentUser instanceof Bidder) {
            bidAmountField.setPromptText("Phien nay khong mo de dat gia");
        } else {
            bidAmountField.setPromptText("Chi bidder moi dat gia duoc");
        }
    }

    private boolean isWithinBiddingWindow(Item item) {
        if (item.getStartTime() == null || item.getEndTime() == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(item.getStartTime()) && now.isBefore(item.getEndTime());
    }

    private String formatCurrency(double amount) {
        return VND_FORMAT.format(amount) + " VND";
    }
}
