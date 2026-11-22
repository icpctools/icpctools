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

public class Multi4647Luxor {
    static TreeMap<String, String> arrowMap() {
        return new TreeMap<>() {
            {
                put("46L", "<–––");
                put("46R", "–––>");
                put("47L", "<–––");
                put("47R", "–––>");
                put("46SpectL", "–––>");
                put("46SpectR", "<–––>");
                put("47SpectL", "<–––>");
                put("47SpectR", "<–––");
                put("46Spare", "<–––>");
                put("47Spare", "<–––>");
                put("presclient14", "<–––");
                put("presclient16", "–––>");
            }
        };
    }

    static TreeMap<String, String> channelMap() {
        return new TreeMap<>() {
            {
                put("46L", "1");
                put("46R", "2");
                put("47L", "5");
                put("47R", "6");
                put("46SpectL", "3");
                put("46SpectR", "4");
                put("47SpectL", "7");
                put("47SpectR", "8");
                put("46Spare", "9");
                put("47Spare", "10");
                put("presclient14", "14");
                put("presclient16", "16");
            }
        };
    }

    static JPanel quickButtons(JList<String> clientsList) {
        JPanel panel = new JPanel(new GridLayout(4, 5));
        Map<String, String> pats = new TreeMap<>(){
            {
                put("", "-4.*");
                put("46", "46");
                put("47", "47");
                put("x", "");
                put("All", "(Spect)?[LR]");
                put("Main", "[67][LR]");
                put("Outer", "(6L|7R)");
                put("Inner", "(6R|7L)");
                put("Spectator", "Spect.*");
                put("46 Main", "46[LR]");
                put("47 Main", "47[LR]");
                put("46 Outer", "46L");
                put("47 Outer", "47R");
                put("46 Inner", "46R");
                put("47 Inner", "47L");
                put("All+", "");
                put("Spares", "Spare");
                put("SpectL", "SpectL");
                put("SpectR", "SpectR");
                put("Tests", "presclient");
            }
        };
        String[] finals = new String[]{"", "46", "47", "x"};
        String[] locations = new String[]{"All", "Main", "Outer", "Inner", "Spectator"};
        String[] xLocs = new String[]{"All+", "Spares", "SpectL", "SpectR", "Tests"};
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

