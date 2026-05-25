package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Admin;
import com.auction.system.model.user.DepositRequest;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
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
    @FXML private Label userCountLabel;
    @FXML private Label itemCountLabel;
    @FXML private Label sellerCountLabel;
    @FXML private Label auctionCountLabel;
    @FXML private Label depositCountLabel;
    @FXML private ListView<User> sellerListView;
    @FXML private ListView<Item> auctionListView;
    @FXML private ListView<DepositRequest> depositListView;
    @FXML private ListView<User> allUserListView;
    @FXML private ListView<Item> allItemListView;
    @FXML private TextField adminUserSearchField;
    @FXML private ComboBox<String> adminRoleFilterComboBox;
    @FXML private TextField adminItemSearchField;
    @FXML private ComboBox<String> adminItemStatusFilterComboBox;
    @FXML private Label managedUserNameValue;
    @FXML private Label managedUserUsernameValue;
    @FXML private Label managedUserEmailValue;
    @FXML private Label managedUserRoleValue;
    @FXML private Label managedUserStatusValue;
    @FXML private Label managedUserBalanceValue;
    @FXML private Button deleteUserButton;
    @FXML private Label managedItemImagePlaceholder;
    @FXML private ImageView managedItemImageView;
    @FXML private Label managedItemNameValue;
    @FXML private Label managedItemCategoryValue;
    @FXML private Label managedItemSellerValue;
    @FXML private Label managedItemPriceValue;
    @FXML private Label managedItemStatusValue;
    @FXML private Label managedItemScheduleValue;
    @FXML private TextArea managedItemDescriptionArea;
    @FXML private Button deleteItemButton;
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
    @FXML private Button btnShowUsers;
    @FXML private Button btnShowItems;
    @FXML private VBox userListContainer;
    @FXML private VBox itemListContainer;
    @FXML private VBox sellerListContainer;
    @FXML private VBox auctionListContainer;
    @FXML private VBox depositListContainer;
    @FXML private VBox sellerDetailPane;
    @FXML private VBox auctionDetailPane;
    @FXML private VBox depositDetailPane;
    @FXML private VBox userManagementDetailPane;
    @FXML private VBox itemManagementDetailPane;

    private final ObservableList<User> allUsers = FXCollections.observableArrayList();
    private final ObservableList<User> visibleUsers = FXCollections.observableArrayList();
    private final ObservableList<Item> allItems = FXCollections.observableArrayList();
    private final ObservableList<Item> visibleItems = FXCollections.observableArrayList();
    private final ObservableList<User> pendingSellers = FXCollections.observableArrayList();
    private final ObservableList<Item> pendingAuctions = FXCollections.observableArrayList();
    private final ObservableList<DepositRequest> pendingDeposits = FXCollections.observableArrayList();
    private Timeline autoRefresh;

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
        if (managedItemDescriptionArea != null) {
            managedItemDescriptionArea.setEditable(false);
            managedItemDescriptionArea.setWrapText(true);
        }
        configureLists();
        configureFilters();
        clearUserManagementDetails();
        clearItemManagementDetails();
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
    public void deleteSelectedUser() {
        User selectedUser = allUserListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showError("Nguoi dung", "Ban chua chon nguoi dung can xoa.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xoa nguoi dung @" + selectedUser.getUserName() + "?");
        confirm.setHeaderText("Xac nhan xoa nguoi dung");
        confirm.showAndWait().ifPresent(button -> {
            if (button.getButtonData().isCancelButton()) {
                return;
            }
            Payload payload = new Payload(PayloadType.DELETE_USER);
            payload.put("userId", selectedUser.getId());
            runAsync(payload, response -> {
                if (!response.isSuccess()) {
                    showError("Nguoi dung", response.getMessage());
                    return;
                }
                showInfo(response.getMessage());
                refreshDashboard();
            });
        });
    }

    @FXML
    public void deleteSelectedItem() {
        Item selectedItem = allItemListView.getSelectionModel().getSelectedItem();
        if (selectedItem == null) {
            showError("San pham", "Ban chua chon san pham can xoa.");
            return;
        }
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Xoa san pham \"" + selectedItem.getName() + "\"?");
        confirm.setHeaderText("Xac nhan xoa san pham");
        confirm.showAndWait().ifPresent(button -> {
            if (button.getButtonData().isCancelButton()) {
                return;
            }
            Payload payload = new Payload(PayloadType.ADMIN_DELETE_ITEM);
            payload.put("itemId", selectedItem.getId());
            runAsync(payload, response -> {
                if (!response.isSuccess()) {
                    showError("San pham", response.getMessage());
                    return;
                }
                showInfo(response.getMessage());
                refreshDashboard();
            });
        });
    }

    @FXML
    public void showManagedItemImagePreview() {
        Image image = managedItemImageView == null ? null : managedItemImageView.getImage();
        if (image == null) {
            showError("Anh san pham", "San pham chua co anh de xem.");
            return;
        }
        ImageView preview = new ImageView(image);
        preview.setFitWidth(800);
        preview.setFitHeight(540);
        preview.setPreserveRatio(true);
        preview.setSmooth(true);
        StackPane content = new StackPane(preview);
        content.setPrefSize(820, 560);
        content.getStyleClass().add("image-preview-dialog");

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Xem ảnh sản phẩm");
        dialog.setHeaderText(managedItemNameValue.getText());
        dialog.getDialogPane().setContent(content);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    @FXML
    public void refreshDashboard() {
        String selectedManagedUserId = allUserListView.getSelectionModel().getSelectedItem() == null
                ? null
                : allUserListView.getSelectionModel().getSelectedItem().getId();
        String selectedManagedItemId = allItemListView.getSelectionModel().getSelectedItem() == null
                ? null
                : allItemListView.getSelectionModel().getSelectedItem().getId();
        String selectedSellerId = sellerListView.getSelectionModel().getSelectedItem() == null
                ? null
                : sellerListView.getSelectionModel().getSelectedItem().getId();
        String selectedItemId = auctionListView.getSelectionModel().getSelectedItem() == null
                ? null
                : auctionListView.getSelectionModel().getSelectedItem().getId();
        String selectedDepositId = depositListView.getSelectionModel().getSelectedItem() == null
                ? null
                : depositListView.getSelectionModel().getSelectedItem().getId();

        runAsync(new Payload(PayloadType.ADMIN_DASHBOARD), response -> {
            if (!response.isSuccess()) {
                showError("Dashboard", response.getMessage());
                return;
            }

            allUsers.setAll(readUsers(response.getBody().get("allUsers")));
            allItems.setAll(readItems(response.getBody().get("allItems")));
            pendingSellers.setAll(readUsers(response.getBody().get("pendingSellers")));
            pendingAuctions.setAll(readItems(response.getBody().get("pendingAuctions")));
            pendingDeposits.setAll(readDeposits(response.getBody().get("pendingDeposits")));
            userCountLabel.setText(String.valueOf(allUsers.size()));
            itemCountLabel.setText(String.valueOf(allItems.size()));
            sellerCountLabel.setText(String.valueOf(pendingSellers.size()));
            auctionCountLabel.setText(String.valueOf(pendingAuctions.size()));
            depositCountLabel.setText(String.valueOf(pendingDeposits.size()));

            applyUserFilters();
            applyItemFilters();
            restoreManagedUserSelection(selectedManagedUserId);
            restoreManagedItemSelection(selectedManagedItemId);
            restoreSellerSelection(selectedSellerId);
            restoreAuctionSelection(selectedItemId);
            restoreDepositSelection(selectedDepositId);
        });
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
        allUserListView.setItems(visibleUsers);
        allUserListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getFullName() + "  |  @" + user.getUserName() + "  |  " + user.getRole());
            }
        });
        allUserListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showUserManagementDetails(newValue));

        allItemListView.setItems(visibleItems);
        allItemListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + "  |  " + item.getCategory() + "  |  " + item.getStatus());
            }
        });
        allItemListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showItemManagementDetails(newValue));

        sellerListView.setItems(pendingSellers);
        sellerListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getFullName() + "  |  @" + user.getUserName());
            }
        });
        sellerListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showSellerDetails(newValue));

        auctionListView.setItems(pendingAuctions);
        auctionListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getName() + "  |  " + formatSchedule(item));
            }
        });
        auctionListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showAuctionDetails(newValue));

        depositListView.setItems(pendingDeposits);
        depositListView.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(DepositRequest request, boolean empty) {
                super.updateItem(request, empty);
                setText(empty || request == null
                        ? null
                        : request.getBidderName() + "  |  " + formatCurrency(request.getAmount()));
            }
        });
        depositListView.getSelectionModel().selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> showDepositDetails(newValue));
    }

    private void configureFilters() {
        if (adminRoleFilterComboBox != null) {
            adminRoleFilterComboBox.setItems(FXCollections.observableArrayList("ALL", "ADMIN", "SELLER", "BIDDER"));
            adminRoleFilterComboBox.getSelectionModel().select("ALL");
            adminRoleFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyUserFilters());
        }
        if (adminUserSearchField != null) {
            adminUserSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyUserFilters());
        }
        if (adminItemStatusFilterComboBox != null) {
            adminItemStatusFilterComboBox.setItems(FXCollections.observableArrayList("ALL", "OPEN", "RUNNING", "FINISHED", "PAID", "CANCELED"));
            adminItemStatusFilterComboBox.getSelectionModel().select("ALL");
            adminItemStatusFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyItemFilters());
        }
        if (adminItemSearchField != null) {
            adminItemSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyItemFilters());
        }
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

    private void applyUserFilters() {
        String query = adminUserSearchField == null ? "" : nullToEmpty(adminUserSearchField.getText()).trim().toLowerCase();
        String role = adminRoleFilterComboBox == null ? "ALL" : adminRoleFilterComboBox.getValue();
        visibleUsers.setAll(allUsers.stream()
                .filter(user -> role == null || "ALL".equals(role) || role.equalsIgnoreCase(user.getRole()))
                .filter(user -> query.isEmpty()
                        || nullToEmpty(user.getFullName()).toLowerCase().contains(query)
                        || nullToEmpty(user.getUserName()).toLowerCase().contains(query)
                        || nullToEmpty(user.getEmail()).toLowerCase().contains(query))
                .toList());
    }

    private void applyItemFilters() {
        String query = adminItemSearchField == null ? "" : nullToEmpty(adminItemSearchField.getText()).trim().toLowerCase();
        String status = adminItemStatusFilterComboBox == null ? "ALL" : adminItemStatusFilterComboBox.getValue();
        visibleItems.setAll(allItems.stream()
                .filter(item -> status == null || "ALL".equals(status)
                        || (item.getStatus() != null && status.equals(item.getStatus().name())))
                .filter(item -> query.isEmpty()
                        || nullToEmpty(item.getName()).toLowerCase().contains(query)
                        || nullToEmpty(item.getCategory()).toLowerCase().contains(query)
                        || nullToEmpty(item.getSellerUsername()).toLowerCase().contains(query))
                .toList());
    }

    private void restoreManagedUserSelection(String userId) {
        if (userId == null) {
            allUserListView.getSelectionModel().clearSelection();
            clearUserManagementDetails();
            return;
        }
        visibleUsers.stream()
                .filter(user -> userId.equals(user.getId()))
                .findFirst()
                .ifPresentOrElse(
                        user -> allUserListView.getSelectionModel().select(user),
                        () -> {
                            allUserListView.getSelectionModel().clearSelection();
                            clearUserManagementDetails();
                        }
                );
    }

    private void restoreManagedItemSelection(String itemId) {
        if (itemId == null) {
            allItemListView.getSelectionModel().clearSelection();
            clearItemManagementDetails();
            return;
        }
        visibleItems.stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .ifPresentOrElse(
                        item -> allItemListView.getSelectionModel().select(item),
                        () -> {
                            allItemListView.getSelectionModel().clearSelection();
                            clearItemManagementDetails();
                        }
                );
    }

    private void restoreSellerSelection(String sellerId) {
        if (sellerId == null) {
            sellerListView.getSelectionModel().clearSelection();
            clearSellerDetails();
            return;
        }

        pendingSellers.stream()
                .filter(user -> sellerId.equals(user.getId()))
                .findFirst()
                .ifPresentOrElse(
                        user -> sellerListView.getSelectionModel().select(user),
                        () -> {
                            sellerListView.getSelectionModel().clearSelection();
                            clearSellerDetails();
                        }
                );
    }

    private void restoreAuctionSelection(String itemId) {
        if (itemId == null) {
            auctionListView.getSelectionModel().clearSelection();
            clearAuctionDetails();
            return;
        }

        pendingAuctions.stream()
                .filter(item -> itemId.equals(item.getId()))
                .findFirst()
                .ifPresentOrElse(
                        item -> auctionListView.getSelectionModel().select(item),
                        () -> {
                            auctionListView.getSelectionModel().clearSelection();
                            clearAuctionDetails();
                        }
                );
    }

    private void restoreDepositSelection(String requestId) {
        if (requestId == null) {
            depositListView.getSelectionModel().clearSelection();
            clearDepositDetails();
            return;
        }

        pendingDeposits.stream()
                .filter(request -> requestId.equals(request.getId()))
                .findFirst()
                .ifPresentOrElse(
                        request -> depositListView.getSelectionModel().select(request),
                        () -> {
                            depositListView.getSelectionModel().clearSelection();
                            clearDepositDetails();
                        }
                );
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

    private void showUserManagementDetails(User user) {
        if (user == null) {
            clearUserManagementDetails();
            return;
        }
        managedUserNameValue.setText(nullToDash(user.getFullName()));
        managedUserUsernameValue.setText("@" + nullToDash(user.getUserName()));
        managedUserEmailValue.setText(nullToDash(user.getEmail()));
        managedUserRoleValue.setText(nullToDash(user.getRole()));
        managedUserStatusValue.setText(user.isApproved() ? "Đã duyệt" : "Chờ duyệt");
        managedUserBalanceValue.setText(formatCurrency(user.getBalance()));
        deleteUserButton.setDisable(false);
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

    private void showItemManagementDetails(Item item) {
        if (item == null) {
            clearItemManagementDetails();
            return;
        }
        managedItemNameValue.setText(nullToDash(item.getName()));
        managedItemCategoryValue.setText(nullToDash(item.getCategory()));
        managedItemSellerValue.setText(formatUserName(item.getSellerUsername(), item.getSellerId()));
        managedItemPriceValue.setText(formatCurrency(item.getCurrentPrice()));
        managedItemStatusValue.setText(item.getStatus() == null ? "-" : item.getStatus().name());
        managedItemScheduleValue.setText(formatSchedule(item));
        managedItemDescriptionArea.setText(nullToEmpty(item.getDescription()));
        setManagedItemImage(item);
        deleteItemButton.setDisable(false);
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

    private void clearUserManagementDetails() {
        managedUserNameValue.setText("-");
        managedUserUsernameValue.setText("-");
        managedUserEmailValue.setText("-");
        managedUserRoleValue.setText("-");
        managedUserStatusValue.setText("-");
        managedUserBalanceValue.setText("-");
        deleteUserButton.setDisable(true);
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

    private void clearItemManagementDetails() {
        managedItemNameValue.setText("-");
        managedItemCategoryValue.setText("-");
        managedItemSellerValue.setText("-");
        managedItemPriceValue.setText("-");
        managedItemStatusValue.setText("-");
        managedItemScheduleValue.setText("-");
        managedItemDescriptionArea.clear();
        if (managedItemImageView != null) {
            managedItemImageView.setImage(null);
        }
        if (managedItemImagePlaceholder != null) {
            managedItemImagePlaceholder.setVisible(true);
            managedItemImagePlaceholder.setManaged(true);
        }
        deleteItemButton.setDisable(true);
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

    private void setManagedItemImage(Item item) {
        String imageBase64 = item == null ? null : item.getImageBase64();
        if (imageBase64 == null || imageBase64.isBlank()) {
            managedItemImageView.setImage(null);
            managedItemImagePlaceholder.setVisible(true);
            managedItemImagePlaceholder.setManaged(true);
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            managedItemImageView.setImage(new Image(new ByteArrayInputStream(bytes), 700, 500, true, true));
            managedItemImagePlaceholder.setVisible(false);
            managedItemImagePlaceholder.setManaged(false);
        } catch (IllegalArgumentException exception) {
            managedItemImageView.setImage(null);
            managedItemImagePlaceholder.setVisible(true);
            managedItemImagePlaceholder.setManaged(true);
        }
    }

    private String formatSchedule(Item item) {
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            return "Chua co lich";
        }
        return DATE_TIME_FORMATTER.format(item.getStartTime()) + " -> "
                + DATE_TIME_FORMATTER.format(item.getEndTime());
    }

    private List<User> readUsers(Object rawUsers) {
        if (!(rawUsers instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .map(this::toUser)
                .filter(user -> user != null)
                .toList();
    }

    private List<Item> readItems(Object rawItems) {
        if (!(rawItems instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .map(this::toItem)
                .filter(item -> item != null)
                .toList();
    }

    private List<DepositRequest> readDeposits(Object rawRequests) {
        if (!(rawRequests instanceof List<?> list)) {
            return List.of();
        }

        return list.stream()
                .map(this::toDepositRequest)
                .filter(request -> request != null)
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

    private String formatUserName(String username, String fallbackId) {
        if (username != null && !username.isBlank()) {
            return "@" + username;
        }
        return fallbackId == null || fallbackId.isBlank() ? "-" : fallbackId;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String nullToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void runAsync(Payload payload, Consumer<ResponsePayload> onResult) {
        Thread thread = new Thread(() -> {
            try {
                AuctionClient client = AppContext.getAuctionClient();
                client.send(payload);
                Payload raw = client.read();
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
        showTab(userListContainer, userManagementDetailPane, btnShowUsers);
    }

    private void showTab(VBox listContainer, VBox detail, Button activeBtn) {
        // Ẩn hết container
        userListContainer.setVisible(false); userListContainer.setManaged(false);
        itemListContainer.setVisible(false); itemListContainer.setManaged(false);
        sellerListContainer.setVisible(false);  sellerListContainer.setManaged(false);
        auctionListContainer.setVisible(false); auctionListContainer.setManaged(false);
        depositListContainer.setVisible(false); depositListContainer.setManaged(false);

        // Ẩn hết detail pane
        userManagementDetailPane.setVisible(false); userManagementDetailPane.setManaged(false);
        itemManagementDetailPane.setVisible(false); itemManagementDetailPane.setManaged(false);
        sellerDetailPane.setVisible(false);  sellerDetailPane.setManaged(false);
        auctionDetailPane.setVisible(false); auctionDetailPane.setManaged(false);
        depositDetailPane.setVisible(false); depositDetailPane.setManaged(false);

        // Reset active style
        btnShowUsers.getStyleClass().remove("queue-tab-button-active");
        btnShowItems.getStyleClass().remove("queue-tab-button-active");
        btnShowSellers.getStyleClass().remove("queue-tab-button-active");
        btnShowAuctions.getStyleClass().remove("queue-tab-button-active");
        btnShowDeposits.getStyleClass().remove("queue-tab-button-active");

        // Hiện cái được chọn
        listContainer.setVisible(true);  listContainer.setManaged(true);
        detail.setVisible(true);         detail.setManaged(true);
        activeBtn.getStyleClass().add("queue-tab-button-active");
    }

    @FXML private void showUserManagement() { showTab(userListContainer, userManagementDetailPane, btnShowUsers); }
    @FXML private void showItemManagement() { showTab(itemListContainer, itemManagementDetailPane, btnShowItems); }
    @FXML private void showSellerQueue()  { showTab(sellerListContainer, sellerDetailPane, btnShowSellers); }
    @FXML private void showAuctionQueue() { showTab(auctionListContainer, auctionDetailPane, btnShowAuctions); }
    @FXML private void showDepositQueue() { showTab(depositListContainer, depositDetailPane, btnShowDeposits); }
}
