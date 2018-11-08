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

package eu.hansolo.fx.gradientpicker.event;

import eu.hansolo.fx.gradientpicker.GradientPicker;


public class GradientEvent {
    private final GradientPicker    GRADIENT_PICKER;
    private final GradientEventType TYPE;


    // ******************** Constructors **************************************
    public GradientEvent(final GradientPicker SRC, final GradientEventType TYPE) {
        this.GRADIENT_PICKER = SRC;
        this.TYPE            = TYPE;
    }


    // ******************** Methods *******************************************
    public GradientPicker getSource() { return GRADIENT_PICKER; }

    public GradientEventType getType() { return TYPE; }
}
