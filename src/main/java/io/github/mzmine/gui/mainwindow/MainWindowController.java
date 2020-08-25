/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.gui.mainwindow;

import com.google.common.collect.Ordering;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data.ModularFeatureList;
import io.github.mzmine.gui.MZmineGUI;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.chromatogram.ChromatogramVisualizerModule;
import io.github.mzmine.modules.visualization.chromatogram.TICVisualizerParameters;
import io.github.mzmine.modules.visualization.featurelisttable.PeakListTableModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerModule;
import io.github.mzmine.modules.visualization.fx3d.Fx3DVisualizerParameters;
import io.github.mzmine.modules.visualization.rawdataoverview.RawDataOverviewPane;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerModule;
import io.github.mzmine.modules.visualization.spectra.simplespectra.SpectraVisualizerParameters;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerModule;
import io.github.mzmine.modules.visualization.twod.TwoDVisualizerParameters;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.taskcontrol.TaskController;
import io.github.mzmine.taskcontrol.TaskPriority;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.taskcontrol.impl.WrappedTask;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.javafx.FxIconUtil;
import java.text.NumberFormat;
import java.util.List;
import java.util.logging.Logger;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.controlsfx.control.StatusBar;

public class MainWindowController {

  private final Logger logger = Logger.getLogger(this.getClass().getName());
  private Boolean reorderListItem = false;
  private static final Image rawDataFileIcon =
      FxIconUtil.loadImageFromResources("icons/fileicon.png");

  private static final Image featureListSingleIcon =
      FxIconUtil.loadImageFromResources("icons/peaklisticon_single.png");

  private static final Image featureListAlignedIcon =
      FxIconUtil.loadImageFromResources("icons/peaklisticon_aligned.png");

  private static final NumberFormat percentFormat = NumberFormat.getPercentInstance();

  @FXML
  private Scene mainScene;

  @FXML
  private ListView<RawDataFile> rawDataTree;

  @FXML
  private ListView<PeakList> featureTree;

  @FXML
  private Tab tvAligned;

  @FXML
  private TreeView<ModularFeatureList> tvAlignedFeatureLists;

  @FXML
  private AnchorPane tbRawData;

  @FXML
  private AnchorPane tbFeatureTable;

  @FXML
  private BorderPane rawDataOverview;

//  @FXML
//  private RawDataOverviewWindowController rawDataOverviewController;

  @FXML
  private TabPane mainTabPane;

  @FXML
  private Tab tabRawDataOverview;

  @FXML
  private Tab tabFeatureListOverview;

  @FXML
  private TableView<WrappedTask> tasksView;

  @FXML
  private StatusBar statusBar;

  @FXML
  private ProgressBar memoryBar;

  @FXML
  private Label memoryBarLabel;

  @FXML
  private TableColumn<WrappedTask, String> taskNameColumn;

  @FXML
  private TableColumn<WrappedTask, TaskPriority> taskPriorityColumn;

  @FXML
  private TableColumn<WrappedTask, TaskStatus> taskStatusColumn;

  @FXML
  private TableColumn<WrappedTask, Double> taskProgressColumn;

  @FXML
  public void initialize() {

    rawDataTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    // rawDataTree.setShowRoot(true);

    rawDataTree.setCellFactory(rawDataListView -> new DraggableListCellWithDraggableFiles<>() {
      @Override
      protected void updateItem(RawDataFile item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || (item == null)) {
          setText("");
          setGraphic(null);
          return;
        }
        setText(item.getName());
//        setTextFill(item.getColor());
        textFillProperty().bind(item.colorProperty());
        setGraphic(new ImageView(rawDataFileIcon));
      }
    });

