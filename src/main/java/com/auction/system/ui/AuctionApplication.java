package com.auction.system.ui;

import com.auction.system.manager.AuctionManager;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.user.Bidder;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Seller;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import java.time.LocalDateTime;

public class AuctionApplication extends Application {
    private final AuctionManager auctionManager = AuctionManager.getInstance();
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private final ObservableList<Bidder> bidders = FXCollections.observableArrayList();

    private final ListView<Item> itemListView = new ListView<>();
    private final Label nameValue = new Label("-");
    private final Label priceValue = new Label("-");
    private final Label statusValue = new Label("-");
    private final Label sellerValue = new Label("-");
    private final Label leaderValue = new Label("-");
    private final Label scheduleValue = new Label("-");
    private final TextArea descriptionArea = new TextArea();
    private final TextArea bidHistoryArea = new TextArea();
    private final ComboBox<Bidder> bidderSelector = new ComboBox<>();
    private final TextField bidAmountField = new TextField();

    @Override
    public void start(Stage stage) {
        seedDemoData();

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
                setText(empty || bidder == null ? "Chon nguoi dau gia" : bidder.getFullName());
            }
        });

        descriptionArea.setEditable(false);
        descriptionArea.setWrapText(true);
        bidHistoryArea.setEditable(false);
        bidHistoryArea.setWrapText(true);

        Button bidButton = new Button("Dat gia");
        bidButton.setMaxWidth(Double.MAX_VALUE);
        bidButton.setOnAction(event -> submitBid());

        VBox detailPanel = buildDetailPanel(bidButton);
        BorderPane root = new BorderPane();
        root.setLeft(buildListPanel());
        root.setCenter(detailPanel);
        root.setStyle("-fx-background-color: linear-gradient(to bottom, #f7efe5, #ead7c3);");

        itemListView.getSelectionModel().selectFirst();

        Scene scene = new Scene(root, 980, 620);
        stage.setTitle("Auction System Demo");
        stage.setScene(scene);
        stage.show();
    }

    private VBox buildListPanel() {
        Label title = new Label("Danh sach phien dau gia");
        title.setFont(Font.font("Arial", 22));

        Label subtitle = new Label("Chon san pham de xem thong tin va dat gia.");

        VBox panel = new VBox(12, title, subtitle, itemListView);
        panel.setPadding(new Insets(20));
        panel.setPrefWidth(360);
        VBox.setVgrow(itemListView, Priority.ALWAYS);
        panel.setStyle("-fx-background-color: rgba(255,255,255,0.72);");
        return panel;
    }

    private VBox buildDetailPanel(Button bidButton) {
        Label title = new Label("Chi tiet phien");
        title.setFont(Font.font("Arial", 24));

        VBox infoBox = new VBox(
                10,
                infoRow("Ten san pham", nameValue),
                infoRow("Gia hien tai", priceValue),
                infoRow("Trang thai", statusValue),
                infoRow("Nguoi ban", sellerValue),
                infoRow("Nguoi dang dan", leaderValue),
                infoRow("Thoi gian", scheduleValue)
        );

        descriptionArea.setPrefRowCount(4);
        bidHistoryArea.setPrefRowCount(10);
        bidderSelector.setPrefWidth(Double.MAX_VALUE);
        bidAmountField.setPromptText("Nhap gia ban muon dat");

        VBox form = new VBox(
                10,
                new Label("Mo ta"),
                descriptionArea,
                new Label("Nguoi dau gia"),
                bidderSelector,
                new Label("Gia moi"),
                bidAmountField,
                bidButton,
                new Label("Lich su dau gia"),
                bidHistoryArea
        );

        VBox panel = new VBox(16, title, infoBox, form);
        panel.setPadding(new Insets(24));
        VBox.setVgrow(descriptionArea, Priority.NEVER);
        VBox.setVgrow(bidHistoryArea, Priority.ALWAYS);
        panel.setStyle("-fx-background-color: rgba(255,248,240,0.88);");
        return panel;
    }

    private HBox infoRow(String labelText, Label valueLabel) {
        Label label = new Label(labelText + ":");
        label.setPrefWidth(120);
        valueLabel.setWrapText(true);

        HBox row = new HBox(10, label, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
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
        sellerValue.setText(item.getSellerId() == null ? "-" : item.getSellerId());
        leaderValue.setText(item.getHighestBidderId() == null ? "Chua co" : item.getHighestBidderId());
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

    private void submitBid() {
        Item selectedItem = itemListView.getSelectionModel().getSelectedItem();
        Bidder selectedBidder = bidderSelector.getValue();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Ban chua chon san pham.");
            return;
        }
        if (selectedBidder == null) {
            showAlert(Alert.AlertType.WARNING, "Ban chua chon nguoi dau gia.");
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
            auctionManager.placeBid(selectedItem.getId(), selectedBidder, bidAmount);
            itemListView.refresh();
            showItemDetails(selectedItem);
            bidAmountField.clear();
            showAlert(Alert.AlertType.INFORMATION, "Dat gia thanh cong.");
        } catch (RuntimeException exception) {
            showAlert(Alert.AlertType.ERROR, exception.getMessage());
        }
    }

    private void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void seedDemoData() {
        Seller seller1 = new Seller("S1", "Nguyen Van Seller", "seller1", "123456");
        Seller seller2 = new Seller("S2", "Tran Thi Seller", "seller2", "123456");
        Bidder bidder1 = new Bidder("B1", "Pham Minh An", "bidder1", "123456");
        Bidder bidder2 = new Bidder("B2", "Le Thu Ha", "bidder2", "123456");
        Bidder bidder3 = new Bidder("B3", "Do Quang Huy", "bidder3", "123456");

        auctionManager.register(seller1);
        auctionManager.register(seller2);
        auctionManager.registerUser(bidder1);
        auctionManager.registerUser(bidder2);
        auctionManager.registerUser(bidder3);

        Item laptop = new Item("1", "Laptop Gaming", "Laptop RTX 4060, RAM 16GB, SSD 1TB.", 15000000, 0, AuctionStatus.OPEN);
        Item phone = new Item("2", "IPhone 14", "May cu 99%, pin 90%, phu kien day du.", 11000000, 0, AuctionStatus.OPEN);
        Item camera = new Item("3", "May anh Sony", "Sony A6400 kem lens kit, hoat dong tot.", 13000000, 0, AuctionStatus.OPEN);

        auctionManager.addItem(laptop, seller1);
        auctionManager.addItem(phone, seller1);
        auctionManager.addItem(camera, seller2);

        LocalDateTime now = LocalDateTime.now();
        auctionManager.startAuction("1", now.minusHours(1), now.plusHours(6));
        auctionManager.startAuction("2", now.minusMinutes(30), now.plusHours(4));
        auctionManager.startAuction("3", now.minusMinutes(15), now.plusHours(2));

        auctionManager.placeBid("1", bidder1, 16000000);
        auctionManager.placeBid("1", bidder2, 16800000);
        auctionManager.placeBid("2", bidder3, 11800000);

        bidders.setAll(bidder1, bidder2, bidder3);
        items.setAll(auctionManager.getAllItems());
    }

    public static void main(String[] args) {
        launch(args);
    }
}
