package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.AddItemPayload;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
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
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
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
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SellerController {

    private static final Logger logger = LoggerFactory.getLogger(SellerController.class);
    private static final Duration POLL_INTERVAL = Duration.seconds(3);
    private static final Gson GSON = GsonProvider.get();
    private static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    @FXML private ListView<Item> sellerItemList;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField startPriceField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button removeButton;
    @FXML private Button refreshButton;
    @FXML private Button startAuctionButton;
    @FXML private Button newItemButton;
    @FXML private Button chooseImageButton;
    @FXML private ImageView imagePreview;
    @FXML private Label imageNameLabel;
    @FXML private Label accountStatusLabel;
    @FXML private Label sellerBalanceLabel;
    @FXML private Label auctionApprovalLabel;
    @FXML private Spinner<Integer> durationHoursSpinner;
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

    private final ObservableList<Item> sellersItem = FXCollections.observableArrayList();
    private Seller currentSeller;
    private Timeline autoRefresh;
    private File selectedImageFile;
    private Timeline countdownTimer;

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
                        : String.format("%s  —  %,.0f ₫  —  %s",
                        item.getName(), item.getCurrentPrice(), describeAuctionState(item)));
            }
        });

        sellerItemList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, picked) -> setChangeButton(picked));

        updateButton.setDisable(true);
        removeButton.setDisable(true);
        startAuctionButton.setDisable(true);
        addButton.setDisable(false);
        configureScheduleInputs();
        updateSellerApprovalState();
        updateSellerBalanceLabel();
        updateAuctionApprovalLabel(null);

        refreshItems();
        autoRefresh = new Timeline(new KeyFrame(POLL_INTERVAL, e -> refreshItems()));
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

        if (name.isEmpty()) { showError("Lỗi", "Tên sản phẩm không được trống"); return; }
        if (desc.isEmpty()) { showError("Lỗi", "Mô tả không được trống"); return; }

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
                    selectedImageFile == null ? null : selectedImageFile.getName(),
                    selectedImageFile == null ? null : encodeImage(selectedImageFile)
            );
        } catch (IOException exception) {
            showError("Lỗi", "Không thể đọc file ảnh.");
            return;
        }

        runAsync(req, resp -> {
            if (resp.isSuccess()) {
                showInfo("Đã thêm: " + name);
                clearForm();
                refreshItems();
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

        if (name.isEmpty()) { showError("Lỗi", "Tên sản phẩm không được trống"); return; }
        if (desc.isEmpty()) { showError("Lỗi", "Mô tả không được trống"); return; }

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
        try {
            if (selectedImageFile != null) {
                req.put("imageFileName", selectedImageFile.getName());
                req.put("imageBase64", encodeImage(selectedImageFile));
            }
        } catch (IOException exception) {
            showError("Loi", "Khong the doc file anh.");
            return;
        }

        runAsync(req, resp -> {
            if (resp.isSuccess()) {
                showInfo("Đã cập nhật: " + name);
                clearForm();
                refreshItems();
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
                    refreshItems();
                } else {
                    showError("Không xoá được", resp.getMessage());
                }
            });
        });
    }

    @FXML
    private void onRefreshClick() { refreshItems(); }

    @FXML
    private void clearFormForNew() {
        clearForm();
    }

    @FXML
    private void setStartAuctionButton() {
        Item selected = sellerItemList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Payload req = new Payload(PayloadType.START_AUCTION);
        LocalDateTime startTime = readScheduleTime(startDatePicker, startHourSpinner, startMinuteSpinner, "bat dau");
        LocalDateTime endTime = readScheduleTime(endDatePicker, endHourSpinner, endMinuteSpinner, "ket thuc");
        if (startTime == null || endTime == null) {
            return;
        }
        if (!endTime.isAfter(startTime)) {
            showError("Thoi gian khong hop le", "Thoi diem ket thuc phai sau thoi diem bat dau.");
            return;
        }

        req.put("id", selected.getId());
        req.put("startTime", startTime.toString());
        req.put("endTime", endTime.toString());

        runAsync(req, resp -> {
            if (resp.isSuccess()) {
                Item updatedItem = toItem(resp.getBody().get("item"));
                if (updatedItem != null) {
                    replaceOrAddSellerItem(updatedItem);
                    sellerItemList.getSelectionModel().select(updatedItem);
                    setChangeButton(updatedItem);
                }
                showInfo("Da gui yeu cau mo phien cho admin: " + selected.getName());
                refreshItems();
            } else {
                showError("Khong mo duoc phien", resp.getMessage());
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
        imagePreview.setImage(new Image(file.toURI().toString(), 128, 96, true, true));
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
        if (currentSeller == null) return;
        Item selectedBeforeRefresh = sellerItemList.getSelectionModel().getSelectedItem();
        String selectedId = selectedBeforeRefresh == null ? null : selectedBeforeRefresh.getId();
        String draftName = nameField.getText();
        String draftDescription = descriptionField.getText();
        String draftStartPrice = startPriceField.getText();
        File draftImageFile = selectedImageFile;
        Image draftImage = imagePreview.getImage();
        String draftImageName = imageNameLabel.getText();
        boolean preserveDraft = selectedId == null
                && (!safeTrim(draftName).isEmpty()
                || !safeTrim(draftDescription).isEmpty()
                || !safeTrim(draftStartPrice).isEmpty()
                || draftImageFile != null);

        Payload req = new Payload(PayloadType.LIST_ITEMS_BY_SELLER);
        req.put("sellerId", currentSeller.getId());

        runAsync(req, resp -> {
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
                sellersItem.setAll(items);
                if (selectedId != null) {
                    sellersItem.stream()
                            .filter(item -> selectedId.equals(item.getId()))
                            .findFirst()
                            .ifPresentOrElse(
                                    item -> sellerItemList.getSelectionModel().select(item),
                                    () -> setChangeButton(null)
                            );
                } else if (preserveDraft) {
                    if (!nameField.isFocused()) nameField.setText(draftName);
                    if (!descriptionField.isFocused()) descriptionField.setText(draftDescription);
                    if (!startPriceField.isFocused()) startPriceField.setText(draftStartPrice);
                    selectedImageFile = draftImageFile;
                    imagePreview.setImage(draftImage);
                    imageNameLabel.setText(draftImageName);
                    updateEditorState(null);
                    updateAuctionApprovalLabel(null);
                } else {
                    setChangeButton(sellerItemList.getSelectionModel().getSelectedItem());
                }
            }
        });
    }

    private void setChangeButton(Item picked) {
        if (picked == null) {
            nameField.clear();
            descriptionField.clear();
            startPriceField.clear();
            selectedImageFile = null;
            imageNameLabel.setText("Chua chon anh");
            imagePreview.setImage(null);
            updateEditorState(null);
            updateAuctionApprovalLabel(null);
            startCountdown(null);
            return;
        }
        nameField.setText(picked.getName());
        descriptionField.setText(picked.getDescription());
        startPriceField.setText(String.valueOf(picked.getStartPrice()));
        selectedImageFile = null;
        imageNameLabel.setText(picked.getImagePath() == null ? "Chua co anh" : picked.getImagePath());
        setImagePreviewFromBase64(picked.getImageBase64());
        updateEditorState(picked);
        updateAuctionApprovalLabel(picked);
        startCountdown(picked);
    }

    private void runAsync(Payload req, Consumer<ResponsePayload> onResult) {
        Thread t = new Thread(() -> {
            try {
                AuctionClient client = AppContext.getAuctionClient();
                client.send(req);
                Payload raw = client.read();
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
        selectedImageFile = null;
        imageNameLabel.setText("Chua chon anh");
        imagePreview.setImage(null);
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
            return;
        }

        byte[] bytes = Base64.getDecoder().decode(imageBase64);
        imagePreview.setImage(new Image(new ByteArrayInputStream(bytes), 128, 96, true, true));
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

        double balance = currentSeller == null ? 0 : currentSeller.getBalance();
        sellerBalanceLabel.setText(String.format("Số dư seller: %,.0f VND", balance));
    }

    private void updateEditorState(Item picked) {
        boolean approved = currentSeller != null && currentSeller.isApproved();
        boolean hasSelection = picked != null;
        boolean running = hasSelection && picked.getStatus() == AuctionStatus.RUNNING;
        boolean pendingApproval = hasPendingAuctionApproval(picked);

        nameField.setDisable(!approved);
        descriptionField.setDisable(!approved);
        startPriceField.setDisable(!approved);
        durationHoursSpinner.setDisable(!approved);
        setScheduleInputsDisabled(!approved);
        newItemButton.setDisable(!approved);
        chooseImageButton.setDisable(!approved);
        addButton.setDisable(!approved || hasSelection);
        updateButton.setDisable(!approved || !hasSelection || running || pendingApproval);
        removeButton.setDisable(!approved || !hasSelection || running || pendingApproval);
        startAuctionButton.setDisable(!approved || !hasSelection || picked.getStatus() != AuctionStatus.OPEN || pendingApproval);
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

    private String describeAuctionState(Item item) {
        if (item == null) {
            return "Chua co du lieu";
        }
        if (item.getStatus() == AuctionStatus.RUNNING) {
            return "Dang mo";
        }
        if (hasPendingAuctionApproval(item)) {
            return "Cho admin duyet";
        }
        if (item.getStatus() == AuctionStatus.FINISHED) {
            return "Da ket thuc";
        }
        return "Chua gui duyet";
    }

    private String safeTrim(String s) { return s == null ? "" : s.trim(); }

    private void configureScheduleInputs() {
        LocalDateTime nowInVietnam = LocalDateTime.now(VIETNAM_ZONE);

        durationHoursSpinner.setValueFactory(new IntegerSpinnerValueFactory(1, 168, 1));
        startHourSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 23, nowInVietnam.getHour()));
        startMinuteSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 59, nowInVietnam.getMinute()));
        endHourSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 23, nowInVietnam.plusHours(1).getHour()));
        endMinuteSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 59, nowInVietnam.getMinute()));

        startHourSpinner.setEditable(true);
        startMinuteSpinner.setEditable(true);
        endHourSpinner.setEditable(true);
        endMinuteSpinner.setEditable(true);
        startDatePicker.setValue(nowInVietnam.toLocalDate());
        endDatePicker.setValue(nowInVietnam.plusHours(1).toLocalDate());

        durationHoursSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
        startDatePicker.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
        startHourSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
        startMinuteSpinner.valueProperty().addListener((obs, oldValue, newValue) -> applyDurationSuggestion());
    }

    private void applyDurationSuggestion() {
        LocalDate startDate = startDatePicker.getValue();
        Integer startHour = startHourSpinner.getValue();
        Integer startMinute = startMinuteSpinner.getValue();
        Integer durationHours = durationHoursSpinner.getValue();
        if (startDate == null || startHour == null || startMinute == null || durationHours == null) {
            return;
        }

        // Ghi chu: seller co the chon truc tiep thoi diem ket thuc.
        // Spinner thoi luong chi dung de goi y nhanh endTime theo gio Viet Nam tu startTime da chon.
        LocalDateTime suggestedEnd = startDate.atTime(startHour, startMinute).plusHours(durationHours);
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
            int hour = hourSpinner.getValue();
            int minute = minuteSpinner.getValue();
            return datePicker.getValue().atTime(hour, minute);
        } catch (RuntimeException exception) {
            showError("Thoi gian khong hop le", "Gio/phut " + label + " phien dau gia khong hop le.");
            return null;
        }
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
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
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
