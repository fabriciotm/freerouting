package app.freerouting.gui;

import app.freerouting.logger.FRLogger;
import java.util.List;

/** Abstract class for windows displaying a list of objects */
public abstract class WindowObjectList extends BoardSavableSubWindow {

  protected static final int DEFAULT_TABLE_SIZE = 20;
  protected final BoardFrame board_frame;
  protected final javax.swing.JPanel south_panel;
  /** The subwindows with information about selected object */
  protected final java.util.Collection<WindowObjectInfo> subwindows =
      new java.util.LinkedList<WindowObjectInfo>();

  private final javax.swing.JPanel main_panel;
  private final java.util.ResourceBundle resources;
  protected javax.swing.JLabel list_empty_message;
  protected javax.swing.JList<Object> list;
  private javax.swing.JScrollPane list_scroll_pane = null;
  private javax.swing.DefaultListModel<Object> list_model = null;
  /** Creates a new instance of ObjectListWindow */
  public WindowObjectList(BoardFrame p_board_frame) {
    this.board_frame = p_board_frame;
    this.resources =
        java.util.ResourceBundle.getBundle(
            "app.freerouting.gui.WindowObjectList", p_board_frame.get_locale());

    // create main panel
    this.main_panel = new javax.swing.JPanel();
    main_panel.setLayout(new java.awt.BorderLayout());
    this.add(main_panel);

    // create a panel for adding buttons
    this.south_panel = new javax.swing.JPanel();
    south_panel.setLayout(new java.awt.BorderLayout());
    main_panel.add(south_panel, java.awt.BorderLayout.SOUTH);

    javax.swing.JPanel button_panel = new javax.swing.JPanel();
    button_panel.setLayout(new java.awt.BorderLayout());
    this.south_panel.add(button_panel, java.awt.BorderLayout.CENTER);

    javax.swing.JPanel north_button_panel = new javax.swing.JPanel();
    button_panel.add(north_button_panel, java.awt.BorderLayout.NORTH);

    javax.swing.JButton show_button = new javax.swing.JButton(resources.getString("info"));
    show_button.setToolTipText(resources.getString("info_tooltip"));
    ShowListener show_listener = new ShowListener();
    show_button.addActionListener(show_listener);
    north_button_panel.add(show_button);

    javax.swing.JButton instance_button = new javax.swing.JButton(resources.getString("select"));
    instance_button.setToolTipText(resources.getString("select_tooltip"));
    SelectListener instance_listener = new SelectListener();
    instance_button.addActionListener(instance_listener);
    north_button_panel.add(instance_button);

    javax.swing.JPanel south_button_panel = new javax.swing.JPanel();
    button_panel.add(south_button_panel, java.awt.BorderLayout.SOUTH);

    javax.swing.JButton invert_button = new javax.swing.JButton(resources.getString("invert"));
    invert_button.setToolTipText(resources.getString("invert_tooltip"));
    invert_button.addActionListener(new InvertListener());
    south_button_panel.add(invert_button);

    javax.swing.JButton recalculate_button =
        new javax.swing.JButton(resources.getString("recalculate"));
    recalculate_button.setToolTipText(resources.getString("recalculate_tooltip"));
    RecalculateListener recalculate_listener = new RecalculateListener();
    recalculate_button.addActionListener(recalculate_listener);
    south_button_panel.add(recalculate_button);

    this.list_empty_message = new javax.swing.JLabel(resources.getString("list_empty"));
    this.list_empty_message.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Dispose this window and all subwindows when closing the window.
    this.addWindowListener(
        new java.awt.event.WindowAdapter() {
          public void windowClosing(java.awt.event.WindowEvent evt) {
            dispose();
          }
        });
  }

  public void setVisible(boolean p_value) {
    if (p_value == true) {
      recalculate();
    }
    super.setVisible(p_value);
  }

