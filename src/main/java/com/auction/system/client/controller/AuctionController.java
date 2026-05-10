package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.User;
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
import javafx.application.Platform;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class AuctionController {
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private final ObservableList<Bidder> bidders = FXCollections.observableArrayList();

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
                setText(item.getName() + " | " + item.getCurrentPrice() + " VND | " + item.getStatus());
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
        configureCurrentUser();
        loadItems();
        startRealtimeListener();
    }

    private void configureCurrentUser() {
        User currentUser = AppContext.getCurrentUser();
        if (currentUser instanceof Bidder bidder) {
            bidders.setAll(bidder);
            bidderSelector.getSelectionModel().selectFirst();
            bidderSelector.setDisable(true);
            return;
        }

        bidders.clear();
        bidderSelector.setDisable(true);
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

        try {
            AuctionClient client = AppContext.getAuctionClient();
            client.send(new BidPayload(selectedItem.getId(), bidAmount));
            ResponsePayload response = readResponse(client);
            if (!response.isSuccess()) {
                showAlert(Alert.AlertType.ERROR, response.getMessage());
                return;
            }

            loadItems();
            itemListView.getSelectionModel().select(findItemById(selectedItem.getId()));
            showItemDetails(findItemById(selectedItem.getId()));
            bidAmountField.clear();
            showAlert(Alert.AlertType.INFORMATION, "Dat gia thanh cong.");
        } catch (IOException | ClassNotFoundException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong the ket noi toi server.");
        } catch (RuntimeException exception) {
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
            descriptionArea.clear();
            bidHistoryArea.clear();
            return;
        }

        nameValue.setText(item.getName());
        priceValue.setText(item.getCurrentPrice() + " VND");
        statusValue.setText(item.getStatus().name());
        sellerValue.setText(isBlank(item.getSellerId()) ? "-" : item.getSellerId());
        leaderValue.setText(isBlank(item.getHighestBidderId()) ? "Chua co" : item.getHighestBidderId());
        scheduleValue.setText(formatSchedule(item));
        descriptionArea.setText(item.getDescription());
        bidHistoryArea.setText(formatBidHistory(item));
    }

    private String formatSchedule(Item item) {
        if (item.getStartTime() == null || item.getEndTime() == null) {
            return "-";
        }
        return item.getStartTime() + " -> " + item.getEndTime();
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
                    .append(bid.getAmount())
                    .append(" VND")
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
                        .filter(Item.class::isInstance)
                        .map(Item.class::cast)
                        .toList());
                itemListView.getSelectionModel().selectFirst();
                showItemDetails(itemListView.getSelectionModel().getSelectedItem());
                return;
            }

            showAlert(Alert.AlertType.ERROR, "Du lieu san pham tu server khong hop le.");
        } catch (IOException | ClassNotFoundException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong the tai danh sach san pham tu server.");
        }
    }

    private ResponsePayload readResponse(AuctionClient client) throws IOException, ClassNotFoundException {
        if (client.read() instanceof ResponsePayload response) {
            if (response.getType() == PayloadType.UPDATE_AUCTION) {
                return readResponse(client);
            }
            return response;
        }
        throw new IOException("Unexpected payload received from server");
    }

    private Item findItemById(String itemId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElse(null);
    }
    // Bắt đầu lắng nghe update realtime từ server
    private void startRealtimeListener() {
        try {
            AuctionClient client = AppContext.getAuctionClient();
            client.startListening(
                    payload -> {
                        // Chạy trên listener thread, cần dùng Platform.runLater
                        // để cập nhật UI trên JavaFX thread
                        if (payload instanceof ResponsePayload rp
                                && rp.getType() == PayloadType.UPDATE_AUCTION) {
                            Platform.runLater(() -> handleAuctionUpdate(rp));
                        }
                    },
                    error -> Platform.runLater(() ->
                            showAlert(Alert.AlertType.ERROR, "Mất kết nối tới server"))
            );
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Không thể bắt đầu lắng nghe server.");
        }
    }

    // Xử lý khi nhận được update từ server
    private void handleAuctionUpdate(ResponsePayload update) {
        Object rawItem = update.getBody().get("item");
        if (!(rawItem instanceof Item updatedItem)) return;

        // Tìm item cũ trong danh sách, thay bằng item mới
        for (int i = 0; i < items.size(); i++) {
            if (Objects.equals(items.get(i).getId(), updatedItem.getId())) {
                items.set(i, updatedItem);

                // Nếu đang xem đúng item này thì cập nhật panel chi tiết luôn
                Item selected = itemListView.getSelectionModel().getSelectedItem();
                if (selected != null && Objects.equals(selected.getId(), updatedItem.getId())) {
                    showItemDetails(updatedItem);
                }
                break;
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
