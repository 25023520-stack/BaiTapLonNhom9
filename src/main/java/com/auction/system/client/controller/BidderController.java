package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDateTime;

public class BidderController extends AuctionController {
    private final ObservableList<Bidder> bidders = FXCollections.observableArrayList();

    @FXML
    private ComboBox<Bidder> bidderSelector;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Button bidButton;

    @FXML
    @Override
    public void initialize() {
        super.initialize();
    }

    @Override
    protected void initializeAuctionActions() {
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
                setText(empty || bidder == null ? "Tài khoản đấu giá" : bidder.getFullName());
            }
        });

        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setDisable(true);
        configureCurrentUser();
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
    public void submitBid() {
        Item selectedItem = getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Bạn chưa chọn sản phẩm.");
            return;
        }

        User currentUser = AppContext.getCurrentUser();
        if (!(currentUser instanceof Bidder)) {
            showAlert(Alert.AlertType.WARNING, "Tài khoản hiện tại không có quyền đặt giá.");
            return;
        }

        double bidAmount;
        try {
            bidAmount = Double.parseDouble(bidAmountField.getText().trim());
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.ERROR, "Giá đặt không hợp lệ.");
            return;
        }
        if (bidAmount <= 0) {
            showAlert(Alert.AlertType.ERROR, "Giá đặt phải lớn hơn 0.");
            return;
        }
        if (selectedItem.getStatus() != AuctionStatus.RUNNING) {
            showAlert(Alert.AlertType.WARNING, "Phiên đấu giá hiện không mở để đặt giá.");
            return;
        }
        if (bidAmount <= selectedItem.getCurrentPrice()) {
            showAlert(Alert.AlertType.WARNING,
                    "Giá đặt phải lớn hơn giá hiện tại " + formatCurrency(selectedItem.getCurrentPrice()) + ".");
            return;
        }

        try {
            AuctionClient client = AppContext.getAuctionClient();
            bidButton.setDisable(true);
            client.send(new BidPayload(selectedItem.getId(), bidAmount));
            ResponsePayload response = readResponse(client);
            if (!response.isSuccess()) {
                updateAuctionActions(selectedItem);
                showAlert(Alert.AlertType.ERROR, response.getMessage());
                return;
            }

            loadItems();
            Item refreshedItem = findItemById(selectedItem.getId());
            selectItem(refreshedItem);
            showItemDetails(refreshedItem);
            bidAmountField.clear();
            showAlert(Alert.AlertType.INFORMATION, "Đặt giá thành công.");
        } catch (IOException exception) {
            updateAuctionActions(selectedItem);
            showAlert(Alert.AlertType.ERROR, "Không thể kết nối tới server.");
        } catch (RuntimeException exception) {
            updateAuctionActions(selectedItem);
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    @Override
    protected void updateAuctionActions(Item item) {
        User currentUser = AppContext.getCurrentUser();
        boolean canBid = currentUser instanceof Bidder
                && item != null
                && item.getStatus() == AuctionStatus.RUNNING
                && isWithinBiddingWindow(item);
        bidButton.setDisable(!canBid);
        bidAmountField.setDisable(!(currentUser instanceof Bidder));
        if (canBid) {
            bidAmountField.setPromptText("Giá phải lớn hơn " + formatCurrency(item.getCurrentPrice()));
        } else if (currentUser instanceof Bidder && item != null && item.getStartTime() != null
                && LocalDateTime.now().isBefore(item.getStartTime())) {
            bidAmountField.setPromptText("Phiên đấu giá chưa bắt đầu");
        } else if (currentUser instanceof Bidder && item != null && item.getEndTime() != null
                && !LocalDateTime.now().isBefore(item.getEndTime())) {
            bidAmountField.setPromptText("Phiên đấu giá đã kết thúc");
        } else if (currentUser instanceof Bidder) {
            bidAmountField.setPromptText("Phiên này không mở để đặt giá");
        } else {
            bidAmountField.setPromptText("Chỉ tài khoản bidder mới đặt giá được");
        }
    }
}