    // Add mouse clicked event handler
    rawDataTree.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        handleShowChromatogram(event);
      }
    });

    featureTree.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
    // featureTree.setShowRoot(true);

    featureTree.setCellFactory(featureListView -> new DraggableListCellWithDraggableFiles<>() {
      @Override
      protected void updateItem(PeakList item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || (item == null)) {
          setText("");
          setGraphic(null);
          return;
        }
        setText(item.getName());
        if (item.getNumberOfRawDataFiles() > 1) {
          setGraphic(new ImageView(featureListAlignedIcon));
        } else {
          setGraphic(new ImageView(featureListSingleIcon));
        }
      }
    });
    // Add mouse clicked event handler
    featureTree.setOnMouseClicked(event -> {
      if (event.getClickCount() == 2) {
        handleOpenFeatureList(event);
      }
    });

    // notify selected tab about raw file selection change
    // TODO: Add the same when the feature model is finally done
    rawDataTree.getSelectionModel().getSelectedItems().addListener(
        (ListChangeListener<RawDataFile>) c -> {
          c.next();
          for (Tab tab : MZmineCore.getDesktop().getAllTabs()) {
            if (tab instanceof MZmineTab && tab.isSelected() && ((MZmineTab) tab)
                .isUpdateOnSelection()) {
              ((MZmineTab) tab).onRawDataFileSelectionChanged(c.getList());
            }
          }
        });

    // update if tab selection in main window changes
    getMainTabPane().getSelectionModel().selectedItemProperty().addListener((obs, old, val) -> {
      if (val instanceof MZmineTab && ((MZmineTab) val).getRawDataFiles() != null) {
        if (!((MZmineTab) val).getRawDataFiles()
            .containsAll(rawDataTree.getSelectionModel().getSelectedItems())
            || ((MZmineTab) val).getRawDataFiles().size() != rawDataTree.getSelectionModel()
            .getSelectedItems().size()) {
          if (((MZmineTab) val).isUpdateOnSelection()) {
            ((MZmineTab) val)
                .onRawDataFileSelectionChanged(rawDataTree.getSelectionModel().getSelectedItems());
          }
        }
        // TODO: Add the same for feature lists
      }
    });

    // taskNameColumn.setPrefWidth(800.0);
    // taskNameColumn.setMinWidth(600.0);
    // taskNameColumn.setMinWidth(100.0);

    ObservableList<WrappedTask> tasksQueue =
        MZmineCore.getTaskController().getTaskQueue().getTasks();
    tasksView.setItems(tasksQueue);

    taskNameColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getTaskDescription()));
    taskPriorityColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getTaskPriority()));

    taskStatusColumn.setCellValueFactory(
        cell -> new ReadOnlyObjectWrapper<>(cell.getValue().getActualTask().getStatus()));
    taskProgressColumn.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(
        cell.getValue().getActualTask().getFinishedPercentage()));
    taskProgressColumn.setCellFactory(column -> new TableCell<WrappedTask, Double>() {

      @Override
      public void updateItem(Double value, boolean empty) {
        super.updateItem(value, empty);
        if (empty) {
          return;
        }
        ProgressBar progressBar = new ProgressBar(value);
        progressBar.setOpacity(0.3);
        progressBar.prefWidthProperty().bind(taskProgressColumn.widthProperty().subtract(20));
        String labelText = percentFormat.format(value);
        Label percentLabel = new Label(labelText);
        percentLabel.setTextFill(Color.BLACK);
        StackPane stack = new StackPane();
        stack.setManaged(true);
        stack.getChildren().addAll(progressBar, percentLabel);
        setGraphic(stack);
        setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
      }
    });


    /*
     * tasksView.setGraphicFactory(task -> { return new Glyph("FontAwesome",
     * FontAwesome.Glyph.COG).size(24.0) .color(Color.BLUE); });
     */

    // Setup the Timeline to update the memory indicator periodically
    final Timeline memoryUpdater = new Timeline();
    int UPDATE_FREQUENCY = 500; // ms
    memoryUpdater.setCycleCount(Animation.INDEFINITE);
    memoryUpdater.getKeyFrames().add(new KeyFrame(Duration.millis(UPDATE_FREQUENCY), e -> {

      tasksView.refresh();

      final long freeMemMB = Runtime.getRuntime().freeMemory() / (1024 * 1024);
      final long totalMemMB = Runtime.getRuntime().totalMemory() / (1024 * 1024);
      final double memory = ((double) (totalMemMB - freeMemMB)) / totalMemMB;

      memoryBar.setProgress(memory);
      memoryBarLabel.setText(freeMemMB + "/" + totalMemMB + " MB free");
    }));
    memoryUpdater.play();

    // Setup the Timeline to update the MZmine tasks periodically final
    /*
     * Timeline msdkTaskUpdater = new Timeline(); UPDATE_FREQUENCY = 50; // ms
     * msdkTaskUpdater.setCycleCount(Animation.INDEFINITE); msdkTaskUpdater.getKeyFrames() .add(new
     * KeyFrame(Duration.millis(UPDATE_FREQUENCY), e -> {
     *
     * Collection<Task<?>> tasks = tasksView.getTasks(); for (Task<?> task : tasks) { if (task
     * instanceof MZmineTask) { MZmineTask mzmineTask = (MZmineTask) task;
     * mzmineTask.refreshStatus(); } } })); msdkTaskUpdater.play();
     */

    RawDataOverviewPane rop = new RawDataOverviewPane(true, true, false, null);
