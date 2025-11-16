package application.pages;

import java.util.List;
import java.util.Optional;

import application.StartCSE360;
import javafx.beans.property.SimpleStringProperty;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import java.io.File;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class StaffHomePage {

    public void show(Stage primaryStage) {
        // Permission guard: allow only STAFF, INSTRUCTOR, or ADMIN
        application.User current = application.StartCSE360.getCurrentUser();
        if (current == null || !(current.getRole() == application.UserRole.STAFF
                || current.getRole() == application.UserRole.INSTRUCTOR
                || current.getRole() == application.UserRole.ADMIN)) {
            javafx.scene.control.Alert a = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
            a.setTitle("Access Denied");
            a.setHeaderText(null);
            a.setContentText("You do not have permission to access Staff Tools.");
            a.showAndWait();
            return;
        }
        Label summaryLabel;
        VBox root = new VBox(12);
        root.setPadding(new Insets(16));
        root.setStyle("-fx-alignment: top-center; -fx-background-color: #f7f9fc;");

        javafx.scene.control.Label header = new javafx.scene.control.Label("Staff Dashboard");
        header.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2b3a67;");
        header.setMaxWidth(Double.MAX_VALUE);
        header.setAlignment(Pos.CENTER);

        ChoiceBox<String> datasetChoice = new ChoiceBox<>();
        datasetChoice.getItems().addAll("Questions", "Answers", "Comments", "FlaggedItems");
        datasetChoice.setValue("FlaggedItems");

        ChoiceBox<String> statusChoice = new ChoiceBox<>();
        statusChoice.getItems().addAll("ALL", "OPEN", "RESOLVED");
        statusChoice.setValue("ALL");

        TableView<String[]> table = new TableView<>();
        table.setPrefWidth(900);
        table.setPrefHeight(400);
        table.setPlaceholder(new Label("No items to display"));

        // Summary label to show counts / debug info
        summaryLabel = new Label("");
        summaryLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #333;");

        HBox controls = new HBox(10);
        controls.setAlignment(Pos.CENTER);

        Button refreshBtn = new Button("Refresh");
        Button flagBtn = new Button("Flag Selected");
        Button addNoteBtn = new Button("Add Note");
        Button resolveBtn = new Button("Mark Resolved");
        Button reportBtn = new Button("Generate Report");
        Button notifyBtn = new Button("Notify Instructors");

        controls.getChildren().addAll(datasetChoice, statusChoice, refreshBtn, flagBtn, addNoteBtn, resolveBtn,
                reportBtn, notifyBtn);
        root.getChildren().addAll(header, controls, summaryLabel, table);

        // Wire actions
        refreshBtn
                .setOnAction(e -> loadDataset(datasetChoice.getValue(), statusChoice.getValue(), table, summaryLabel));
        // Auto-refresh when selection changes
        datasetChoice.getSelectionModel().selectedItemProperty()
                .addListener(
                        (obs, oldVal, newVal) -> loadDataset(newVal, statusChoice.getValue(), table, summaryLabel));
        statusChoice.getSelectionModel().selectedItemProperty()
                .addListener(
                        (obs, oldVal, newVal) -> loadDataset(datasetChoice.getValue(), newVal, table, summaryLabel));

        // Debug: print who opened the staff page
        System.out.println("Opening StaffHomePage for user: " + current.getUserName() + " role=" + current.getRole());

        flagBtn.setOnAction(e -> {
            String[] sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("No selection", "Please select an item to flag.");
                return;
            }
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Flag Reason");
            dialog.setHeaderText("Enter reason for flagging:");
            Optional<String> res = dialog.showAndWait();
            res.ifPresent(reason -> {
                try {
                    String itemType = datasetChoice.getValue().equals("FlaggedItems") ? "Unknown"
                            : datasetChoice.getValue();
                    int itemId = Integer.parseInt(sel[0]);
                    StartCSE360.getDatabaseHelper().flagItem(itemType, itemId, "staff", reason);
                    showAlert("Flagged", "Item flagged successfully.");
                    loadDataset(datasetChoice.getValue(), statusChoice.getValue(), table, summaryLabel);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Failed to flag item.");
                }
            });
        });

        addNoteBtn.setOnAction(e -> {
            String[] sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("No selection", "Please select an item to add a note.");
                return;
            }
            TextInputDialog dialog = new TextInputDialog();
            dialog.setTitle("Add Staff Note");
            dialog.setHeaderText("Enter private note text:");
            Optional<String> res = dialog.showAndWait();
            res.ifPresent(note -> {
                try {
                    int itemId = Integer.parseInt(sel[0]);
                    String itemType = datasetChoice.getValue();
                    StartCSE360.getDatabaseHelper().addStaffNote(itemType, itemId, "staff", note);
                    showAlert("Saved", "Staff note added.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    showAlert("Error", "Failed to save note.");
                }
            });
        });

        resolveBtn.setOnAction(e -> {
            String[] sel = table.getSelectionModel().getSelectedItem();
            if (sel == null) {
                showAlert("No selection", "Please select a flagged item to mark resolved.");
                return;
            }
            try {
                int flagId = Integer.parseInt(sel[0]);
                boolean ok = StartCSE360.getDatabaseHelper().updateFlagStatus(flagId, "RESOLVED");
                if (ok) {
                    showAlert("Updated", "Flag marked as RESOLVED.");
                    loadDataset(datasetChoice.getValue(), statusChoice.getValue(), table, summaryLabel);
                } else {
                    showAlert("Not updated", "Could not update flag status.");
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Error", "Failed to update status.");
            }
        });

        reportBtn.setOnAction(e -> {
            // Summary of flagged items (respecting status filter)
            List<String[]> rows = StartCSE360.getDatabaseHelper().fetchFlaggedItems();
            int total = rows.size();
            int openCount = 0;
            for (String[] r : rows) {
                if (r.length > 5 && "OPEN".equalsIgnoreCase(r[5]))
                    openCount++;
            }

            TextInputDialog choice = new TextInputDialog("summary");
            choice.setTitle("Report Options");
            choice.setHeaderText("Type 'export' to save CSV, or 'summary' to view summary");
            Optional<String> pick = choice.showAndWait();
            if (pick.isPresent() && "export".equalsIgnoreCase(pick.get().trim())) {
                FileChooser fc = new FileChooser();
                fc.setInitialFileName("flagged_items.csv");
                File out = fc.showSaveDialog(null);
                if (out != null) {
                    // Export currently displayed rows (so filters apply)
                    boolean ok = exportTableToCSV(table.getItems(), out);
                    if (ok)
                        showAlert("Exported", "CSV exported to: " + out.getAbsolutePath());
                    else
                        showAlert("Error", "Failed to export CSV.");
                }
            } else {
                showAlert("Report", "Flagged items total: " + total + "\nOpen: " + openCount);
            }
        });

        notifyBtn.setOnAction(e -> {
            // Notify instructors via message table
            TextInputDialog dialog = new TextInputDialog("Attention: flagged content needs review.");
            dialog.setTitle("Notify Instructors");
            dialog.setHeaderText("Message to instructors:");
            Optional<String> res = dialog.showAndWait();
            res.ifPresent(msg -> {
                StartCSE360.getDatabaseHelper().sendMessage("staff", "instructors", msg);
                showAlert("Sent", "Notification sent to instructors.");
            });
        });

        // Load initial dataset
        loadDataset(datasetChoice.getValue(), statusChoice.getValue(), table, summaryLabel);

        // Open staff UI in a modal window so student/instructor pages are unaffected
        Scene scene = new Scene(root, 1000, 600);
        Stage staffStage = new Stage();
        staffStage.initOwner(primaryStage);
        staffStage.initModality(Modality.APPLICATION_MODAL);
        staffStage.setScene(scene);
        staffStage.setTitle("Staff Page");
        staffStage.showAndWait();
    }

    private void loadDataset(String name, String statusFilter, TableView<String[]> table, Label summaryLabel) {
        table.getItems().clear();
        table.getColumns().clear();

        if ("Questions".equals(name)) {
            // columns: id,user,creationDate,title,content
            createColumn(table, "ID", 0);
            createColumn(table, "User", 1);
            createColumn(table, "Date", 2);
            createColumn(table, "Title", 3);
            createColumn(table, "Content", 4);
            List<String[]> rows = StartCSE360.getDatabaseHelper().fetchAllQuestions();
            table.getItems().addAll(rows);
        } else if ("Answers".equals(name)) {
            createColumn(table, "ID", 0);
            createColumn(table, "User", 1);
            createColumn(table, "Date", 2);
            createColumn(table, "Content", 3);
            List<String[]> rows = StartCSE360.getDatabaseHelper().fetchAllAnswers();
            table.getItems().addAll(rows);
        } else if ("Comments".equals(name)) {
            createColumn(table, "ID", 0);
            createColumn(table, "User", 1);
            createColumn(table, "Date", 2);
            createColumn(table, "Content", 3);
            List<String[]> rows = StartCSE360.getDatabaseHelper().fetchAllComments();
            table.getItems().addAll(rows);
        } else { // FlaggedItems
            createColumn(table, "FlagId", 0);
            createColumn(table, "ItemType", 1);
            createColumn(table, "ItemId", 2);
            createColumn(table, "Flagger", 3);
            createColumn(table, "Reason", 4);
            createColumn(table, "Status", 5);
            createColumn(table, "Created", 6);
            List<String[]> rows = StartCSE360.getDatabaseHelper().fetchFlaggedItems();
            if (statusFilter == null || "ALL".equalsIgnoreCase(statusFilter)) {
                table.getItems().addAll(rows);
            } else {
                for (String[] r : rows) {
                    if (r.length > 5 && statusFilter.equalsIgnoreCase(r[5])) {
                        table.getItems().add(r);
                    }
                }
            }
        }
        // Update summary label and debug output
        int shown = table.getItems().size();
        if ("FlaggedItems".equals(name)) {
            List<String[]> all = StartCSE360.getDatabaseHelper().fetchFlaggedItems();
            int total = all.size();
            int openCount = 0;
            for (String[] r : all) {
                if (r.length > 5 && "OPEN".equalsIgnoreCase(r[5]))
                    openCount++;
            }
            if (summaryLabel != null) {
                summaryLabel.setText("Showing " + shown + " (total flagged: " + total + ", open: " + openCount + ")");
            }
            System.out.println(
                    "StaffHomePage: dataset=FlaggedItems shown=" + shown + " total=" + total + " open=" + openCount);
        } else {
            if (summaryLabel != null) {
                summaryLabel.setText("Showing " + shown + " rows for " + name);
            }
            System.out.println("StaffHomePage: dataset=" + name + " shown=" + shown);
        }
    }

    private boolean exportTableToCSV(java.util.List<String[]> rows, File out) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(out, "UTF-8")) {
            if (rows == null || rows.isEmpty()) {
                pw.println("no_rows");
                return true;
            }
            for (String[] r : rows) {
                String[] copy = new String[r.length];
                for (int i = 0; i < r.length; i++) {
                    String v = r[i] == null ? "" : r[i];
                    v = v.replace("\"", "\"\"");
                    if (v.contains(",") || v.contains("\n") || v.contains("\"")) {
                        v = "\"" + v + "\"";
                    }
                    copy[i] = v;
                }
                pw.println(String.join(",", copy));
            }
            pw.flush();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void createColumn(TableView<String[]> table, String title, int idx) {
        TableColumn<String[], String> col = new TableColumn<>(title);
        col.setCellValueFactory(cell -> {
            String[] arr = cell.getValue();
            if (arr == null || arr.length <= idx || arr[idx] == null)
                return new SimpleStringProperty("");
            return new SimpleStringProperty(arr[idx]);
        });
        col.setCellFactory(c -> new TableCell<String[], String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                }
            }
        });
        table.getColumns().add(col);
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
