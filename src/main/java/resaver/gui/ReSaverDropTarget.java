/*
 * Copyright 2019 Mark.
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
package resaver.gui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Logger;

/**
 * Handles file-drop events.
 *
 * @author Mark Fairchild
 *
 */
@SuppressWarnings("serial")
public class ReSaverDropTarget extends DropTarget {

    public ReSaverDropTarget(Consumer<Path> handler) {
        this.HANDLER = Objects.requireNonNull(handler);
    }
    
    /**
     *
     * @param event
     * 
     */
    @Override
    public synchronized void drop(DropTargetDropEvent event) {
        try {
            Objects.requireNonNull(event, "The event must not be null.");
            event.acceptDrop(DnDConstants.ACTION_COPY);

            final Transferable TRANSFER = event.getTransferable();
            Objects.requireNonNull(TRANSFER, "The DnD transferable must not be null.");

            final Object DATA = TRANSFER.getTransferData(DataFlavor.javaFileListFlavor);
            Objects.requireNonNull(DATA, "The DnD data block must not be null.");

            if (DATA instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                final java.util.List<File> FILES = (java.util.List<File>) DATA;
                FILES.stream()
                        .map(f -> f.toPath())
                        .findFirst()
                        .ifPresent(HANDLER);
            }
        } catch (UnsupportedFlavorException | IOException ex) {
            LOG.warning(String.format("Drop and drop problem: %s", ex.getMessage()));
        }
    }

    final private Consumer<Path> HANDLER;
    static final private Logger LOG = Logger.getLogger(ReSaverDropTarget.class.getCanonicalName());
}
