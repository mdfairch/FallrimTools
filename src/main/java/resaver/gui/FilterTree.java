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
package resaver.gui;

import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.tree.*;
import resaver.ess.*;
import resaver.ess.papyrus.*;
import resaver.gui.FilterTreeModel.Node;

/**
 * A JTree that supports filtering.
 *
 * @author Mark Fairchild
 */
@SuppressWarnings("serial")
final public class FilterTree extends JTree {

    /**
     * Creates a new <code>FilterTree</code>.
     */
    public FilterTree() {
        super(new FilterTreeModel());
        this.deleteHandler = null;
        this.deleteFormsHandler = null;
        this.deleteInstancesHandler = null;
        this.purgeHandler = null;
        this.editHandler = null;
        this.MI_PURGE = new JMenuItem("Purge (1 plugin)", KeyEvent.VK_P);
        this.MI_PURGES = new JMenuItem("Purge (%d plugins)", KeyEvent.VK_P);
        this.MI_DELETE = new JMenuItem("Delete", KeyEvent.VK_D);
        this.MI_FILTER = new JMenuItem("Set filter for this plugin", KeyEvent.VK_F);
        this.MI_DELETE_FORMS = new JMenuItem("Delete plugin changeforms", KeyEvent.VK_C);
        this.MI_DELETE_INSTANCES = new JMenuItem("Delete plugin script instances", KeyEvent.VK_S);
        this.MI_ZERO_THREAD = new JMenuItem("Terminate", KeyEvent.VK_Z);
        this.MI_FIND_OWNER = new JMenuItem("Find owner", KeyEvent.VK_F);
        this.MI_CLEANSE_FLST = new JMenuItem("Cleanse Formlist", KeyEvent.VK_C);
        this.MI_COMPRESS_UNCOMPRESSED = new JRadioButtonMenuItem("No compression");
        this.MI_COMPRESS_ZLIB = new JRadioButtonMenuItem("ZLib compression");
        this.MI_COMPRESS_LZ4 = new JRadioButtonMenuItem("LZ4 compression");
        this.TREE_POPUP_MENU = new JPopupMenu();
        this.PLUGIN_POPUP_MENU = new JPopupMenu();
        this.COMPRESSION_POPUP_MENU = new JPopupMenu();
        this.COMPRESSION_GROUP = new ButtonGroup();
        this.initComponents();
    }

