package org.icpc.tools.presentation.admin.internal;

import org.icpc.tools.client.core.BasicClient;
import org.icpc.tools.client.core.IConnectionListener;
import org.icpc.tools.contest.Trace;
import org.icpc.tools.contest.model.feed.JSONParser;
import org.icpc.tools.presentation.core.internal.PresentationInfo;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class MultiView {
    private List<BasicClient> basicClients = new ArrayList<>();
    private Map<String, BasicClient.Client> urlNameClients = new TreeMap<>();
    private Map<String, BasicClient> urlNameBasicClients = new TreeMap<>();

    public static enum Preset {
        // Sections
        Rehearsal(),
        Start(),
        Early(),
        Middle(),
        Freeze(),
        End(),
        After(),
        Spectator(),
        Test(),
        // Hidden
        HIDDEN("Hidden", "hidden"),
        // Logos
        CCS("CCS", "org.icpc.tools.presentation.contest.internal.presentations.CCSPresentation"),
        TOOLS("ICPC Tools", "org.icpc.tools.presentation.core.internal.ICPCToolsPresentation"),
        LOGO_A("LogoA", "org.icpc.tools.presentation.contest.internal.presentations.LogoAPresentation"),
        INTRO("Team Intro", "org.icpc.tools.presentation.contest.internal.presentations.map.TeamIntroPresentation"),
        // Count-down & start
        COUNTDOWN("Countdown", "org.icpc.tools.presentation.contest.internal.presentations.clock.CountdownPresentation"),
        STATUS_COUNTDOWN("StatusCountdown", "org.icpc.tools.presentation.contest.internal.presentations.clock.StatusCountdownPresentation"),
        POLAR("Polar", "org.icpc.tools.presentation.contest.internal.presentations.clock.PolarCountdownPresentation"),
        FIRST_SOLUTION("First Solution", "org.icpc.tools.presentation.contest.internal.scoreboard.FirstSolutionPresentation"),
        JUDGE_QUEUE("Judge Queue", "org.icpc.tools.presentation.contest.internal.scoreboard.JudgePresentation"),
        // Scoreboard
        TILES("Tiles", "org.icpc.tools.presentation.contest.internal.tile.TileScoreboardPresentation"),
        LEADER("Leaderboard",
                "org.icpc.tools.presentation.contest.internal.scoreboard.LeaderboardPresentation"),
        PROBLEM_SUMMARY("Problem Summary", "org.icpc.tools.presentation.contest.internal.presentations.ProblemSummaryPresentation"),
        // Misc
        BALLOON_COLOR("Balloon Color", "org.icpc.tools.presentation.contest.internal.presentations.ProblemColorsPresentation"),
        BALLOON_PATH("Balloon Path", "org.icpc.tools.presentation.contest.internal.presentations.floor.BalloonFloorPresentation"),
        BALLOON_MAP("Balloon Map", "org.icpc.tools.presentation.contest.internal.presentations.map.BalloonMapPresentation"),
        FIRST_TO_SOLVE("First to Solve", "org.icpc.tools.presentation.contest.internal.scoreboard.FirstToSolvePresentation"),
        TIMELINE("Timeline", "org.icpc.tools.presentation.contest.internal.scoreboard.TimelinePresentation"),
        LANGUAGES("Languages Chart", "org.icpc.tools.presentation.contest.internal.chart.LanguageSummaryChart"),
        PROBLEM_COMPARISON("Problem Chart", "org.icpc.tools.presentation.contest.internal.chart.ProblemComparisonChart"),
        PROBLEM_BARS("Problem Bars", "org.icpc.tools.presentation.contest.internal.chart.ProblemSummaryChart"),
        // Test
        SYNC("Synchronization", "org.icpc.tools.presentation.contest.internal.presentations.test.TestSyncPresentation"),
        MESSAGE("Message", "org.icpc.tools.presentation.contest.internal.presentations.MessagePresentation"),
        LONG("-                                                              -", "");
        ;
        Preset(String name, String presClass) {
            this.name = name;
            this.presClass = presClass;
        }
        Preset() {
            this.name = "- " + this.toString();
        }
        String name, presClass;

        public static Preset[] presetList() {
            return new Preset[]{
                    HIDDEN,
                    Rehearsal,
                    CCS,
                    LOGO_A,
                    TOOLS,
                    INTRO,

                    Start,
                    STATUS_COUNTDOWN,
                    POLAR,
                    FIRST_SOLUTION,

                    Early,
                    BALLOON_MAP,

                    Middle,
                    TILES,
                    LEADER,
                    JUDGE_QUEUE,
                    PROBLEM_SUMMARY,

                    Freeze,
                    COUNTDOWN,

                    End,
                    COUNTDOWN,

                    After,
                    MESSAGE,

                    Spectator,
                    BALLOON_COLOR,
                    BALLOON_PATH,
                    BALLOON_MAP,
                    FIRST_TO_SOLVE,
                    TIMELINE,
                    LANGUAGES,
                    PROBLEM_COMPARISON,
                    PROBLEM_BARS,

                    Test,
                    SYNC,
                    MESSAGE,
                    LONG,
            };
        }
    }

    public MultiView(String[] urls, String user, String password) {
        for (String loopUrl : urls) {
            Trace.trace(Trace.INFO, "Connecting to " + loopUrl + " / " + urls.length);
            final String url = loopUrl;
            BasicClient client = new BasicClient(url, user, password, null, user, "admin", "pres-admin") {
                /**
                 * @throws IOException
                 */
                @Override
                protected void handlePresentationList(JSONParser.JsonObject obj) throws IOException {
                }

                @Override
                protected void clientsChanged(Client[] clients) {
                    updateClients(url, this, clients);
                }

                @Override
                protected void handleInfo(int sourceUID, JSONParser.JsonObject obj) {
                }

                @Override
                protected void handleLogResponse(JSONParser.JsonObject obj) throws IOException {
                }

                @Override
                protected void handleSnapshotResponse(JSONParser.JsonObject obj) throws IOException {
                }
            };

            client.addListener(new IConnectionListener() {
                @Override
                public void connectionStateChanged(final boolean connected) {
                    Trace.trace(Trace.INFO, "Connection state changed: " + url + " " + connected);
                }
            });
            basicClients.add(client);
        }
    }

    public void connect() {
        for (BasicClient client : basicClients) {
            client.connect();
        }
    }
    JPanel mainPanel;
    JList<String> compList;
    JList<String> clientsList;
    JList<String> messagesList;
    public JPanel createMainPanel() {
        JPanel mainPanel = new JPanel(new BorderLayout());
        this.mainPanel = mainPanel;
        mainPanel.setPreferredSize(new Dimension(1200, 1000));

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        mainPanel.add(mainSplit);
        //mainPanel.setLayout(new GridLayout(1, 2));

        // Left Panel
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());
        JLabel clientsLabel = new JLabel("Clients");
        JList<String> clientsList = new JList<>(new String[]{"Clients"});
        clientsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        this.clientsList = clientsList;
        leftPanel.add(clientsLabel, BorderLayout.NORTH);
        leftPanel.add(new JScrollPane(clientsList), BorderLayout.CENTER);

        // Quick buttons
        JPanel quickButtonsPanel = quickButtons();
        leftPanel.add(quickButtonsPanel, BorderLayout.SOUTH);

        // Right Panel
        JSplitPane rightPanel = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

        JSplitPane presentationsPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        // Presets Section
        JPanel presetsPanel = new JPanel(new BorderLayout());
        presentationsPanel.setLeftComponent(presetsPanel);
        JLabel presetsLabel = new JLabel("Presets");
        Preset[] presets = Preset.presetList();
        String[] preStr = Arrays.stream(presets).map((p) -> p.name).toList().toArray(new String[0]);
        JList<String> presetsList = new JList<>(preStr);
        presetsPanel.add(presetsLabel, BorderLayout.NORTH);
        presetsPanel.add(new JScrollPane(presetsList), BorderLayout.CENTER);
        JButton applyPreset = new JButton("Apply Preset");
        applyPreset.addActionListener(e -> {
            applyPreset(presetsList.getSelectedValue());
        });
        presetsPanel.add(applyPreset, BorderLayout.SOUTH);

        // Composite Section
        JPanel compPanel = new JPanel(new BorderLayout());
        presentationsPanel.setRightComponent(compPanel);
        JLabel compLabel = new JLabel("Composite");
        String[] compStr = Arrays.stream(presets).map((p) -> p.name).toList().toArray(new String[0]);
        JList<String> compList = new JList<>(compStr);
        this.compList = compList;
        compPanel.add(compLabel, BorderLayout.NORTH);
        compPanel.add(new JScrollPane(compList), BorderLayout.CENTER);
        JPanel compButtons = new JPanel(new GridLayout(1, 2));
        JButton applyComp = new JButton("Apply Composite");
        applyComp.addActionListener(e -> {
            applyComposite(compList.getSelectedValue());
        });
        JButton reload = new JButton("Reload");
        reload.addActionListener(e -> {
            composites();
        });
        compButtons.add(applyComp);
        compButtons.add(reload);
        compPanel.add(compButtons, BorderLayout.SOUTH);

        // Messages Section
        JPanel messagesPanel = new JPanel(new BorderLayout());
        JLabel messagesLabel = new JLabel("Messages");
        JList<String> messagesList = new JList<>(new String[]{
                "",
                "",
                "Contest is Finished\nPlease Exit\n<--->",
                "Contest is Finished\nPlease Exit\nThrough the main doors\n<--->",
                "Time is up!\nPlease exit!\n<--->",
                "Please exit\nThrough the main doors\n<--->",
                "Please leave contest floor\n<--->",
                "Exit, please\n<--->",
                "EXIT, please\n<--->",
                "EXIT!\nNOW!\n<--->",
                "Buses are leaving!\nPlease EXIT!\nNOW!\n<--->",
                "EXIT!!!!11!!!1!\nNOW!!!1!\n<--->",
                "WFNN",
                "CHNN",
        });
        this.messagesList = messagesList;
        messagesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        //JTextField messageInput = new JTextField();
        JTextArea messageInputArea = new JTextArea(3, 50);
        JPanel messageExtra = new JPanel(new GridLayout(2, 1));
        JCheckBox exitArrowsCheckBox = new JCheckBox("Exit arrows");
        messageExtra.add(exitArrowsCheckBox);
        exitArrowsCheckBox.addActionListener(e -> {
            if (exitArrowsCheckBox.isSelected()) {
                messageInputArea.setText(messageInputArea.getText() + "\n<--->");
            } else {
                messageInputArea.setText(messageInputArea.getText().replace("\n<--->", ""));
            }
        });
        messagesList.addListSelectionListener(e -> {
            messageInputArea.setText(messagesList.getSelectedValue());
        });
        JPanel messageEmojis = new JPanel(new GridLayout(1, 7));
        for (String emoLoop : new String[]{"🙂", "🐪", "🐫"}) {
            String emo = emoLoop;
            JButton emoButton = new JButton(emo);
            //emoButton.setPreferredSize(new Dimension(50, 50));
            emoButton.setFont(emoButton.getFont().deriveFont(25.0f));
            emoButton.addActionListener(e -> {
                messageInputArea.insert(emo, messageInputArea.getCaretPosition());
            });
            messageEmojis.add(emoButton);
        }
        messageExtra.add(messageEmojis);

        JPanel messageApplyPanel = new JPanel(new BorderLayout());
        JButton applyMessage = new JButton("Apply Message");
        messageApplyPanel.add(applyMessage, BorderLayout.NORTH);

        JPanel messageInputPanel = new JPanel(new BorderLayout());
        messageInputPanel.add(messageInputArea, BorderLayout.CENTER);
        messageInputPanel.add(messageExtra, BorderLayout.EAST);
        messageApplyPanel.add(messageInputPanel, BorderLayout.CENTER);
        JPanel messageInputButtonPanel = new JPanel(new GridLayout(1, 5));
        JButton upMessage = new JButton("^");
        JButton downMessage = new JButton("v");
        JButton setMessage = new JButton("Set");
        setMessage.addActionListener(e -> {
            messageUpdate(messageInputArea.getText(), MessageAction.SET);
        });
        applyMessage.addActionListener(e -> {
            messageUpdate(messageInputArea.getText(), MessageAction.SET);
            applyMessage(messagesList.getSelectedValue());
        });
        JButton addMessage = new JButton("Add");
        addMessage.addActionListener(e -> {
            messageUpdate(messageInputArea.getText(), MessageAction.ADD);
        });
        JButton removeMessage = new JButton("Remove");
        messageInputButtonPanel.add(upMessage);
        messageInputButtonPanel.add(downMessage);
        messageInputButtonPanel.add(setMessage);
        messageInputButtonPanel.add(addMessage);
        messageInputButtonPanel.add(removeMessage);
        messageApplyPanel.add(messageInputButtonPanel, BorderLayout.SOUTH);
        upMessage.addActionListener(e -> {
            messageUpdate(null, MessageAction.UP);
        });
        downMessage.addActionListener(e -> {
            messageUpdate(null, MessageAction.DOWN);
        });
        removeMessage.addActionListener(e -> {
            messageUpdate(null, MessageAction.REMOVE);
        });

        messagesPanel.add(messagesLabel, BorderLayout.NORTH);
        messagesPanel.add(new JScrollPane(messagesList), BorderLayout.CENTER);
        messagesPanel.add(messageApplyPanel, BorderLayout.SOUTH);

        // Adding components to rightPanel
        rightPanel.setTopComponent(presentationsPanel);
        rightPanel.setBottomComponent(messagesPanel);

        // Add left and right panels to main panel
        mainSplit.setLeftComponent(leftPanel);
        mainSplit.setRightComponent(rightPanel);

        composites();

        return mainPanel;
    }

    List<CompositePresentationInfo> composites;
    private void composites() {
        List<String> comps = new ArrayList<>();
        try {
            composites = PresentationListIO.load();
            composites.sort(Comparator.comparing(PresentationInfo::getCategory));
            String lastCat = "";
            for (CompositePresentationInfo list : composites) {
                Trace.trace(Trace.INFO, "Composite: " + list.getName() + " " + list.infos.size());
                if (!list.getCategory().equals(lastCat)) {
                    comps.add("- " + list.getCategory());
                    lastCat = list.getCategory();
                }
                comps.add(list.getName());
                Trace.trace(Trace.INFO, "Comp " + list.getName() + " class " + list.getClassName());
            }
        } catch (Exception e) {
            Trace.trace(Trace.ERROR, "Error loading custom plans", e);
        }
        compList.setListData(comps.toArray(new String[0]));
    }

    private synchronized void updateClients(String url, BasicClient bc, BasicClient.Client[] clients) {
        // update urlNameClients
        Map<String, BasicClient.Client> map = new TreeMap<>();
        for (BasicClient.Client c : clients) {
            String urlName = url + "-" + c.name;
            map.put(url + "-" + c.name, c);
            urlNameBasicClients.put(urlName, bc);
        }
        urlNameClients.putAll(map);
        Map<String, Boolean> remove = new TreeMap<>();
        for (String urlName : urlNameClients.keySet()) {
            if (urlName.startsWith(url) && !map.containsKey(urlName)) {
                remove.put(urlName, true);
            }
        }
        for (String urlName : remove.keySet()) {
            urlNameClients.remove(urlName);
        }

        // update clientsList
        // TODO: maintain selection through update!
        List<String> clientNames = new ArrayList<>();
        for (BasicClient.Client client : urlNameClients.values()) {
            if (client.name.startsWith("Coach ")) {
                continue;
            }
            if (client.name.startsWith("Balloon ")) {
                continue;
            }
            Trace.trace(Trace.INFO, "Client: " + client.name);
            clientNames.add(client.name);
        }
        clientNames.sort(String::compareTo);
        clientsList.setListData(clientNames.toArray(new String[0]));
        mainPanel.repaint();
    }

    private Map<String, BasicClient.Client> selectedClients() {
        Map<String, BasicClient.Client> map = new TreeMap<>();
        if (clientsList.getSelectedIndices() != null) {
            for (int i : clientsList.getSelectedIndices()) {
                String clientName = clientsList.getModel().getElementAt(i);
                for (Map.Entry<String, BasicClient.Client> entry : urlNameClients.entrySet()) {
                    String urlName = entry.getKey();
                    BasicClient.Client client = entry.getValue();
                    if (client.name.equals(clientName)) {
                        map.put(urlName, client);
                    }
                }
            }
        }
        return map;
    }

    private void applyPreset(String preset) {
        if (preset == null) {
            return;
        }
        Preset foundPre = null;
        String msg = "";
        for (Preset pre : Preset.values()) {
            if (pre.name.equals(preset)) {
                foundPre = pre;
                if (pre == Preset.MESSAGE) {
                    msg = messagesList.getSelectedValue();
                }
                break;
            }
        }
        if (foundPre != null) {
            applyPresentation(foundPre.presClass, msg, null);
        }
    }

    private void applyPresentation(String presClass, String msg, Map<String, String> props) {
        if (presClass.isEmpty()) {
            return;
        }
        for (Map.Entry<String, BasicClient.Client> entry : selectedClients().entrySet()) {
            BasicClient.Client client = entry.getValue();
            String urlName = entry.getKey();
            BasicClient bc = urlNameBasicClients.get(urlName);
            if (bc == null) {
                continue;
            }
            int[] uids = new int[]{client.uid};
            try {
                if (presClass.equals("hidden")) {
                    bc.sendProperty(uids, "hidden", "true");
                } else {
                    bc.sendProperty(uids, "hidden", "false");
                }
                bc.sendProperty(uids, "presentation", "1100|" + presClass);
                if (!msg.isEmpty()) {
                    int ind = presClass.lastIndexOf('.');
                    String propKey = "property[" + presClass.substring(ind + 1) + "|" + presClass.hashCode() + "]";

                    String arrowMsg = msg;
                    Map<String, String> arrowMap = new TreeMap<>(){
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
                    Map<String, String> channelMap = new TreeMap<>() {
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
                    for (Map.Entry<String, String> entry2 : arrowMap.entrySet()) {
                        if (client.name.contains(entry2.getKey())) {
                            arrowMsg = arrowMsg.replace("<--->", entry2.getValue());
                            String nn = entry2.getKey().substring(0, 2);
                            switch (entry2.getKey()) {
                                case "presclient14":
                                    nn = "46";
                                    break;
                                case "presclient16":
                                    nn = "47";
                                    break;
                            }
                            arrowMsg = arrowMsg.replace("WFNN", "WF" + nn);
                            String ch = channelMap.get(entry2.getKey());
                            if (ch == null) {
                                ch = "0";
                            }
                            arrowMsg = arrowMsg.replace("CHNN", ch);
                        }
                    }

                    bc.sendProperty(uids, propKey, arrowMsg);
                    for (Map.Entry<String, String> entry2 : props.entrySet()) {
                        bc.sendProperty(uids, propKey, entry2.getKey());
                    }
                }
            } catch (IOException e) {
                Trace.trace(Trace.ERROR, "Error sending property to " + urlName, e);
            }
        }
    }

    private void applyMessage(String message) {
        applyPresentation(Preset.MESSAGE.presClass, message, null);
    }

    private void applyComposite(String compName) {
        for (CompositePresentationInfo comp : composites) {
            if (comp.getName().equals(compName)) {
                Trace.trace(Trace.INFO, "Applying composite: " + comp.getName() + " " + comp.getClassName());
                String[] presClasses = comp.getClassName().split("\\|");
                for (String presClass : presClasses) {
                    Trace.trace(Trace.INFO, " - " + presClass);
                }
                Map<String, String> props = new TreeMap<>();
                for (PresentationInfo info : comp.infos) {
                    if (info.getProperties() != null) {
                        for (String prop : info.getProperties()) {
                            props.put(prop, "");
                        }
                    }
                }
                applyPresentation(comp.getClassName(), "", props);
            }
        }
    }

    private enum MessageAction {
        UP, DOWN, ADD, REMOVE, SET
    }
    private void messageUpdate(String newMessage, MessageAction action) {
        int idx = messagesList.getSelectedIndex();
        List<String> list = new ArrayList<>();
        for (int i = 0; i < messagesList.getModel().getSize(); i++) {
            list.add(messagesList.getModel().getElementAt(i));
        }
        if (newMessage != null) {
            if (action == MessageAction.ADD || list.isEmpty()) {
                idx++;
                list.add(idx, newMessage);
            } else {
                list.set(idx < 0 ? 0 : idx, newMessage);
            }
        }
        if (action == MessageAction.UP || action == MessageAction.DOWN) {
            int idx2 = idx + (action == MessageAction.UP ? -1 : 1);
            if (idx2 >= 0 && idx2 < list.size()) {
                String tmp = list.get(idx);
                list.set(idx, list.get(idx2));
                list.set(idx2, tmp);
                idx = idx2;
            }
        }
        if (action == MessageAction.REMOVE && idx >= 0) {
            list.remove(idx);
        }
        messagesList.setListData(list.toArray(new String[0]));
        if (idx >= 0 && idx < list.size()) {
            messagesList.setSelectedIndex(idx);
        }
    }

    private JPanel quickButtons() {
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
