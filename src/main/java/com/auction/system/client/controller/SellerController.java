package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.AddItemPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.common.payload.UserProfilePayload;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.item.Item;
import com.auction.system.model.user.Seller;
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
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Cursor;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

public class SellerController {

    private static final Logger logger = LoggerFactory.getLogger(SellerController.class);
    private static final Duration POLL_INTERVAL = Duration.seconds(3);
    private static final Gson GSON = GsonProvider.get();
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final DateTimeFormatter PROFILE_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private ListView<Item> sellerItemList;
    @FXML private Node sellerCatalogView;
    @FXML private Node sellerDetailView;
    @FXML private Node sellerProfileView;
    @FXML private FlowPane sellerProductGridPane;
    @FXML private Label emptySellerCatalogLabel;
    @FXML private TextField sellerSearchField;
    @FXML private ComboBox<String> sellerCategoryFilterComboBox;
    @FXML private ComboBox<String> sellerStatusFilterComboBox;
    @FXML private Label sellerTotalCountLabel;
    @FXML private Label sellerRunningCountLabel;
    @FXML private Label sellerPendingCountLabel;
    @FXML private Label sellerSoldCountLabel;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField startPriceField;
    @FXML private ComboBox<String> categoryComboBox;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button removeButton;
    @FXML private Button refreshButton;
    @FXML private Button startAuctionButton;
    @FXML private Button relistButton;
    @FXML private Button newItemButton;
    @FXML private Button chooseImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Label sellerImagePlaceholder;
    @FXML private Label imageNameLabel;
    @FXML private Label imageValidationLabel;
    @FXML private Label accountStatusLabel;
    @FXML private Label sellerBalanceLabel;
    @FXML private Label auctionApprovalLabel;
    @FXML private Spinner<Integer> durationHoursSpinner;
    @FXML private Spinner<Integer> durationMinutesSpinner;
    @FXML private DatePicker startDatePicker;
    @FXML private Spinner<Integer> startHourSpinner;
    @FXML private Spinner<Integer> startMinuteSpinner;
    @FXML private DatePicker endDatePicker;
    @FXML private Spinner<Integer> endHourSpinner;
    @FXML private Spinner<Integer> endMinuteSpinner;
    @FXML private HBox auctionInfoBox;
    @FXML private Label currentBidLabel;
    @FXML private Label highestBidderLabel;
    @FXML private Label timeRemainingLabel;
    @FXML private Label sellerProfileNameLabel;
    @FXML private Label sellerProfileUsernameLabel;
    @FXML private Label sellerProfileEmailLabel;
    @FXML private Label sellerProfileApprovalLabel;
    @FXML private Label sellerProfileBalanceLabel;
    @FXML private Label sellerProfileRevenueLabel;
    @FXML private Label sellerProfileSoldItemsLabel;
    @FXML private Label sellerProfileFinishedItemsLabel;
    @FXML private Label sellerProfileCanceledItemsLabel;
    @FXML private Label sellerProfileSuccessRateLabel;
    @FXML private TextArea sellerProfileResultArea;

    private final ObservableList<Item> sellersItem = FXCollections.observableArrayList();
    private final Map<String, Image> sellerImageCache = new HashMap<>();
    private Seller currentSeller;
    private Timeline autoRefresh;
    private File selectedImageFile;
    private Timeline countdownTimer;
    private boolean suppressSelectionChange;
    private boolean suppressScheduleSuggestion;
    private boolean refreshInFlight;
    private boolean editorModeActive;
    private int sellerGridColumnCount = 1;
    private double sellerGridCardWidth = 286;

    private record EditorDraft(
            String name,
            String description,
            String startPrice,
            String category,
            File imageFile,
            Image image,
            String imageName,
            LocalDate startDate,
            Integer startHour,
            Integer startMinute,
            LocalDate endDate,
            Integer endHour,
            Integer endMinute,
            Integer durationHours,
            Integer durationMinutes
    ) {
    }

    private record AuctionSchedule(LocalDateTime startTime, LocalDateTime endTime) {
    }

