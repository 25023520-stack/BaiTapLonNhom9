package com.auction.system.client.controller;

import com.auction.system.client.context.AppContext;
import com.auction.system.client.network.AuctionClient;
import com.auction.system.common.json.GsonProvider;
import com.auction.system.common.payload.Payload;
import com.auction.system.common.payload.PayloadType;
import com.auction.system.common.payload.ResponsePayload;
import com.auction.system.model.auction.AuctionStatus;
import com.auction.system.model.auction.Bid;
import com.auction.system.model.item.Item;
import com.google.gson.Gson;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AuctionController {
    private static final Gson GSON = GsonProvider.get();
    private static final String CATEGORY_FILTER_ALL = "ALL";
    private static final double MIN_PRODUCT_CARD_WIDTH = 260;
    private static final double MAX_PRODUCT_CARD_WIDTH = 360;
    private static final double PRODUCT_CARD_HORIZONTAL_INSET = 24;
    private static final double PRODUCT_IMAGE_ASPECT_RATIO = 0.62;
    private static final double DETAIL_COLUMN_GAP = 18;
    private static final double DETAIL_IMAGE_PANEL_MIN_WIDTH = 240;
    private static final double DETAIL_IMAGE_PANEL_MAX_WIDTH = 320;
    private static final double DETAIL_INFO_MIN_WIDTH = 320;
    private static final double DETAIL_INFO_MAX_WIDTH = 600;
    private static final double DETAIL_ACTION_MIN_WIDTH = 280;
    private static final double DETAIL_ACTION_MAX_WIDTH = 330;
    private static final double DETAIL_IMAGE_ASPECT_RATIO = 0.84;
    private static final DecimalFormat VND_FORMAT =
            new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(new Locale("vi", "VN")));
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final ObservableList<Item> allItems = FXCollections.observableArrayList();
    private final ObservableList<Item> items = FXCollections.observableArrayList();
    private final Map<String, Image> imageCache = new HashMap<>();
    private final Map<String, ProductCardView> productCardViews = new HashMap<>();

    private Timeline countdownTimer;
    private String selectedItemId;

    @FXML
    private Node mainCatalogView;

    @FXML
    private Node auctionDetailView;

    @FXML
    private Node profileView;

    @FXML
    private GridPane productGridPane;

    @FXML
    private ScrollPane productCatalogScrollPane;

    @FXML
    private StackPane productGridShell;

    @FXML
    private Label emptyCatalogLabel;

    @FXML
    private Label nameValue;

    @FXML
    private Label priceValue;

    @FXML
    private Label statusValue;

    @FXML
    private Label categoryValue;

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
    private ScrollPane auctionDetailScrollPane;

    @FXML
    private ImageView itemImageView;

    @FXML
    private Label detailImagePlaceholder;

    @FXML
    private HBox auctionDetailContent;

    @FXML
    private VBox auctionImagePanel;

    @FXML
    private StackPane auctionDetailImageFrame;

    @FXML
    private VBox auctionInfoColumn;

    @FXML
    private VBox auctionActionPanel;

    @FXML
    private ComboBox<String> categoryFilterComboBox;

    @FXML
    private TextField itemSearchField;

    @FXML
    protected void initialize() {
        configureItemFilters();
        configureProductGridSizing();
        configureAuctionDetailSizing();

        if (descriptionArea != null) {
            descriptionArea.setEditable(false);
            descriptionArea.setWrapText(true);
        }
        if (bidHistoryArea != null) {
            bidHistoryArea.setEditable(false);
            bidHistoryArea.setWrapText(true);
        }

        initializeAuctionActions();
        showItemDetails(null);
        showMainCatalog();
        startCountdownTimer();
        startRealtimeListener();
        loadItems();
    }

    protected void initializeAuctionActions() {
    }

    private void configureItemFilters() {
        if (categoryFilterComboBox != null) {
            ObservableList<String> categoryOptions = FXCollections.observableArrayList();
            categoryOptions.add(CATEGORY_FILTER_ALL);
            categoryOptions.addAll(Item.getSupportedCategories());
            categoryFilterComboBox.setItems(categoryOptions);
            categoryFilterComboBox.getSelectionModel().select(CATEGORY_FILTER_ALL);
            categoryFilterComboBox.valueProperty().addListener((obs, oldValue, newValue) -> applyFilters(false));
        }

        if (itemSearchField != null) {
            itemSearchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters(false));
        }
    }

    private void configureProductGridSizing() {
        if (productGridPane == null) {
            return;
        }
        if (mainCatalogView instanceof Region mainCatalogRegion) {
            mainCatalogRegion.setMaxWidth(Double.MAX_VALUE);
        }
        if (productGridShell != null) {
            productGridShell.setMaxWidth(Double.MAX_VALUE);
            productGridShell.widthProperty().addListener((obs, oldWidth, newWidth) -> requestProductGridResize());
        }
        if (productCatalogScrollPane != null) {
            productCatalogScrollPane.setMaxWidth(Double.MAX_VALUE);
            productCatalogScrollPane.setFitToWidth(true);
            productCatalogScrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        }
        if (productCatalogScrollPane != null) {
            productCatalogScrollPane.viewportBoundsProperty()
                    .addListener((obs, oldBounds, newBounds) -> requestProductGridResize());
            productCatalogScrollPane.widthProperty().addListener((obs, oldWidth, newWidth) -> requestProductGridResize());
        }
        productGridPane.setMinWidth(0);
        productGridPane.setMaxWidth(Double.MAX_VALUE);
        productGridPane.setAlignment(Pos.TOP_LEFT);
        productGridPane.widthProperty().addListener((obs, oldWidth, newWidth) -> requestProductGridResize());
        Platform.runLater(this::requestProductGridResize);
    }

    private void requestProductGridResize() {
        resizeProductGridToViewport();
        Platform.runLater(this::resizeProductGridToViewport);
    }

    private void resizeProductGridToViewport() {
        if (productGridPane == null) {
            return;
        }

        double catalogWidth = productCatalogWidth();
        if (catalogWidth <= 0) {
            return;
        }

        productGridPane.setMinWidth(0);
        productGridPane.setPrefWidth(catalogWidth);
        productGridPane.setMaxWidth(Double.MAX_VALUE);
        resizeProductCards();
    }

    private double productCatalogWidth() {
        double width = productCatalogViewportWidth();

        if (productGridShell != null) {
            width = maxPositive(width, innerWidth(productGridShell.getWidth(), productGridShell));
            width = maxPositive(width, innerWidth(productGridShell.getLayoutBounds().getWidth(), productGridShell));
            width = maxPositive(width, innerWidth(productGridShell.getBoundsInParent().getWidth(), productGridShell));
        }

        if (productCatalogScrollPane != null) {
            width = maxPositive(width, productCatalogScrollPane.getViewportBounds().getWidth());
            width = maxPositive(width, innerWidth(productCatalogScrollPane.getWidth(), productCatalogScrollPane));
            width = maxPositive(width,
                    innerWidth(productCatalogScrollPane.getLayoutBounds().getWidth(), productCatalogScrollPane));
            width = maxPositive(width,
                    innerWidth(productCatalogScrollPane.getBoundsInParent().getWidth(), productCatalogScrollPane));
        }

        if (width > 0) {
            return width;
        }

        return productGridPane == null ? 0 : productGridPane.getWidth();
    }

    private double productCatalogViewportWidth() {
        if (productCatalogScrollPane == null) {
            return 0;
        }

        double viewportWidth = productCatalogScrollPane.getViewportBounds().getWidth();
        if (viewportWidth > 0) {
            return viewportWidth;
        }

        return innerWidth(productCatalogScrollPane.getWidth(), productCatalogScrollPane);
    }

    private double innerWidth(double width, Region region) {
        if (width <= 0 || region == null) {
            return width;
        }
        return width - region.getInsets().getLeft() - region.getInsets().getRight();
    }

    private double maxPositive(double current, double candidate) {
        return candidate > current ? candidate : current;
    }

    private void configureAuctionDetailSizing() {
        if (auctionDetailContent == null) {
            return;
        }
        configureDetailTextWrapping();
        auctionDetailContent.setFillHeight(false);
        auctionDetailContent.widthProperty().addListener((obs, oldWidth, newWidth) -> resizeAuctionDetailColumns());
        if (auctionDetailScrollPane != null) {
            auctionDetailScrollPane.setFitToWidth(true);
            auctionDetailScrollPane.viewportBoundsProperty()
                    .addListener((obs, oldBounds, newBounds) -> resizeAuctionDetailColumns());
            auctionDetailScrollPane.widthProperty().addListener((obs, oldWidth, newWidth) -> resizeAuctionDetailColumns());
        }
        setDetailColumnGrowNever(auctionImagePanel);
        setDetailColumnGrowNever(auctionInfoColumn);
        setDetailColumnGrowNever(auctionActionPanel);
        Platform.runLater(this::resizeAuctionDetailColumns);
    }

    private void setDetailColumnGrowNever(Node column) {
        if (column != null) {
            HBox.setHgrow(column, Priority.NEVER);
        }
    }

    private void configureDetailTextWrapping() {
        configureDetailLabel(nameValue);
        configureDetailLabel(priceValue);
        configureDetailLabel(statusValue);
        configureDetailLabel(categoryValue);
        configureDetailLabel(sellerValue);
        configureDetailLabel(leaderValue);
        configureDetailLabel(scheduleValue);
        configureDetailLabel(countdownValue);

        setDetailValueGrow(categoryValue);
        setDetailValueGrow(sellerValue);
        setDetailValueGrow(leaderValue);
        setDetailValueGrow(scheduleValue);
        setDetailValueGrow(countdownValue);
    }

    private void configureDetailLabel(Label label) {
        if (label == null) {
            return;
        }
        label.setMinWidth(0);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setWrapText(true);
    }

    private void setDetailValueGrow(Label label) {
        if (label != null) {
            HBox.setHgrow(label, Priority.ALWAYS);
        }
    }

    @FXML
    public void handleLogout(ActionEvent event) {
        stopTimers();
        try {
            AppContext.logout();
        } catch (IOException ignored) {
            // Local session closes even if server disconnect acknowledgement fails.
        }

        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/com/auction/system/client/view/Login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loginView));
            stage.setTitle("Dang nhap");
            stage.show();
        } catch (IOException exception) {
            showAlert(Alert.AlertType.ERROR, "Không thể mở màn hình đăng nhập.");
        }
    }

    @FXML
    protected void showMainCatalog() {
        setViewVisible(mainCatalogView, true);
        setViewVisible(auctionDetailView, false);
        setViewVisible(profileView, false);
        requestProductGridResize();
        refreshProductCardSummaries();
    }

    protected void showAuctionDetail() {
        setViewVisible(mainCatalogView, false);
        setViewVisible(auctionDetailView, true);
        setViewVisible(profileView, false);
        Platform.runLater(() -> {
            resizeAuctionDetailColumns();
            setItemImage(getSelectedItem());
        });
    }

    protected void showProfileView() {
        setViewVisible(mainCatalogView, false);
        setViewVisible(auctionDetailView, false);
        setViewVisible(profileView, true);
    }

    private void setViewVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }

    protected void stopTimers() {
        if (countdownTimer != null) {
            countdownTimer.stop();
        }
    }

    protected void showItemDetails(Item item) {
        if (item == null) {
            selectedItemId = null;
            setLabelText(nameValue, "-");
            setLabelText(priceValue, "-");
            setLabelText(statusValue, "-");
            setLabelText(categoryValue, "-");
            setLabelText(sellerValue, "-");
            setLabelText(leaderValue, "-");
            setLabelText(scheduleValue, "-");
            setLabelText(countdownValue, "-");
            if (descriptionArea != null) {
                descriptionArea.clear();
            }
            if (bidHistoryArea != null) {
                bidHistoryArea.clear();
            }
            setItemImage(null);
            updateAuctionActions(null);
            refreshProductCardSelection();
            return;
        }

        selectedItemId = item.getId();
        setLabelText(nameValue, safeText(item.getName(), "-"));
        setLabelText(priceValue, formatCurrency(item.getCurrentPrice()));
        setLabelText(statusValue, displayStatus(item).name());
        setLabelText(categoryValue, item.getCategory());
        setLabelText(sellerValue, formatUsername(item.getSellerUsername(), item.getSellerId(), "-"));
        setLabelText(leaderValue, formatUsername(item.getHighestBidderUsername(), item.getHighestBidderId(), "Chưa có"));
        setLabelText(scheduleValue, formatSchedule(item));
        updateCountdown(item);

        if (descriptionArea != null) {
            descriptionArea.setText(safeText(item.getDescription(), "Chưa có mô tả."));
        }
        if (bidHistoryArea != null) {
            bidHistoryArea.setText(formatBidHistory(item));
        }
        setItemImage(item);
        updateAuctionActions(item);
        refreshProductCardSelection();
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text);
        }
    }

    private void setItemImage(Item item) {
        if (itemImageView == null) {
            return;
        }
        double requestedWidth = Math.max(285, itemImageView.getFitWidth());
        double requestedHeight = Math.max(248, itemImageView.getFitHeight());
        Image image = getProductImage(item, requestedWidth, requestedHeight);
        itemImageView.setImage(image);
        if (detailImagePlaceholder != null) {
            detailImagePlaceholder.setVisible(image == null);
            detailImagePlaceholder.setManaged(image == null);
        }
    }

    private void startCountdownTimer() {
        countdownTimer = new Timeline(new KeyFrame(javafx.util.Duration.seconds(1), event -> {
            Item selectedItem = getSelectedItem();
            updateCountdown(selectedItem);
            updateAuctionActions(selectedItem);
            refreshProductCardSummaries();
        }));
        countdownTimer.setCycleCount(Timeline.INDEFINITE);
        countdownTimer.play();
    }

    private void updateCountdown(Item item) {
        if (countdownValue == null) {
            return;
        }
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            countdownValue.setText("-");
            return;
        }

        countdownValue.setText(formatCountdownText(item));
        setLabelText(statusValue, displayStatus(item).name());
    }

    private String formatCountdownText(Item item) {
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            return "-";
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime())) {
            return "Sắp bắt đầu  •  " + formatRemainingTime(Duration.between(now, item.getStartTime()));
        }
        if (now.isBefore(item.getEndTime())) {
            return "Đang diễn ra  •  còn " + formatRemainingTime(Duration.between(now, item.getEndTime()));
        }
        return "Đã kết thúc";
    }

    private String formatCardTime(Item item) {
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            return "Chưa có lịch";
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(item.getStartTime())) {
            return "Bắt đầu sau " + formatRemainingTime(Duration.between(now, item.getStartTime()));
        }
        if (now.isBefore(item.getEndTime())) {
            return "Còn " + formatRemainingTime(Duration.between(now, item.getEndTime()));
        }
        return "Đã kết thúc";
    }

    private AuctionStatus displayStatus(Item item) {
        if (item == null || item.getStatus() == null) {
            return AuctionStatus.OPEN;
        }
        if (item.getStatus() == AuctionStatus.RUNNING
                && item.getEndTime() != null
                && LocalDateTime.now().isAfter(item.getEndTime())) {
            return AuctionStatus.FINISHED;
        }
        return item.getStatus();
    }

    private String formatRemainingTime(Duration duration) {
        long totalSeconds = Math.max(0, duration.getSeconds());
        long days = totalSeconds / 86_400;
        long hours = (totalSeconds % 86_400) / 3_600;
        long minutes = (totalSeconds % 3_600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%d ngày %02d:%02d:%02d", days, hours, minutes, seconds);
        }
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    private String formatSchedule(Item item) {
        if (item == null || item.getStartTime() == null || item.getEndTime() == null) {
            return "-";
        }
        return DATE_TIME_FORMATTER.format(item.getStartTime()) + " -> " + DATE_TIME_FORMATTER.format(item.getEndTime());
    }

    private String formatBidHistory(Item item) {
        if (item == null || item.getBidHistory().isEmpty()) {
            return "Chưa có lượt đặt giá nào.";
        }

        LocalDateTime now = LocalDateTime.now();
        StringBuilder builder = new StringBuilder();
        List<Bid> history = item.getBidHistory();
        for (int i = history.size() - 1; i >= 0; i--) {
            Bid bid = history.get(i);
            String bidderName = bid.getBidder() != null ? bid.getBidder().getFullName() : bid.getBidderId();
            builder.append("[")
                    .append(bid.getBidType())
                    .append("] ")
                    .append(bidderName)
                    .append("  —  đã đặt ")
                    .append(formatCurrency(bid.getAmount()))
                    .append("  •  ")
                    .append(formatRelativeTime(bid.getTimestamp(), now))
                    .append(System.lineSeparator());
        }
        return builder.toString();
    }

    @FXML
    protected void showBidHistoryDialog() {
        Item selectedItem = getSelectedItem();
        if (selectedItem == null) {
            showAlert(Alert.AlertType.WARNING, "Bạn chưa chọn sản phẩm.");
            return;
        }

        TextArea historyView = new TextArea(formatBidHistory(selectedItem));
        historyView.setEditable(false);
        historyView.setWrapText(true);
        historyView.setPrefSize(760, 460);

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Lịch sử đấu giá");
        dialog.setHeaderText("Lịch sử đấu giá - " + selectedItem.getName());
        dialog.getDialogPane().setContent(historyView);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private String formatRelativeTime(LocalDateTime time, LocalDateTime now) {
        if (time == null) {
            return "vừa xong";
        }
        long totalSeconds = Duration.between(time, now).getSeconds();
        if (totalSeconds < 60) return "vừa xong";
        if (totalSeconds < 3600) return (totalSeconds / 60) + " phút trước";
        if (totalSeconds < 86_400) return (totalSeconds / 3600) + " tiếng trước";
        return (totalSeconds / 86_400) + " ngày trước";
    }

    protected void showAlert(Alert.AlertType alertType, String message) {
        Alert alert = new Alert(alertType);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    protected void loadItems() {
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
                String selectedId = currentSelectedItemId();
                allItems.setAll(itemList.stream()
                        .map(this::toItem)
                        .filter(Objects::nonNull)
                        .toList());
                applyFilters(selectedId, false);
                return;
            }

            showAlert(Alert.AlertType.ERROR, "Dữ liệu sản phẩm từ server không hợp lệ.");
        } catch (IOException exception) {
            stopTimers();
            goToServerDown();
        }
    }

    private void applyFilters(boolean selectFirstWhenMissing) {
        applyFilters(currentSelectedItemId(), selectFirstWhenMissing);
    }

    private void applyFilters(String preferredItemId, boolean selectFirstWhenMissing) {
        List<Item> filteredItems = allItems.stream()
                .filter(this::matchesSelectedCategory)
                .filter(this::matchesSearchText)
                .toList();

        items.setAll(filteredItems);
        renderProductCards();

        Item selectedItem = preferredItemId == null ? null : findItemById(preferredItemId);
        if (selectedItem != null) {
            selectedItemId = selectedItem.getId();
            if (isAuctionDetailVisible()) {
                showItemDetails(selectedItem);
            } else {
                refreshProductCardSelection();
            }
            return;
        }

        if (selectFirstWhenMissing && !items.isEmpty()) {
            selectItem(items.get(0));
            if (isAuctionDetailVisible()) {
                showItemDetails(getSelectedItem());
            }
            return;
        }

        if (preferredItemId != null) {
            showItemDetails(null);
            if (isAuctionDetailVisible()) {
                showMainCatalog();
            }
        }
    }

    private boolean isAuctionDetailVisible() {
        return auctionDetailView != null && auctionDetailView.isVisible();
    }

    private boolean matchesSelectedCategory(Item item) {
        if (item == null || categoryFilterComboBox == null) {
            return true;
        }

        String selectedCategory = categoryFilterComboBox.getValue();
        if (selectedCategory == null || CATEGORY_FILTER_ALL.equals(selectedCategory)) {
            return true;
        }

        return selectedCategory.equals(item.getCategory());
    }

    private boolean matchesSearchText(Item item) {
        if (item == null || itemSearchField == null) {
            return true;
        }

        String query = itemSearchField.getText();
        if (query == null || query.isBlank()) {
            return true;
        }

        String name = item.getName();
        return name != null && name.toLowerCase(Locale.ROOT).contains(query.trim().toLowerCase(Locale.ROOT));
    }

    private void renderProductCards() {
        if (productGridPane == null) {
            return;
        }

        productGridPane.getChildren().clear();
        productCardViews.clear();

        for (Item item : items) {
            ProductCardView cardView = createProductCard(item);
            productCardViews.put(item.getId(), cardView);
            productGridPane.getChildren().add(cardView.card);
        }
        requestProductGridResize();
        if (productCatalogScrollPane != null) {
            productCatalogScrollPane.setVvalue(0);
        }

        boolean empty = items.isEmpty();
        if (emptyCatalogLabel != null) {
            emptyCatalogLabel.setVisible(empty);
            emptyCatalogLabel.setManaged(empty);
        }
        refreshProductCardSelection();
    }

    private ProductCardView createProductCard(Item item) {
        VBox card = new VBox(10);
        card.getStyleClass().add("product-card");
        card.setPrefWidth(MIN_PRODUCT_CARD_WIDTH);
        card.setMinWidth(MIN_PRODUCT_CARD_WIDTH);
        card.setMaxWidth(MIN_PRODUCT_CARD_WIDTH);
        card.setCursor(Cursor.HAND);
        card.setOnMouseClicked(event -> openAuctionDetail(item));

        StackPane imageFrame = new StackPane();
        imageFrame.getStyleClass().add("product-card-image");
        imageFrame.setPrefSize(MIN_PRODUCT_CARD_WIDTH - PRODUCT_CARD_HORIZONTAL_INSET, 178);
        imageFrame.setMinHeight(178);
        Image image = getProductImage(item, 420, 280);
        ImageView imageView = null;
        if (image == null) {
            Label placeholder = new Label("No Image");
            placeholder.getStyleClass().add("product-card-placeholder");
            imageFrame.getChildren().add(placeholder);
        } else {
            imageView = new ImageView(image);
            imageView.setFitWidth(MIN_PRODUCT_CARD_WIDTH - PRODUCT_CARD_HORIZONTAL_INSET);
            imageView.setFitHeight(178);
            imageView.setPreserveRatio(true);
            imageView.setSmooth(true);
            imageFrame.getChildren().add(imageView);
        }

        Label categoryLabel = new Label();
        categoryLabel.getStyleClass().add("product-card-category");
        Label statusLabel = new Label();
        HBox badgeRow = new HBox(8, categoryLabel, new Region(), statusLabel);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(badgeRow.getChildren().get(1), Priority.ALWAYS);

        Label titleLabel = new Label();
        titleLabel.getStyleClass().add("product-card-title");
        titleLabel.setWrapText(true);

        Label priceLabel = new Label();
        priceLabel.getStyleClass().add("product-card-price");
        priceLabel.setWrapText(true);

        Label sellerLabel = new Label();
        Label timeLabel = new Label();
        Label leaderLabel = new Label();

        VBox metaBox = new VBox(6,
                createProductMetaRow("Người bán", sellerLabel),
                createProductMetaRow("Thời gian", timeLabel),
                createProductMetaRow("Dẫn đầu", leaderLabel)
        );
        metaBox.getStyleClass().add("product-card-meta");

        card.getChildren().addAll(imageFrame, badgeRow, titleLabel, priceLabel, metaBox);

        ProductCardView view = new ProductCardView(card, imageFrame, imageView, categoryLabel, statusLabel, titleLabel,
                priceLabel, sellerLabel, timeLabel, leaderLabel);
        updateProductCardView(view, item);
        return view;
    }

    private void resizeProductCards() {
        if (productGridPane == null || productCardViews.isEmpty()) {
            return;
        }

        double catalogWidth = productCatalogWidth();
        if (catalogWidth <= 0) {
            catalogWidth = productGridPane.getWidth();
        }
        if (catalogWidth <= 0) {
            catalogWidth = productGridPane.getPrefWidth();
        }
        if (catalogWidth <= 0) {
            return;
        }

        double availableWidth = Math.max(MIN_PRODUCT_CARD_WIDTH, catalogWidth
                - productGridPane.getInsets().getLeft()
                - productGridPane.getInsets().getRight());
        double hgap = productGridPane.getHgap();
        int maxColumnsByMinimumWidth = Math.max(1,
                (int) Math.floor((availableWidth + hgap) / (MIN_PRODUCT_CARD_WIDTH + hgap)));
        int columns = Math.max(1, Math.min(items.size(), maxColumnsByMinimumWidth));
        double cardWidth = (availableWidth - hgap * (columns - 1)) / columns;
        cardWidth = clamp(cardWidth, MIN_PRODUCT_CARD_WIDTH, MAX_PRODUCT_CARD_WIDTH);

        double imageWidth = Math.max(0, cardWidth - PRODUCT_CARD_HORIZONTAL_INSET);
        double imageHeight = Math.max(178, Math.min(320, imageWidth * PRODUCT_IMAGE_ASPECT_RATIO));

        productGridPane.setAlignment(Pos.TOP_LEFT);

        for (ProductCardView view : productCardViews.values()) {
            view.card.setMinWidth(cardWidth);
            view.card.setPrefWidth(cardWidth);
            view.card.setMaxWidth(cardWidth);
            view.imageFrame.setMinWidth(imageWidth);
            view.imageFrame.setPrefWidth(imageWidth);
            view.imageFrame.setMaxWidth(imageWidth);
            view.imageFrame.setMinHeight(imageHeight);
            view.imageFrame.setPrefHeight(imageHeight);
            view.imageFrame.setMaxHeight(imageHeight);
            if (view.imageView != null) {
                view.imageView.setFitWidth(imageWidth);
                view.imageView.setFitHeight(imageHeight);
            }
        }
        layoutProductCards(columns);
        productGridPane.requestLayout();
        if (productCatalogScrollPane != null) {
            productCatalogScrollPane.requestLayout();
        }
    }

    private void layoutProductCards(int columns) {
        int safeColumns = Math.max(1, columns);
        for (int index = 0; index < productGridPane.getChildren().size(); index++) {
            Node child = productGridPane.getChildren().get(index);
            GridPane.setColumnIndex(child, index % safeColumns);
            GridPane.setRowIndex(child, index / safeColumns);
        }
    }

    private void resizeAuctionDetailColumns() {
        if (auctionDetailContent == null) {
            return;
        }

        double viewportWidth = auctionDetailViewportWidth();
        if (viewportWidth <= 0) {
            viewportWidth = auctionDetailContent.getWidth();
        }
        if (viewportWidth <= 0) {
            viewportWidth = auctionDetailContent.getBoundsInParent().getWidth();
        }
        if (viewportWidth <= 0) {
            return;
        }

        double availableWidth = Math.max(0, viewportWidth - DETAIL_COLUMN_GAP * 2);
        double actionWidth = clamp(availableWidth * 0.24, DETAIL_ACTION_MIN_WIDTH, DETAIL_ACTION_MAX_WIDTH);
        double imagePanelWidth = clamp(availableWidth * 0.25, DETAIL_IMAGE_PANEL_MIN_WIDTH,
                DETAIL_IMAGE_PANEL_MAX_WIDTH);
        double infoWidth = clamp(availableWidth - imagePanelWidth - actionWidth, DETAIL_INFO_MIN_WIDTH,
                DETAIL_INFO_MAX_WIDTH);
        DetailColumnWidths columnWidths = fitDetailColumnsToViewport(viewportWidth, imagePanelWidth, infoWidth,
                actionWidth);
        imagePanelWidth = columnWidths.imagePanelWidth();
        infoWidth = columnWidths.infoWidth();
        actionWidth = columnWidths.actionWidth();

        double detailContentWidth = imagePanelWidth + infoWidth + actionWidth + DETAIL_COLUMN_GAP * 2;
        double imageFrameWidth = Math.max(200, imagePanelWidth - 40);
        double imageFrameHeight = clamp(imageFrameWidth * DETAIL_IMAGE_ASPECT_RATIO, 210, 300);

        auctionDetailContent.setAlignment(Pos.TOP_CENTER);
        auctionDetailContent.setMinWidth(Math.min(detailContentWidth, viewportWidth));
        auctionDetailContent.setPrefWidth(Math.max(detailContentWidth, viewportWidth));
        auctionDetailContent.setMaxWidth(Double.MAX_VALUE);
        setFixedWidth(auctionImagePanel, imagePanelWidth);
        setFixedWidth(auctionInfoColumn, infoWidth);
        setFixedWidth(auctionActionPanel, actionWidth);

        if (auctionDetailImageFrame != null) {
            auctionDetailImageFrame.setMinWidth(imageFrameWidth);
            auctionDetailImageFrame.setPrefWidth(imageFrameWidth);
            auctionDetailImageFrame.setMaxWidth(imageFrameWidth);
            auctionDetailImageFrame.setMinHeight(imageFrameHeight);
            auctionDetailImageFrame.setPrefHeight(imageFrameHeight);
            auctionDetailImageFrame.setMaxHeight(imageFrameHeight);
        }
        if (itemImageView != null) {
            itemImageView.setFitWidth(Math.max(0, imageFrameWidth - 20));
            itemImageView.setFitHeight(Math.max(0, imageFrameHeight - 20));
        }
    }

    private double auctionDetailViewportWidth() {
        if (auctionDetailScrollPane == null) {
            return 0;
        }

        double viewportWidth = auctionDetailScrollPane.getViewportBounds().getWidth();
        if (viewportWidth > 0) {
            return viewportWidth;
        }

        return innerWidth(auctionDetailScrollPane.getWidth(), auctionDetailScrollPane);
    }

    private DetailColumnWidths fitDetailColumnsToViewport(double viewportWidth, double imagePanelWidth,
                                                          double infoWidth, double actionWidth) {
        double totalWidth = imagePanelWidth + infoWidth + actionWidth + DETAIL_COLUMN_GAP * 2;
        double overflow = totalWidth - viewportWidth;
        if (overflow <= 0) {
            return new DetailColumnWidths(imagePanelWidth, infoWidth, actionWidth);
        }

        double infoShrink = Math.min(overflow, infoWidth - DETAIL_INFO_MIN_WIDTH);
        infoWidth -= infoShrink;
        overflow -= infoShrink;

        double imageShrink = Math.min(overflow, imagePanelWidth - DETAIL_IMAGE_PANEL_MIN_WIDTH);
        imagePanelWidth -= imageShrink;
        overflow -= imageShrink;

        double actionShrink = Math.min(overflow, actionWidth - DETAIL_ACTION_MIN_WIDTH);
        actionWidth -= actionShrink;

        return new DetailColumnWidths(imagePanelWidth, infoWidth, actionWidth);
    }

    private void setFixedWidth(Region region, double width) {
        if (region == null) {
            return;
        }
        region.setMinWidth(width);
        region.setPrefWidth(width);
        region.setMaxWidth(width);
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private record DetailColumnWidths(double imagePanelWidth, double infoWidth, double actionWidth) {
    }

    private HBox createProductMetaRow(String labelText, Label valueLabel) {
        Label label = new Label(labelText);
        label.getStyleClass().add("product-card-meta-label");
        valueLabel.getStyleClass().add("product-card-meta-value");
        valueLabel.setWrapText(true);
        valueLabel.setMaxWidth(Double.MAX_VALUE);

        HBox row = new HBox(8, label, valueLabel);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(valueLabel, Priority.ALWAYS);
        return row;
    }

    private void updateProductCardView(ProductCardView view, Item item) {
        view.categoryLabel.setText(item.getCategory());
        view.titleLabel.setText(safeText(item.getName(), "Sản phẩm chưa đặt tên"));
        view.priceLabel.setText(formatCurrency(item.getCurrentPrice()));
        view.sellerLabel.setText(formatUsername(item.getSellerUsername(), item.getSellerId(), "-"));
        view.timeLabel.setText(formatCardTime(item));
        view.leaderLabel.setText(formatUsername(item.getHighestBidderUsername(), item.getHighestBidderId(), "Chưa có"));

        AuctionStatus status = displayStatus(item);
        view.statusLabel.setText(status.name());
        view.statusLabel.getStyleClass().setAll("product-card-status", statusStyleClass(status));
    }

    private void refreshProductCardSummaries() {
        for (Item item : items) {
            ProductCardView view = productCardViews.get(item.getId());
            if (view != null) {
                updateProductCardView(view, item);
            }
        }
        refreshProductCardSelection();
    }

    private void refreshProductCardSelection() {
        for (Map.Entry<String, ProductCardView> entry : productCardViews.entrySet()) {
            boolean selected = Objects.equals(entry.getKey(), selectedItemId);
            ObservableList<String> styleClasses = entry.getValue().card.getStyleClass();
            if (selected && !styleClasses.contains("product-card-selected")) {
                styleClasses.add("product-card-selected");
            } else if (!selected) {
                styleClasses.remove("product-card-selected");
            }
        }
    }

    private String statusStyleClass(AuctionStatus status) {
        if (status == AuctionStatus.RUNNING) {
            return "status-running";
        }
        if (status == AuctionStatus.OPEN) {
            return "status-open";
        }
        if (status == AuctionStatus.PAID) {
            return "status-paid";
        }
        if (status == AuctionStatus.CANCELED) {
            return "status-canceled";
        }
        return "status-finished";
    }

    private void openAuctionDetail(Item item) {
        Item latestItem = item == null ? null : findItemById(item.getId());
        if (latestItem == null) {
            return;
        }
        selectItem(latestItem);
        showItemDetails(latestItem);
        showAuctionDetail();
    }

    @FXML
    protected void showSelectedImagePreview() {
        Item item = getSelectedItem();
        if (item == null) {
            showAlert(Alert.AlertType.WARNING, "Bạn chưa chọn sản phẩm.");
            return;
        }
        showImagePreview(item, safeText(item.getName(), "Ảnh sản phẩm"));
    }

    protected void showImagePreview(Item item, String title) {
        Image image = getProductImage(item, 1100, 800);
        StackPane content = new StackPane();
        content.setPrefSize(820, 560);
        content.getStyleClass().add("image-preview-dialog");

        if (image == null) {
            Label placeholder = new Label("Sản phẩm chưa có ảnh.");
            placeholder.getStyleClass().add("image-placeholder-text");
            content.getChildren().add(placeholder);
        } else {
            ImageView preview = new ImageView(image);
            preview.setFitWidth(800);
            preview.setFitHeight(540);
            preview.setPreserveRatio(true);
            preview.setSmooth(true);
            content.getChildren().add(preview);
        }

        Alert dialog = new Alert(Alert.AlertType.INFORMATION);
        dialog.setTitle("Xem ảnh sản phẩm");
        dialog.setHeaderText(title);
        dialog.getDialogPane().setContent(content);
        dialog.setResizable(true);
        dialog.showAndWait();
    }

    private Image getProductImage(Item item, double requestedWidth, double requestedHeight) {
        if (item == null) {
            return null;
        }

        String imageBase64 = item.getImageBase64();
        if (imageBase64 != null && !imageBase64.isBlank()) {
            String cacheKey = "base64:"
                    + safeText(item.getId(), "item")
                    + ":"
                    + (int) requestedWidth
                    + "x"
                    + (int) requestedHeight
                    + ":"
                    + Integer.toHexString(imageBase64.hashCode());
            Image cached = imageCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            try {
                byte[] bytes = Base64.getDecoder().decode(imageBase64);
                Image image = new Image(new ByteArrayInputStream(bytes), requestedWidth, requestedHeight, true, true);
                imageCache.put(cacheKey, image);
                return image;
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }

        String imagePath = item.getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }

        try {
            Path path = Path.of(imagePath);
            if (!Files.exists(path)) {
                return null;
            }

            String cacheKey = "path:"
                    + path.toAbsolutePath()
                    + ":"
                    + (int) requestedWidth
                    + "x"
                    + (int) requestedHeight;
            Image cached = imageCache.get(cacheKey);
            if (cached != null) {
                return cached;
            }

            Image image = new Image(path.toUri().toString(), requestedWidth, requestedHeight, true, true);
            if (!image.isError()) {
                imageCache.put(cacheKey, image);
                return image;
            }
        } catch (RuntimeException ignored) {
            return null;
        }

        return null;
    }

    private String currentSelectedItemId() {
        return selectedItemId;
    }

    protected ResponsePayload readResponse(AuctionClient client) throws IOException {
        Payload raw = client.read();
        if (raw == null) {
            throw new IOException("Server returned null");
        }

        ResponsePayload response = new ResponsePayload();
        response.setType(raw.getType());
        raw.getBody().forEach(response::put);
        return response;
    }

    protected Item findItemById(String itemId) {
        return allItems.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElse(null);
    }

    protected Item getSelectedItem() {
        return selectedItemId == null ? null : findItemById(selectedItemId);
    }

    protected void selectItem(Item item) {
        selectedItemId = item == null ? null : item.getId();
        refreshProductCardSelection();
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
                    error -> Platform.runLater(() -> {
                        stopTimers();
                        goToServerDown();
                    })
            );
        } catch (IOException e) {
            stopTimers();
            goToServerDown();
        }
    }

    private void handleAuctionUpdate(ResponsePayload update) {
        String eventType = String.valueOf(update.getBody().get("eventType"));
        if ("BALANCE_UPDATED".equals(eventType)) {
            String userId = String.valueOf(update.getBody().get("userId"));
            Object rawBalance = update.getBody().get("newBalance");
            double newBalance = rawBalance instanceof Number n ? n.doubleValue() : 0;
            onBalanceUpdated(userId, newBalance);
            return;
        }

        Object rawItem = update.getBody().get("item");
        Item updatedItem = toItem(rawItem);
        if (updatedItem == null) {
            return;
        }

        String selectedId = currentSelectedItemId();

        if ("ITEM_ADDED".equals(eventType)) {
            if (findItemById(updatedItem.getId()) == null) {
                allItems.add(updatedItem);
            }
            applyFilters(selectedId, false);
            return;
        }

        if ("ITEM_REMOVED".equals(eventType)) {
            allItems.removeIf(i -> Objects.equals(i.getId(), updatedItem.getId()));
            applyFilters(selectedId, false);
            return;
        }

        boolean updatedExisting = false;
        for (int i = 0; i < allItems.size(); i++) {
            if (Objects.equals(allItems.get(i).getId(), updatedItem.getId())) {
                Item previousItem = allItems.get(i);
                if ((updatedItem.getImageBase64() == null || updatedItem.getImageBase64().isBlank())
                        && previousItem.getImageBase64() != null) {
                    updatedItem.setImageBase64(previousItem.getImageBase64());
                }
                allItems.set(i, updatedItem);
                updatedExisting = true;
                break;
            }
        }

        if (!updatedExisting) {
            allItems.add(updatedItem);
        }

        applyFilters(selectedId, false);
        onAuctionEvent(updatedItem, eventType);
    }

    protected void onAuctionEvent(Item item, String eventType) {
    }

    protected void onBalanceUpdated(String userId, double newBalance) {
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

    private void goToServerDown() {
        Stage stage = currentStage();
        if (stage != null) {
            AppContext.goToServerDown(stage);
        }
    }

    private Stage currentStage() {
        Node node = productGridPane;
        if (node == null) {
            node = mainCatalogView;
        }
        if (node == null) {
            node = auctionDetailView;
        }
        if (node == null || node.getScene() == null) {
            return null;
        }
        return (Stage) node.getScene().getWindow();
    }

    private String formatUsername(String username, String fallbackId, String emptyText) {
        if (username != null && !username.isBlank()) {
            return "@" + username;
        }

        if (fallbackId != null && !fallbackId.isBlank()) {
            return fallbackId;
        }

        return emptyText;
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    protected void updateAuctionActions(Item item) {
    }

    protected boolean isWithinBiddingWindow(Item item) {
        if (item.getStartTime() == null || item.getEndTime() == null) {
            return true;
        }

        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(item.getStartTime()) && now.isBefore(item.getEndTime());
    }

    protected String formatCurrency(double amount) {
        return VND_FORMAT.format(amount) + " VND";
    }

    private static final class ProductCardView {
        private final VBox card;
        private final StackPane imageFrame;
        private final ImageView imageView;
        private final Label categoryLabel;
        private final Label statusLabel;
        private final Label titleLabel;
        private final Label priceLabel;
        private final Label sellerLabel;
        private final Label timeLabel;
        private final Label leaderLabel;

        private ProductCardView(VBox card, StackPane imageFrame, ImageView imageView,
                                Label categoryLabel, Label statusLabel, Label titleLabel,
                                Label priceLabel, Label sellerLabel, Label timeLabel, Label leaderLabel) {
            this.card = card;
            this.imageFrame = imageFrame;
            this.imageView = imageView;
            this.categoryLabel = categoryLabel;
            this.statusLabel = statusLabel;
            this.titleLabel = titleLabel;
            this.priceLabel = priceLabel;
            this.sellerLabel = sellerLabel;
            this.timeLabel = timeLabel;
            this.leaderLabel = leaderLabel;
        }
    }
}