//    rop.setClosable(false); // as the default tab, this should not be removed
    rop.setWindowChangeAllowed(false);
    addTab(rop);

  }

  public ListView<RawDataFile> getRawDataTree() {
    return rawDataTree;
  }

  public ListView<PeakList> getFeatureTree() {
    return featureTree;
  }

  /*
   * public TaskProgressView<Task<?>> getTaskTable() { return tasksView; }
   */

  public StatusBar getStatusBar() {
    return statusBar;
  }

  public TableView<WrappedTask> getTasksView() {
    return tasksView;
  }

  public void handleShowChromatogram(Event event) {
    logger.finest("Activated Show chromatogram menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(ChromatogramVisualizerModule.class);
    parameters.getParameter(TICVisualizerParameters.DATA_FILES).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(ChromatogramVisualizerModule.class, parameters);
    }
  }

  public void handleShowMsSpectrum(Event event) {
    logger.finest("Activated Show MS spectrum menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(SpectraVisualizerModule.class);
    parameters.getParameter(SpectraVisualizerParameters.dataFiles).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(SpectraVisualizerModule.class, parameters);
    }
  }

  public void handleShow2DPlot(Event event) {
    logger.finest("Activated Show 2D plot menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(TwoDVisualizerModule.class);
    parameters.getParameter(TwoDVisualizerParameters.dataFiles).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(TwoDVisualizerModule.class, parameters);
    }

  }

  public void handleShow3DPlot(Event event) {
    logger.finest("Activated Show 3D plot menu item");
    var selectedFiles = MZmineGUI.getSelectedRawDataFiles();
    ParameterSet parameters =
        MZmineCore.getConfiguration().getModuleParameters(Fx3DVisualizerModule.class);
    parameters.getParameter(Fx3DVisualizerParameters.dataFiles).setValue(
        RawDataFilesSelectionType.SPECIFIC_FILES, selectedFiles.toArray(new RawDataFile[0]));
    ExitCode exitCode = parameters.showSetupDialog(true);
    if (exitCode == ExitCode.OK) {
      MZmineCore.runMZmineModule(Fx3DVisualizerModule.class, parameters);
    }
  }

  public void handleShowMsMsPlot(Event event) {
  }

  public void handleSort(Event event) {
    if (!(event.getSource() instanceof ListView)) {
      return;
    }
    ListView<?> sourceList = (ListView<?>) event.getSource();
    List<?> files = sourceList.getItems();
    files.sort(Ordering.usingToString());
  }

  public void handleRemoveFileExtension(Event event) {
  }

  public void handleExportFile(Event event) {
  }

  public void handleRenameFile(Event event) {
  }

  public void handleRemoveRawData(Event event) {

    if (rawDataTree.getSelectionModel() == null) {
      return;
    }

    // Get selected tree items
    ObservableList<RawDataFile> rows = rawDataTree.getSelectionModel().getSelectedItems();

    // Loop through all selected tree items
    if (rows != null) {
      for (int i = rows.size() - 1; i >= 0; i--) {
        RawDataFile row = rows.get(i);
        MZmineCore.getProjectManager().getCurrentProject().removeFile(row);
      }
      // rawDataTree.getSelectionModel().clearSelection();
    }
  }

  public void handleOpenFeatureList(Event event) {
    var selectedFeatureLists = MZmineGUI.getSelectedFeatureLists();
    for (PeakList fl : selectedFeatureLists) {
      PeakListTableModule.showNewPeakListVisualizerWindow(fl);
    }
  }

  public void handleShowFeatureListSummary(Event event) {
  }

  public void handleShowScatterPlot(Event event) {
  }

  public void handleRenameFeatureList(Event event) {
  }

  @FXML
  public void handleRemoveFeatureList(Event event) {
    PeakList selectedFeatureLists[] = MZmineCore.getDesktop().getSelectedPeakLists();
    for (PeakList fl : selectedFeatureLists) {
      MZmineCore.getProjectManager().getCurrentProject().removePeakList(fl);
    }
  }

  @FXML
  public void handleCancelTask(Event event) {
    var selectedTasks = tasksView.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      t.getActualTask().cancel();
    }
  }

  @FXML
  public void handleCancelAllTasks(Event event) {
    for (WrappedTask t : tasksView.getItems()) {
      t.getActualTask().cancel();
    }
  }

  @FXML
  public void handleSetHighPriority(Event event) {
    TaskController taskController = MZmineCore.getTaskController();
    var selectedTasks = tasksView.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      taskController.setTaskPriority(t.getActualTask(), TaskPriority.HIGH);
    }
  }

  @FXML
  public void handleSetNormalPriority(Event event) {
    TaskController taskController = MZmineCore.getTaskController();
    var selectedTasks = tasksView.getSelectionModel().getSelectedItems();
    for (WrappedTask t : selectedTasks) {
      taskController.setTaskPriority(t.getActualTask(), TaskPriority.NORMAL);
    }
  }

  @FXML
  public void handleMemoryBarClick(Event e) {
    // Run garbage collector on a new thread, so it doesn't block the GUI
    new Thread(() -> {
      logger.info("Freeing unused memory");
      System.gc();
    }).start();
  }

  private TabPane getMainTabPane() {
    return mainTabPane;
  }

  /**
   * @return Current tabs wrapped in {@link FXCollections#unmodifiableObservableList}
   */
  public ObservableList<Tab> getTabs() {
    return FXCollections.unmodifiableObservableList(getMainTabPane().getTabs());
  }

  public void addTab(Tab tab) {
    if (tab instanceof MZmineTab) {
      ((MZmineTab) tab).updateOnSelectionProperty().addListener(((obs, old, val) -> {
        if (val.booleanValue()) {
          if (((MZmineTab) tab).getRawDataFiles() != null && (!((MZmineTab) tab).getRawDataFiles()
              .containsAll(rawDataTree.getSelectionModel().getSelectedItems())
              || ((MZmineTab) tab).getRawDataFiles().size() != rawDataTree.getSelectionModel()
              .getSelectedItems().size())) {
            ((MZmineTab) tab)
                .onRawDataFileSelectionChanged(rawDataTree.getSelectionModel().getSelectedItems());
          }
        }
      }));
      // TODO: add same for feature lists
    }

    getMainTabPane().getTabs().add(tab);
    getMainTabPane().getSelectionModel().select(tab);
  }

  @FXML
  public void handleSetRawDataFileColor(Event event) {
    BooleanProperty apply = new SimpleBooleanProperty(false);

    if(getRawDataTree().getSelectionModel().getSelectedItem() == null) {
      return;
    }

    Stage popup = new Stage();

    ColorPicker picker = new ColorPicker(getRawDataTree().getSelectionModel().getSelectedItem().getColor());
    picker.getCustomColors().addAll(MZmineCore.getConfiguration().getDefaultColorPalette());

    VBox box = new VBox(5);

    Button btnApply = new Button("Apply");
    Button btnCancel = new Button("Cancel");
    btnApply.setOnAction(e -> {
      apply.set(true);
      popup.hide();
    });
    btnCancel.setOnAction(e -> popup.hide());
    ButtonBar.setButtonData(btnApply, ButtonData.APPLY);
    ButtonBar.setButtonData(btnCancel, ButtonData.CANCEL_CLOSE);

    ButtonBar btnBar = new ButtonBar();
    btnBar.getButtons().addAll(btnApply, btnCancel);
    box.getChildren().addAll(picker, btnBar);

    popup.setScene(new Scene(box));
    popup.setTitle("Please choose a color for " + getRawDataTree().getSelectionModel().getSelectedItem().getName());
    popup.show();

    popup.setOnHiding(e -> {
      if(picker.getValue() != null && apply.get())
        getRawDataTree().getSelectionModel().getSelectedItem().setColor(picker.getValue());
    });
  }
}