  protected void recalculate() {
    if (this.list_scroll_pane != null) {
      main_panel.remove(this.list_scroll_pane);
    }
    main_panel.remove(this.list_empty_message);
    // Create display list
    this.list_model = new javax.swing.DefaultListModel<>();
    this.list = new javax.swing.JList<Object>(this.list_model);
    this.list.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));
    this.fill_list();
    if (this.list.getVisibleRowCount() > 0) {
      list_scroll_pane = new javax.swing.JScrollPane(this.list);
      main_panel.add(list_scroll_pane, java.awt.BorderLayout.CENTER);
    } else {
      main_panel.add(list_empty_message, java.awt.BorderLayout.CENTER);
    }
    this.pack();

    this.list.addMouseListener(
        new java.awt.event.MouseAdapter() {
          public void mouseClicked(java.awt.event.MouseEvent evt) {
            if (evt.getClickCount() > 1) {
              select_instances();
            }
          }
        });
  }

  public void dispose() {
    for (WindowObjectInfo curr_subwindow : this.subwindows) {
      if (curr_subwindow != null) {
        curr_subwindow.dispose();
      }
    }
    super.dispose();
  }

  protected void add_to_list(Object p_object) {
    this.list_model.addElement(p_object);
  }

  /** Fills the list with the objects to display. */
  protected abstract void fill_list();

  protected abstract void select_instances();

  /** Saves also the filter string to disk. */
  public void save(java.io.ObjectOutputStream p_object_stream) {
    int[] selected_indices;
    if (this.list != null) {
      selected_indices = this.list.getSelectedIndices();
    } else {
      selected_indices = new int[0];
    }
    try {
      p_object_stream.writeObject(selected_indices);
    } catch (java.io.IOException e) {
      FRLogger.error("WindowObjectList.save: save failed", e);
    }
    super.save(p_object_stream);
  }

  public boolean read(java.io.ObjectInputStream p_object_stream) {
    int[] saved_selected_indices = null;
    try {
      saved_selected_indices = (int[]) p_object_stream.readObject();
    } catch (Exception e) {
      FRLogger.error("WindowObjectListWithFilter.read: read failed", e);
      return false;
    }
    boolean result = super.read(p_object_stream);
    if (this.list != null && saved_selected_indices.length > 0) {
      this.list.setSelectedIndices(saved_selected_indices);
    }
    return result;
  }

  /** Listens to the button for showing the selected padstacks */
  private class ShowListener implements java.awt.event.ActionListener {
    private static final int WINDOW_OFFSET = 30;

    public void actionPerformed(java.awt.event.ActionEvent p_evt) {
      List<Object> selected_objects = list.getSelectedValuesList();
      if (selected_objects.size() <= 0) {
        return;
      }
      java.util.Collection<WindowObjectInfo.Printable> object_list =
          new java.util.LinkedList<WindowObjectInfo.Printable>();
      for (int i = 0; i < selected_objects.size(); ++i) {
        object_list.add((WindowObjectInfo.Printable) (selected_objects.get(i)));
      }
      app.freerouting.board.CoordinateTransform coordinate_transform =
          board_frame.board_panel.board_handling.coordinate_transform;
      WindowObjectInfo new_window =
          WindowObjectInfo.display(
              resources.getString("window_title"), object_list, board_frame, coordinate_transform);
      java.awt.Point loc = getLocation();
      java.awt.Point new_window_location =
          new java.awt.Point(
              (int) (loc.getX() + WINDOW_OFFSET), (int) (loc.getY() + WINDOW_OFFSET));
      new_window.setLocation(new_window_location);
      subwindows.add(new_window);
    }
  }

  /** Listens to the button for showing the selected incompletes */
  private class SelectListener implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent p_evt) {
      select_instances();
    }
  }

  /** Listens to the button for inverting the selection */
  private class InvertListener implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent p_evt) {
      if (list_model == null) {
        return;
      }
      int[] new_selected_indices = new int[list_model.getSize() - list.getSelectedIndices().length];
      int curr_index = 0;
      for (int i = 0; i < list_model.getSize(); ++i) {
        if (!list.isSelectedIndex(i)) {
          new_selected_indices[curr_index] = i;
          ++curr_index;
        }
      }
      list.setSelectedIndices(new_selected_indices);
    }
  }

  /** Listens to the button for recalculating the content of the window */
  private class RecalculateListener implements java.awt.event.ActionListener {
    public void actionPerformed(java.awt.event.ActionEvent p_evt) {
      recalculate();
    }
  }
}
