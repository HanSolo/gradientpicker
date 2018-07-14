package eu.hansolo.fx.gradientpicker;

import eu.hansolo.fx.gradientpicker.tool.GradientLookup;
import eu.hansolo.fx.gradientpicker.tool.NumberTextField;
import javafx.beans.DefaultProperty;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.event.EventType;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.effect.BlurType;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.InnerShadow;
import javafx.scene.image.Image;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;
import javafx.stage.Popup;
import javafx.util.StringConverter;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import static eu.hansolo.fx.gradientpicker.tool.Helper.clamp;


/**
 * User: hansolo
 * Date: 13.07.18
 * Time: 12:29
 */
@DefaultProperty("children")
public class GradientPicker extends Region {
    private static final double                   PREFERRED_WIDTH  = 200;
    private static final double                   PREFERRED_HEIGHT = 70;
    private static final double                   MINIMUM_WIDTH    = 50;
    private static final double                   MINIMUM_HEIGHT   = 70;
    private static final double                   MAXIMUM_WIDTH    = 1024;
    private static final double                   MAXIMUM_HEIGHT   = 70;
    private static final double                   PADDING_LEFT     = 10;
    private static final double                   PADDING_RIGHT    = 2 * PADDING_LEFT;
    private static final double                   BOX_HEIGHT       = 20;
    private static final double                   HANDLE_CENTER    = 4.5;
    private static final double                   DRAG_Y_OFFSET    = 13;
    private static final double                   HANDLE_HEIGHT    = 13;
    private              BooleanBinding           showing;
    private              double                   width;
    private              double                   height;
    private              LinearGradient           gradient;
    private              Rectangle                gradientBackground;
    private              Rectangle                gradientBox;
    private              Pane                     pane;
    private              ObservableList<Handle>   handles;
    private              double                   dragX;
    private              double                   dragY;
    private              EventHandler<MouseEvent> mouseHandler;
    private              Handle                   selectedHandle;
    private              GradientLookup           lookup;
    private              ColorPicker              colorPicker;
    private              Popup                    alphaPopup;
    private              StackPane                alphaPopupPane;
    private              Rectangle                alphaPopupBounds;
    private              Slider                   alphaSlider;
    private              NumberTextField          alphaTextField;
    private              HBox                     alphaBox;
    private              Popup                    positionPopup;
    private              StackPane                positionPopupPane;
    private              Rectangle                positionPopupBounds;
    private              NumberTextField          positionTextField;
    private              HBox                     positionBox;


    // ******************** Constructors **************************************
    public GradientPicker() {
        getStylesheets().add(GradientPicker.class.getResource("gradientpicker.css").toExternalForm());
        handles      = FXCollections.observableArrayList();
        mouseHandler = e -> {
            final EventType<? extends MouseEvent> TYPE = e.getEventType();
            final Handle     handle     = (Handle) e.getSource();
            final HandleType handleType = handle.getType();
            if (TYPE.equals(MouseEvent.MOUSE_PRESSED)) {
                focusHandle(handle);
                selectedHandle = handle;
                if (e.isAltDown() || e.isSecondaryButtonDown()) {
                    if (HandleType.COLOR_HANDLE == handleType) {
                        colorPicker.setValue(handle.getFill());
                        colorPicker.show();
                    } else {
                        alphaSlider.setValue(handle.getAlpha());
                        showAlphaPopup(getScene(), e);
                    }
                } else if (e.isControlDown()) {
                    if (HandleType.COLOR_HANDLE == handleType) { showPositionPopup(getScene(), e); }
                }
                dragX = handle.getLayoutX() - e.getScreenX();
                dragY = handle.getLayoutY() - e.getScreenY();
            } else if (TYPE.equals(MouseEvent.MOUSE_DRAGGED)) {
                handle.setLayoutX(clamp(gradientBox.getX() - HANDLE_CENTER, gradientBox.getX() + gradientBox.getWidth() - HANDLE_CENTER, e.getScreenX() + dragX));
                handle.setLayoutY(clamp(gradientBox.getY() + gradientBox.getHeight(), gradientBox.getY() + gradientBox.getHeight() + DRAG_Y_OFFSET, e.getScreenY() + dragY));
                handle.setFraction((handle.getLayoutX() - gradientBox.getX() + HANDLE_CENTER) / gradientBox.getWidth());
                updateGradient();
            } else if (TYPE.equals(MouseEvent.MOUSE_RELEASED)) {
                if (handle.getLayoutY() >= gradientBox.getY() + gradientBox.getHeight() + DRAG_Y_OFFSET) {
                    if (null != handle.getLinkedHandle()) {
                        handle.getLinkedHandle().layoutXProperty().unbindBidirectional(handle.layoutXProperty());
                        handles.remove(handle.getLinkedHandle());
                    }
                    handles.remove(handle);
                } else {
                    handle.setLayoutY(gradientBox.getY() + gradientBox.getHeight());
                }
            }
        };
        lookup = new GradientLookup();
        initGraphics();
        initPopups();
        registerListeners();
    }


