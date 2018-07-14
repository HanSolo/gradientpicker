/*
 * Copyright (c) 2018 by Gerrit Grunwald
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.hansolo.fx.gradientpicker.tool;

import javafx.beans.value.ChangeListener;
import javafx.scene.control.TextField;


public class NumberTextField extends TextField {
    private int                    noBeforeDecimalPoint;
    private int                    decimals;
    private ChangeListener<String> textListener;
    private String                 regex;


    // ******************** Constructors **************************************
    public NumberTextField() {
        this("", 1, 3);
    }
    public NumberTextField(final String CONTENT) {
        this(CONTENT,1, 3 );
    }
    public NumberTextField(final String CONTENT, final int NO_BEFORE_DECIMALPOINT, final int DECIMALS) {
        super(CONTENT);
        noBeforeDecimalPoint = NO_BEFORE_DECIMALPOINT;
        decimals             = DECIMALS;
        regex                = "\\d{0," + noBeforeDecimalPoint + "}([\\.]\\d{0," + decimals + "})?";
        textListener         = (o, ov, nv) -> { if (!nv.matches(regex)) { setText(ov); } };
        registerListeners();
    }

    private void registerListeners() {
        textProperty().addListener(textListener);
    }


    // ******************** Methods *******************************************
    public int getNoBeforeDecimalPoint() { return noBeforeDecimalPoint; }
    public void setNoBeforeDecimalPoint(final int AMOUNT) {
        if (AMOUNT < 0) { throw new IllegalArgumentException("Amount of numbers cannot be negative"); }
        noBeforeDecimalPoint = AMOUNT;
        updateRegex();
    }

    public int getDecimals() { return decimals; }
    public void setDecimals(final int DECIMALS) {
        if (DECIMALS < 0) { throw new IllegalArgumentException("No of decimals cannot be negative"); }
        decimals = DECIMALS;
        updateRegex();
    }

    private void updateRegex() {
        regex = "\\d{0," + noBeforeDecimalPoint + "}([\\.]\\d{0," + decimals + "})?";
    }
}
