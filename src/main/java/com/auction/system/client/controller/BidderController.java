package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.payload.BidPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.common.payload.AutoBidPayload;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.user.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.time.LocalDateTime;

public class BidderController extends AuctionController {
    private final ObservableList<Bidder> bidders = FXCollections.observableArrayList();

    @FXML
    private ComboBox<Bidder> bidderSelector;

    @FXML
    private TextField maxBidField;

    @FXML
    private TextField incrementField;

    @FXML
    private Button autoBidButton;

    @FXML
    private Button cancelAutoBidButton;

    @FXML
    private Label autoBidStatusValue;

    @FXML
    private TextField bidAmountField;

    @FXML
    private Button bidButton;

    @FXML
    private Label balanceValue;

    @FXML
    private TextField depositAmountField;

    @FXML
    private Button depositButton;

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

        autoBidButton.setMaxWidth(Double.MAX_VALUE);
        autoBidButton.setDisable(true);

        cancelAutoBidButton.setMaxWidth(Double.MAX_VALUE);
        cancelAutoBidButton.setDisable(true);
        updateAutoBidStatus(null);

        configureCurrentUser();
    }

    private void configureCurrentUser() {
        User currentUser = AppContext.getCurrentUser();
        if (currentUser instanceof Bidder bidder) {
            bidders.setAll(bidder);
            bidderSelector.getSelectionModel().selectFirst();
            bidderSelector.setDisable(true);

            bidAmountField.setDisable(false);
            maxBidField.setDisable(false);
            incrementField.setDisable(false);

            balanceValue.setText(formatCurrency(bidder.getBalance()));
            depositAmountField.setDisable(false);
            depositButton.setDisable(false);
            return;
        }

        bidders.clear();
        bidderSelector.setDisable(true);
        bidAmountField.setDisable(true);
        bidButton.setDisable(true);
        balanceValue.setText("-");
        depositAmountField.setDisable(true);
        depositButton.setDisable(true);
    }

    @FXML
    public void submitDepositRequest() {
        User currentUser = AppContext.getCurrentUser();
        if (!(currentUser instanceof Bidder)) {
            showAlert(Alert.AlertType.WARNING, "Tai khoan hien tai khong phai bidder.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(depositAmountField.getText().trim());
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.ERROR, "So tien nap khong hop le.");
            return;
        }
        if (amount <= 0) {
            showAlert(Alert.AlertType.ERROR, "So tien nap phai lon hon 0.");
            return;
        }

        try {
            AuctionClient client = AppContext.getAuctionClient();
            Payload payload = new Payload(PayloadType.REQUEST_DEPOSIT);
            payload.put("amount", amount);
            depositButton.setDisable(true);
            client.send(payload);
            ResponsePayload response = readResponse(client);
            if (!response.isSuccess()) {
                showAlert(Alert.AlertType.ERROR, response.getMessage());
                return;
            }

            // Bidder chi gui yeu cau; so du chi thay doi sau khi admin duyet.
            depositAmountField.clear();
            showAlert(Alert.AlertType.INFORMATION, "Da gui yeu cau nap tien cho admin duyet.");
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Khong the ket noi toi server.");
        } finally {
            depositButton.setDisable(false);
        }
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

    @FXML
    public void cancelAutoBid() {
        Item selectedItem = getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Bạn chưa chọn sản phẩm.");
            return;
        }

        User currentUser = AppContext.getCurrentUser();
        if (!(currentUser instanceof Bidder)) {
            showAlert(Alert.AlertType.WARNING, "Tài khoản hiện tại không có quyền hủy đấu giá tự động.");
            return;
        }

        try {
            AuctionClient client = AppContext.getAuctionClient();
            cancelAutoBidButton.setDisable(true);
            client.send(AutoBidPayload.cancel(selectedItem.getId()));
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
            showAlert(Alert.AlertType.INFORMATION, "Đã hủy đấu giá tự động.");
        } catch (IOException exception) {
            updateAuctionActions(selectedItem);
            showAlert(Alert.AlertType.ERROR, "Không thể kết nối tới server.");
        } catch (RuntimeException exception) {
            updateAuctionActions(selectedItem);
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    @FXML
    public void submitAutoBid() {
        Item selectedItem = getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Bạn chưa chọn sản phẩm.");
            return;
        }

        User currentUser = AppContext.getCurrentUser();
        if (!(currentUser instanceof Bidder)) {
            showAlert(Alert.AlertType.WARNING, "Tài khoản hiện tại không có quyền bật đấu giá tự động.");
            return;
        }

        double maxBid;
        double incrementAmount;

        try {
            maxBid = Double.parseDouble(maxBidField.getText().trim());
            incrementAmount = Double.parseDouble(incrementField.getText().trim());
        } catch (NumberFormatException exception) {
            showAlert(Alert.AlertType.ERROR, "Giá tối đa và bước tăng giá phải là số hợp lệ.");
            return;
        }

        if (selectedItem.getStatus() != AuctionStatus.RUNNING) {
            showAlert(Alert.AlertType.WARNING, "Phiên đấu giá hiện không mở để bật đấu giá tự động.");
            return;
        }

        if (maxBid <= selectedItem.getCurrentPrice()) {
            showAlert(Alert.AlertType.WARNING,
                    "Giá tối đa phải lớn hơn giá hiện tại " + formatCurrency(selectedItem.getCurrentPrice()) + ".");
            return;
        }

        if (incrementAmount <= 0) {
            showAlert(Alert.AlertType.ERROR, "Bước tăng giá phải lớn hơn 0.");
            return;
        }

        try {
            AuctionClient client = AppContext.getAuctionClient();
            autoBidButton.setDisable(true);

            client.send(new AutoBidPayload(selectedItem.getId(), maxBid, incrementAmount));
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

            maxBidField.clear();
            incrementField.clear();

            showAlert(Alert.AlertType.INFORMATION, "Đã bật đấu giá tự động.");
        } catch (IOException exception) {
            updateAuctionActions(selectedItem);
            showAlert(Alert.AlertType.ERROR, "Không thể kết nối tới server.");
        } catch (RuntimeException exception) {
            updateAuctionActions(selectedItem);
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    @Override
    protected void onAuctionEvent(Item item, String eventType) {
        if (!"AUCTION_FINISHED".equals(eventType)) return;

        User currentUser = AppContext.getCurrentUser();
        if (!(currentUser instanceof Bidder bidder)) return;
        if (!bidder.getId().equals(item.getHighestBidderId())) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Bạn đã thắng đấu giá!");
        alert.setHeaderText("Chúc mừng! Bạn đã thắng phiên đấu giá: " + item.getName());
        alert.setContentText("Giá cuối cùng: " + formatCurrency(item.getCurrentPrice()) + "\nBạn có muốn thanh toán không?");

        ButtonType payButton = new ButtonType("Thanh toán", ButtonBar.ButtonData.YES);
        ButtonType declineButton = new ButtonType("Từ chối", ButtonBar.ButtonData.NO);
        alert.getButtonTypes().setAll(payButton, declineButton);

        alert.showAndWait().ifPresent(result -> {
            if (result == payButton) {
                submitPayment(item.getId());
            } else {
                submitDecline(item.getId());
            }
        });
    }

    private void submitPayment(String itemId) {
        try {
            AuctionClient client = AppContext.getAuctionClient();
            Payload payload = new Payload(PayloadType.MARK_AS_PAID);
            payload.put("itemId", itemId);
            client.send(payload);
            ResponsePayload response = readResponse(client);
            if (!response.isSuccess()) {
                showAlert(Alert.AlertType.ERROR, response.getMessage());
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Thanh toán thành công!");
                loadItems();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Không thể kết nối tới server.");
        }
    }

    private void submitDecline(String itemId) {
        try {
            AuctionClient client = AppContext.getAuctionClient();
            Payload payload = new Payload(PayloadType.DECLINE_WIN);
            payload.put("itemId", itemId);
            client.send(payload);
            ResponsePayload response = readResponse(client);
            if (!response.isSuccess()) {
                showAlert(Alert.AlertType.ERROR, response.getMessage());
            } else {
                showAlert(Alert.AlertType.INFORMATION, "Bạn đã từ chối thanh toán. Phiên đấu giá đã bị hủy.");
                loadItems();
            }
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Không thể kết nối tới server.");
        }
    }

    @Override
    protected void updateAuctionActions(Item item) {
        User currentUser = AppContext.getCurrentUser();

        boolean isBidder = currentUser instanceof Bidder;
        boolean canBid = isBidder
                && item != null
                && item.getStatus() == AuctionStatus.RUNNING
                && isWithinBiddingWindow(item);

        bidButton.setDisable(!canBid);
        autoBidButton.setDisable(!canBid);
        cancelAutoBidButton.setDisable(!isBidder || item == null || !item.isCurrentUserAutoBidActive());

        bidAmountField.setDisable(!isBidder);
        maxBidField.setDisable(!isBidder);
        incrementField.setDisable(!isBidder);
        updateAutoBidStatus(item);

        if (canBid) {
            bidAmountField.setPromptText("Giá phải lớn hơn " + formatCurrency(item.getCurrentPrice()));
            maxBidField.setPromptText("Giá tối đa lớn hơn " + formatCurrency(item.getCurrentPrice()));
            incrementField.setPromptText("Bước tăng mỗi lần");
        } else if (isBidder && item != null && item.getStartTime() != null
                && LocalDateTime.now().isBefore(item.getStartTime())) {
            bidAmountField.setPromptText("Phiên đấu giá chưa bắt đầu");
            maxBidField.setPromptText("Phiên đấu giá chưa bắt đầu");
            incrementField.setPromptText("Phiên đấu giá chưa bắt đầu");
        } else if (isBidder && item != null && item.getEndTime() != null
                && !LocalDateTime.now().isBefore(item.getEndTime())) {
            bidAmountField.setPromptText("Phiên đấu giá đã kết thúc");
            maxBidField.setPromptText("Phiên đấu giá đã kết thúc");
            incrementField.setPromptText("Phiên đấu giá đã kết thúc");
        } else if (isBidder) {
            bidAmountField.setPromptText("Phiên này không mở để đặt giá");
            maxBidField.setPromptText("Phiên này không mở để auto-bid");
            incrementField.setPromptText("Phiên này không mở để auto-bid");
        } else {
            bidAmountField.setPromptText("Chỉ tài khoản bidder mới đặt giá được");
            maxBidField.setPromptText("Chỉ tài khoản bidder mới dùng auto-bid");
            incrementField.setPromptText("Chỉ tài khoản bidder mới dùng auto-bid");
        }
    }

    private void updateAutoBidStatus(Item item) {
        if (autoBidStatusValue == null) {
            return;
        }
        if (item == null || !item.isCurrentUserAutoBidActive()) {
            autoBidStatusValue.setText("Chưa bật");
            return;
        }
        autoBidStatusValue.setText(
                "Đang bật | Max: " + formatCurrency(item.getCurrentUserAutoBidMaxBid())
                        + " | Bước: " + formatCurrency(item.getCurrentUserAutoBidIncrementAmount())
        );
    }
    @Override
    protected void onBalanceUpdated(String userId, double newBalance) {
        User currentUser = AppContext.getCurrentUser();
        if (currentUser == null || !currentUser.getId().equals(userId)) return;
        currentUser.setBalance(newBalance);
        balanceValue.setText(formatCurrency(newBalance));
    }
}