    @FXML
    public void initialize() {
        User user = AppContext.getCurrentUser();
        if (!(user instanceof Seller seller)) {
            showError("Phiên đăng nhập", "Vui lòng đăng nhập với tài khoản seller");
            return;
        }
        currentSeller = seller;

        sellerItemList.setItems(sellersItem);
        sellerItemList.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(Item item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null
                        : String.format("[%s] %s  —  %,.0f ₫  —  %s",
                        item.getCategory(), item.getName(), item.getCurrentPrice(), describeAuctionState(item)));
            }
        });

        sellerItemList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, picked) -> {
                    if (!suppressSelectionChange) {
                        setChangeButton(picked);
                    }
                });
        if (sellerProductGridPane != null) {
            sellerProductGridPane.widthProperty().addListener((obs, oldValue, newValue) -> renderSellerCards());
        }

        updateButton.setDisable(true);
        removeButton.setDisable(true);
        startAuctionButton.setDisable(true);
        relistButton.setDisable(true);
        addButton.setDisable(false);
        configureCategoryComboBox();
        configureSellerCatalogFilters();
        configureScheduleInputs();
        updateSellerApprovalState();
        updateSellerBalanceLabel();
        updateAuctionApprovalLabel(null);
        updateImageValidationLabel(null);
        showSellerCatalog();

        autoRefresh = new Timeline(new KeyFrame(POLL_INTERVAL, e -> refreshItems(false)));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();
        startRealtimeListener();
    }

    @FXML
    private void setAddButton() {
        if (sellerItemList.getSelectionModel().getSelectedItem() != null) {
            showError("Dang chon san pham", "Bam 'San pham moi' truoc khi them san pham khac.");
            return;
        }

        String name = safeTrim(nameField.getText());
        String desc = safeTrim(descriptionField.getText());
        String priceText = safeTrim(startPriceField.getText());
        String category = readSelectedCategory();

        if (name.isEmpty()) { showError("Lỗi", "Tên sản phẩm không được trống"); return; }
        if (desc.isEmpty()) { showError("Lỗi", "Mô tả không được trống"); return; }
        if (category == null) { showError("Lỗi", "Vui lòng chọn loại sản phẩm"); return; }
        if (selectedImageFile == null) {
            updateImageValidationLabel("Vui lòng chọn ảnh sản phẩm trước khi thêm.");
            showError("Thiếu ảnh sản phẩm", "Seller bắt buộc phải chọn ảnh khi thêm sản phẩm.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showError("Lỗi", "Giá khởi điểm phải là số");
            return;
        }
        if (price <= 0) { showError("Lỗi", "Giá khởi điểm phải > 0"); return; }

        String newId = "ITEM-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Payload req;
        try {
            req = new AddItemPayload(
                    newId,
                    name,
                    desc,
                    price,
                    currentSeller.getId(),
                    category,
                    selectedImageFile == null ? null : selectedImageFile.getName(),
                    selectedImageFile == null ? null : encodeImage(selectedImageFile)
            );
        } catch (IOException exception) {
            showError("Lỗi", "Không thể đọc file ảnh.");
            return;
        }

        runAsync(req, resp -> {
            if (resp.isSuccess()) {
                Item createdItem = toItem(resp.getBody().get("item"));
                if (createdItem != null) {
                    replaceOrAddSellerItem(createdItem);
                }
                showInfo("Da them san pham. Chon san pham va bam 'Gui duyet phien' khi ban muon mo dau gia.");
                clearForm();
                showSellerCatalog();
            } else {
                showError("Không thêm được", resp.getMessage());
            }
        });
    }

    @FXML
    private void setUpdateButton() {
        Item selected = sellerItemList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        String name = safeTrim(nameField.getText());
        String desc = safeTrim(descriptionField.getText());
        String priceText = safeTrim(startPriceField.getText());
        String category = readSelectedCategory();

        if (name.isEmpty()) { showError("Lỗi", "Tên sản phẩm không được trống"); return; }
        if (desc.isEmpty()) { showError("Lỗi", "Mô tả không được trống"); return; }
        if (category == null) { showError("Lỗi", "Vui lòng chọn loại sản phẩm"); return; }
        if (!hasImageForUpdate(selected)) {
            updateImageValidationLabel("Sản phẩm phải có ảnh trước khi cập nhật.");
            showError("Thiếu ảnh sản phẩm", "Vui lòng chọn ảnh cho sản phẩm trước khi cập nhật.");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showError("Lỗi", "Giá khởi điểm phải là số");
            return;
        }
        if (price <= 0) { showError("Lỗi", "Giá khởi điểm phải > 0"); return; }

        Payload req = new Payload(PayloadType.UPDATE_ITEM);
        req.put("id", selected.getId());
        req.put("name", name);
        req.put("description", desc);
        req.put("startPrice", price);
        req.put("sellerId", currentSeller.getId());
        req.put("category", category);
        try {
            if (selectedImageFile != null) {
                req.put("imageFileName", selectedImageFile.getName());
                req.put("imageBase64", encodeImage(selectedImageFile));
            }
        } catch (IOException exception) {
            showError("Loi", "Khong the doc file anh.");
            return;
        }

        boolean wasCanceled = selected.getStatus() == AuctionStatus.CANCELED;
        runAsync(req, resp -> {
            if (resp.isSuccess()) {
                if (wasCanceled) {
                    showInfo("Da cap nhat san pham. Vui long gui duyet phien lai truoc khi dau gia.");
                    clearForm();
                    showSellerCatalog();
                    return;
                }
                showInfo("Đã cập nhật: " + name);
                clearForm();
                showSellerCatalog();
            } else {
                showError("Không cập nhật được", resp.getMessage());
            }
        });
    }

    @FXML
    private void  setRemoveButton() {
        Item selected = sellerItemList.getSelectionModel().getSelectedItem();
        if (selected == null) return;

        Alert confirm = new Alert(AlertType.CONFIRMATION,
                "Xoá \"" + selected.getName() + "\"?",
                ButtonType.OK, ButtonType.CANCEL);
        confirm.setHeaderText("Xác nhận xoá");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            Payload req = new Payload(PayloadType.REMOVE_ITEM);
            req.put("id", selected.getId());
            req.put("sellerId", currentSeller.getId());

            runAsync(req, resp -> {
                if (resp.isSuccess()) {
                    showInfo("Đã xoá");
                    clearForm();
                    showSellerCatalog();
                } else {
                    showError("Không xoá được", resp.getMessage());
                }
            });
        });
    }

    @FXML
    private void onRefreshClick() {
        if (editorModeActive) {
            showInfo("Hãy lưu hoặc quay lại trước khi làm mới danh sách.");
            return;
        }

        refreshItems(true);
    }

    @FXML
    private void clearFormForNew() {
        clearForm();
        showSellerDetail();
    }

    @FXML
    private void setStartAuctionButton() {
        Item selected = sellerItemList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Payload req = new Payload(PayloadType.START_AUCTION);
        AuctionSchedule schedule = readValidatedAuctionSchedule();
        if (schedule == null) {
            return;
        }

        req.put("id", selected.getId());
        req.put("startTime", schedule.startTime().toString());
        req.put("endTime", schedule.endTime().toString());

        runAsync(req, resp -> {
            if (resp.isSuccess()) {
                Item updatedItem = toItem(resp.getBody().get("item"));
                if (updatedItem != null) {
                    replaceOrAddSellerItem(updatedItem);
                    sellerItemList.getSelectionModel().select(updatedItem);
                    setChangeButton(updatedItem);
                }
                showInfo("Da gui yeu cau mo phien cho admin: " + selected.getName());
                showSellerCatalog();
            } else {
                showError("Khong mo duoc phien", resp.getMessage());
            }
        });
    }

    @FXML
    private void setRelistAuctionButton() {
        Item selected = sellerItemList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }
        if (selected.getStatus() != AuctionStatus.CANCELED) {
            showError("Khong dang lai duoc", "Chi phien da huy moi co the dang lai.");
            return;
        }

        AuctionSchedule schedule = readValidatedAuctionSchedule();
        if (schedule == null) {
            return;
        }

        Payload req = new Payload(PayloadType.RELIST_AUCTION);
        req.put("id", selected.getId());
        req.put("startTime", schedule.startTime().toString());
        req.put("endTime", schedule.endTime().toString());

        runAsync(req, resp -> {
            if (resp.isSuccess()) {
                Item updatedItem = toItem(resp.getBody().get("item"));
                if (updatedItem != null) {
                    replaceOrAddSellerItem(updatedItem);
                    sellerItemList.getSelectionModel().select(updatedItem);
                    setChangeButton(updatedItem);
                }
                showInfo("Da dang lai phien dau gia: " + selected.getName());
                showSellerCatalog();
            } else {
                showError("Khong dang lai duoc", resp.getMessage());
            }
        });
    }

    @FXML
    private void chooseImage() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Chon anh san pham");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(
                "Image files", "*.png", "*.jpg", "*.jpeg"
        ));

        Stage stage = (Stage) nameField.getScene().getWindow();
        File file = chooser.showOpenDialog(stage);
        if (file == null) {
            return;
        }

        selectedImageFile = file;
        imageNameLabel.setText(file.getName());
        imagePreview.setImage(new Image(file.toURI().toString(), 240, 205, true, true));
        updateSellerImagePlaceholder();
        updateImageValidationLabel(null);
    }

    @FXML
    public void showSellerCatalog() {
        editorModeActive = false;

        setViewVisible(sellerCatalogView, true);
        setViewVisible(sellerDetailView, false);
        setViewVisible(sellerProfileView, false);
        renderSellerCards();

        resumeAutoRefresh();
        refreshItems(true);
    }
    /*
    Khi quay lại danh sách seller
→ thoát chế độ thêm/sửa
→ bật lại auto refresh
→ refresh danh sách ngay lập tức
*/

    private void showSellerDetail() {
        editorModeActive = true;
        pauseAutoRefresh();

        setViewVisible(sellerCatalogView, false);
        setViewVisible(sellerDetailView, true);
        setViewVisible(sellerProfileView, false);
    }

    @FXML
    private void showSellerProfile() {
        editorModeActive = false;
        pauseAutoRefresh();

        setViewVisible(sellerCatalogView, false);
        setViewVisible(sellerDetailView, false);
        setViewVisible(sellerProfileView, true);
        loadSellerProfile();
    }

    private void loadSellerProfile() {
        Payload req = new Payload(PayloadType.GET_USER_PROFILE);
        runAsync(req, resp -> {
            if (!resp.isSuccess()) {
                showError("Khong tai duoc ho so", resp.getMessage());
                return;
            }

            UserProfilePayload profile = toProfile(resp.getBody().get("profile"));
            if (profile == null) {
                showError("Khong tai duoc ho so", "Du lieu ho so khong hop le.");
                return;
            }

            renderSellerProfile(profile);
        });
    }

    private UserProfilePayload toProfile(Object rawProfile) {
        if (rawProfile == null) {
            return null;
        }
        if (rawProfile instanceof UserProfilePayload profile) {
            return profile;
        }
        try {
            return GSON.fromJson(GSON.toJson(rawProfile), UserProfilePayload.class);
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private void renderSellerProfile(UserProfilePayload profile) {
        setLabelText(sellerProfileNameLabel, emptyToDash(profile.getFullName()));
        setLabelText(sellerProfileUsernameLabel, "@" + emptyToDash(profile.getUsername()));
        setLabelText(sellerProfileEmailLabel, emptyToDash(profile.getEmail()));
        setLabelText(sellerProfileApprovalLabel, profile.isApproved()
                ? "Tai khoan da duoc duyet"
                : "Tai khoan dang cho admin duyet");
        setLabelText(sellerProfileBalanceLabel, formatVnd(profile.getBalance()));

        if (currentSeller != null && currentSeller.getId().equals(profile.getUserId())) {
            currentSeller.setBalance(profile.getBalance());
            currentSeller.setApproved(profile.isApproved());
            updateSellerBalanceLabel();
            updateSellerApprovalState();
        }

        UserProfilePayload.SellerStats stats = profile.getSellerStats();
        setLabelText(sellerProfileRevenueLabel, formatVnd(stats == null ? 0 : stats.getRevenue()));
        setLabelText(sellerProfileSoldItemsLabel, String.valueOf(stats == null ? 0 : stats.getSoldItems()));
        setLabelText(sellerProfileFinishedItemsLabel,
                String.valueOf(stats == null ? 0 : stats.getFinishedUnpaidItems()));
        setLabelText(sellerProfileCanceledItemsLabel, String.valueOf(stats == null ? 0 : stats.getCanceledItems()));
        setLabelText(sellerProfileSuccessRateLabel,
                stats == null ? "0%" : formatPercent(stats.getPaymentSuccessRate()));

        if (sellerProfileResultArea != null) {
            sellerProfileResultArea.setText(formatSellerResults(profile.getSellerResults()));
        }
    }

    private String formatSellerResults(List<UserProfilePayload.SellerResultEntry> results) {
        if (results == null || results.isEmpty()) {
            return "Chua co item nao co ket qua ban.";
        }

        StringBuilder builder = new StringBuilder();
        for (UserProfilePayload.SellerResultEntry entry : results) {
            builder.append(formatProfileDate(entry.getEndTime()))
                    .append(" | ")
                    .append(emptyToDash(entry.getItemName()))
                    .append(" | ")
                    .append(formatVnd(entry.getFinalPrice()))
                    .append(" | Winner: @")
                    .append(emptyToDash(entry.getWinnerUsername()))
                    .append(" | ")
                    .append(profileStatusText(entry.getStatus()))
                    .append(" | ")
                    .append(sellerResultHint(entry.getStatus()))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    private String sellerResultHint(String status) {
        return switch (status == null ? "" : status) {
            case "PAID" -> "Hoan tat";
            case "FINISHED" -> "Cho bidder thanh toan";
            case "CANCELED" -> "Co the dang lai neu can";
            default -> "-";
        };
    }

    private String profileStatusText(String status) {
        return switch (status == null ? "" : status) {
            case "RUNNING" -> "Dang dau gia";
            case "FINISHED" -> "Da ket thuc";
            case "PAID" -> "Da thanh toan";
            case "CANCELED" -> "Da huy";
            case "OPEN" -> "Chua mo";
            default -> emptyToDash(status);
        };
    }

    private String formatProfileDate(LocalDateTime value) {
        return value == null ? "-" : PROFILE_DATE_FORMATTER.format(value);
    }

    private String formatVnd(double value) {
        return String.format("%,.0f VND", value);
    }

    private String formatPercent(double value) {
        return String.format("%.0f%%", value * 100);
    }

    private void setLabelText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    private String emptyToDash(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void pauseAutoRefresh() {
        if (autoRefresh != null) {
            autoRefresh.pause();
        }
    }

    private void resumeAutoRefresh() {
        if (autoRefresh != null) {
            autoRefresh.play();
        }
    }

    private void setViewVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    @FXML
    private void showSelectedSellerImagePreview() {
        Image image = imagePreview == null ? null : imagePreview.getImage();
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

        Alert dialog = new Alert(AlertType.INFORMATION);
        dialog.setTitle("Xem ảnh sản phẩm");
        dialog.setHeaderText(safeTrim(nameField.getText()).isEmpty() ? "Ảnh sản phẩm" : nameField.getText());
        dialog.getDialogPane().setContent(content);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        if (autoRefresh != null) {
            autoRefresh.stop();
        }
        if (countdownTimer != null) {
            countdownTimer.stop();
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
            showError("Đăng xuất", "Không thể mở màn hình đăng nhập.");
        }
    }

    private void refreshItems() {
        refreshItems(false);
    }

    private void refreshItems(boolean force) {
        if (currentSeller == null) return;

        if (!force && editorModeActive) {
            return;
        }

        if (refreshInFlight) return;
        refreshInFlight = true;
        Item selectedBeforeRefresh = sellerItemList.getSelectionModel().getSelectedItem();
        String selectedId = selectedBeforeRefresh == null ? null : selectedBeforeRefresh.getId();
        EditorDraft draft = captureEditorDraft();
        boolean detailVisible = sellerDetailView != null && sellerDetailView.isVisible();
        boolean preserveNewDraft = selectedId == null && detailVisible && hasNewItemDraft(draft);
        boolean preserveSelectedDraft = selectedId != null && detailVisible && isEditorDirty(selectedBeforeRefresh, draft);

        Payload req = new Payload(PayloadType.LIST_ITEMS_BY_SELLER);
        req.put("sellerId", currentSeller.getId());

        runAsync(req, resp -> {
            try {
                if (!resp.isSuccess()) return;
                Boolean approved = resp.getBoolean("approved");
                if (approved != null) {
                    currentSeller.setApproved(approved);
                    updateSellerApprovalState();
                }
                Double sellerBalance = resp.getDouble("sellerBalance");
                if (sellerBalance != null) {
                    // Ghi chu: server la nguon du lieu chinh cho so du seller sau khi phien ban thanh cong.
                    currentSeller.setBalance(sellerBalance);
                    updateSellerBalanceLabel();
                }
                Object rawItems = resp.getBody().get("items");
                if (rawItems instanceof List<?> list) {
                    List<Item> items = list.stream()
                            .map(this::toItem)
                            .filter(i -> i != null)
                            .toList();
                    suppressSelectionChange = true;
                    try {
                        sellersItem.setAll(items);
                        if (selectedId == null) {
                            sellerItemList.getSelectionModel().clearSelection();
                        } else {
                            sellersItem.stream()
                                    .filter(item -> selectedId.equals(item.getId()))
                                    .findFirst()
                                    .ifPresentOrElse(
                                            item -> sellerItemList.getSelectionModel().select(item),
                                            () -> sellerItemList.getSelectionModel().clearSelection()
                                    );
                        }
                    } finally {
                        suppressSelectionChange = false;
                    }
                    renderSellerCards();

                    Item selectedAfterRefresh = sellerItemList.getSelectionModel().getSelectedItem();
                    if (selectedAfterRefresh != null) {
                        if (preserveSelectedDraft) {
                            restoreEditorDraft(draft);
                            updateEditorState(selectedAfterRefresh);
                            updateAuctionApprovalLabel(selectedAfterRefresh);
                            updateHighestBidderLabel(selectedAfterRefresh);
                            if (selectedAfterRefresh.getStatus() != AuctionStatus.RUNNING) {
                                startCountdown(null);
                            }
                        } else {
                            setChangeButton(selectedAfterRefresh);
                        }
                    } else if (preserveNewDraft) {
                        restoreEditorDraft(draft);
                        updateEditorState(null);
                        updateAuctionApprovalLabel(null);
                        updateHighestBidderLabel(null);
                        startCountdown(null);
                    } else {
                        setChangeButton(null);
                    }
                }
            } finally {
                refreshInFlight = false;
            }
        });
    }

    private void setChangeButton(Item picked) {
        if (picked == null) {
            nameField.clear();
            descriptionField.clear();
            startPriceField.clear();
            resetCategorySelection();
            selectedImageFile = null;
            imageNameLabel.setText("Chua chon anh");
            imagePreview.setImage(null);
            updateSellerImagePlaceholder();
            updateImageValidationLabel(null);
            resetScheduleDefaults();
            updateHighestBidderLabel(null);
            updateEditorState(null);
            updateAuctionApprovalLabel(null);
            startCountdown(null);
            return;
        }
        nameField.setText(picked.getName());
        descriptionField.setText(picked.getDescription());
        startPriceField.setText(String.valueOf(picked.getStartPrice()));
        if (categoryComboBox != null) {
            categoryComboBox.getSelectionModel().select(picked.getCategory());
        }
        selectedImageFile = null;
        imageNameLabel.setText(picked.getImagePath() == null ? "Chua co anh" : picked.getImagePath());
        setImagePreviewFromBase64(picked.getImageBase64());
        updateImageValidationLabel(null);
        populateSchedule(picked);
        updateEditorState(picked);
        updateAuctionApprovalLabel(picked);
        updateHighestBidderLabel(picked);
        startCountdown(picked);
    }

    private void runAsync(Payload req, Consumer<ResponsePayload> onResult) {
        Thread t = new Thread(() -> {
            try {
                AuctionClient client = AppContext.getAuctionClient();
                Payload raw = client.request(req);
                ResponsePayload resp = new ResponsePayload();
                resp.setType(raw.getType());
                raw.getBody().forEach(resp::put);
                Platform.runLater(() -> onResult.accept(resp));
            } catch (IOException ex) {
                logger.error("Network error", ex);
                Platform.runLater(() -> {
                    if (autoRefresh != null) autoRefresh.stop();
                    Stage stage = (Stage) sellerItemList.getScene().getWindow();
                    AppContext.goToServerDown(stage);
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private Item toItem(Object raw) {
        if (raw == null) return null;
        if (raw instanceof Item item) return item;
        try { return GSON.fromJson(GSON.toJson(raw), Item.class); }
        catch (RuntimeException e) { return null; }
    }

    private void replaceOrAddSellerItem(Item updatedItem) {
        for (int i = 0; i < sellersItem.size(); i++) {
            if (updatedItem.getId().equals(sellersItem.get(i).getId())) {
                sellersItem.set(i, updatedItem);
                return;
            }
        }
        sellersItem.add(updatedItem);
    }

    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
        startPriceField.clear();
        resetCategorySelection();
        selectedImageFile = null;
        imageNameLabel.setText("Chua chon anh");
        imagePreview.setImage(null);
        updateSellerImagePlaceholder();
        updateImageValidationLabel(null);
        resetScheduleDefaults();
        updateHighestBidderLabel(null);
        startCountdown(null);
        sellerItemList.getSelectionModel().clearSelection();
        updateEditorState(null);
        updateAuctionApprovalLabel(null);
    }

    private String encodeImage(File file) throws IOException {
        return Base64.getEncoder().encodeToString(Files.readAllBytes(file.toPath()));
    }

    private void setImagePreviewFromBase64(String imageBase64) {
        if (imageBase64 == null || imageBase64.isBlank()) {
            imagePreview.setImage(null);
            updateSellerImagePlaceholder();
            return;
        }

        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            imagePreview.setImage(new Image(new ByteArrayInputStream(bytes), 240, 205, true, true));
        } catch (IllegalArgumentException exception) {
            imagePreview.setImage(null);
        }
        updateSellerImagePlaceholder();
    }

    private void updateSellerApprovalState() {
        boolean approved = currentSeller != null && currentSeller.isApproved();
        if (accountStatusLabel != null) {
            accountStatusLabel.setText(approved
                    ? "Tai khoan seller da duoc duyet. Ban co the tao san pham va gui yeu cau mo phien."
                    : "Tai khoan seller dang cho admin duyet. Chuc nang quan ly san pham tam thoi bi khoa.");
        }
        updateEditorState(sellerItemList == null ? null : sellerItemList.getSelectionModel().getSelectedItem());
    }

    private void updateSellerBalanceLabel() {
        if (sellerBalanceLabel == null) {
            return;
        }

        // Lấy username và số dư từ đối tượng currentSeller
        String username = currentSeller == null ? "Unknown" : currentSeller.getUserName();
        double balance = currentSeller == null ? 0 : currentSeller.getBalance();

        // Sử dụng \n để xuống dòng giữa Username và Số dư
        sellerBalanceLabel.setText(String.format("Seller: %s\nSố dư: %,.0f VND", username, balance));
    }

    private boolean isPendingPayment(Item item){
        return item != null
                && item.getStatus() == AuctionStatus.FINISHED
                && !safeTrim(item.getHighestBidderId()).isEmpty();
    }

    private void updateEditorState(Item picked) {
        boolean approved = currentSeller != null && currentSeller.isApproved();
        boolean hasSelection = picked != null;
        boolean pendingApproval = hasPendingAuctionApproval(picked);
        boolean approvedWaitingStart = hasApprovedScheduledAuction(picked);
        boolean canRelist = approved && hasSelection && picked.getStatus() == AuctionStatus.CANCELED;

        boolean canEdit = hasSelection
                && picked.getStatus() != AuctionStatus.RUNNING
                && !isPendingPayment(picked)
                && picked.getStatus() != AuctionStatus.PAID
                && !pendingApproval
                && !approvedWaitingStart;
        boolean canEditProductFields = approved && (!hasSelection || canEdit);

        nameField.setDisable(!canEditProductFields);
        descriptionField.setDisable(!canEditProductFields);
        startPriceField.setDisable(!canEditProductFields);
        if (categoryComboBox != null) {
            categoryComboBox.setDisable(!canEditProductFields);
        }
        boolean canEditExistingSchedule = approved
                && hasSelection
                && picked.getStatus() == AuctionStatus.OPEN
                && !pendingApproval
                && !approvedWaitingStart;
        boolean canEditSchedule = canEditExistingSchedule || canRelist;
        durationHoursSpinner.setDisable(!canEditSchedule);
        durationMinutesSpinner.setDisable(!canEditSchedule);
        setScheduleInputsDisabled(!canEditSchedule);
        newItemButton.setDisable(!approved);
        chooseImageButton.setDisable(!canEditProductFields);
        addButton.setDisable(!approved || hasSelection);

        boolean canRemove = hasSelection
                && picked.getStatus() != AuctionStatus.RUNNING
                && !pendingApproval
                && !approvedWaitingStart
                && !isPendingPayment(picked);

        updateButton.setDisable(!approved || !canEdit);
        removeButton.setDisable(!approved || !canRemove);
        startAuctionButton.setDisable(!canEditExistingSchedule);
        relistButton.setDisable(!canRelist);
    }

    private void updateAuctionApprovalLabel(Item item) {
        if (auctionApprovalLabel == null) {
            return;
        }

        auctionApprovalLabel.setText(item == null
                ? "Trang thai phien: Chua chon san pham."
                : "Trang thai phien: " + describeAuctionState(item));
    }

    private boolean hasPendingAuctionApproval(Item item) {
        return item != null
                && !item.isAuctionApproved()
                && item.getStatus() == AuctionStatus.OPEN
                && item.getStartTime() != null
                && item.getEndTime() != null;
    }

    private boolean hasApprovedScheduledAuction(Item item) {
        return item != null
                && item.isAuctionApproved()
                && item.getStatus() == AuctionStatus.OPEN
                && item.getStartTime() != null
                && item.getEndTime() != null;
    }

    private String describeAuctionState(Item item) {
        if (item == null) return "Chua co du lieu";
        if (item.getStatus() == AuctionStatus.RUNNING) return "Dang mo";
        if (item.getStatus() == AuctionStatus.CANCELED) return "Da huy";
        if (item.getStatus() == AuctionStatus.PAID) return "Da thanh toan";
        if (hasPendingAuctionApproval(item)) return "Cho admin duyet";
        if (hasApprovedScheduledAuction(item)) return "Da duyet, cho den gio bat dau";
        if (item.getStatus() == AuctionStatus.FINISHED) {
            return safeTrim(item.getHighestBidderId()).isEmpty() ? "Da ket thuc" : "Cho thanh toan";
        }
        return "Chua gui duyet";
    }

    private String safeTrim(String s) { return s == null ? "" : s.trim(); }

    private void configureCategoryComboBox() {
        if (categoryComboBox == null) {
            return;
        }

        categoryComboBox.setItems(FXCollections.observableArrayList(Item.getSupportedCategories()));
        resetCategorySelection();
    }

    private void configureSellerCatalogFilters() {
        if (sellerCategoryFilterComboBox != null) {
            ObservableList<String> categories = FXCollections.observableArrayList();
            categories.add("Tất cả");
            categories.addAll(Item.getSupportedCategories());
            sellerCategoryFilterComboBox.setItems(categories);
            sellerCategoryFilterComboBox.getSelectionModel().select("Tất cả");
            sellerCategoryFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> renderSellerCards());
        }

        if (sellerStatusFilterComboBox != null) {
            sellerStatusFilterComboBox.setItems(FXCollections.observableArrayList(
                    "Tất cả",
                    "Chưa gửi duyệt",
                    "Chờ admin duyệt",
                    "Đã duyệt, chờ bắt đầu",
                    "Đang đấu giá",
                    "Đã kết thúc",
                    "Đã thanh toán",
                    "Đã hủy"
            ));
            sellerStatusFilterComboBox.getSelectionModel().select("Tất cả");
            sellerStatusFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> renderSellerCards());
        }

        if (sellerSearchField != null) {
            sellerSearchField.textProperty().addListener((obs, oldValue, newValue) -> renderSellerCards());
        }
    }

    private void renderSellerCards() {
        updateSellerStats();
        if (sellerProductGridPane == null) {
            return;
        }

        sellerProductGridPane.getChildren().clear();
        List<Item> visibleItems = sellersItem.stream()
                .filter(this::matchesSellerSearch)
                .filter(this::matchesSellerCategory)
                .filter(this::matchesSellerStatus)
                .toList();
        updateSellerGridMetrics(visibleItems.size());

        for (Item item : visibleItems) {
            sellerProductGridPane.getChildren().add(createSellerCard(item));
        }

        boolean empty = visibleItems.isEmpty();
        if (emptySellerCatalogLabel != null) {
            emptySellerCatalogLabel.setVisible(empty);
            emptySellerCatalogLabel.setManaged(empty);
        }
    }

    private void updateSellerGridMetrics(int visibleItemCount) {
        if (sellerProductGridPane == null) {
            return;
        }

        double gridWidth = sellerProductGridPane.getWidth();
        if (gridWidth <= 0) {
            gridWidth = sellerProductGridPane.getPrefWrapLength();
        }
        double gap = sellerProductGridPane.getHgap() <= 0 ? 18 : sellerProductGridPane.getHgap();
        double availableWidth = Math.max(240, gridWidth - 8);
        int maxColumns = Math.max(1, (int) Math.floor((availableWidth + gap) / (250 + gap)));
        sellerGridColumnCount = visibleItemCount <= 0 ? maxColumns : Math.min(maxColumns, visibleItemCount);
        sellerGridCardWidth = Math.max(240,
                Math.floor((availableWidth - gap * (sellerGridColumnCount - 1)) / sellerGridColumnCount));
    }

    private Node createSellerCard(Item item) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(sellerGridCardWidth);
        card.setMinWidth(sellerGridCardWidth);
        card.setMaxWidth(sellerGridCardWidth);
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> {
            sellerItemList.getSelectionModel().select(item);
            setChangeButton(item);
            showSellerDetail();
        });

        StackPane imageFrame = new StackPane();
        imageFrame.getStyleClass().add("product-card-image");
        double imageWidth = Math.max(190, sellerGridCardWidth - 24);
        double imageHeight = Math.max(150, Math.min(220, imageWidth * 0.62));
        imageFrame.setPrefSize(imageWidth, imageHeight);
        imageFrame.setMinHeight(imageHeight);
        Image image = getSellerProductImage(item);
        if (image == null) {
            Label placeholder = new Label("Chưa có ảnh");
            placeholder.getStyleClass().add("product-card-placeholder");
            imageFrame.getChildren().add(placeholder);
        } else {
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(imageWidth);
            imageView.setFitHeight(imageHeight);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageFrame.getChildren().add(imageView);
        }

        Label categoryLabel = new Label(item.getCategory());
        categoryLabel.getStyleClass().add("product-card-category");
        Label statusLabel = new Label(statusDisplayText(item));
        statusLabel.getStyleClass().setAll("product-card-status", statusStyleClass(item));
        HBox badgeRow = new HBox(8, categoryLabel, new Region(), statusLabel);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(badgeRow.getChildren().get(1), Priority.ALWAYS);

        Label titleLabel = new Label(safeTrim(item.getName()).isEmpty() ? "Sản phẩm chưa đặt tên" : item.getName());
        titleLabel.getStyleClass().add("product-card-title");
        titleLabel.setWrapText(true);

        Label priceLabel = new Label(String.format("%,.0f VND", item.getCurrentPrice() > 0 ? item.getCurrentPrice() : item.getStartPrice()));
        priceLabel.getStyleClass().add("product-card-price");
        priceLabel.setWrapText(true);

        VBox metaBox = new VBox(6,
                createSellerMetaRow("Lịch", formatScheduleShort(item)),
                createSellerMetaRow("Dẫn đầu", safeTrim(item.getHighestBidderUsername()).isEmpty() ? "Chưa có" : item.getHighestBidderUsername()),
                createSellerMetaRow("Phiên", describeAuctionState(item))
        );
        metaBox.getStyleClass().add("product-card-meta");

        card.getChildren().addAll(imageFrame, badgeRow, titleLabel, priceLabel, metaBox);
        return card;
    }

    private HBox createSellerMetaRow(String labelText, String valueText) {
        Label label = new Label(labelText);
        label.getStyleClass().add("product-card-meta-label");
        Label value = new Label(valueText);
        value.getStyleClass().add("product-card-meta-value");
        value.setWrapText(true);
        value.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(8, label, value);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(value, Priority.ALWAYS);
        return row;
    }

    private boolean matchesSellerSearch(Item item) {
        if (sellerSearchField == null || sellerSearchField.getText() == null || sellerSearchField.getText().isBlank()) {
            return true;
        }
        String name = item.getName();
        return name != null && name.toLowerCase().contains(sellerSearchField.getText().trim().toLowerCase());
    }

    private boolean matchesSellerCategory(Item item) {
        if (sellerCategoryFilterComboBox == null) {
            return true;
        }
        String category = sellerCategoryFilterComboBox.getValue();
        return category == null || "Tất cả".equals(category) || category.equals(item.getCategory());
    }

    private boolean matchesSellerStatus(Item item) {
        if (sellerStatusFilterComboBox == null) {
            return true;
        }
        String status = sellerStatusFilterComboBox.getValue();
        return status == null
                || "Tất cả".equals(status)
                || status.equals(statusKey(item))
                || status.equals(statusDisplayText(item));
    }

    private String statusKey(Item item) {
        if (hasPendingAuctionApproval(item)) {
            return "PENDING_APPROVAL";
        }
        if (hasApprovedScheduledAuction(item)) {
            return "APPROVED_SCHEDULED";
        }
        return item.getStatus() == null ? "OPEN" : item.getStatus().name();
    }

    private String statusStyleClass(Item item) {
        return switch (statusKey(item)) {
            case "RUNNING" -> "status-running";
            case "OPEN", "PENDING_APPROVAL" -> "status-open";
            case "APPROVED_SCHEDULED" -> "status-approved";
            case "PAID" -> "status-paid";
            case "CANCELED" -> "status-canceled";
            default -> "status-finished";
        };
    }

    private String statusDisplayText(Item item) {
        return switch (statusKey(item)) {
            case "RUNNING" -> "Đang đấu giá";
            case "PENDING_APPROVAL" -> "Chờ admin duyệt";
            case "APPROVED_SCHEDULED" -> "Đã duyệt, chờ bắt đầu";
            case "FINISHED" -> "Đã kết thúc";
            case "PAID" -> "Đã thanh toán";
            case "CANCELED" -> "Đã hủy";
            default -> "Chưa gửi duyệt";
        };
    }

    private String formatScheduleShort(Item item) {
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            return "Chưa có lịch";
        }
        return formatDateTimeShort(item.getStartTime()) + " -> " + formatDateTimeShort(item.getEndTime());
    }

    private Image getSellerProductImage(Item item) {
        String imageBase64 = item == null ? null : item.getImageBase64();
        if (imageBase64 == null || imageBase64.isBlank()) {
            return null;
        }
        String key = item.getId() + ":" + Integer.toHexString(imageBase64.hashCode());
        Image cached = sellerImageCache.get(key);
        if (cached != null) {
            return cached;
        }
        try {
            byte[] bytes = Base64.getDecoder().decode(imageBase64);
            Image image = new Image(new ByteArrayInputStream(bytes), 420, 280, true, true);
            sellerImageCache.put(key, image);
            return image;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private void updateSellerStats() {
        if (sellerTotalCountLabel != null) {
            sellerTotalCountLabel.setText(String.valueOf(sellersItem.size()));
        }
        if (sellerRunningCountLabel != null) {
            sellerRunningCountLabel.setText(String.valueOf(sellersItem.stream()
                    .filter(item -> item.getStatus() == AuctionStatus.RUNNING)
                    .count()));
        }
        if (sellerPendingCountLabel != null) {
            sellerPendingCountLabel.setText(String.valueOf(sellersItem.stream()
                    .filter(this::hasPendingAuctionApproval)
                    .count()));
        }
        if (sellerSoldCountLabel != null) {
            sellerSoldCountLabel.setText(String.valueOf(sellersItem.stream()
                    .filter(item -> item.getStatus() == AuctionStatus.PAID)
                    .count()));
        }
    }

    private void updateSellerImagePlaceholder() {
        if (sellerImagePlaceholder == null || imagePreview == null) {
            return;
        }
        boolean empty = imagePreview.getImage() == null;
        sellerImagePlaceholder.setVisible(empty);
        sellerImagePlaceholder.setManaged(empty);
    }

    private String readSelectedCategory() {
        if (categoryComboBox == null) {
            return Item.DEFAULT_CATEGORY;
        }

        String category = categoryComboBox.getValue();
        if (category == null || category.isBlank()) {
            return null;
        }
        return Item.normalizeCategory(category);
    }

    private void resetCategorySelection() {
        if (categoryComboBox != null) {
            categoryComboBox.getSelectionModel().select(Item.DEFAULT_CATEGORY);
        }
    }

    private void setTextIfChanged(TextInputControl control, String value) {
        String newValue = value == null ? "" : value;

        if (!Objects.equals(control.getText(), newValue)) {
            control.setText(newValue);
        }
    }

    private EditorDraft captureEditorDraft() {
        return new EditorDraft(
                nameField.getText(),
                descriptionField.getText(),
                startPriceField.getText(),
                categoryComboBox == null ? null : categoryComboBox.getValue(),
                selectedImageFile,
                imagePreview.getImage(),
                imageNameLabel.getText(),
                startDatePicker.getValue(),
                startHourSpinner.getValue(),
                startMinuteSpinner.getValue(),
                endDatePicker.getValue(),
                endHourSpinner.getValue(),
                endMinuteSpinner.getValue(),
                durationHoursSpinner.getValue(),
                durationMinutesSpinner.getValue()
        );
    }

    private void restoreEditorDraft(EditorDraft draft) {
        if (draft == null) {
            return;
        }

        setTextIfChanged(nameField, draft.name());
        setTextIfChanged(descriptionField, draft.description());
        setTextIfChanged(startPriceField, draft.startPrice());

        if (draft.category() != null && categoryComboBox != null) {
            String normalizedCategory = Item.normalizeCategory(draft.category());
            if (!Objects.equals(categoryComboBox.getValue(), normalizedCategory)) {
                categoryComboBox.getSelectionModel().select(normalizedCategory);
            }
        }
        selectedImageFile = draft.imageFile();
        imagePreview.setImage(draft.image());
        imageNameLabel.setText(draft.imageName() == null ? "Chua chon anh" : draft.imageName());
        suppressScheduleSuggestion = true;
        try {
            if (draft.durationHours() != null && durationHoursSpinner.getValueFactory() != null) {
                durationHoursSpinner.getValueFactory().setValue(draft.durationHours());
            }
            if (draft.durationMinutes() != null && durationMinutesSpinner.getValueFactory() != null) {
                durationMinutesSpinner.getValueFactory().setValue(draft.durationMinutes());
            }
            startDatePicker.setValue(draft.startDate());
            setSpinnerValue(startHourSpinner, draft.startHour());
            setSpinnerValue(startMinuteSpinner, draft.startMinute());
            endDatePicker.setValue(draft.endDate());
            setSpinnerValue(endHourSpinner, draft.endHour());
            setSpinnerValue(endMinuteSpinner, draft.endMinute());
        } finally {
            suppressScheduleSuggestion = false;
        }
        updateSellerImagePlaceholder();
    }

    private boolean hasNewItemDraft(EditorDraft draft) {
        return draft != null
                && (!safeTrim(draft.name()).isEmpty()
                || !safeTrim(draft.description()).isEmpty()
                || !safeTrim(draft.startPrice()).isEmpty()
                || (draft.category() != null && !Item.DEFAULT_CATEGORY.equals(draft.category()))
                || draft.imageFile() != null);
    }

    private boolean isEditorDirty(Item selected, EditorDraft draft) {
        if (selected == null || draft == null) {
            return false;
        }
        if (!Objects.equals(safeTrim(draft.name()), safeTrim(selected.getName()))) return true;
        if (!Objects.equals(safeTrim(draft.description()), safeTrim(selected.getDescription()))) return true;
        if (!Objects.equals(Item.normalizeCategory(draft.category()), selected.getCategory())) return true;
        if (draft.imageFile() != null) return true;

        try {
            double draftPrice = Double.parseDouble(safeTrim(draft.startPrice()));
            if (Double.compare(draftPrice, selected.getStartPrice()) != 0) {
                return true;
            }
        } catch (NumberFormatException exception) {
            if (!safeTrim(draft.startPrice()).isEmpty()) {
                return true;
            }
        }

        LocalDateTime draftStart = toDateTime(draft.startDate(), draft.startHour(), draft.startMinute());
        LocalDateTime draftEnd = toDateTime(draft.endDate(), draft.endHour(), draft.endMinute());
        return !Objects.equals(draftStart, selected.getStartTime())
                || !Objects.equals(draftEnd, selected.getEndTime());
    }

    private boolean hasImageForUpdate(Item selected) {
        return selectedImageFile != null
                || (selected != null && !safeTrim(selected.getImagePath()).isEmpty())
                || (selected != null && !safeTrim(selected.getImageBase64()).isEmpty());
    }

    private void updateImageValidationLabel(String message) {
        if (imageValidationLabel == null) {
            return;
        }
        boolean visible = message != null && !message.isBlank();
        imageValidationLabel.setText(visible ? message : "");
        imageValidationLabel.setVisible(visible);
        imageValidationLabel.setManaged(visible);
    }

    private void updateHighestBidderLabel(Item item) {
        if (highestBidderLabel == null) {
            return;
        }
        String highestBidder = item == null ? "" : safeTrim(item.getHighestBidderUsername());
        highestBidderLabel.setText(highestBidder.isEmpty() ? "Chưa có" : highestBidder);
    }

    private void populateSchedule(Item item) {
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            resetScheduleDefaults();
            return;
        }
        if (item.getStatus() == AuctionStatus.CANCELED) {
            LocalDateTime start = defaultAuctionStartTime();
            long minutes = Math.max(1, java.time.Duration.between(item.getStartTime(), item.getEndTime()).toMinutes());
            setScheduleInputs(start, start.plusMinutes(minutes));
            return;
        }
        setScheduleInputs(item.getStartTime(), item.getEndTime());
    }

    private void resetScheduleDefaults() {
        LocalDateTime start = defaultAuctionStartTime();
        setScheduleInputs(start, start.plusHours(1));
    }

    private LocalDateTime defaultAuctionStartTime() {
        return LocalDateTime.now(VIETNAM_ZONE)
                .plusMinutes(1)
                .withSecond(0)
                .withNano(0);
    }

    private void setScheduleInputs(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return;
        }
        suppressScheduleSuggestion = true;
        try {
            startDatePicker.setValue(start.toLocalDate());
            setSpinnerValue(startHourSpinner, start.getHour());
            setSpinnerValue(startMinuteSpinner, start.getMinute());
            endDatePicker.setValue(end.toLocalDate());
            setSpinnerValue(endHourSpinner, end.getHour());
            setSpinnerValue(endMinuteSpinner, end.getMinute());
            long minutes = Math.max(1, java.time.Duration.between(start, end).toMinutes());
            int hours = (int) Math.min(168, Math.max(0, minutes / 60));
            int remainingMinutes = (int) Math.max(0, minutes % 60);
            setSpinnerValue(durationHoursSpinner, hours);
            setSpinnerValue(durationMinutesSpinner, remainingMinutes);
        } finally {
            suppressScheduleSuggestion = false;
        }
    }

    private LocalDateTime toDateTime(LocalDate date, Integer hour, Integer minute) {
        if (date == null || hour == null || minute == null) {
            return null;
        }
        return date.atTime(hour, minute);
    }

    private void setSpinnerValue(Spinner<Integer> spinner, Integer value) {
        if (spinner == null || spinner.getValueFactory() == null || value == null) {
            return;
        }
        spinner.getValueFactory().setValue(value);
    }

    private int readSpinnerValue(Spinner<Integer> spinner) {
        String editorText = spinner.getEditor() == null ? null : spinner.getEditor().getText();
        int value = editorText == null || editorText.isBlank()
                ? spinner.getValue()
                : Integer.parseInt(editorText.trim());
        if (spinner.getValueFactory() instanceof IntegerSpinnerValueFactory factory) {
            if (value < factory.getMin() || value > factory.getMax()) {
                throw new IllegalArgumentException("Spinner value out of range");
            }
        }
        spinner.getValueFactory().setValue(value);
        return value;
    }

    private String formatDateTimeShort(LocalDateTime value) {
        return String.format("%02d:%02d %s", value.getHour(), value.getMinute(), value.toLocalDate());
    }

    private void configureScheduleInputs() {
        LocalDateTime defaultStart = defaultAuctionStartTime();

        durationHoursSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 168, 1));
        durationMinutesSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 59, 0));
        startHourSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 23, defaultStart.getHour()));
        startMinuteSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 59, defaultStart.getMinute()));
        endHourSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 23, defaultStart.plusHours(1).getHour()));
        endMinuteSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 59, defaultStart.getMinute()));

        startHourSpinner.setEditable(true);
        startMinuteSpinner.setEditable(true);
        durationHoursSpinner.setEditable(true);
        durationMinutesSpinner.setEditable(true);
        endHourSpinner.setEditable(true);
        endMinuteSpinner.setEditable(true);
        setScheduleInputs(defaultStart, defaultStart.plusHours(1));

        durationHoursSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
        durationMinutesSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
        startDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
        startHourSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
        startMinuteSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
    }

    private void applyDurationSuggestion() {
        if (suppressScheduleSuggestion) {
            return;
        }
        LocalDate startDate = startDatePicker.getValue();
        Integer startHour = startHourSpinner.getValue();
        Integer startMinute = startMinuteSpinner.getValue();
        Integer durationHours = durationHoursSpinner.getValue();
        Integer durationMinutes = durationMinutesSpinner.getValue();
        if (startDate == null || startHour == null || startMinute == null
                || durationHours == null || durationMinutes == null) {
            return;
        }

        // Ghi chu: seller co the chon truc tiep thoi diem ket thuc.
        // Spinner thoi luong chi dung de goi y nhanh endTime theo gio Viet Nam tu startTime da chon.
        LocalDateTime suggestedEnd = startDate.atTime(startHour, startMinute)
                .plusHours(durationHours)
                .plusMinutes(durationMinutes);
        endDatePicker.setValue(suggestedEnd.toLocalDate());
        endHourSpinner.getValueFactory().setValue(suggestedEnd.getHour());
        endMinuteSpinner.getValueFactory().setValue(suggestedEnd.getMinute());
    }

    private LocalDateTime readScheduleTime(
            DatePicker datePicker,
            Spinner<Integer> hourSpinner,
            Spinner<Integer> minuteSpinner,
            String label
    ) {
        if (datePicker.getValue() == null) {
            showError("Thieu thoi gian", "Vui long chon ngay " + label + " phien dau gia.");
            return null;
        }

        try {
            int hour = readSpinnerValue(hourSpinner);
            int minute = readSpinnerValue(minuteSpinner);
            return datePicker.getValue().atTime(hour, minute);
        } catch (RuntimeException exception) {
            showError("Thoi gian khong hop le", "Gio/phut " + label + " phien dau gia khong hop le.");
            return null;
        }
    }

    private AuctionSchedule readValidatedAuctionSchedule() {
        LocalDateTime startTime = readScheduleTime(startDatePicker, startHourSpinner, startMinuteSpinner, "bat dau");
        LocalDateTime endTime = readScheduleTime(endDatePicker, endHourSpinner, endMinuteSpinner, "ket thuc");
        if (startTime == null || endTime == null) {
            return null;
        }
        if (!endTime.isAfter(startTime)) {
            showError("Thoi gian khong hop le", "Thoi diem ket thuc phai sau thoi diem bat dau.");
            return null;
        }
        LocalDateTime now = LocalDateTime.now(VIETNAM_ZONE);
        if (!endTime.isAfter(now)) {
            showError("Thoi gian khong hop le", "Thoi diem ket thuc phai nam trong tuong lai.");
            return null;
        }
        return new AuctionSchedule(startTime, endTime);
    }

    private void setScheduleInputsDisabled(boolean disabled) {
        startDatePicker.setDisable(disabled);
        startHourSpinner.setDisable(disabled);
        startMinuteSpinner.setDisable(disabled);
        endDatePicker.setDisable(disabled);
        endHourSpinner.setDisable(disabled);
        endMinuteSpinner.setDisable(disabled);
    }

    private void showInfo(String msg) {
        Alert a = new Alert(AlertType.INFORMATION, msg);
        a.setHeaderText(null);
        a.showAndWait();
    }

    private void showError(String header, String msg) {
        Alert a = new Alert(AlertType.ERROR, msg);
        a.setHeaderText(header);
        a.showAndWait();
    }
    // THÊM MỚI: đếm ngược thời gian còn lại và hiển thị giá cao nhất
    private void startCountdown(Item item) {
        if (countdownTimer != null) countdownTimer.stop();
        if (item == null || item.getStatus() != AuctionStatus.RUNNING
                || item.getEndTime() == null) {
            if (auctionInfoBox != null) {
                auctionInfoBox.setVisible(false);
                auctionInfoBox.setManaged(false);
            }
            if (currentBidLabel != null) {
                currentBidLabel.setText("—");
            }
            if (timeRemainingLabel != null) {
                timeRemainingLabel.setText("—");
            }
            updateHighestBidderLabel(item);
            return;
        }

        auctionInfoBox.setVisible(true);
        auctionInfoBox.setManaged(true);

        // Cập nhật giá cao nhất
        double highestPrice = item.getCurrentPrice() > 0
                ? item.getCurrentPrice() : item.getStartPrice();
        currentBidLabel.setText(String.format("%,.0f ₫", highestPrice));
        if (highestBidderLabel != null) {
            // Ghi chu: seller chi can thay ten bidder dang dan dau, khong hien thi ID noi bo.
            String highestBidder = safeTrim(item.getHighestBidderUsername());
            highestBidderLabel.setText(highestBidder.isEmpty() ? "Chua co" : highestBidder);
        }

        // Đếm ngược mỗi giây
        countdownTimer = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            java.time.LocalDateTime now = java.time.LocalDateTime.now(VIETNAM_ZONE);
            java.time.Duration remaining = java.time.Duration.between(now, item.getEndTime());
            if (remaining.isNegative() || remaining.isZero()) {
                timeRemainingLabel.setText("Đã kết thúc");
                countdownTimer.stop();
            } else {
                long h = remaining.toHours();
                long m = remaining.toMinutesPart();
                long s = remaining.toSecondsPart();
                timeRemainingLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
            }
        }));
        countdownTimer.setCycleCount(Animation.INDEFINITE);
        countdownTimer.play();
    }

    private void startRealtimeListener() {
        try {
            AuctionClient client = AppContext.getAuctionClient();
            client.startListening(
                    payload -> {
                        if (payload == null || payload.getType() != PayloadType.UPDATE_AUCTION) return;
                        String eventType = String.valueOf(payload.getBody().get("eventType"));
                        if ("BALANCE_UPDATED".equals(eventType)) {
                            String userId = String.valueOf(payload.getBody().get("userId"));
                            Object rawBalance = payload.getBody().get("newBalance");
                            double newBalance = rawBalance instanceof Number n ? n.doubleValue() : 0;
                            if (currentSeller != null && currentSeller.getId().equals(userId)) {
                                Platform.runLater(() -> {
                                    currentSeller.setBalance(newBalance);
                                    updateSellerBalanceLabel();
                                });
                            }
                        }
                        if ("ITEM_REMOVED".equals(eventType)) {
                            Object rawItem = payload.getBody().get("item");
                            if (rawItem != null) {
                                String removedId = rawItem instanceof com.auction.system.model.item.Item i
                                        ? i.getId()
                                        : String.valueOf(((com.google.gson.internal.LinkedTreeMap<?,?>) rawItem).get("id"));
                                Platform.runLater(() -> {
                                    sellersItem.removeIf(i -> Objects.equals(i.getId(), removedId));
                                    Item selected = sellerItemList.getSelectionModel().getSelectedItem();
                                    if (selected != null && Objects.equals(selected.getId(), removedId)) {
                                        setChangeButton(null);
                                    }
                                    renderSellerCards();
                                });
                            }
                            return;
                        }

                        if ("ITEM_UPDATED".equals(eventType) || "ITEM_ADDED".equals(eventType)) {
                            Platform.runLater(this::refreshItems);
                            return;
                        }
                        // Các event khác (BID_PLACED, AUCTION_FINISHED...) SellerController
                        // không cần xử lý realtime — autoRefresh 3s đã đủ
                    },
                    error -> Platform.runLater(() -> {
                        if (autoRefresh != null) autoRefresh.stop();
                        Stage stage = (Stage) sellerItemList.getScene().getWindow();
                        AppContext.goToServerDown(stage);
                    })
            );
        } catch (IOException e) {
            logger.error("Cannot start realtime listener for seller", e);
        }
    }
}