    // ******************** Initialization ************************************
    private void initGraphics() {
        if (Double.compare(getPrefWidth(), 0.0) <= 0 || Double.compare(getPrefHeight(), 0.0) <= 0 || Double.compare(getWidth(), 0.0) <= 0 ||
            Double.compare(getHeight(), 0.0) <= 0) {
            if (getPrefWidth() > 0 && getPrefHeight() > 0) {
                setPrefSize(getPrefWidth(), getPrefHeight());
            } else {
                setPrefSize(PREFERRED_WIDTH, PREFERRED_HEIGHT);
            }
        }

        getStyleClass().add("gradient-picker");

        gradient = new LinearGradient(0, 0, 1, 0, true, CycleMethod.NO_CYCLE);

        gradientBackground = new Rectangle(10, 10, 180, 20);
        gradientBackground.setFill(new ImagePattern(new Image(getClass().getResourceAsStream("opacitypattern.png")), 0, 5, 20, 20, false));

        gradientBox = new Rectangle(10, 10, 180, 20);
        gradientBox.setFill(gradient);
        gradientBox.setStroke(Color.web("#918c8f"));

        Tooltip tooltip = new Tooltip("Double click to add a new stop");
        Tooltip.install(gradientBox, tooltip);

        colorPicker = new ColorPicker();
        colorPicker.getStyleClass().add("button");
        colorPicker.setTranslateY(65);
        colorPicker.setVisible(false);
        colorPicker.setManaged(false);

        pane = new Pane(gradientBackground, gradientBox, colorPicker);

        getChildren().setAll(pane);
    }
    
