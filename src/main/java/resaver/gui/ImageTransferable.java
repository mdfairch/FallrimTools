/*
 * Copyright 2023 Mark.
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

import java.util.Objects;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

/**
 * Put image in clipboard.
 * @author Mark
 */
class ImageTransferable implements Transferable {
    
    static void CopyToClipboard(Image image)
    {
        ImageTransferable transfer = new ImageTransferable(image);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(transfer, null);
    }
    
    private ImageTransferable(Image image) 
    {
        this.IMAGE = Objects.requireNonNull(image);
    }
    
    final private Image IMAGE;

    @Override
    public DataFlavor[] getTransferDataFlavors() {
         return new DataFlavor[] { DataFlavor.imageFlavor };
    }

    @Override
    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.imageFlavor.equals(flavor);
    }

    @Override
    public Image getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
        if (!isDataFlavorSupported(flavor)) throw new UnsupportedFlavorException(flavor);
        return this.IMAGE;
    }
}
