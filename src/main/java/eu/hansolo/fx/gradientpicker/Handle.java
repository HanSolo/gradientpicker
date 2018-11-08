package eu.hansolo.fx.gradientpicker;


import eu.hansolo.fx.gradientpicker.event.HandleEvent;
import eu.hansolo.fx.gradientpicker.event.HandleObserver;
import eu.hansolo.fx.gradientpicker.event.HandleEventType;
import eu.hansolo.fx.gradientpicker.tool.Helper;
import javafx.beans.DefaultProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.DoublePropertyBase;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.ClosePath;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.StrokeType;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static eu.hansolo.fx.gradientpicker.tool.Helper.clamp;


/**
 * User: hansolo
 * Date: 13.07.18
 * Time: 12:52
 */
@DefaultProperty("children")
public class Handle extends Region implements Comparable<Handle>{
    public  static final double                     HANDLE_SIZE      = 12;
    private static final double                     PREFERRED_WIDTH  = HANDLE_SIZE;
    private static final double                     PREFERRED_HEIGHT = HANDLE_SIZE;
    private static final double                     MINIMUM_WIDTH    = HANDLE_SIZE;
    private static final double                     MINIMUM_HEIGHT   = HANDLE_SIZE;
    private static final double                     MAXIMUM_WIDTH    = HANDLE_SIZE;
    private static final double                     MAXIMUM_HEIGHT   = HANDLE_SIZE;
    private static final double                     ASPECT_RATIO     = PREFERRED_HEIGHT / PREFERRED_WIDTH;
    private        final HandleEvent                COLOR_EVENT      = new HandleEvent(Handle.this, HandleEventType.COLOR);
    private        final HandleEvent                ALPHA_EVENT      = new HandleEvent(Handle.this, HandleEventType.ALPHA);
    private        final HandleEvent           FRACTION_EVENT   = new HandleEvent(Handle.this, HandleEventType.FRACTION);
    private        final HandleEvent           TYPE_EVENT       = new HandleEvent(Handle.this, HandleEventType.TYPE);
    private              List<HandleObserver>  observers;
    private              double                size;
    private              double                width;
    private              double                height;
    private              Circle                path;
    private              Color                 _fill;
    private              ObjectProperty<Color> fill;
    private              Color                 _stroke;
    private              ObjectProperty<Color> stroke;
    private              double                _alpha;
    private              DoubleProperty        alpha;
    private              double                _fraction;
    private              DoubleProperty        fraction;
    private              HandleType            _type;
    private              ObjectProperty<HandleType> type;
    private              Color                      _focusColor;
    private              ObjectProperty<Color>      focusColor;
    private              Handle                     linkedHandle;
    private              Tooltip                    tooltip;
    private              Map<HandleType, String>    tooltipTexts;


    // ******************** Constructors **************************************
    public Handle() {
        this(HandleType.COLOR_HANDLE, 0, Color.web("#282828"), 1.0, Color.web("#282828"));
    }
    public Handle(final HandleType TYPE, final double FRACTION, final Color FILL) {
        this(TYPE, FRACTION, FILL, 1.0, Color.web("#282828"));
    }
    public Handle(final HandleType TYPE, final double FRACTION, final double ALPHA) {
        this(TYPE, FRACTION, Color.web("#282828"), ALPHA, Color.web("#282828"));
    }
    public Handle(final HandleType TYPE, final double FRACTION, final Color FILL, final double ALPHA, final Color STROKE) {
        observers    = new CopyOnWriteArrayList<>();
        _type        = TYPE;
        _fraction    = FRACTION;
        _fill        = FILL;
        _alpha       = ALPHA;
        _stroke      = STROKE;
        _focusColor  = Color.web("#039ED3");
        linkedHandle = null;
        tooltipTexts = new HashMap<>(2);
        tooltipTexts.put(HandleType.ALPHA_HANDLE, "Right mouse button to set alpha");
        tooltipTexts.put(HandleType.COLOR_HANDLE, "Right mouse button to select color\nCTRL + left mouse button to set fraction\nDrag down to remove handle");
        tooltip      = new Tooltip(tooltipTexts.get(_type));

        initGraphics();
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

        path = new Circle(HANDLE_SIZE * 0.5);
        path.setStrokeWidth(1);
        path.setFill(getFill());
        path.setStroke(getStroke());
        path.setStrokeType(StrokeType.INSIDE);

        setShape(path);

        setBackground(new Background(new BackgroundFill(getStroke(), CornerRadii.EMPTY, Insets.EMPTY),
                                     new BackgroundFill(Color.web("#dbdbdb"), CornerRadii.EMPTY, new Insets(1)),
                                     new BackgroundFill(getFill(), CornerRadii.EMPTY, new Insets(2))));

        if (HandleType.COLOR_HANDLE == getType()) { setRotate(180); }

        Tooltip.install(Handle.this, tooltip);
    }