    /**
     * Initialize the swing and AWT components.
     */
    private void initComponents() {
        this.setLargeModel(true);
        this.setRootVisible(true);
        this.setShowsRootHandles(true);

        this.TREE_POPUP_MENU.add(this.MI_DELETE);
        this.TREE_POPUP_MENU.add(this.MI_ZERO_THREAD);
        this.TREE_POPUP_MENU.add(this.MI_FIND_OWNER);
        this.TREE_POPUP_MENU.add(this.MI_CLEANSE_FLST);
        this.TREE_POPUP_MENU.add(this.MI_PURGES);
        this.PLUGIN_POPUP_MENU.add(this.MI_PURGE);
        this.PLUGIN_POPUP_MENU.add(this.MI_FILTER);
        this.PLUGIN_POPUP_MENU.add(this.MI_DELETE_FORMS);
        this.PLUGIN_POPUP_MENU.add(this.MI_DELETE_INSTANCES);

        this.COMPRESSION_POPUP_MENU.add(this.MI_COMPRESS_UNCOMPRESSED);
        this.COMPRESSION_POPUP_MENU.add(this.MI_COMPRESS_ZLIB);
        this.COMPRESSION_POPUP_MENU.add(this.MI_COMPRESS_LZ4);

        COMPRESSION_GROUP.add(this.MI_COMPRESS_UNCOMPRESSED);
        COMPRESSION_GROUP.add(this.MI_COMPRESS_ZLIB);
        COMPRESSION_GROUP.add(this.MI_COMPRESS_LZ4);

        this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "deleteSelected");
        this.getActionMap().put("deleteSelected", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                deleteNodes();
            }
        });

        this.MI_DELETE.addActionListener(e -> deleteNodes());

        this.MI_FILTER.addActionListener(e -> {
            if (null != this.pluginFilterHandler) {
                Plugin plugin = (Plugin) ((Node) getSelectionPath().getLastPathComponent()).getElement();
                this.pluginFilterHandler.accept(plugin);
            }
        });

        this.MI_PURGE.addActionListener(e -> {
            Plugin plugin = (Plugin) ((Node) getSelectionPath().getLastPathComponent()).getElement();
            if (null != this.purgeHandler) {
                this.purgeHandler.accept(Collections.singleton(plugin));
            }
        });

        this.MI_PURGES.addActionListener(e -> {
            if (null != this.purgeHandler) {
                final TreePath[] PATHS = getSelectionPaths();
                if (null == PATHS || PATHS.length == 0) {
                    return;
                }

                final Map<Element, Node> ELEMENTS = getModel().parsePaths(PATHS);
                final List<Plugin> PLUGINS = ELEMENTS.keySet()
                        .stream()
                        .filter(v -> v instanceof Plugin)
                        .map(v -> (Plugin) v)
                        .collect(Collectors.toList());
                this.purgeHandler.accept(PLUGINS);
            }
        });

        this.MI_DELETE_FORMS.addActionListener(e -> {
            Plugin plugin = (Plugin) ((Node) getSelectionPath().getLastPathComponent()).getElement();
            if (null != this.deleteFormsHandler) {
                this.deleteFormsHandler.accept(plugin);
            }
        });

        this.MI_DELETE_INSTANCES.addActionListener(e -> {
            Plugin plugin = (Plugin) ((Node) getSelectionPath().getLastPathComponent()).getElement();
            if (null != this.deleteInstancesHandler) {
                this.deleteInstancesHandler.accept(plugin);
            }
        });

        this.MI_ZERO_THREAD.addActionListener(e -> {
            if (null != this.zeroThreadHandler) {
                final TreePath[] PATHS = getSelectionPaths();
                if (null == PATHS || PATHS.length == 0) {
                    return;
                }

                final Map<Element, Node> ELEMENTS = getModel().parsePaths(PATHS);
                final List<ActiveScript> THREADS = ELEMENTS.keySet()
                        .stream()
                        .filter(ESS.THREAD)
                        .map(v -> (ActiveScript) v)
                        .collect(Collectors.toList());
                this.zeroThreadHandler.accept(THREADS);
            }
        });

        this.MI_FIND_OWNER.addActionListener(e -> {
            Element element = ((Node) getSelectionPath().getLastPathComponent()).getElement();
            if (null != this.findHandler) {
                if (element instanceof ActiveScript) {
                    ActiveScript script = (ActiveScript) element;
                    if (null != script.getInstance()) {
                        this.findHandler.accept(script.getInstance());
                    }
                } else if (element instanceof StackFrame) {
                    StackFrame frame = (StackFrame) element;
                    Variable owner = frame.getOwner();
                    if (null != owner && owner instanceof Variable.Ref) {
                        Variable.Ref ref = (Variable.Ref) frame.getOwner();
                        this.findHandler.accept(ref.getReferent());
                    }
                } else if (element instanceof ArrayInfo) {
                    ArrayInfo array = (ArrayInfo) element;
                    if (null != array.getHolder()) {
                        this.findHandler.accept(array.getHolder());
                    }

                }
            }

        });

        this.MI_CLEANSE_FLST.addActionListener(e -> {
            ChangeForm form = (ChangeForm) ((Node) getSelectionPath().getLastPathComponent()).getElement();
            /*
            ChangeFormFLST flst = (ChangeFormFLST) form.getData();
            if (null != this.cleanFLSTHandler) {
                this.cleanFLSTHandler.accept(flst);
            }*/
        });

        this.MI_COMPRESS_UNCOMPRESSED.addActionListener(e -> {
            if (this.compressionHandler != null) {
                this.compressionHandler.accept(CompressionType.UNCOMPRESSED);
            }
        });
        this.MI_COMPRESS_ZLIB.addActionListener(e -> {
            if (this.compressionHandler != null) {
                this.compressionHandler.accept(CompressionType.ZLIB);
            }
        });
        this.MI_COMPRESS_LZ4.addActionListener(e -> {
            if (this.compressionHandler != null) {
                this.compressionHandler.accept(CompressionType.LZ4);
            }
        });

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent evt) {
                Objects.requireNonNull(editHandler);
                
                if (evt.isPopupTrigger()) {
                    int x = evt.getPoint().x;
                    int y = evt.getPoint().y;

                    TreePath path = getClosestPathForLocation(x, y);
                    TreePath[] paths = getSelectionPaths();

                    if (!Arrays.asList(paths).contains(path)) {
                        setSelectionPath(path);
                        paths = getSelectionPaths();
                    }

                    final Map<Element, Node> ELEMENTS = getModel().parsePaths(paths);
                    
                    if (ELEMENTS.size() == 1) {
                        MI_PURGES.setEnabled(false);
                        final Element ELEMENT = ELEMENTS.keySet().iterator().next();

                        if (ELEMENT instanceof ESS) {
                            final ESS ESS = (ESS) ELEMENT;
                            if (ESS.supportsCompression()) {
                                switch (ESS.getHeader().getCompression()) {
                                    case UNCOMPRESSED:
                                        MI_COMPRESS_UNCOMPRESSED.setSelected(true);
                                        break;
                                    case ZLIB:
                                        MI_COMPRESS_ZLIB.setSelected(true);
                                        break;
                                    case LZ4:
                                        MI_COMPRESS_LZ4.setSelected(true);
                                        break;
                                }
                                COMPRESSION_POPUP_MENU.show(evt.getComponent(), evt.getX(), evt.getY());
                            }
                        } else if (ELEMENT instanceof GlobalVariable) {
                            editHandler.accept((GlobalVariable) ELEMENT);

                        } else if (ELEMENT instanceof Plugin) {
                            PLUGIN_POPUP_MENU.show(evt.getComponent(), evt.getX(), evt.getY());

                        } else if (ESS.DELETABLE.test(ELEMENT) || ESS.THREAD.test(ELEMENT) || ESS.OWNABLE.test(ELEMENT)) {
                            MI_DELETE.setText("Delete (1 element)");
                            MI_DELETE.setVisible(ESS.DELETABLE.test(ELEMENT));
                            MI_ZERO_THREAD.setVisible(ESS.THREAD.test(ELEMENT));
                            MI_FIND_OWNER.setVisible(ESS.OWNABLE.test(ELEMENT));
                            MI_CLEANSE_FLST.setVisible(ELEMENT instanceof ChangeForm && ((ChangeForm) ELEMENT).getType() == ChangeForm.Type.FLST);
                            TREE_POPUP_MENU.show(evt.getComponent(), evt.getX(), evt.getY());
                        }

                    } else if (ELEMENTS.size() > 1) {

                        MI_FIND_OWNER.setVisible(false);
                        MI_CLEANSE_FLST.setVisible(false);                       
                        MI_PURGE.setEnabled(false);
                        
                        int purgeable = (int) ELEMENTS.keySet().stream().filter(ESS.PURGEABLE).count();
                        int deletable = (int) ELEMENTS.keySet().stream().filter(ESS.DELETABLE).count();
                        int threads = (int) ELEMENTS.keySet().stream().filter(ESS.THREAD).count();

                        if (purgeable == ELEMENTS.size()) {
                            MI_PURGES.setEnabled(true);
                            MI_PURGES.setText(String.format("Purge (%d plugins)", purgeable));
                        } else {
                            MI_PURGES.setEnabled(false);
                        }

                        if (deletable > 0) {
                            MI_DELETE.setEnabled(true);
                            MI_DELETE.setText(String.format("Delete (%d elements)", deletable));
                        } else {
                            MI_DELETE.setEnabled(false);
                        }

                        MI_ZERO_THREAD.setVisible(threads > 0);

                        TREE_POPUP_MENU.show(evt.getComponent(), evt.getX(), evt.getY());
                    }
                }
            }
        });
    }

    /**
     * Clears the <code>ESS</code>.
     */
    public void clearESS() {
        this.setModel(new FilterTreeModel());
    }

    /**
     * Uses an <code>ESS</code> to create the tree's data model.
     *
     * @param ess The <code>ESS</code>.
     * @param model A <code>FilterTreeModel</code>.
     * @param filter An optional setFilter.
     *
     */
    public void setESS(ESS ess, FilterTreeModel model, Predicate<Node> filter) {
        final TreePath[] PATHS = this.getSelectionPaths();

        if (null != filter) {
            model.setFilter(filter);
        }

        if (null != model) {
            this.setModel(model);
        }

        if (null != PATHS) {
            for (int i = 0; i < PATHS.length; i++) {
                PATHS[i] = this.getModel().rebuildPath(PATHS[i]);
            }

            this.setSelectionPaths(PATHS);
        }
    }

    /**
     * Searches for the <code>Node</code> that represents a specified
     * <code>Element</code> and returns it.
     *
     * @param element The <code>Element</code> to find.
     * @return The corresponding <code>Node</code> or null if the
     * <code>Element</code> was not found.
     */
    public TreePath findPath(Element element) {
        Objects.requireNonNull(element);
        return this.getModel().findPath(element);
    }

    /**
     * Sets the delete handler.
     *
     * @param newHandler The new delete handler.
     */
    public void setDeleteHandler(Consumer<Map<Element, Node>> newHandler) {
        this.deleteHandler = newHandler;
    }

    /**
     * Sets the edit handler.
     *
     * @param newHandler The new edit handler.
     */
    public void setEditHandler(Consumer<Element> newHandler) {
        this.editHandler = newHandler;
    }

    /**
     * Sets the setFilter plugin handler.
     *
     * @param newHandler The new delete handler.
     */
    public void setFilterPluginsHandler(Consumer<Plugin> newHandler) {
        this.pluginFilterHandler = newHandler;
    }

    /**
     * Sets the purge plugins handler.
     *
     * @param newHandler The new delete handler.
     */
    public void setPurgeHandler(Consumer<Collection<Plugin>> newHandler) {
        this.purgeHandler = newHandler;
    }

    /**
     * Sets the delete plugin forms handler.
     *
     * @param newHandler The new delete handler.
     */
    public void setDeleteFormsHandler(Consumer<Plugin> newHandler) {
        this.deleteFormsHandler = newHandler;
    }

    /**
     * Sets the delete plugin instances handler.
     *
     * @param newHandler The new delete handler.
     */
    public void setDeleteInstancesHandler(Consumer<Plugin> newHandler) {
        this.deleteInstancesHandler = newHandler;
    }

    /**
     * Sets the zero active script handler.
     *
     * @param newHandler The new handler.
     */
    public void setZeroThreadHandler(Consumer<List<ActiveScript>> newHandler) {
        this.zeroThreadHandler = newHandler;
    }

    /**
     * Sets the find element handler.
     *
     * @param newHandler The new handler.
     */
    public void setFindHandler(Consumer<Element> newHandler) {
        this.findHandler = newHandler;
    }

    /**
     * Sets the cleanse formlist handler.
     *
     * @param newHandler The new handler.
     */
    public void setCleanseFLSTHandler(Consumer<ChangeFormFLST> newHandler) {
        this.cleanFLSTHandler = newHandler;
    }

    /**
     * Sets the compression type handler.
     *
     * @param newHandler The new compression type handler.
     */
    public void setCompressionHandler(Consumer<CompressionType> newHandler) {
        this.compressionHandler = newHandler;
    }

    /**
     * Deletes a node by submitting it back to the app.
     */
    private void deleteNodes() {
        if (null == this.deleteHandler) {
            return;
        }

        final TreePath[] PATHS = getSelectionPaths();
        if (null == PATHS || PATHS.length == 0) {
            return;
        }

        final Map<Element, Node> ELEMENTS = getModel().parsePaths(PATHS);
        this.deleteHandler.accept(ELEMENTS);
    }

    @Override
    public FilterTreeModel getModel() {
        return (FilterTreeModel) super.getModel();
    }

    final private JMenuItem MI_PURGE;
    final private JMenuItem MI_PURGES;
    final private JMenuItem MI_DELETE;
    final private JMenuItem MI_FILTER;
    final private JMenuItem MI_DELETE_FORMS;
    final private JMenuItem MI_DELETE_INSTANCES;
    final private JMenuItem MI_ZERO_THREAD;
    final private JMenuItem MI_FIND_OWNER;
    final private JMenuItem MI_CLEANSE_FLST;
    final private JRadioButtonMenuItem MI_COMPRESS_UNCOMPRESSED;
    final private JRadioButtonMenuItem MI_COMPRESS_ZLIB;
    final private JRadioButtonMenuItem MI_COMPRESS_LZ4;
    final ButtonGroup COMPRESSION_GROUP;
    final private JPopupMenu TREE_POPUP_MENU;
    final private JPopupMenu PLUGIN_POPUP_MENU;
    final private JPopupMenu COMPRESSION_POPUP_MENU;
    private Consumer<Map<Element, Node>> deleteHandler;
    private Consumer<List<ActiveScript>> zeroThreadHandler;
    private Consumer<Element> editHandler;
    private Consumer<Plugin> pluginFilterHandler;
    private Consumer<Plugin> deleteFormsHandler;
    private Consumer<Plugin> deleteInstancesHandler;
    private Consumer<Collection<Plugin>> purgeHandler;
    private Consumer<Element> findHandler;
    private Consumer<ChangeFormFLST> cleanFLSTHandler;
    private Consumer<CompressionType> compressionHandler;
    

}
