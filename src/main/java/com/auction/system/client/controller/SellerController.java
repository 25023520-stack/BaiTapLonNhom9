package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
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
import javafx.stage.Stage;
import javafx.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

public class SellerController {

    private static final Logger logger = LoggerFactory.getLogger(SellerController.class);
    private static final Duration POLL_INTERVAL = Duration.seconds(3);
    private static final Gson GSON = GsonProvider.get();

    @FXML private ListView<Item> sellerItemList;
    @FXML private TextField nameField;
    @FXML private TextArea descriptionField;
    @FXML private TextField startPriceField;
    @FXML private Button addButton;
    @FXML private Button updateButton;
    @FXML private Button removeButton;
    @FXML private Button refreshButton;

    private final ObservableList<Item> sellersItem = FXCollections.observableArrayList();
    private Seller currentSeller;
    private Timeline autoRefresh;

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
                        item.getName(), item.getStartPrice(), item.getStatus()));
            }
        });

        sellerItemList.getSelectionModel().selectedItemProperty()
                .addListener((obs, old, picked) -> setChangeButton(picked));

        updateButton.setDisable(true);
        removeButton.setDisable(true);

        refreshItems();
        autoRefresh = new Timeline(new KeyFrame(POLL_INTERVAL, e -> refreshItems()));
        autoRefresh.setCycleCount(Animation.INDEFINITE);
        autoRefresh.play();
    }

    @FXML
    private void setAddButton() {
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

        Payload req = new Payload(PayloadType.ADD_ITEM);
        req.put("id", newId);
        req.put("name", name);
        req.put("description", desc);
        req.put("startPrice", price);
        req.put("sellerId", currentSeller.getId());

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
            showError("Đăng xuất", "Không thể mở màn hình đăng nhập.");
        }
    }

    private void refreshItems() {
        if (currentSeller == null) return;
        Payload req = new Payload(PayloadType.LIST_ITEMS_BY_SELLER);
        req.put("sellerId", currentSeller.getId());

        runAsync(req, resp -> {
            if (!resp.isSuccess()) return;
            Object rawItems = resp.getBody().get("items");
            if (rawItems instanceof List<?> list) {
                List<Item> items = list.stream()
                        .map(this::toItem)
                        .filter(i -> i != null)
                        .toList();
                sellersItem.setAll(items);
            }
        });
    }

    private void setChangeButton(Item picked) {
        if (picked == null) {
            nameField.clear();
            descriptionField.clear();
            startPriceField.clear();
            updateButton.setDisable(true);
            removeButton.setDisable(true);
            return;
        }
        nameField.setText(picked.getName());
        descriptionField.setText(picked.getDescription());
        startPriceField.setText(String.valueOf(picked.getStartPrice()));
        updateButton.setDisable(false);
        removeButton.setDisable(false);
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
                Platform.runLater(() -> showError("Lỗi mạng", ex.getMessage()));
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

    private void clearForm() {
        nameField.clear();
        descriptionField.clear();
        startPriceField.clear();
        sellerItemList.getSelectionModel().clearSelection();
    }

    private String safeTrim(String s) { return s == null ? "" : s.trim(); }

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
}
