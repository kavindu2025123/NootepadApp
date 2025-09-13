package app;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class NotepadApp {
    private JFrame frame;
    private JTextArea textArea;
    private JFileChooser fileChooser;
    private File currentFile;
    private UndoManager undoManager;
    private boolean isSaved = true;

    public NotepadApp() {
        initUI();
    }

    private void initUI() {
        frame = new JFrame("Untitled - NotepadApp");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 650);

        textArea = new JTextArea();
        textArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        undoManager = new UndoManager();
        textArea.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        // Menu
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem newItem = new JMenuItem("New");
        JMenuItem openItem = new JMenuItem("Open...");
        JMenuItem saveItem = new JMenuItem("Save");
        JMenuItem saveAsItem = new JMenuItem("Save As...");
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(newItem);
        fileMenu.add(openItem);
        fileMenu.addSeparator();
        fileMenu.add(saveItem);
        fileMenu.add(saveAsItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);

        JMenu editMenu = new JMenu("Edit");
        JMenuItem undoItem = new JMenuItem("Undo");
        JMenuItem redoItem = new JMenuItem("Redo");
        JMenuItem cutItem = new JMenuItem("Cut");
        JMenuItem copyItem = new JMenuItem("Copy");
        JMenuItem pasteItem = new JMenuItem("Paste");
        editMenu.add(undoItem);
        editMenu.add(redoItem);
        editMenu.addSeparator();
        editMenu.add(cutItem);
        editMenu.add(copyItem);
        editMenu.add(pasteItem);

        JMenu viewMenu = new JMenu("View");
        JCheckBoxMenuItem wrapItem = new JCheckBoxMenuItem("Word Wrap", true);
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        viewMenu.add(wrapItem);

        JMenu helpMenu = new JMenu("Help");
        JMenuItem aboutItem = new JMenuItem("About");
        helpMenu.add(aboutItem);

        menuBar.add(fileMenu);
        menuBar.add(editMenu);
        menuBar.add(viewMenu);
        menuBar.add(helpMenu);
        frame.setJMenuBar(menuBar);

        fileChooser = new JFileChooser();

        // Actions
        newItem.addActionListener(e -> newFile());
        openItem.addActionListener(e -> openFile());
        saveItem.addActionListener(e -> saveFile());
        saveAsItem.addActionListener(e -> saveFileAs());
        exitItem.addActionListener(e -> exitApp());

        undoItem.addActionListener(e -> { if (undoManager.canUndo()) undoManager.undo(); });
        redoItem.addActionListener(e -> { if (undoManager.canRedo()) undoManager.redo(); });
        cutItem.addActionListener(e -> textArea.cut());
        copyItem.addActionListener(e -> textArea.copy());
        pasteItem.addActionListener(e -> textArea.paste());

        wrapItem.addActionListener(ev -> {
            boolean wrap = wrapItem.isSelected();
            textArea.setLineWrap(wrap);
            textArea.setWrapStyleWord(wrap);
        });

        aboutItem.addActionListener(e ->
                JOptionPane.showMessageDialog(frame,
                        "NotepadApp\nJava Swing — simple text editor\nFeatures: New/Open/Save/Save As, Undo/Redo, Word wrap",
                        "About", JOptionPane.INFORMATION_MESSAGE));

        textArea.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { changed(); }
            public void removeUpdate(DocumentEvent e)  { changed(); }
            public void changedUpdate(DocumentEvent e) { changed(); }
            private void changed() { isSaved = false; updateTitle(); }
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private void newFile() {
        if (!confirmSave()) return;
        currentFile = null;
        textArea.setText("");
        isSaved = true;
        undoManager.discardAllEdits();
        updateTitle();
    }

    private void openFile() {
        if (!confirmSave()) return;
        int res = fileChooser.showOpenDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try {
                byte[] bytes = java.nio.file.Files.readAllBytes(file.toPath());
                textArea.setText(new String(bytes, StandardCharsets.UTF_8));
                currentFile = file;
                isSaved = true;
                undoManager.discardAllEdits();
                updateTitle();
            } catch (IOException ex) {
                showError("Could not open file: " + ex.getMessage());
            }
        }
    }

    private void saveFile() {
        if (currentFile == null) { saveFileAs(); return; }
        try (Writer w = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentFile), StandardCharsets.UTF_8))) {
            w.write(textArea.getText());
            isSaved = true;
            updateTitle();
        } catch (IOException ex) {
            showError("Could not save file: " + ex.getMessage());
        }
    }

    private void saveFileAs() {
        int res = fileChooser.showSaveDialog(frame);
        if (res == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            saveFile();
        }
    }

    private boolean confirmSave() {
        if (!isSaved && textArea.getDocument().getLength() > 0) {
            int opt = JOptionPane.showConfirmDialog(frame, "Do you want to save changes?", "Save",
                    JOptionPane.YES_NO_CANCEL_OPTION);
            if (opt == JOptionPane.CANCEL_OPTION) return false;
            if (opt == JOptionPane.YES_OPTION) saveFile();
        }
        return true;
    }

    private void exitApp() {
        if (!confirmSave()) return;
        frame.dispose();
    }

    private void updateTitle() {
        String name = (currentFile == null) ? "Untitled" : currentFile.getName();
        frame.setTitle((isSaved ? "" : "*") + name + " - NotepadApp");
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(frame, msg, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(NotepadApp::new);
    }
}
