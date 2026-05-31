package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Admin;
import com.auction.system.model.payment.DepositRequest;
import com.auction.system.model.user.User;
import com.google.gson.Gson;
import javafx.animation.Animation;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class AdminController {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminController.class);
    private static final Duration REFRESH_INTERVAL = Duration.seconds(5);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final Gson GSON = GsonProvider.get();

    @FXML private Label adminNameLabel;
    @FXML private Label sellerCountLabel;
    @FXML private Label auctionCountLabel;
    @FXML private Label depositCountLabel;
    @FXML private ListView<User> sellerListView;
    @FXML private ListView<Item> auctionListView;
    @FXML private ListView<DepositRequest> depositListView;
    @FXML private Label sellerNameValue;
    @FXML private Label sellerUsernameValue;
    @FXML private Label sellerEmailValue;
    @FXML private Label sellerStatusValue;
    @FXML private Label itemNameValue;
    @FXML private Label itemSellerValue;
    @FXML private Label itemScheduleValue;
    @FXML private Label itemStatusValue;
    @FXML private TextArea itemDescriptionArea;
    @FXML private ImageView itemImageView;
    @FXML private Button approveSellerButton;
    @FXML private Button approveAuctionButton;
    @FXML private Button rejectAuctionButton;
    @FXML private Label depositBidderValue;
    @FXML private Label depositAmountValue;
    @FXML private Label depositCreatedValue;
    @FXML private Label depositStatusValue;
    @FXML private Button approveDepositButton;
    @FXML private Button btnShowSellers;
    @FXML private Button btnShowAuctions;
    @FXML private Button btnShowDeposits;
    @FXML private VBox sellerListContainer;
    @FXML private VBox auctionListContainer;
    @FXML private VBox depositListContainer;
    @FXML private VBox sellerDetailPane;
    @FXML private VBox auctionDetailPane;
    @FXML private VBox depositDetailPane;

    private final ObservableList<User> pendingSellers = FXCollections.observableArrayList();
    private final ObservableList<Item> pendingAuctions = FXCollections.observableArrayList();
    private final ObservableList<DepositRequest> pendingDeposits = FXCollections.observableArrayList();
    private Timeline autoRefresh;
    private final String[] previousSellersSig  = {null};
    private final String[] previousAuctionsSig = {null};
    private final String[] previousDepositsSig = {null};

    @FXML
    public void initialize() {
        User currentUser = AppContext.getCurrentUser();
        if (!(currentUser instanceof Admin admin)) {
            showError("Phien dang nhap", "Vui long dang nhap voi tai khoan admin.");
            return;
        }

        adminNameLabel.setText(admin.getFullName());
        itemDescriptionArea.setEditable(false);
        itemDescriptionArea.setWrapText(true);
        configureLists();
        clearSellerDetails();
        clearAuctionDetails();
        clearDepositDetails();
        initTabs();
        refreshDashboard();

        autoRefresh = new Timeline(new KeyFrame(REFRESH_INTERVAL, event -> refreshDashboard()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();
    }

    @FXML
    public void approveSeller() {
        updateSellerApproval(true);
    }

    @FXML
    public void approveAuction() {
        updateAuctionApproval(true);
    }

    @FXML
    public void rejectAuction() {
        updateAuctionApproval(false);
    }

    @FXML
    public void approveDeposit() {
        updateDepositApproval();
    }

    @FXML
    public void refreshDashboard() {
        String selectedSellerId  = sellerListView.getSelectionModel().getSelectedItem()  == null ? null : sellerListView.getSelectionModel().getSelectedItem().getId();
        String selectedItemId    = auctionListView.getSelectionModel().getSelectedItem() == null ? null : auctionListView.getSelectionModel().getSelectedItem().getId();
        String selectedDepositId = depositListView.getSelectionModel().getSelectedItem() == null ? null : depositListView.getSelectionModel().getSelectedItem().getId();

        runAsync(new Payload(PayloadType.ADMIN_DASHBOARD), response -> {
            if (!response.isSuccess()) {
                showError("Dashboard", response.getMessage());
                return;
            }

            List<User> newSellers = readList(response.getBody().get("pendingSellers"), this::toUser);
            updateListIfChanged(newSellers,
                    computeSignature(newSellers, u -> u.getId() + "|" + u.getUserName() + "|" + u.getFullName() + "|" + u.isApproved()),
                    previousSellersSig, pendingSellers, sellerListView, sellerCountLabel);

            List<Item> newAuctions = readList(response.getBody().get("pendingAuctions"), this::toItem);
            updateListIfChanged(newAuctions,
                    computeSignature(newAuctions, i -> i.getId() + "|" + i.getName() + "|" + i.getStatus() + "|" + i.isAuctionApproved() + "|" + i.getStartTime() + "|" +
                            i.getEndTime()),
                    previousAuctionsSig, pendingAuctions, auctionListView, auctionCountLabel);

            List<DepositRequest> newDeposits = readList(response.getBody().get("pendingDeposits"), this::toDepositRequest);
            updateListIfChanged(newDeposits,
                    computeSignature(newDeposits, r -> r.getId() + "|" + r.getBidderName() + "|" + r.getAmount() + "|" + r.getStatus()),
                    previousDepositsSig, pendingDeposits, depositListView, depositCountLabel);

            restoreSelection(selectedSellerId, pendingSellers,  User::getId, sellerListView, this::clearSellerDetails);
            restoreSelection(selectedItemId, pendingAuctions, Item::getId, auctionListView, this::clearAuctionDetails);
            restoreSelection(selectedDepositId, pendingDeposits, DepositRequest::getId, depositListView, this::clearDepositDetails);
        });
    }

    private <T> void updateListIfChanged(
            List<T> newList,
            String newSig,
            String[] prevSigHolder,
            ObservableList<T> observable,
            ListView<T> view,
            Label countLabel) {
        if (!newSig.equals(prevSigHolder[0])) {
            prevSigHolder[0] = newSig;
            observable.setAll(newList);
            view.refresh();
        }
        countLabel.setText(String.valueOf(observable.size()));
    }


    @FXML
    public void handleLogout(ActionEvent event) {
        if (autoRefresh != null) {
            autoRefresh.stop();
        }

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
            showError("Dang xuat", "Khong the mo man hinh dang nhap.");
        }
    }

    private void configureLists() {
        configureListView(sellerListView, pendingSellers,
                u -> u.getFullName() + "  |  @" + u.getUserName(),
                (obs, old, val) -> showSellerDetails(val));

        configureListView(auctionListView, pendingAuctions,
                i -> i.getName() + "  |  " + formatSchedule(i),
                (obs, old, val) -> showAuctionDetails(val));

        configureListView(depositListView, pendingDeposits,
                r -> r.getBidderName() + "  |  " + formatCurrency(r.getAmount()),
                (obs, old, val) -> showDepositDetails(val));
    }

    private <T> void configureListView(
            ListView<T> listView,
            ObservableList<T> items,
            java.util.function.Function<T, String> display,
            javafx.beans.value.ChangeListener<T> onSelect) {
        listView.setItems(items);
        listView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : display.apply(item));
            }
        });
        listView.getSelectionModel().selectedItemProperty().addListener(onSelect);
    }

    private void updateSellerApproval(boolean approved) {
        User selectedSeller = sellerListView.getSelectionModel().getSelectedItem();
        if (selectedSeller == null) {
            showError("Seller", "Ban chua chon seller can xu ly.");
            return;
        }

        Payload payload = new Payload(PayloadType.APPROVE_SELLER);
        payload.put("sellerId", selectedSeller.getId());
        payload.put("approved", approved);

        runAsync(payload, response -> {
            if (!response.isSuccess()) {
                showError("Seller", response.getMessage());
                return;
            }

            showInfo(response.getMessage());
            refreshDashboard();
        });
    }

    private void updateAuctionApproval(boolean approved) {
        Item selectedItem = auctionListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("Auction", "Ban chua chon yeu cau phien dau gia.");
            return;
        }

        Payload payload = new Payload(PayloadType.APPROVE_AUCTION);
        payload.put("itemId", selectedItem.getId());
        payload.put("approved", approved);

        runAsync(payload, response -> {
            if (!response.isSuccess()) {
                showError("Auction", response.getMessage());
                return;
            }

            showInfo(response.getMessage());
            refreshDashboard();
        });
    }

    private void updateDepositApproval() {
        // Client admin chi gui len id request can duyet.
        // Viec cong tien va khoa transaction nam o server de dam bao bao mat va nhat quan du lieu.
        DepositRequest selectedRequest = depositListView.getSelectionModel().getSelectedItem();
        if (selectedRequest == null) {
            showError("Nap tien", "Ban chua chon yeu cau nap tien.");
            return;
        }

        Payload payload = new Payload(PayloadType.APPROVE_DEPOSIT);
        payload.put("requestId", selectedRequest.getId());

        runAsync(payload, response -> {
            if (!response.isSuccess()) {
                showError("Nap tien", response.getMessage());
                return;
            }

            showInfo(response.getMessage());
            refreshDashboard();
        });
    }

    private void showSellerDetails(User seller) {
        if (seller == null) {
            clearSellerDetails();
            return;
        }

        sellerNameValue.setText(seller.getFullName());
        sellerUsernameValue.setText("@" + seller.getUserName());
        sellerEmailValue.setText(seller.getEmail());
        sellerStatusValue.setText(seller.isApproved() ? "Da duyet" : "Cho duyet");
        approveSellerButton.setDisable(false);
    }

    private void showAuctionDetails(Item item) {
        if (item == null) {
            clearAuctionDetails();
            return;
        }

        itemNameValue.setText(item.getName());
        itemSellerValue.setText(item.getSellerId());
        itemScheduleValue.setText(formatSchedule(item));
        itemStatusValue.setText(item.isAuctionApproved() ? "Da duyet" : "Cho duyet");
        itemDescriptionArea.setText(item.getDescription());
        setItemImage(item);
        approveAuctionButton.setDisable(false);
        rejectAuctionButton.setDisable(false);
    }

    private void showDepositDetails(DepositRequest request) {
        if (request == null) {
            clearDepositDetails();
            return;
        }

        depositBidderValue.setText(request.getBidderName() + " (" + request.getBidderId() + ")");
        depositAmountValue.setText(formatCurrency(request.getAmount()));
        depositCreatedValue.setText(request.getCreatedAt() == null ? "-" : DATE_TIME_FORMATTER.format(request.getCreatedAt()));
        depositStatusValue.setText(request.getStatus());
        approveDepositButton.setDisable(false);
    }

    private void clearSellerDetails() {
        sellerNameValue.setText("-");
        sellerUsernameValue.setText("-");
        sellerEmailValue.setText("-");
        sellerStatusValue.setText("-");
        approveSellerButton.setDisable(true);
    }

    private void clearAuctionDetails() {
        itemNameValue.setText("-");
        itemSellerValue.setText("-");
        itemScheduleValue.setText("-");
        itemStatusValue.setText("-");
        itemDescriptionArea.clear();
        itemImageView.setImage(null);
        approveAuctionButton.setDisable(true);
        rejectAuctionButton.setDisable(true);
    }

    private void clearDepositDetails() {
        depositBidderValue.setText("-");
        depositAmountValue.setText("-");
        depositCreatedValue.setText("-");
        depositStatusValue.setText("-");
        approveDepositButton.setDisable(true);
    }

    private void setItemImage(Item item) {
        String imageBase64 = item == null ? null : item.getImageBase64();
        if (imageBase64 == null || imageBase64.isBlank()) {
            itemImageView.setImage(null);
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            itemImageView.setImage(new Image(new ByteArrayInputStream(bytes), 72, 72, true, true));
        } catch (IllegalArgumentException exception) {
            itemImageView.setImage(null);
        }
    }

    private String formatSchedule(Item item) {
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            return "Chua co lich";
        }
        return DATE_TIME_FORMATTER.format(item.getStartTime()) + " -> "
                + DATE_TIME_FORMATTER.format(item.getEndTime());
    }

    private <T> List<T> readList(Object raw, java.util.function.Function<Object, T> converter) {
        if (!(raw instanceof List<?> list)) return List.of();
        return list.stream()
                .map(converter)
                .filter(item -> item != null)
                .toList();
    }

    @SuppressWarnings("unchecked")
    private User toUser(Object rawUser) {
        if (rawUser instanceof User user) {
            return user;
        }
        if (rawUser instanceof Map<?, ?> map) {
            return AppContext.mapToUser((Map<String, Object>) map);
        }
        return null;
    }

    private Item toItem(Object rawItem) {
        if (rawItem instanceof Item item) {
            return item;
        }
        if (rawItem == null) {
            return null;
        }
        try {
            return GSON.fromJson(GSON.toJson(rawItem), Item.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private DepositRequest toDepositRequest(Object rawRequest) {
        if (rawRequest instanceof DepositRequest request) {
            return request;
        }
        if (rawRequest == null) {
            return null;
        }
        try {
            return GSON.fromJson(GSON.toJson(rawRequest), DepositRequest.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private String formatCurrency(double amount) {
        return String.format("%,.0f VND", amount);
    }

    private void runAsync(Payload payload, Consumer<ResponsePayload> onResult) {
        Thread thread = new Thread(() -> {
            try {
                AuctionClient client = AppContext.getAuctionClient();
                Payload raw = client.request(payload);
                ResponsePayload response = new ResponsePayload();
                response.setType(raw.getType());
                raw.getBody().forEach(response::put);
                Platform.runLater(() -> onResult.accept(response));
            } catch (IOException exception) {
                LOGGER.error("Admin request failed", exception);
                Platform.runLater(() -> {
                    if (autoRefresh != null) {
                        autoRefresh.stop();
                    }
                    Stage stage = (Stage) sellerListView.getScene().getWindow();
                    AppContext.goToServerDown(stage);
                });
            }
        }, "admin-dashboard-request");
        thread.setDaemon(true);
        thread.start();
    }

    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, message);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showError(String header, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setHeaderText(header);
        alert.showAndWait();
    }

    private void initTabs() {
        showTab(sellerListContainer, sellerDetailPane, btnShowSellers);
    }

    private void showTab(VBox listContainer, VBox detail, Button activeBtn) {
        // Ẩn hết container
        sellerListContainer.setVisible(false);  sellerListContainer.setManaged(false);
        auctionListContainer.setVisible(false); auctionListContainer.setManaged(false);
        depositListContainer.setVisible(false); depositListContainer.setManaged(false);

        // Ẩn hết detail pane
        sellerDetailPane.setVisible(false);  sellerDetailPane.setManaged(false);
        auctionDetailPane.setVisible(false); auctionDetailPane.setManaged(false);
        depositDetailPane.setVisible(false); depositDetailPane.setManaged(false);

        // Reset active style
        btnShowSellers.getStyleClass().remove("queue-tab-button-active");
        btnShowAuctions.getStyleClass().remove("queue-tab-button-active");
        btnShowDeposits.getStyleClass().remove("queue-tab-button-active");

        // Hiện cái được chọn
        listContainer.setVisible(true);  listContainer.setManaged(true);
        detail.setVisible(true);         detail.setManaged(true);
        activeBtn.getStyleClass().add("queue-tab-button-active");
    }

    private <T> String computeSignature(List<T> list, java.util.function.Function<T, String> extractor) {
        if (list == null || list.isEmpty()) return "EMPTY";
        StringBuilder sb = new StringBuilder();
        for (T item : list) {
            if (item != null) sb.append(extractor.apply(item)).append(';');
        }
        return sb.toString();
    }

    private <T> void restoreSelection(
            String id,
            ObservableList<T> list,
            java.util.function.Function<T, String> idExtractor,
            ListView<T> listView,
            Runnable onClear) {
        if (id == null) {
            listView.getSelectionModel().clearSelection();
            onClear.run();
            return;
        }
        list.stream()
                .filter(item -> id.equals(idExtractor.apply(item)))
                .findFirst()
                .ifPresentOrElse(
                        item -> listView.getSelectionModel().select(item),
                        () -> { listView.getSelectionModel().clearSelection(); onClear.run(); }
                );
    }

    @FXML private void showSellerQueue()  { showTab(sellerListContainer, sellerDetailPane, btnShowSellers); }
    @FXML private void showAuctionQueue() { showTab(auctionListContainer, auctionDetailPane, btnShowAuctions); }
    @FXML private void showDepositQueue() { showTab(depositListContainer, depositDetailPane, btnShowDeposits); }
}

