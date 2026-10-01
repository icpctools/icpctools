package org.icpc.tools.presentation.admin.internal;

import javax.swing.JButton;
import javax.swing.JList;
import javax.swing.JPanel;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class Multi48Astana {
    static TreeMap<String, String> arrowMap() {
        return new TreeMap<>() {
            {
                put("green-presclient1", "<–––>"); // Left big team screen
                put("green-presclient2", "<–––>"); // Right big team screen
                put("green-presclient3", "<–––>"); // Leftmost spectator tv screen
                put("green-presclient4", "<–––>"); // Second spectator tv screen, left section
                put("green-presclient5", "<–––>"); // Third spectator tv screen, right section
                put("green-presclient6", "<–––>"); // Rightmost spectator tv screen
                put("green-presclient7", "<–––>"); // Spare channel
                put("green-presclient8", "<–––>"); // Dedicated clock monitor, no video channel
                put("green-presclientxtra", "<–––>"); // Extra computer, no video channel
            }
        };
    }

    static TreeMap<String, String> channelMap() {
        return new TreeMap<>() {
            {
                put("green-presclient1", "1"); // Left big team screen
                put("green-presclient2", "2"); // Right big team screen
                put("green-presclient3", "3"); // Leftmost spectator tv screen
                put("green-presclient4", "4"); // Second spectator tv screen, left section
                put("green-presclient5", "5"); // Third spectator tv screen, right section
                put("green-presclient6", "6"); // Rightmost spectator tv screen
                put("green-presclient7", "7"); // Spare channel
                put("green-presclient8", "8"); // Dedicated clock monitor, no video channel
                put("green-presclientxtra", "X"); // Extra computer, no video channel
            }
        };
    }

    static JPanel quickButtons(JList<String> clientsList) {
        JPanel panel = new JPanel(new GridLayout(2, 5));
        Map<String, String> pats = new TreeMap<>(){
            {
                put("", "");
                put("x", "");
                put("All", "client[1-6]");
                put("Main", "client[1-2]");
                put("Left", "client[135]");
                put("Right", "client[246]");
                put("Spectator", "client[3-6]");
                put("All+", "client[1-8]");
                put("Spares", "client[7]");
                put("SpectInner", "client[45]");
                put("SpectOuter", "client[36]");
                put("Tests", "clientxtra");
            }
        };
        String[] finals = new String[]{"", "x"};
        String[] locations = new String[]{"All", "Main", "Spectator", "Left", "Right"};
        String[] xLocs = new String[]{"All+", "Spares", "SpectInner", "SpectOuter", "Tests"};
        for (String fin : finals) {
            for (String loc : fin.equals("x") ? xLocs : locations) {
                String name = fin + (fin.length() > 0 ? " " : "") + loc;
                String patStr = ".*" + pats.get(fin) + pats.get(loc) + ".*";
                if (pats.containsKey(name)) {
                    patStr = ".*" + pats.get(name) + ".*";
                }
                Pattern pat = Pattern.compile(patStr);
                JButton button = new JButton(name);
                button.addActionListener(e -> {
                    List<Integer> idxs = new ArrayList<>();
                    for (int i = 0; i < clientsList.getModel().getSize(); i++) {
                        String clientName = clientsList.getModel().getElementAt(i);
                        if (pat.matcher(clientName).matches()) {
                            idxs.add(i);
                        }
                    }
                    clientsList.setSelectedIndices(idxs.stream().mapToInt(i -> i).toArray());
                });
                panel.add(button);
            }
        }
        return panel;
    }
}