    private void initPopups() {
        DropShadow shadow = new DropShadow();
        shadow.setBlurType(BlurType.GAUSSIAN);
        shadow.setRadius(4);
        shadow.setOffsetY(2);

        /*
        colorPicker = new ColorPicker();
        colorPicker.getStyleClass().add("button");

        colorPickerPopupPane = new StackPane();
        colorPickerPopupPane.setPadding(new Insets(10));
        colorPickerPopupBounds = new Rectangle(225, 50);
        Shape colorPickerPopupShape = createPopupShape(colorPickerPopupBounds.getWidth(), colorPickerPopupBounds.getHeight());
        colorPickerPopupPane.getChildren().setAll(colorPickerPopupShape, colorPicker);
        colorPickerPopupPane.setEffect(shadow);

        colorPickerPopup = new Popup();
        colorPickerPopup.setHideOnEscape(true);
        colorPickerPopup.setAutoHide(true);
        colorPickerPopup.getContent().add(colorPicker);
        */

        alphaSlider = new Slider();
        alphaSlider.setMin(0.0);
        alphaSlider.setMax(1.0);
        alphaSlider.setValue(1.0);
        alphaSlider.setMinWidth(150);
        alphaSlider.setFocusTraversable(false);

        alphaTextField = new NumberTextField();
        alphaTextField.setPrefWidth(55);
        alphaTextField.setDecimals(3);
        alphaTextField.setText("1.0");
        alphaTextField.setFocusTraversable(false);

        alphaBox = new HBox();
        alphaBox.setSpacing(10);
        alphaBox.setAlignment(Pos.CENTER);
        alphaBox.getChildren().addAll(alphaSlider, alphaTextField);

        Label positionLabel = new Label("Position:");
        positionLabel.setTextFill(Color.WHITE);

        positionTextField = new NumberTextField();
        positionTextField.setDecimals(3);
        positionTextField.setPrefWidth(55);
        positionTextField.setFocusTraversable(false);

        positionBox = new HBox();
        positionBox.setSpacing(10);
        positionBox.setAlignment(Pos.CENTER);
        positionBox.getChildren().addAll(positionLabel, positionTextField);

        alphaPopupPane = new StackPane();
        alphaPopupPane.setPadding(new Insets(10));
        alphaPopupBounds = new Rectangle(225, 50);
        Shape alphaPopupShape = createPopupShape(alphaPopupBounds.getWidth(), alphaPopupBounds.getHeight());
        alphaPopupPane.getChildren().setAll(alphaPopupShape, alphaBox);
        alphaPopupPane.setEffect(shadow);
        alphaPopup = new Popup();
        alphaPopup.setHideOnEscape(true);
        alphaPopup.setAutoHide(true);
        alphaPopup.getContent().add(alphaPopupPane);

        positionPopupPane = new StackPane();
        positionPopupPane.setPadding(new Insets(10));
        positionPopupBounds = new Rectangle(135, 50);
        Shape positionPopupShape = createPopupShape(positionPopupBounds.getWidth(), positionPopupBounds.getHeight());
        positionPopupPane.getChildren().setAll(positionPopupShape, positionBox);
        positionPopupPane.setEffect(shadow);
        positionPopup = new Popup();
        positionPopup.setHideOnEscape(true);
        positionPopup.setAutoHide(true);
        positionPopup.getContent().add(positionPopupPane);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
        handles.addListener((ListChangeListener<Handle>) change -> {
            while (change.next()) {
                if (change.wasAdded())    {
                    change.getAddedSubList().forEach(handle -> {
                        if (HandleType.COLOR_HANDLE == handle.getType()) {
                            handle.addEventFilter(MouseEvent.MOUSE_DRAGGED, mouseHandler);
                            handle.addEventFilter(MouseEvent.MOUSE_RELEASED, mouseHandler);
                        }
                        handle.addEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
                    });
                    updateHandles();
                    updateGradient();
                }
                if (change.wasRemoved())  {
                    change.getRemoved().forEach(handle -> {
                        if (HandleType.COLOR_HANDLE == handle.getType()) {
                            handle.removeEventFilter(MouseEvent.MOUSE_DRAGGED, mouseHandler);
                            handle.removeEventFilter(MouseEvent.MOUSE_RELEASED, mouseHandler);
                        }
                        handle.removeEventFilter(MouseEvent.MOUSE_PRESSED, mouseHandler);
                    });
                    updateHandles();
                    updateGradient();
                }
                if (change.wasReplaced()) {
                    updateHandles();
                    updateGradient();
                }
            }
        });
        gradientBox.setOnMousePressed(e -> {
            if (e.getButton().equals(MouseButton.PRIMARY)) {
                if (e.getClickCount() == 2) { addHandleAt(e); }
            }
        });
        focusedProperty().addListener(o -> focusHandle(null));

        colorPicker.setOnAction(e -> {
            selectedHandle.setFill(Color.color(colorPicker.getValue().getRed(), colorPicker.getValue().getGreen(), colorPicker.getValue().getBlue(), selectedHandle.getLinkedHandle().getAlpha()));
            colorPicker.hide();
            updateGradient();
            updateHandles();
        });

        positionTextField.setOnAction(actionEvent -> {
            if (!positionTextField.getText().isEmpty() && null != selectedHandle) {
                selectedHandle.setFraction(Double.parseDouble(positionTextField.getText()));
                updateHandles();
                updateGradient();
                positionPopup.hide();
            }
        });
        alphaSlider.valueProperty().addListener(o -> {
            if (null != selectedHandle) {
                double alpha = alphaSlider.getValue();
                selectedHandle.setAlpha(alpha);
                selectedHandle.setFill(Color.rgb(0, 0, 0, alpha));
                updateGradient();
            }
        });
        alphaSlider.setOnMouseReleased(mouseEvent -> alphaPopup.hide());
        alphaTextField.setOnAction(actionEvent -> alphaPopup.hide());
        alphaTextField.textProperty().bindBidirectional(alphaSlider.valueProperty(), new StringConverter<Number>() {
            @Override public String toString(Number number) { return String.format(Locale.US, "%.3f", number); }
            @Override public Number fromString(String text) { return (null == text || text.isEmpty()) ? 0.0 : Double.parseDouble(text); }
        });
        
        if (null != getScene()) {
            setupBinding();
        } else {
            sceneProperty().addListener((o1, ov1, nv1) -> {
                if (null == nv1) { return; }
                if (null != getScene().getWindow()) {
                    setupBinding();
                } else {
                    sceneProperty().get().windowProperty().addListener((o2, ov2, nv2) -> {
                        if (null == nv2) { return; }
                        setupBinding();
                    });
                }
            });
        }
    }


    // ******************** Methods *******************************************
    @Override public void layoutChildren() {
        super.layoutChildren();
    }

