/*
 * Copyright 2016 Mark Fairchild.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package resaver;

import java.util.logging.Logger;
import javax.swing.DefaultBoundedRangeModel;
import resaver.gui.Saver;

/**
 *
 * @author Mark Fairchild
 */
final public class ProgressModel extends DefaultBoundedRangeModel {

    public ProgressModel() {
        this(18);
    }

    public ProgressModel(int max) {
        super(0, 0, 0, max);
    }

    public ProgressModel(double max) {
        this((int) Math.round(max));
    }

    synchronized public void modifyValue(int delta) {
        super.setValue(this.getValue() + delta);
        //LOG.info(String.format("Progress: %d/%d (%d)", this.getValue(), this.getMaximum(), delta));

    }

    synchronized public void modVSq(double delta) {
        this.modifyValue((int) (Math.sqrt(delta)));
    }

    @Override
    synchronized public void setValue(int n) {
        super.setValue(n);
    }

    static final private Logger LOG = Logger.getLogger(Saver.class.getCanonicalName());
}