    private void registerListeners() {
        widthProperty().addListener(o -> resize());
        heightProperty().addListener(o -> resize());
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

    @Override public int compareTo(final Handle HANDLE) { return Double.compare(getFraction(), HANDLE.getFraction()); }

    public Color getFill() { return null == fill ? _fill : fill.get(); }
    public void setFill(final Color FILL) {
        if (null == fill) {
            _fill = FILL;
            updateBackground();
            fireHandleEvent(COLOR_EVENT);
        } else {
            fill.set(FILL);
        }
    }
    public ObjectProperty<Color> fillProperty() {
        if (null == fill) {
            fill = new ObjectPropertyBase<Color>() {
                @Override protected void invalidated() {
                    updateBackground();
                    fireHandleEvent(COLOR_EVENT);
                }
                @Override public Object getBean() { return Handle.this; }
                @Override public String getName() { return "fill"; }
            };
            _fill = null;
        }
        return fill;
    }

    public Color getStroke() { return null == stroke ? _stroke : stroke.get(); }
    public void setStroke(final Color STROKE) {
        if (null == stroke) {
            _stroke = STROKE;
            updateBackground();
        } else {
            stroke.set(STROKE);
        }
    }
    public ObjectProperty<Color> strokeProperty() {
        if (null == stroke) {
            stroke = new ObjectPropertyBase<Color>() {
                @Override protected void invalidated() { updateBackground(); }
                @Override public Object getBean() { return Handle.this; }
                @Override public String getName() { return "stroke"; }
            };
            _stroke = null;
        }
        return stroke;
    }

    public double getAlpha() { return null == alpha ? _alpha : alpha.get(); }
    public void setAlpha(final double ALPHA) {
        if (null == alpha) {
            _alpha = Helper.clamp(0.0, 1.0, ALPHA);
            fireHandleEvent(ALPHA_EVENT);
        } else {
            alpha.set(ALPHA);
        }
    }
    public DoubleProperty alphaProperty() {
        if (null == alpha) {
            alpha = new DoublePropertyBase() {
                @Override protected void invalidated() {
                    set(Helper.clamp(0.0, 1.0, get()));
                    fireHandleEvent(ALPHA_EVENT);
                }
                @Override public Object getBean() { return Handle.this; }
                @Override public String getName() { return "alpha"; }
            };
        }
        return alpha;
    }

    public double getFraction() { return null == fraction ? _fraction : fraction.get(); }
    public void setFraction(final double FRACTION) {
        if (null == fraction) {
            _fraction = Helper.clamp(0.0, 1.0, FRACTION);
            fireHandleEvent(FRACTION_EVENT);
        } else {
            fraction.set(FRACTION);
        }
    }
    public DoubleProperty fractionProperty() {
        if (null == fraction) {
            fraction = new DoublePropertyBase() {
                @Override protected void invalidated() {
                    set(Helper.clamp(0.0, 1.0, get()));
                    fireHandleEvent(FRACTION_EVENT);
                }
                @Override public Object getBean() { return Handle.this; }
                @Override public String getName() { return "fraction"; }
            };
        }
        return fraction;
    }

    public HandleType getType() { return null == type ? _type : type.get(); }
    public void setType(final HandleType TYPE) {
        if (null == type) {
            _type = TYPE;
            setRotate(HandleType.COLOR_HANDLE == TYPE ? 180 : 0);
            fireHandleEvent(TYPE_EVENT);
        } else {
            type.set(TYPE);
        }
    }
    public ObjectProperty<HandleType> typeProperty() {
        if (null == type) {
            type = new ObjectPropertyBase<HandleType>() {
                @Override protected void invalidated() {
                    setRotate(HandleType.COLOR_HANDLE == get() ? 180 : 0);
                    fireHandleEvent(TYPE_EVENT);
                }
                @Override public Object getBean() { return Handle.this; }
                @Override public String getName() { return "type"; }
            };
            _type = null;
        }
        return type;
    }

    public Color getFocusColor() { return null == focusColor ? _focusColor : focusColor.get(); }
    public void setFocusColor(final Color COLOR) {
        if (null == focusColor) {
            _focusColor = COLOR;
        } else {
            focusColor.set(COLOR);
        }
    }
    public ObjectProperty<Color> focusColorProperty() {
        if (null == focusColor) {
            focusColor = new ObjectPropertyBase<Color>() {
                @Override public Object getBean() { return Handle.this; }
                @Override public String getName() { return "focusColor"; }
            };
            _focusColor = null;
        }
        return focusColor;
    }

    public String getTooltipText() { return tooltipTexts.get(getType()); }
    public void setTooltipText(final String TEXT) { tooltipTexts.put(getType(), TEXT); }

    public Handle getLinkedHandle() { return linkedHandle; }
    public void setLinkedHandle(final Handle HANDLE) {
        if (null == HANDLE) return;
        if (null != linkedHandle) {
            linkedHandle.layoutXProperty().unbindBidirectional(Handle.this.layoutXProperty());
        }
        linkedHandle = HANDLE;
        linkedHandle.layoutXProperty().bindBidirectional(Handle.this.layoutXProperty());

    }

    public void setFocus(final boolean FOCUSED) { path.setStroke(FOCUSED ? getFocusColor() : getStroke()); }

    private Color createColor() {
        Color fill = getFill();
        return Color.color(fill.getRed(), fill.getGreen(), fill.getBlue(), getAlpha());
    }

    private void updateBackground() {
        setBackground(new Background(new BackgroundFill(getStroke(), CornerRadii.EMPTY, Insets.EMPTY),
                                     new BackgroundFill(Color.WHITE, CornerRadii.EMPTY, new Insets(1)),
                                     new BackgroundFill(createColor(), CornerRadii.EMPTY, new Insets(2))));
    }


    // ******************** EventHandling *************************************
    public void addHandleEventListener(final HandleObserver OBSERVER) {
        if (!observers.contains(OBSERVER)) { observers.add(OBSERVER); }
    }
    public void removeHandleEventListener(final HandleObserver OBSERVER) {
        if (observers.contains(OBSERVER)) { observers.remove(OBSERVER); }
    }
    public void fireHandleEvent(final HandleEvent EVENT) {
        observers.forEach(observer -> observer.onHandleEvent(EVENT));
    }


    // ******************** Resizing ******************************************
    private void resize() {
        width  = getWidth() - getInsets().getLeft() - getInsets().getRight();
        height = getHeight() - getInsets().getTop() - getInsets().getBottom();
        size   = width < height ? width : height;

        if (ASPECT_RATIO * width > height) {
            width = 1 / (ASPECT_RATIO / height);
        } else if (1 / (ASPECT_RATIO / height) > width) {
            height = ASPECT_RATIO * width;
        }

        if (width > 0 && height > 0) {
            setMaxSize(width, height);
            setPrefSize(width, height);
        }
    }
}
