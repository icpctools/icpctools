package org.icpc.tools.presentation.admin.internal;

import org.icpc.tools.presentation.core.internal.PresentationInfo;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CompositeQuickEdit {
    private JList<String> compList;
    private JList<String> presentationList;
    private DefaultListModel<String> compListModel;
    private DefaultListModel<String> presentationListModel;
    private List<CompositePresentationInfo> composites;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Composite Quick Edit");
            MultiAdmin.swingDark(frame);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1200, 1000);
            frame.getContentPane().add(new CompositeQuickEdit().createPanel());
            frame.setVisible(true);
        });
    }

    private JPanel createPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setResizeWeight(0.5);

        // Left panel for composite list
        compListModel = new DefaultListModel<>();
        compList = new JList<>(compListModel);
        compList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        compList.addListSelectionListener(e -> updatePresentations());
        JScrollPane leftScrollPane = new JScrollPane(compList);
        splitPane.setLeftComponent(leftScrollPane);

        // Right panel for presentation list
        presentationListModel = new DefaultListModel<>();
        presentationList = new JList<>(presentationListModel);
        JScrollPane rightScrollPane = new JScrollPane(presentationList);
        splitPane.setRightComponent(rightScrollPane);

        // Buttons for managing presentations
        JPanel buttonPanel = new JPanel(new GridLayout(1, 4, 5, 5));
        JButton upButton = new JButton("^");
        JButton downButton = new JButton("v");
        JButton reloadButton = new JButton("Reload");

        upButton.addActionListener(e -> moveItem(-1));
        downButton.addActionListener(e -> moveItem(1));
        reloadButton.addActionListener(e -> reloadComposites());

        buttonPanel.add(upButton);
        buttonPanel.add(downButton);
        buttonPanel.add(reloadButton);

        panel.add(splitPane, BorderLayout.CENTER);
        panel.add(buttonPanel, BorderLayout.SOUTH);

        // Initial load of composites
        reloadComposites();
        return panel;
    }

    private void updatePresentations() {
        presentationListModel.clear();
        int selectedIndex = compList.getSelectedIndex();
        if (selectedIndex >= 0 && selectedIndex < composites.size()) {
            CompositePresentationInfo info = composites.get(selectedIndex);
            String[] presClasses = info.getClassName().split("\\|");

            for (String presentation : presClasses) {
                presentationListModel.addElement(presentation);
            }
        }
    }

    private void moveItem(int direction) {
        int selectedIndex = presentationList.getSelectedIndex();
        if (selectedIndex < 0 || presentationListModel.size() < 2) {
            return; // No selection or only one item
        }
        int targetIndex = selectedIndex + direction;
        if (targetIndex >= 0 && targetIndex < presentationListModel.size()) {
            String item = presentationListModel.remove(selectedIndex);
            presentationListModel.add(targetIndex, item);
            presentationList.setSelectedIndex(targetIndex);
        }
    }

    private void reloadComposites() {
        compListModel.clear();
        composites(); // Loads and populates the 'composites' and updates 'compListModel'
    }

    private void composites() {
        List<String> comps = new ArrayList<>();
        try {
            composites = PresentationListIO.load();
            composites.sort(Comparator.comparing(PresentationInfo::getCategory));
            for (CompositePresentationInfo list : composites) {
                comps.add(list.getCategory() + " - " + list.getName());
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Error loading composites: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
        compListModel.addAll(comps);
    }
}