    @Override protected double computeMinWidth(final double HEIGHT) { return MINIMUM_WIDTH; }
    @Override protected double computeMinHeight(final double WIDTH) { return MINIMUM_HEIGHT; }
    @Override protected double computePrefWidth(final double HEIGHT) { return super.computePrefWidth(HEIGHT); }
    @Override protected double computePrefHeight(final double WIDTH) { return super.computePrefHeight(WIDTH); }
    @Override protected double computeMaxWidth(final double HEIGHT) { return MAXIMUM_WIDTH; }
    @Override protected double computeMaxHeight(final double WIDTH) { return MAXIMUM_HEIGHT; }

    @Override public ObservableList<Node> getChildren() { return super.getChildren(); }

    public List<Stop> getStops() { return gradient.getStops(); }
    public String getStopsAsString(final boolean CSS) {
        StringBuilder gradientString = new StringBuilder();
        for (Stop stop : gradient.getStops()) {
            if (CSS) {
                gradientString.append("rgba(")
                              .append((int) (stop.getColor().getRed() * 255))
                              .append(", ")
                              .append((int) (stop.getColor().getGreen() * 255))
                              .append(", ")
                              .append((int) (stop.getColor().getBlue() * 255))
                              .append(", ")
                              .append(String.format(Locale.US, "%.3f", stop.getColor().getOpacity()))
                              .append(") ");
                gradientString.append(Math.floor(stop.getOffset() * 100)).append("%");
            } else {
                gradientString.append("new Stop(")
                              .append(String.format(Locale.US, "%.3f", stop.getOffset()))
                              .append(", Color.rgb(")
                              .append((int) (stop.getColor().getRed() * 255))
                              .append(", ")
                              .append((int) (stop.getColor().getGreen() * 255))
                              .append(", ")
                              .append((int) (stop.getColor().getBlue() * 255))
                              .append(", ")
                              .append(String.format(Locale.US, "%.2f", stop.getColor().getOpacity()))
                              .append("))");
            }
            if (stop.equals(gradient.getStops().get(gradient.getStops().size() - 1))) {
                gradientString.append("\n");
            } else {
                gradientString.append(",\n");
            }
        }
        return gradientString.toString();
    }

    public boolean isShowing() { return null == showing ? false : showing.get(); }

    private void setupBinding() {
        showing = Bindings.selectBoolean(sceneProperty(), "window", "showing");
        showing.addListener((o, ov, nv) -> {
            if (nv) {
                addHandle(new Handle(HandleType.COLOR_HANDLE, 0.0, Color.WHITE));
                addHandle(new Handle(HandleType.COLOR_HANDLE, 1.0, Color.BLACK));
            }
        });
    }

    private void addHandleAt(final MouseEvent MOUSE_EVENT) {
        double fraction    = (MOUSE_EVENT.getX() - gradientBox.getX()) / gradientBox.getWidth();
        Color  pickedColor = calculateColor(fraction);
        Handle colorHandle = new Handle(HandleType.COLOR_HANDLE, fraction, pickedColor);
        addHandle(colorHandle);
    }

    private void addHandle(final Handle HANDLE) {
        double fraction = HANDLE.getFraction();
        double alpha    = calculateOpacity(fraction);

        HANDLE.setLayoutX((fraction* gradientBox.getWidth()));
        HANDLE.setLayoutY(gradientBox.getY() + gradientBox.getHeight());

        Handle alphaHandle = new Handle(HandleType.ALPHA_HANDLE, fraction, alpha);
        alphaHandle.setLayoutX((fraction * gradientBox.getWidth()));
        alphaHandle.setLayoutY(gradientBox.getY() - HANDLE_HEIGHT);

        HANDLE.setLinkedHandle(alphaHandle);

        focusHandle(HANDLE);
        handles.addAll(alphaHandle, HANDLE);
    }

    private void updateHandles() {
        pane.getChildren().setAll(gradientBackground, gradientBox, colorPicker);
        for (Handle handle : handles) {
            if (HandleType.COLOR_HANDLE == handle.getType()) {
                handle.setLayoutX(gradientBox.getX() + (handle.getFraction() * gradientBox.getWidth()) - HANDLE_CENTER);
                handle.setLayoutY(gradientBox.getY() + gradientBox.getHeight());
            } else {
                handle.setLayoutY(gradientBox.getY() - HANDLE_HEIGHT);
            }
            
            pane.getChildren().addAll(handle);
        }
    }

    private void updateGradient() {
        List<Stop> stops = handles.stream()
                                  .sorted(Comparator.naturalOrder())
                                  .filter(handle -> HandleType.COLOR_HANDLE == handle.getType())
                                  .map(handle -> new Stop(handle.getFraction(), Color.color(handle.getFill().getRed(), handle.getFill().getGreen(), handle.getFill().getBlue(), handle.getLinkedHandle().getAlpha())))
                                  .collect(Collectors.toList());
        gradient = new LinearGradient(0.0, 0.0, 1.0, 0.0, true, CycleMethod.NO_CYCLE, stops);
        gradientBox.setFill(gradient);
    }

    private Color calculateColor(final double FRACTION) {
        lookup.setStops(gradient.getStops());
        return lookup.getColorAt(FRACTION);
    }

    private double calculateOpacity(final double FRACTION) {
        FXCollections.sort(handles);
        Handle lowerBound = null;
        Handle upperBound = null;
        for (Handle handle : handles) {
            if (FRACTION > handle.getFraction()) {
                lowerBound = handle;
            } else if (FRACTION < handle.getFraction()) {
                upperBound = handle;
                break;
            }
        }
        double alpha = 1.0;
        if (null != lowerBound && null != upperBound) {
            double deltaFraction  = upperBound.getFraction() - lowerBound.getFraction();
            //double deltaOpacity   = Math.abs(upperBound.getOpacityHandle().getColorOpacity() - lowerBound.getOpacityHandle().getColorOpacity());
            //double fractionFactor = 1.0 / deltaFraction * (FRACTION - lowerBound.getFraction());
            //alpha = fractionFactor * deltaOpacity + lowerBound.getOpacityHandle().getColorOpacity();
            alpha = 1.0;
        }
        return alpha;
    }

    private void focusHandle(final Handle HANDLE) {
        handles.forEach(handle -> handle.setFocus(false));
        if (null == HANDLE) return;
        HANDLE.setFocus(true);
    }

    private Shape createPopupShape(final double WIDTH, final double HEIGHT) {
        Rectangle shape = new Rectangle(WIDTH, HEIGHT);
        shape.setArcWidth(10);
        shape.setArcHeight(10);

        LinearGradient gradient =
            new LinearGradient(0, shape.getLayoutBounds().getMinY(), 0, shape.getLayoutBounds().getMinY() + shape.getLayoutBounds().getHeight(), false, CycleMethod.NO_CYCLE,
                               new Stop(0.0, Color.rgb(48, 48, 48, 0.8)),
                               new Stop(1.0, Color.rgb(33, 33, 33, 0.8)));
        shape.setFill(gradient);

        InnerShadow innerShadow = new InnerShadow();
        innerShadow.setRadius(4);
        innerShadow.setBlurType(BlurType.GAUSSIAN);
        innerShadow.setColor(Color.rgb(255, 255, 255, 0.6));
        innerShadow.setOffsetX(0);
        innerShadow.setOffsetY(0);

        shape.setEffect(innerShadow);

        return shape;
    }

    private void showAlphaPopup(final Scene SCENE, final MouseEvent EVENT) {
        if (null == SCENE) return;
        final double popupX = EVENT.getScreenX() - HANDLE_CENTER - alphaPopupBounds.getWidth() * 0.5;
        final double popupY = EVENT.getScreenY() - alphaPopupBounds.getHeight() * 1.7;
        alphaPopup.show(SCENE.getWindow(), popupX, popupY);
    }
    
    private void showPositionPopup(final Scene SCENE, final MouseEvent EVENT) {
        if (null == SCENE) return;
        final double popupX = EVENT.getScreenX() - positionPopupBounds.getWidth() * 0.5;
        final double popupY = EVENT.getScreenY() + 10;
        positionTextField.setText(String.format(Locale.US, "%.3f", selectedHandle.getFraction()));
        positionPopup.show(SCENE.getWindow(), popupX, popupY);
    }


    // ******************** Resizing ******************************************
    private void resize() {
        width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();

        if (width > 0 && height > 0) {
            pane.setMaxSize(width, height);
            pane.setPrefSize(width, height);
            pane.relocate((getWidth() - width) * 0.5, (getHeight() - height) * 0.5);

            gradientBackground.setWidth(width - PADDING_RIGHT);
            gradientBackground.setHeight(BOX_HEIGHT);
            gradientBackground.setX(PADDING_LEFT);
            gradientBackground.setY((height - BOX_HEIGHT) * 0.5);

            gradientBox.setWidth(width - PADDING_RIGHT);
            gradientBox.setHeight(BOX_HEIGHT);
            gradientBox.setX(PADDING_LEFT);
            gradientBox.setY((height - BOX_HEIGHT) * 0.5);

            updateHandles();
            updateGradient();
        }
    }
}
