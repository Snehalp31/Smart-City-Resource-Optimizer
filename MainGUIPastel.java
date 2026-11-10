import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.net.*;
import java.security.*;
import java.util.*;
import java.util.List;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Smart City Emergency Resource Optimizer
 * Java 17+, Swing GUI, weighted city graph, Dijkstra allocation and file persistence.
 */
public class MainGUIPastel {
    private static final Path DATA = Paths.get("data");
    private static final Path ACCOUNTS = DATA.resolve("accounts.txt");
    private static final Path AREAS = DATA.resolve("areas.txt");
    private static final Path ROADS = DATA.resolve("roads.txt");
    private static final Path RESOURCES = DATA.resolve("resources.txt");
    private static final Path REQUESTS = DATA.resolve("requests.txt");
    private static final Path ACTIVITY = DATA.resolve("activity-history.log");

    private static final Color BG = new Color(245, 247, 252);
    private static final Color PANEL = new Color(255, 255, 255);
    private static final Color PANEL2 = new Color(235, 241, 250);
    private static final Color TEXT = new Color(48, 58, 76);
    private static final Color MUTED = new Color(105, 117, 139);
    private static final Color BLUE = new Color(111, 145, 205);
    private static final Color TEAL = new Color(105, 174, 166);
    private static final Color PEACH = new Color(224, 158, 132);

    static final CityModel CITY = new CityModel();
    static final Map<String, Account> ACCOUNTS_MAP = new LinkedHashMap<>();
    static final List<EmergencyRequest> REQUESTS_LIST = new ArrayList<>();

    private static String currentUser;
    private static String currentRole;
    private static JFrame mainFrame;
    private static JTextArea output;
    private static GraphPanel graphPanel;
    private static JLabel statusLabel;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
            try {
                Storage.initialize();
                Storage.loadAll();
                try { MapServer.start(); }
                catch (IOException mapError) { System.err.println("Live map server unavailable: " + mapError.getMessage()); }
                showLogin();
            } catch (Exception ex) {
                ex.printStackTrace();
                JOptionPane.showMessageDialog(null,
                    "Could not initialize application data:\n" + ex.getMessage(),
                    "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    private static void showLogin() {
        JFrame frame = new JFrame("Smart City | Emergency Resource Optimizer");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(900, 620);
        frame.setMinimumSize(new Dimension(780, 560));
        frame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new GridLayout(1, 2, 0, 0));
        root.setBackground(BG);

        JPanel hero = new JPanel();
        hero.setLayout(new BoxLayout(hero, BoxLayout.Y_AXIS));
        hero.setBackground(new Color(24, 43, 76));
        hero.setBorder(new EmptyBorder(48, 42, 42, 38));
        JLabel brand = new JLabel("SMART CITY");
        brand.setFont(new Font("Segoe UI", Font.BOLD, 15));
        brand.setForeground(new Color(145, 190, 255));
        JLabel heroTitle = new JLabel("<html>Emergency<br>response,<br>coordinated.</html>");
        heroTitle.setFont(new Font("Segoe UI", Font.BOLD, 34));
        heroTitle.setForeground(Color.WHITE);
        JLabel heroText = new JLabel("<html><div style='width:270px;line-height:1.5'>Coordinate city resources, manage emergency requests and support faster response decisions through one operations workspace.</div></html>");
        heroText.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        heroText.setForeground(new Color(211, 222, 240));
        JLabel heroFoot = new JLabel("OPENSTREETMAP  ·  DIJKSTRA ROUTING  ·  JAVA");
        heroFoot.setFont(new Font("Segoe UI", Font.BOLD, 10));
        heroFoot.setForeground(new Color(145, 165, 195));
        hero.add(brand); hero.add(Box.createVerticalStrut(58)); hero.add(heroTitle);
        hero.add(Box.createVerticalStrut(22)); hero.add(heroText); hero.add(Box.createVerticalGlue()); hero.add(heroFoot);

        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(BG);
        JPanel card = new JPanel(new GridBagLayout());
        card.setBackground(PANEL);
        card.setBorder(new CompoundBorder(new LineBorder(new Color(226,232,242), 1, true), new EmptyBorder(28,30,28,30)));
        card.setPreferredSize(new Dimension(370, 470));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx=0; c.gridy=0; c.weightx=1; c.fill=GridBagConstraints.HORIZONTAL;
        c.insets=new Insets(0,0,8,0);
        JLabel welcome = new JLabel("Welcome back"); welcome.setFont(new Font("Segoe UI",Font.BOLD,25)); welcome.setForeground(TEXT); card.add(welcome,c);
        c.gridy++; JLabel sub = new JLabel("Choose your portal and sign in to continue."); sub.setForeground(MUTED); sub.setFont(new Font("Segoe UI",Font.PLAIN,13)); card.add(sub,c);

        c.gridy++; c.insets=new Insets(22,0,8,0);
        JLabel portalLabel = new JLabel("SELECT PORTAL"); portalLabel.setFont(new Font("Segoe UI",Font.BOLD,11)); portalLabel.setForeground(MUTED); card.add(portalLabel,c);
        c.gridy++; c.insets=new Insets(0,0,16,0);
        JRadioButton citizen = new JRadioButton("Citizen portal", true);
        JRadioButton municipal = new JRadioButton("Municipal staff");
        citizen.setOpaque(true); municipal.setOpaque(true);
        citizen.setBackground(new Color(239,245,255)); municipal.setBackground(new Color(239,245,255));
        citizen.setFont(new Font("Segoe UI",Font.BOLD,13)); municipal.setFont(new Font("Segoe UI",Font.BOLD,13));
        citizen.setBorder(new EmptyBorder(11,12,11,12)); municipal.setBorder(new EmptyBorder(11,12,11,12));
        ButtonGroup roles = new ButtonGroup(); roles.add(citizen); roles.add(municipal);
        JPanel roleChoices = new JPanel(new GridLayout(1,2,8,0)); roleChoices.setOpaque(false); roleChoices.add(citizen); roleChoices.add(municipal); card.add(roleChoices,c);

        c.gridy++; c.insets=new Insets(0,0,6,0); JLabel userLabel = new JLabel("USER ID"); userLabel.setFont(new Font("Segoe UI",Font.BOLD,11)); userLabel.setForeground(MUTED); card.add(userLabel,c);
        c.gridy++; JTextField idField = new JTextField(); styleInput(idField); card.add(idField,c);
        c.gridy++; c.insets=new Insets(14,0,6,0); JLabel passLabel = new JLabel("PASSWORD"); passLabel.setFont(new Font("Segoe UI",Font.BOLD,11)); passLabel.setForeground(MUTED); card.add(passLabel,c);
        c.gridy++; c.insets=new Insets(0,0,14,0); JPasswordField passwordField = new JPasswordField(); styleInput(passwordField); card.add(passwordField,c);

        JButton login = button("Sign in", new Color(43,100,185)); login.setFont(new Font("Segoe UI",Font.BOLD,14));
        c.gridy++; c.insets=new Insets(4,0,8,0); card.add(login,c);
        JButton signup = button("Create citizen account", new Color(20,139,125)); signup.setFont(new Font("Segoe UI",Font.BOLD,13));
        c.gridy++; c.insets=new Insets(0,0,12,0); card.add(signup,c);
        JLabel portalHint = new JLabel("<html><div style='width:300px;text-align:center'>Municipal access is issued by the system administrator. Public registration is for citizens only.</div></html>",SwingConstants.CENTER);
        portalHint.setFont(new Font("Segoe UI",Font.PLAIN,11)); portalHint.setForeground(MUTED); c.gridy++; card.add(portalHint,c);

        Runnable updatePortal = () -> {
            boolean isCitizen = citizen.isSelected();
            signup.setEnabled(isCitizen);
            signup.setText(isCitizen ? "Create citizen account" : "Municipal accounts are provisioned by admin");
            signup.setBackground(isCitizen ? new Color(20,139,125) : new Color(165,174,188));
            portalHint.setText(isCitizen
                ? "<html><div style='width:300px;text-align:center'>Citizens can register here. Municipal staff must use an administrator-provisioned account.</div></html>"
                : "<html><div style='width:300px;text-align:center'>Use the municipal credentials configured by your administrator. Municipal registration is intentionally disabled.</div></html>");
        };
        citizen.addActionListener(e -> updatePortal.run()); municipal.addActionListener(e -> updatePortal.run());

        login.addActionListener(e -> {
            String id=idField.getText().trim(); char[] pw=passwordField.getPassword();
            Account a=ACCOUNTS_MAP.get(id);
            String expectedRole=citizen.isSelected()?"citizen":"municipal";
            boolean valid=a!=null && expectedRole.equals(a.role) && Passwords.verify(pw,a.salt,a.passwordHash);
            Arrays.fill(pw,'\0');
            if(!valid) {
                JOptionPane.showMessageDialog(frame,
                    "The user ID/password is invalid for the selected portal. Check whether you selected Citizen or Municipal staff.",
                    "Sign-in failed",JOptionPane.ERROR_MESSAGE); return;
            }
            currentUser=id; currentRole=a.role; Storage.logActivity(currentUser,"LOGIN_"+currentRole.toUpperCase(Locale.ROOT));
            frame.dispose(); showDashboard();
        });
        signup.addActionListener(e -> {
            if(!citizen.isSelected()) return;
            String id=idField.getText().trim(); char[] pw=passwordField.getPassword();
            if(id.isBlank()||pw.length<8) { Arrays.fill(pw,'\0'); JOptionPane.showMessageDialog(frame,"Enter a user ID and a password of at least 8 characters."); return; }
            if(ACCOUNTS_MAP.containsKey(id)) { Arrays.fill(pw,'\0'); JOptionPane.showMessageDialog(frame,"That user ID already exists."); return; }
            try {
                Account a=Passwords.create(id,pw,"citizen"); Arrays.fill(pw,'\0'); ACCOUNTS_MAP.put(id,a); Storage.saveAccounts();
                currentUser=id; currentRole="citizen"; Storage.logActivity(currentUser,"ACCOUNT_REGISTERED"); frame.dispose(); showDashboard();
            } catch(Exception ex) { Arrays.fill(pw,'\0'); showError("Could not create account",ex); }
        });
        c.gridy++; c.insets=new Insets(12,0,0,0);
        JLabel adminHelp = new JLabel("<html><div style='width:300px;text-align:center'>First-time admin setup: configure INITIAL_ADMIN_USER and INITIAL_ADMIN_PASSWORD before launching the app. See README.md.</div></html>",SwingConstants.CENTER);
        adminHelp.setFont(new Font("Segoe UI",Font.PLAIN,10)); adminHelp.setForeground(new Color(130,140,155)); card.add(adminHelp,c);

        right.add(card); root.add(hero); root.add(right); frame.setContentPane(root); frame.setVisible(true);
        frame.getRootPane().setDefaultButton(login);
    }

    private static void styleInput(JComponent input) {
        input.setFont(new Font("Segoe UI",Font.PLAIN,14));
        input.setPreferredSize(new Dimension(300,40));
        input.setBorder(new CompoundBorder(new LineBorder(new Color(214,222,234),1,true),new EmptyBorder(8,10,8,10)));
        input.setBackground(new Color(250,252,255));
    }

    private static void showDashboard() {
        mainFrame = new JFrame("Smart City Operations | " + currentUser);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1320, 820); mainFrame.setMinimumSize(new Dimension(1050,680)); mainFrame.setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout(0,0)); root.setBackground(BG);
        JPanel header = new JPanel(new BorderLayout(20,0)); header.setBackground(PANEL);
        header.setBorder(new CompoundBorder(new MatteBorder(0,0,1,0,new Color(226,232,240)),new EmptyBorder(17,24,17,24)));
        JPanel brandBlock = new JPanel(); brandBlock.setOpaque(false); brandBlock.setLayout(new BoxLayout(brandBlock,BoxLayout.Y_AXIS));
        JLabel brand = new JLabel("SMART CITY  /  EMERGENCY OPERATIONS"); brand.setFont(new Font("Segoe UI",Font.BOLD,11)); brand.setForeground(new Color(45,105,185));
        JLabel heading = new JLabel("Operations dashboard"); heading.setFont(new Font("Segoe UI",Font.BOLD,22)); heading.setForeground(TEXT);
        brandBlock.add(brand); brandBlock.add(Box.createVerticalStrut(3)); brandBlock.add(heading); header.add(brandBlock,BorderLayout.WEST);
        JPanel userBlock = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,4)); userBlock.setOpaque(false);
        JLabel user = new JLabel(currentUser+"  ·  "+currentRole.toUpperCase(Locale.ROOT)); user.setFont(new Font("Segoe UI",Font.BOLD,12)); user.setForeground(TEXT);
        JButton logout = button("Sign out",new Color(92,108,130)); logout.addActionListener(e->{mainFrame.dispose();currentUser=null;currentRole=null;showLogin();});
        userBlock.add(user); userBlock.add(logout); header.add(userBlock,BorderLayout.EAST); root.add(header,BorderLayout.NORTH);

        JPanel sidebar = new JPanel(); sidebar.setBackground(new Color(247,249,253)); sidebar.setBorder(new MatteBorder(0,0,0,1,new Color(226,232,240)));
        sidebar.setLayout(new BoxLayout(sidebar,BoxLayout.Y_AXIS)); sidebar.setBorder(new CompoundBorder(new MatteBorder(0,0,0,1,new Color(226,232,240)),new EmptyBorder(20,14,18,14)));
        sidebar.setPreferredSize(new Dimension(270,0));
        JLabel navTitle=new JLabel("WORKSPACE"); navTitle.setFont(new Font("Segoe UI",Font.BOLD,11)); navTitle.setForeground(MUTED); navTitle.setBorder(new EmptyBorder(0,8,12,0)); sidebar.add(navTitle);
        if("municipal".equals(currentRole)) {
            addNavSection(sidebar,"CITY NETWORK");
            addAction(sidebar,"＋  Add City Area",BLUE,MainGUIPastel::addArea);
            addAction(sidebar,"＋  Add Weighted Road",BLUE,MainGUIPastel::addRoad);
            addAction(sidebar,"＋  Resource Center",BLUE,MainGUIPastel::addCenter);
            addNavSection(sidebar,"RESOURCE FLEET");
            addAction(sidebar,"＋  Add Resource",TEAL,MainGUIPastel::addResource);
            addAction(sidebar,"Edit Resource",TEAL,MainGUIPastel::editResource);
            addAction(sidebar,"Remove Resource",PEACH,MainGUIPastel::removeResource);
            addAction(sidebar,"Show All Resources",TEAL,MainGUIPastel::showResources);
            addNavSection(sidebar,"EMERGENCY DESK");
            addAction(sidebar,"View All Requests",BLUE,MainGUIPastel::showAllRequests);
            addAction(sidebar,"Search Requests",TEAL,MainGUIPastel::searchRequests);
            addAction(sidebar,"Task Management",TEAL,MainGUIPastel::manageTasks);
            addAction(sidebar,"Reassign Resource",PEACH,MainGUIPastel::reassignResource);
            addAction(sidebar,"Mark Task Complete",PEACH,MainGUIPastel::markComplete);
            addNavSection(sidebar,"INSIGHTS & MAPS");
            addAction(sidebar,"Current Weather",TEAL,MainGUIPastel::showWeather);
            addAction(sidebar,"Areas & Roads",BLUE,MainGUIPastel::showAreasAndRoads);
            addAction(sidebar,"Analytics & Reports",BLUE,MainGUIPastel::showAnalytics);
            addAction(sidebar,"Activity History",BLUE,MainGUIPastel::showActivityHistory);
            addAction(sidebar,"Export Requests CSV",TEAL,MainGUIPastel::exportRequestsCsv);
            addAction(sidebar,"Live Operations Map",PEACH,MainGUIPastel::openLiveMap);
        } else {
            addNavSection(sidebar,"CITIZEN SERVICES");
            addAction(sidebar,"Create Emergency Request",PEACH,MainGUIPastel::createRequest);
            addAction(sidebar,"View My Requests",BLUE,MainGUIPastel::showMyRequests);
            addAction(sidebar,"Show Available Resources",TEAL,MainGUIPastel::showResources);
            addAction(sidebar,"Current Weather",TEAL,MainGUIPastel::showWeather);
            addAction(sidebar,"Live City Map",PEACH,MainGUIPastel::openLiveMap);
        }
        sidebar.add(Box.createVerticalGlue());
        JLabel sideNote=new JLabel("<html><div style='width:220px'>Local prototype · Data stays in your configured database or data folder.</div></html>"); sideNote.setFont(new Font("Segoe UI",Font.PLAIN,10)); sideNote.setForeground(MUTED); sidebar.add(sideNote);
        JScrollPane navScroll=new JScrollPane(sidebar); navScroll.setBorder(null); navScroll.getVerticalScrollBar().setUnitIncrement(18); root.add(navScroll,BorderLayout.WEST);

        JPanel content=new JPanel(new BorderLayout(0,16)); content.setOpaque(false); content.setBorder(new EmptyBorder(22,24,18,24));
        JPanel intro=new JPanel(new BorderLayout()); intro.setOpaque(false);
        JLabel hello=new JLabel("Good day, "+currentUser); hello.setFont(new Font("Segoe UI",Font.BOLD,23)); hello.setForeground(TEXT);
        JLabel desc=new JLabel("Here is your current city response workspace."); desc.setFont(new Font("Segoe UI",Font.PLAIN,13)); desc.setForeground(MUTED);
        JPanel introText=new JPanel(); introText.setOpaque(false); introText.setLayout(new BoxLayout(introText,BoxLayout.Y_AXIS)); introText.add(hello); introText.add(Box.createVerticalStrut(4)); introText.add(desc); intro.add(introText,BorderLayout.WEST);
        JLabel rolePill=new JLabel("  "+currentRole.toUpperCase(Locale.ROOT)+" PORTAL  "); rolePill.setOpaque(true); rolePill.setBackground(new Color(230,239,252)); rolePill.setForeground(new Color(43,91,155)); rolePill.setFont(new Font("Segoe UI",Font.BOLD,11)); rolePill.setBorder(new EmptyBorder(8,8,8,8)); intro.add(rolePill,BorderLayout.EAST); content.add(intro,BorderLayout.NORTH);

        JPanel workspace=cardPanel(); workspace.setLayout(new BorderLayout(0,12)); workspace.setBorder(new CompoundBorder(new LineBorder(new Color(225,231,240),1,true),new EmptyBorder(18,18,18,18)));
        JPanel workspaceHead=new JPanel(new BorderLayout()); workspaceHead.setOpaque(false);
        JLabel activityTitle=new JLabel("Workspace activity"); activityTitle.setFont(new Font("Segoe UI",Font.BOLD,16)); activityTitle.setForeground(TEXT);
        JLabel activitySub=new JLabel("Select an action from the navigation panel. Results and reports appear here."); activitySub.setFont(new Font("Segoe UI",Font.PLAIN,12)); activitySub.setForeground(MUTED);
        JPanel wh=new JPanel(); wh.setOpaque(false); wh.setLayout(new BoxLayout(wh,BoxLayout.Y_AXIS)); wh.add(activityTitle); wh.add(Box.createVerticalStrut(4)); wh.add(activitySub); workspaceHead.add(wh,BorderLayout.WEST); workspace.add(workspaceHead,BorderLayout.NORTH);
        output=new JTextArea(); output.setEditable(false); output.setLineWrap(true); output.setWrapStyleWord(true); output.setFont(new Font("Segoe UI",Font.PLAIN,14)); output.setForeground(TEXT); output.setBackground(new Color(250,252,255)); output.setBorder(new EmptyBorder(16,16,16,16));
        output.setText("Welcome to the Smart City operations workspace.\n\nChoose an action from the left navigation to manage city areas, resources, emergency requests, tasks, reports or the live map.\n\nTip: municipal users should configure city areas and weighted roads before adding resources and testing Dijkstra route suggestions.");
        workspace.add(new JScrollPane(output),BorderLayout.CENTER);
        graphPanel=new GraphPanel(CITY); graphPanel.setPreferredSize(new Dimension(700,180)); graphPanel.setBorder(new CompoundBorder(new LineBorder(new Color(226,232,240),1,true),new EmptyBorder(8,8,8,8)));
        JPanel graphWrap=new JPanel(new BorderLayout(0,6)); graphWrap.setOpaque(false); JLabel graphTitle=new JLabel("Configured city network preview"); graphTitle.setFont(new Font("Segoe UI",Font.BOLD,12)); graphTitle.setForeground(MUTED); graphWrap.add(graphTitle,BorderLayout.NORTH); graphWrap.add(graphPanel,BorderLayout.CENTER); workspace.add(graphWrap,BorderLayout.SOUTH);
        content.add(workspace,BorderLayout.CENTER);
        statusLabel=new JLabel("  Data location: "+DATA.toAbsolutePath()); statusLabel.setFont(new Font("Segoe UI",Font.PLAIN,11)); statusLabel.setForeground(MUTED); content.add(statusLabel,BorderLayout.SOUTH);
        root.add(content,BorderLayout.CENTER); mainFrame.setContentPane(root); mainFrame.setVisible(true); graphPanel.repaint();
    }

    private static void addNavSection(JPanel parent,String title) {
        JLabel section=new JLabel(title); section.setFont(new Font("Segoe UI",Font.BOLD,10)); section.setForeground(new Color(119,132,153));
        section.setBorder(new EmptyBorder(14,8,7,0)); section.setAlignmentX(Component.LEFT_ALIGNMENT); parent.add(section);
    }

    private static void addAction(JPanel parent, String name, Color color, Runnable action) {
        JButton b = button(name, color);
        b.setAlignmentX(Component.CENTER_ALIGNMENT);
        b.setMaximumSize(new Dimension(Integer.MAX_VALUE, 38));
        b.setPreferredSize(new Dimension(230, 38));
        b.setAlignmentX(Component.LEFT_ALIGNMENT);
        b.setHorizontalAlignment(SwingConstants.LEFT);
        b.addActionListener(e -> {
            try { action.run(); }
            catch (Exception ex) { showError("Action failed: " + name, ex); }
        });
        parent.add(b);
        parent.add(Box.createVerticalStrut(8));
    }

    /** Fetches current conditions from the free Open-Meteo API for a configured city area. */
    private static void showWeather() {
        if (CITY.coordinates.isEmpty()) {
            info("No city coordinates are configured yet. Add a city area with latitude and longitude first.");
            return;
        }
        String area = choose("Choose a configured area for current weather:", sorted(CITY.coordinates.keySet()).toArray(new String[0]));
        if (area == null) return;
        GeoPoint point = CITY.coordinates.get(area);
        output.setText("Loading current weather for " + area + " from Open-Meteo...\nPlease wait.");
        new SwingWorker<WeatherService.Weather, Void>() {
            @Override protected WeatherService.Weather doInBackground() throws Exception {
                return WeatherService.fetch(point.lat, point.lng);
            }
            @Override protected void done() {
                try {
                    WeatherService.Weather w = get();
                    output.setText("CURRENT WEATHER — " + area + "\n"
                            + "----------------------------------------\n"
                            + "Conditions: " + w.description + "\n"
                            + String.format(Locale.ROOT, "Temperature: %.1f °C\nFeels like: %.1f °C\n", w.temperatureC, w.apparentTemperatureC)
                            + String.format(Locale.ROOT, "Relative humidity: %.0f %%\nPrecipitation: %.1f mm\nWind speed: %.1f km/h\n", w.relativeHumidity, w.precipitationMm, w.windSpeedKmh)
                            + "\nCoordinates: " + point.lat + ", " + point.lng + "\n"
                            + "Source: Open-Meteo (current conditions)\n"
                            + "Weather data is informational and should not replace official emergency alerts.");
                } catch (Exception ex) {
                    Throwable cause = ex.getCause() == null ? ex : ex.getCause();
                    showError("Could not retrieve weather from Open-Meteo", cause instanceof Exception ? (Exception) cause : new Exception(cause));
                }
            }
        }.execute();
    }

    private static void addArea() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        JTextField name = new JTextField();
        JTextField lat = new JTextField();
        JTextField lng = new JTextField();
        JPanel form = formPanel(new String[]{"Area name", "Latitude (decimal)", "Longitude (decimal)"},
                new JComponent[]{name, lat, lng});
        if (JOptionPane.showConfirmDialog(mainFrame, form, "Add city area", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String area = name.getText().trim();
        if (area.isEmpty()) { info("Area name is required."); return; }
        if (CITY.adj.containsKey(area)) { info("Area already exists."); return; }
        try {
            double latitude = Double.parseDouble(lat.getText().trim());
            double longitude = Double.parseDouble(lng.getText().trim());
            if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180)
                throw new IllegalArgumentException("Coordinates are outside valid latitude/longitude ranges.");
            CITY.addArea(area);
            CITY.coordinates.put(area, new GeoPoint(latitude, longitude));
            persistCity();
            log("Added area with verified user-supplied coordinates: " + area);
        } catch (NumberFormatException ex) { info("Enter valid decimal latitude and longitude."); }
          catch (IllegalArgumentException ex) { info(ex.getMessage()); }
    }

    private static void addRoad() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        if (CITY.adj.size() < 2) { info("Add at least two areas before adding a road."); return; }
        JTextField a = new JTextField(), b = new JTextField(), d = new JTextField();
        JPanel p = formPanel(new String[]{"Area A", "Area B", "Distance (km)"}, new JComponent[]{a,b,d});
        if (JOptionPane.showConfirmDialog(mainFrame, p, "Add weighted road", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        int distance;
        try { distance = Integer.parseInt(d.getText().trim()); }
        catch (NumberFormatException ex) { info("Distance must be a positive whole number."); return; }
        if (distance <= 0) { info("Distance must be greater than zero."); return; }
        if (!CITY.adj.containsKey(a.getText().trim()) || !CITY.adj.containsKey(b.getText().trim())) {
            info("Both areas must already exist."); return;
        }
        if (a.getText().trim().equals(b.getText().trim())) { info("Choose two different areas."); return; }
        CITY.addRoad(a.getText().trim(), b.getText().trim(), distance);
        persistCity();
        log("Road added: " + a.getText().trim() + " ↔ " + b.getText().trim() + " (" + distance + " km)");
    }

    private static void addCenter() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        if (CITY.adj.isEmpty()) { info("Add an area first."); return; }
        String area = choose("Choose resource-center area:", CITY.adj.keySet().toArray(new String[0]));
        if (area == null) return;
        CITY.resources.putIfAbsent(area, new ArrayList<>());
        persistResources();
        log("Resource center ready at " + area);
    }

    private static void addResource() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        if (CITY.resources.isEmpty()) { info("Add a resource center first."); return; }
        String[] centers = CITY.resources.keySet().toArray(new String[0]);
        JComboBox<String> area = new JComboBox<>(centers);
        JTextField type = new JTextField(), id = new JTextField(), driver = new JTextField();
        JPanel p = formPanel(new String[]{"Center area", "Resource type", "Vehicle/resource ID", "Driver name"},
                new JComponent[]{area,type,id,driver});
        if (JOptionPane.showConfirmDialog(mainFrame, p, "Add resource", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String resourceId = id.getText().trim();
        String resourceType = type.getText().trim();
        if (resourceId.isEmpty() || resourceType.isEmpty() || driver.getText().trim().isEmpty()) {
            info("All resource fields are required."); return;
        }
        if (CITY.findResource(resourceId) != null) { info("Resource ID already exists."); return; }
        String center = (String) area.getSelectedItem();
        CITY.resources.get(center).add(new Resource(resourceType, resourceId, driver.getText().trim(), true));
        persistResources();
        log("Added " + resourceType + " " + resourceId + " at " + center);
        graphPanel.repaint();
    }


    private static void editResource() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        List<Resource> all = CITY.allResources();
        if (all.isEmpty()) { info("No resources to edit."); return; }
        String[] choices = all.stream().map(r -> r.id + " — " + r.type).toArray(String[]::new);
        String choice = choose("Select a resource:", choices);
        if (choice == null) return;
        String id = choice.split(" — ",2)[0];
        Resource r = CITY.findResource(id);
        if (r == null) return;
        if (!r.available) { info("An occupied resource cannot be moved or edited until its task is released."); return; }
        JTextField type = new JTextField(r.type), driver = new JTextField(r.driver);
        JComboBox<String> centers = new JComboBox<>(CITY.resources.keySet().toArray(new String[0]));
        String currentCenter = CITY.resources.entrySet().stream().filter(e -> e.getValue().contains(r))
                .map(Map.Entry::getKey).findFirst().orElse("");
        centers.setSelectedItem(currentCenter);
        JPanel form = formPanel(new String[]{"Resource type","Driver name","Resource center"},
                new JComponent[]{type,driver,centers});
        if (JOptionPane.showConfirmDialog(mainFrame,form,"Edit resource "+id,JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        if(type.getText().trim().isEmpty()||driver.getText().trim().isEmpty()) { info("Type and driver are required."); return; }
        String newCenter=(String)centers.getSelectedItem();
        CITY.resources.get(currentCenter).remove(r);
        r.type=type.getText().trim(); r.driver=driver.getText().trim();
        CITY.resources.get(newCenter).add(r);
        persistResources();
        log("Updated resource "+id);
    }
    private static void removeResource() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        List<Resource> available = CITY.allResources().stream().filter(r -> r.available).toList();
        if(available.isEmpty()){info("No available resources can be removed. Active resources are protected.");return;}
        String[] choices=available.stream().map(r->r.id+" — "+r.type).toArray(String[]::new);
        String choice=choose("Select an available resource to remove:",choices);
        if(choice==null)return;
        String id=choice.split(" — ",2)[0];
        int confirm=JOptionPane.showConfirmDialog(mainFrame,"Remove resource "+id+"?","Confirm removal",JOptionPane.YES_NO_OPTION);
        if(confirm!=JOptionPane.YES_OPTION)return;
        for(List<Resource> list:CITY.resources.values()) list.removeIf(r->r.id.equals(id));
        persistResources(); log("Removed resource "+id);
    }
    private static void showAreasAndRoads() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        StringBuilder b=new StringBuilder("CITY AREAS AND WEIGHTED ROADS\\n\\n");
        for(String area:sorted(CITY.adj.keySet())) {
            GeoPoint pt=CITY.coordinates.get(area);
            b.append(area);
            if(pt!=null)b.append(" [").append(pt.lat).append(", ").append(pt.lng).append("]");
            b.append("\\n");
            for(Map.Entry<String,Integer> edge:CITY.adj.get(area).entrySet())
                if(area.compareTo(edge.getKey())<0)b.append("   ↔ ").append(edge.getKey()).append(" — ").append(edge.getValue()).append(" km\\n");
        }
        output.setText(b.toString());
    }
    private static void searchRequests() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        String term=ask("Search by request ID, type, area or status:");
        if(term==null)return;
        String q=term.toLowerCase(Locale.ROOT);
        StringBuilder b=new StringBuilder("REQUEST SEARCH RESULTS\\n\\n");
        for(EmergencyRequest r:REQUESTS_LIST)
            if(r.id.toLowerCase(Locale.ROOT).contains(q)||r.type.toLowerCase(Locale.ROOT).contains(q)||
               r.location.toLowerCase(Locale.ROOT).contains(q)||r.status.toLowerCase(Locale.ROOT).contains(q))
                b.append(r.toDisplay()).append("\\n");
        output.setText(b.toString());
    }
    private static void showActivityHistory() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        try { output.setText(JdbcStore.isActive() ? JdbcStore.readActivity() :
                (Files.exists(ACTIVITY) ? Files.readString(ACTIVITY,StandardCharsets.UTF_8) : "No activity recorded.")); }
        catch(Exception ex){showError("Could not read activity history",ex);}
    }

    private static void showResources() {
        StringBuilder sb = new StringBuilder("RESOURCE CENTERS AND RESOURCES\n\n");
        if (CITY.resources.isEmpty()) sb.append("No resource centers configured.\n");
        for (String area : sorted(CITY.resources.keySet())) {
            sb.append(area).append(":\n");
            List<Resource> list = CITY.resources.get(area);
            if (list.isEmpty()) sb.append("  (no resources)\n");
            for (Resource r : list)
                sb.append("  ").append(r.type).append(" | ID: ").append(r.id)
                  .append(" | Driver: ").append(r.driver).append(" | Available: ").append(r.available).append("\n");
        }
        output.setText(sb.toString());
    }

    private static void createRequest() {
        if (CITY.resources.isEmpty()) info("No resource centers have been configured yet.");
        JTextField area = new JTextField(), type = new JTextField(), description = new JTextField();
        JTextField lat = new JTextField(), lng = new JTextField();
        JComboBox<String> priority = new JComboBox<>(new String[]{"Critical", "High", "Medium", "Low"});
        JPanel p = formPanel(new String[]{"Emergency area", "Emergency type", "Description", "Latitude", "Longitude", "Priority"},
                new JComponent[]{area,type,description,lat,lng,priority});
        if (JOptionPane.showConfirmDialog(mainFrame, p, "Create emergency request", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) return;
        String loc = area.getText().trim(), typ = type.getText().trim();
        if (loc.isEmpty() || typ.isEmpty() || description.getText().trim().isEmpty()) { info("Area, type and description are required."); return; }
        if (!CITY.adj.containsKey(loc)) { info("That area does not exist in the city graph. Ask municipal staff to add it."); return; }
        double incidentLat, incidentLng;
        try {
            incidentLat = Double.parseDouble(lat.getText().trim());
            incidentLng = Double.parseDouble(lng.getText().trim());
            if (incidentLat < -90 || incidentLat > 90 || incidentLng < -180 || incidentLng > 180) throw new NumberFormatException();
        } catch (NumberFormatException ex) { info("Enter valid incident latitude and longitude."); return; }
        EmergencyRequest req = new EmergencyRequest(UUID.randomUUID().toString(), currentUser, typ, loc,
                priority.getSelectedIndex(), "Pending", "None", "", description.getText().trim(),
                incidentLat, incidentLng, java.time.Instant.now().toString());
        Allocation allocation = CITY.allocateNearest(loc, typ);
        if (allocation != null) {
            req.resourceId = allocation.resource.id;
            req.status = "Assigned";
            req.route = String.join(" -> ", allocation.path) + " (" + allocation.distance + " km)";
            info("Allocated resource " + allocation.resource.id + "\nFrom: " + allocation.center +
                    "\nShortest route: " + req.route);
        } else {
            info("Request recorded as pending. No available matching resource could be reached.");
        }
        if (allocation != null) persistResources();
        REQUESTS_LIST.add(req);
        Storage.saveRequests();
        log("Request " + req.id.substring(0, 8) + ": " + typ + " at " + loc + " — " + req.status);
        graphPanel.repaint();
    }

    private static void markComplete() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        List<Resource> busy = CITY.allResources().stream().filter(r -> !r.available).toList();
        if (busy.isEmpty()) { info("There are no assigned resources to complete."); return; }
        String[] ids = busy.stream().map(r -> r.id + " — " + r.type).toArray(String[]::new);
        String selected = choose("Select completed resource:", ids);
        if (selected == null) return;
        String id = selected.split(" — ", 2)[0];
        Resource r = CITY.findResource(id);
        if (r == null) return;
        r.available = true;
        for (EmergencyRequest req : REQUESTS_LIST)
            if (req.resourceId.equals(id) && ("Assigned".equals(req.status) || "In Progress".equals(req.status))) req.status = "Completed";
        persistResources();
        Storage.saveRequests();
        log("Resource " + id + " marked available; associated assigned requests marked completed.");
        graphPanel.repaint();
    }

    private static void showAllRequests() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        StringBuilder sb = new StringBuilder("ALL EMERGENCY REQUESTS\n\n");
        if (REQUESTS_LIST.isEmpty()) sb.append("No requests recorded.\n");
        for (EmergencyRequest r : REQUESTS_LIST) sb.append(r.toDisplay()).append("\n");
        output.setText(sb.toString());
    }

    private static void showMyRequests() {
        if (!"citizen".equals(currentRole)) { info("Citizen account required."); return; }
        StringBuilder sb = new StringBuilder("MY EMERGENCY REQUESTS\n\n");
        boolean found = false;
        for (EmergencyRequest r : REQUESTS_LIST) if (r.requester.equals(currentUser)) {
            sb.append(r.toDisplay()).append("\n"); found = true;
        }
        if (!found) sb.append("You have not created any requests yet.\n");
        output.setText(sb.toString());
    }


    private static void manageTasks() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        List<EmergencyRequest> active = REQUESTS_LIST.stream()
                .filter(r -> !r.status.equals("Completed") && !r.status.equals("Cancelled")).toList();
        if (active.isEmpty()) { info("No active tasks."); return; }
        String[] choices = active.stream().map(r -> r.id.substring(0,8)+" | "+r.type+" | "+r.status+" | "+r.location).toArray(String[]::new);
        String selected = choose("Select a request to update:", choices);
        if (selected == null) return;
        int index = -1;
        for (int i=0;i<active.size();i++) if (choices[i].equals(selected)) { index=i; break; }
        if (index < 0) return;
        EmergencyRequest req = active.get(index);
        String[] statuses = {"Assigned", "In Progress", "Completed", "Cancelled"};
        String next = choose("Set request status:", statuses);
        if (next == null) return;
        if (next.equals("Completed") || next.equals("Cancelled")) {
            Resource resource = CITY.findResource(req.resourceId);
            if (resource != null) resource.available = true;
        }
        req.status = next;
        persistResources();
        Storage.saveRequests();
        log("Request " + req.id.substring(0,8) + " status changed to " + next);
        graphPanel.repaint();
    }


    private static void reassignResource() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        List<EmergencyRequest> active = REQUESTS_LIST.stream()
            .filter(r -> !r.status.equals("Completed") && !r.status.equals("Cancelled")).toList();
        if(active.isEmpty()){info("No active or pending requests.");return;}
        String[] options=active.stream().map(r->r.id.substring(0,8)+" | "+r.type+" | "+r.status+" | "+r.location).toArray(String[]::new);
        String selected=choose("Choose request to allocate/reassign:",options);
        if(selected==null)return;
        int index=Arrays.asList(options).indexOf(selected);
        if(index<0)return;
        EmergencyRequest req=active.get(index);
        Resource old=CITY.findResource(req.resourceId);
        if(old!=null)old.available=true;
        Allocation allocation=CITY.allocateNearest(req.location,req.type);
        if(allocation==null){
            if(old!=null)old.available=false;
            info("No alternative available matching resource can be routed to this request.");
            return;
        }
        req.resourceId=allocation.resource.id;
        req.status="Assigned";
        req.route=String.join(" -> ",allocation.path)+" ("+allocation.distance+" km)";
        persistResources(); Storage.saveRequests();
        log("Request "+req.id.substring(0,8)+" reassigned to "+req.resourceId);
    }

    private static void showAnalytics() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        long totalResources = CITY.allResources().size();
        long available = CITY.allResources().stream().filter(r -> r.available).count();
        long occupied = totalResources - available;
        long pending = REQUESTS_LIST.stream().filter(r -> r.status.equals("Pending")).count();
        long active = REQUESTS_LIST.stream().filter(r -> r.status.equals("Assigned") || r.status.equals("In Progress")).count();
        long completed = REQUESTS_LIST.stream().filter(r -> r.status.equals("Completed")).count();
        long cancelled = REQUESTS_LIST.stream().filter(r -> r.status.equals("Cancelled")).count();

        Map<String,Long> statuses = new LinkedHashMap<>();
        for(String k:List.of("Pending","Assigned","In Progress","Completed","Cancelled"))
            statuses.put(k,REQUESTS_LIST.stream().filter(r->r.status.equals(k)).count());
        Map<String,Long> priorities = new LinkedHashMap<>();
        for(String k:List.of("Critical","High","Medium","Low"))
            priorities.put(k,REQUESTS_LIST.stream().filter(r->priorityName(r.priority).equals(k)).count());
        Map<String,Long> utilization = new LinkedHashMap<>();
        utilization.put("Available",available); utilization.put("Occupied",occupied);
        Map<String,Long> types = new TreeMap<>();
        for(EmergencyRequest r:REQUESTS_LIST) types.merge(r.type,1L,Long::sum);

        StringBuilder report = new StringBuilder("OPERATIONS ANALYTICS\n\n");
        report.append("Areas: ").append(CITY.adj.size()).append("\nResource centers: ").append(CITY.resources.size())
              .append("\nResources: ").append(totalResources).append("\nAvailable: ").append(available)
              .append("\nOccupied: ").append(occupied).append("\n\nRequests: ").append(REQUESTS_LIST.size())
              .append("\nPending: ").append(pending).append("\nActive: ").append(active)
              .append("\nCompleted: ").append(completed).append("\nCancelled: ").append(cancelled);
        output.setText(report.toString());

        JFrame frame=new JFrame("Analytics & Reports — Live application data");
        frame.setSize(980,700); frame.setLocationRelativeTo(mainFrame);
        JPanel root=new JPanel(new BorderLayout(12,12)); root.setBackground(BG); root.setBorder(new EmptyBorder(16,16,16,16));
        JLabel title=new JLabel("Operations analytics");
        title.setFont(new Font("Segoe UI",Font.BOLD,21)); title.setForeground(TEXT);
        JLabel subtitle=new JLabel("Charts are calculated from persisted application records.");
        subtitle.setForeground(MUTED);
        JPanel header=new JPanel(new GridLayout(2,1,2,2)); header.setOpaque(false);header.add(title);header.add(subtitle);
        root.add(header,BorderLayout.NORTH);
        JPanel charts=new JPanel(new GridLayout(2,2,12,12));charts.setOpaque(false);
        charts.add(new BarChartPanel("Requests by status",statuses));
        charts.add(new BarChartPanel("Requests by priority",priorities));
        charts.add(new BarChartPanel("Resource utilization",utilization));
        charts.add(new BarChartPanel("Requests by emergency type",types));
        root.add(charts,BorderLayout.CENTER);
        frame.setContentPane(root);frame.setVisible(true);
    }
    static class BarChartPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        private final String title;
        private final transient Map<String,Long> values;
        BarChartPanel(String title,Map<String,Long> values) {
            this.title=title;this.values=new LinkedHashMap<>(values);
            setBackground(Color.WHITE);
            setBorder(new CompoundBorder(new LineBorder(new Color(229,233,241),1,true),new EmptyBorder(12,12,12,12)));
        }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(TEXT);g2.setFont(new Font("Segoe UI",Font.BOLD,14));g2.drawString(title,14,24);
            int max=1;for(long v:values.values())max=(int)Math.max(max,v);
            int x0=18, y0=45, chartW=Math.max(120,getWidth()-38), chartH=Math.max(80,getHeight()-92);
            int n=Math.max(1,values.size()), slot=chartW/n, barW=Math.max(12,slot-20);
            int i=0;
            Color[] palette={BLUE,TEAL,PEACH,new Color(143,132,190),new Color(116,157,183)};
            for(Map.Entry<String,Long> e:values.entrySet()) {
                int barH=(int)(chartH*(e.getValue()/(double)max));
                int x=x0+i*slot+(slot-barW)/2, y=y0+chartH-barH;
                g2.setColor(palette[i%palette.length]);g2.fillRoundRect(x,y,barW,barH,8,8);
                g2.setColor(TEXT);g2.setFont(new Font("Segoe UI",Font.BOLD,11));
                String val=String.valueOf(e.getValue());g2.drawString(val,x+(barW-g2.getFontMetrics().stringWidth(val))/2,y-5);
                g2.setFont(new Font("Segoe UI",Font.PLAIN,10));g2.setColor(MUTED);
                String label=e.getKey();FontMetrics fm=g2.getFontMetrics();
                if(fm.stringWidth(label)>slot-2 && label.length()>9)label=label.substring(0,8)+"…";
                g2.drawString(label,x0+i*slot+(slot-fm.stringWidth(label))/2,y0+chartH+18);
                i++;
            }
            g2.dispose();
        }
    }
    private static String priorityName(int priority) {
        String[] p={"Critical","High","Medium","Low"};
        return p[Math.max(0,Math.min(3,priority))];
    }

    private static void exportRequestsCsv() {
        if (!"municipal".equals(currentRole)) { info("Municipal access required."); return; }
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new java.io.File("emergency-requests.csv"));
        if (chooser.showSaveDialog(mainFrame) != JFileChooser.APPROVE_OPTION) return;
        try (java.io.PrintWriter writer = new java.io.PrintWriter(chooser.getSelectedFile(), StandardCharsets.UTF_8)) {
            writer.println("request_id,requester,type,description,location,latitude,longitude,priority,status,resource_id,created_at");
            for (EmergencyRequest r : REQUESTS_LIST) {
                writer.println(csv(r.id)+","+csv(r.requester)+","+csv(r.type)+","+csv(r.description)+","+csv(r.location)+","+
                    r.latitude+","+r.longitude+","+csv(priorityName(r.priority))+","+csv(r.status)+","+csv(r.resourceId)+","+csv(r.createdAt));
            }
            info("CSV report exported successfully.");
        } catch (Exception ex) { showError("CSV export failed", ex); }
    }
    private static String csv(String s) { return "\"" + (s==null?"":s.replace("\"","\"\"")) + "\""; }

    private static void openLiveMap() {
        try {
            if (!java.awt.Desktop.isDesktopSupported()) { info("Desktop browser launch is not supported on this machine."); return; }
            java.awt.Desktop.getDesktop().browse(new URI("http://127.0.0.1:8765/map"));
        } catch (Exception ex) { showError("Could not open live map", ex); }
    }

    public static String mapDataJson() {
        StringBuilder json = new StringBuilder("{\"areas\":[");
        boolean first=true;
        for (String area : sorted(CITY.adj.keySet())) {
            GeoPoint pt=CITY.coordinates.get(area);
            if (pt==null) continue;
            if(!first) json.append(",");
            first=false;
            json.append("{\"name\":").append(jsonString(area)).append(",\"lat\":").append(pt.lat)
                .append(",\"lng\":").append(pt.lng).append(",\"center\":").append(CITY.resources.containsKey(area)).append("}");
        }
        json.append("],\"roads\":[");
        first=true;
        for (Map.Entry<String, Map<String, Integer>> fromEntry : CITY.adj.entrySet()) {
            for (Map.Entry<String, Integer> road : fromEntry.getValue().entrySet()) {
                String from = fromEntry.getKey();
                String to = road.getKey();
                // Roads are bidirectional in CityModel; emit each edge only once.
                if (from.compareTo(to) >= 0) continue;
                if (!first) json.append(",");
                first=false;
                json.append("{\"from\":").append(jsonString(from))
                    .append(",\"to\":").append(jsonString(to))
                    .append(",\"distance\":").append(road.getValue()).append("}");
            }
        }
        json.append("],\"resources\":[");
        first=true;
        for (Map.Entry<String,List<Resource>> entry : CITY.resources.entrySet()) {
            GeoPoint pt=CITY.coordinates.get(entry.getKey());
            if(pt==null) continue;
            for(Resource r:entry.getValue()) {
                if(!first) json.append(",");
                first=false;
                json.append("{\"id\":").append(jsonString(r.id)).append(",\"type\":").append(jsonString(r.type))
                    .append(",\"area\":").append(jsonString(entry.getKey())).append(",\"available\":").append(r.available)
                    .append(",\"lat\":").append(pt.lat).append(",\"lng\":").append(pt.lng).append("}");
            }
        }
        json.append("],\"requests\":[");
        first=true;
        for(EmergencyRequest r:REQUESTS_LIST) {
            if(!"municipal".equals(currentRole) && !r.requester.equals(currentUser)) continue;
            if(!first) json.append(",");
            first=false;
            json.append("{\"id\":").append(jsonString(r.id.substring(0,Math.min(8,r.id.length()))))
                .append(",\"type\":").append(jsonString(r.type)).append(",\"status\":").append(jsonString(r.status))
                .append(",\"priority\":").append(jsonString(priorityName(r.priority)))
                .append(",\"location\":").append(jsonString(r.location)).append(",\"lat\":").append(r.latitude)
                .append(",\"lng\":").append(r.longitude).append("}");
        }
        return json.append("]}").toString();
    }
    public static String mapRouteJson(String area, String type) {
        Allocation route = CITY.previewNearest(area, type);
        if (route == null) return "{\"found\":false}";
        StringBuilder json = new StringBuilder("{\"found\":true,\"resourceId\":")
                .append(jsonString(route.resource.id))
                .append(",\"center\":").append(jsonString(route.center))
                .append(",\"distance\":").append(route.distance)
                .append(",\"path\":[");
        for (int i = 0; i < route.path.size(); i++) {
            if (i > 0) json.append(",");
            json.append(jsonString(route.path.get(i)));
        }
        return json.append("]}").toString();
    }
    private static String jsonString(String value) {
        StringBuilder b=new StringBuilder("\"");
        for(char c:value.toCharArray()) {
            switch(c) {
                case '"' -> b.append("\\\"");
                case '\\' -> b.append("\\\\");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if(c<32) b.append(String.format("\\u%04x",(int)c)); else b.append(c); }
            }
        }
        return b.append("\"").toString();
    }

    private static void persistCity() {
        try { Storage.saveAreasAndRoads(); } catch (Exception ex) { showError("Could not save city graph", ex); }
        if (graphPanel != null) graphPanel.repaint();
    }
    private static void persistResources() {
        try { Storage.saveResources(); } catch (Exception ex) { showError("Could not save resources", ex); }
        if (graphPanel != null) graphPanel.repaint();
    }
    private static void log(String message) {
        Storage.logActivity(currentUser,message);
        if (output != null) output.append(message + "\n");
        if (statusLabel != null) statusLabel.setText(message);
    }

    private static JPanel formPanel(String[] labels, JComponent[] fields) {
        JPanel p = new JPanel(new GridLayout(labels.length, 2, 8, 8));
        for (int i=0;i<labels.length;i++) { p.add(new JLabel(labels[i] + ":")); p.add(fields[i]); }
        return p;
    }
    private static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(PANEL);
        p.setBorder(new CompoundBorder(new LineBorder(new Color(229,233,241), 1, true), new EmptyBorder(12,12,12,12)));
        return p;
    }
    private static JLabel label(String s) {
        JLabel l = new JLabel(s); l.setForeground(TEXT); l.setFont(new Font("Segoe UI", Font.PLAIN, 14)); return l;
    }
    private static JButton button(String s, Color color) {
        JButton b = new JButton(s);
        b.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        b.setForeground(Color.WHITE); b.setBackground(color); b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 12, 9, 12)); b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
    private static String ask(String prompt) {
        return JOptionPane.showInputDialog(mainFrame, prompt);
    }
    private static String choose(String prompt, String[] values) {
        if (values.length == 0) return null;
        return (String) JOptionPane.showInputDialog(mainFrame, prompt, "Select",
                JOptionPane.QUESTION_MESSAGE, null, values, values[0]);
    }
    private static void info(String s) { JOptionPane.showMessageDialog(mainFrame, s); }
    private static void showError(String title, Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(mainFrame, title + ":\n" + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
    private static List<String> sorted(Collection<String> values) {
        List<String> result = new ArrayList<>(values); Collections.sort(result); return result;
    }

    static class Resource {
        String type, id, driver;
        boolean available;
        Resource(String type, String id, String driver, boolean available) {
            this.type=type; this.id=id; this.driver=driver; this.available=available;
        }
    }
    static class EmergencyRequest {
        String id, requester, type, location, status, resourceId, route, description, createdAt;
        double latitude, longitude;
        int priority;
        EmergencyRequest(String id, String requester, String type, String location, int priority,
                         String status, String resourceId, String route) {
            this(id, requester, type, location, priority, status, resourceId, route, "", 0, 0,
                    java.time.Instant.now().toString());
        }
        EmergencyRequest(String id, String requester, String type, String location, int priority,
                         String status, String resourceId, String route, String description,
                         double latitude, double longitude, String createdAt) {
            this.id=id; this.requester=requester; this.type=type; this.location=location;
            this.priority=priority; this.status=status; this.resourceId=resourceId; this.route=route;
            this.description=description; this.latitude=latitude; this.longitude=longitude; this.createdAt=createdAt;
        }
        String toDisplay() {
            String[] p = {"Critical", "High", "Medium", "Low"};
            return "Request " + id.substring(0, Math.min(8,id.length())) + " | " + type + " | " + location +
                    " | Priority: " + p[Math.max(0,Math.min(3,priority))] + " | " + status +
                    " | Resource: " + resourceId + (route.isBlank() ? "" : " | Route: " + route) +
                    " | By: " + requester + " | Created: " + createdAt +
                    (description.isBlank() ? "" : " | " + description);
        }
    }
    static class GeoPoint {
        double lat, lng;
        GeoPoint(double lat, double lng) { this.lat=lat; this.lng=lng; }
    }
    static class Allocation {
        Resource resource; String center; int distance; List<String> path;
        Allocation(Resource r, String c, int d, List<String> p) { resource=r; center=c; distance=d; path=p; }
    }
    static class CityModel {
        Map<String, Map<String,Integer>> adj = new LinkedHashMap<>();
        Map<String, List<Resource>> resources = new LinkedHashMap<>();
        Map<String, GeoPoint> coordinates = new LinkedHashMap<>();
        void addArea(String a) { adj.putIfAbsent(a, new LinkedHashMap<>()); }
        void addRoad(String a,String b,int d) { adj.get(a).put(b,d); adj.get(b).put(a,d); }
        List<Resource> allResources() {
            List<Resource> all = new ArrayList<>(); resources.values().forEach(all::addAll); return all;
        }
        Resource findResource(String id) {
            for (Resource r: allResources()) if (r.id.equals(id)) return r;
            return null;
        }
        Allocation allocateNearest(String destination, String type) {
            Allocation best = findNearest(destination, type);
            if (best != null) best.resource.available = false;
            return best;
        }
        // Non-mutating preview used by the live map; it runs the same Dijkstra logic as allocation.
        Allocation previewNearest(String destination, String type) {
            return findNearest(destination, type);
        }
        private Allocation findNearest(String destination, String type) {
            if(!adj.containsKey(destination)) return null;
            Map<String,Integer> dist = new HashMap<>();
            Map<String,String> parent = new HashMap<>();
            PriorityQueue<NodeDistance> pq = new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));
            for (String node: adj.keySet()) dist.put(node, Integer.MAX_VALUE);
            dist.put(destination,0); pq.add(new NodeDistance(destination,0));
            while(!pq.isEmpty()) {
                NodeDistance current=pq.poll();
                if(current.distance!=dist.get(current.node)) continue;
                for(Map.Entry<String,Integer> edge: adj.get(current.node).entrySet()) {
                    if(edge.getValue()<=0 || current.distance > Integer.MAX_VALUE-edge.getValue()) continue;
                    int next=current.distance+edge.getValue();
                    if(next < dist.getOrDefault(edge.getKey(),Integer.MAX_VALUE)) {
                        dist.put(edge.getKey(),next); parent.put(edge.getKey(),current.node);
                        pq.add(new NodeDistance(edge.getKey(),next));
                    }
                }
            }
            Allocation best=null;
            for(Map.Entry<String,List<Resource>> entry: resources.entrySet()) {
                int d=dist.getOrDefault(entry.getKey(),Integer.MAX_VALUE);
                if(d==Integer.MAX_VALUE) continue;
                for(Resource r:entry.getValue()) if(r.available && r.type.equalsIgnoreCase(type)) {
                    if(best==null || d<best.distance) {
                        List<String> path=new ArrayList<>();
                        String at=entry.getKey(); path.add(at);
                        while(!at.equals(destination) && parent.containsKey(at)) {
                            at=parent.get(at); path.add(at);
                        }
                        best=new Allocation(r,entry.getKey(),d,path);
                    }
                }
            }
            return best;
        }
    }
    static class NodeDistance {
        String node; int distance;
        NodeDistance(String node,int distance){this.node=node;this.distance=distance;}
    }

    static class Storage {
        static void initialize() throws IOException {
            Files.createDirectories(DATA);
            try { JdbcStore.initializeIfAvailable(); }
            catch(Exception ex) { throw new IOException("Configured database initialization failed.",ex); }
            if(!JdbcStore.isActive()) {
                for(Path p: List.of(ACCOUNTS,AREAS,ROADS,RESOURCES,REQUESTS,ACTIVITY))
                    if(Files.notExists(p)) Files.createFile(p);
            }
        }
        static void loadAll() throws IOException {
            if(JdbcStore.isActive()) {
                try {
                    JdbcStore.loadAccounts(); JdbcStore.loadCity(); JdbcStore.loadResources(); JdbcStore.loadRequests();
                } catch(Exception ex) { throw new IOException("Could not load application data from database.",ex); }
            } else {
                loadAccounts(); loadCity(); loadResources(); loadRequests();
            }
            bootstrapAdminIfConfigured();
        }
        static void bootstrapAdminIfConfigured() throws IOException {
            boolean hasMunicipal=ACCOUNTS_MAP.values().stream().anyMatch(a -> "municipal".equals(a.role));
            String adminId=System.getenv("INITIAL_ADMIN_USER");
            String adminPassword=System.getenv("INITIAL_ADMIN_PASSWORD");
            if(!hasMunicipal && adminId!=null && !adminId.isBlank() && adminPassword!=null && adminPassword.length()>=12) {
                try {
                    ACCOUNTS_MAP.put(adminId, Passwords.create(adminId,adminPassword.toCharArray(),"municipal"));
                    saveAccounts();
                    logActivity(adminId,"INITIAL_MUNICIPAL_ADMIN_CREATED");
                } catch(Exception ex) { throw new IOException("Could not bootstrap initial municipal administrator.",ex); }
            }
        }
        static void logActivity(String actor,String action) {
            if(JdbcStore.isActive()) { JdbcStore.logActivity(actor,action); return; }
            try {
                String safeActor=(actor==null?"system":actor).replace("|","/");
                String safeAction=(action==null?"":action).replace("|","/");
                Files.writeString(ACTIVITY,java.time.Instant.now()+" | "+safeActor+" | "+safeAction+System.lineSeparator(),
                        StandardCharsets.UTF_8,StandardOpenOption.CREATE,StandardOpenOption.APPEND);
            } catch(IOException ex) { System.err.println("Activity log write failed: "+ex.getMessage()); }
        }
        static void loadAccounts() throws IOException {
            if(JdbcStore.isActive()) { try { JdbcStore.loadAccounts(); return; } catch(Exception ex) { throw new IOException(ex); } }
            ACCOUNTS_MAP.clear();
            for(String line: Files.readAllLines(ACCOUNTS,StandardCharsets.UTF_8)) {
                String[] x=line.split("\\|",-1);
                if(x.length==4) ACCOUNTS_MAP.put(unesc(x[0]),new Account(unesc(x[1]),unesc(x[2]),unesc(x[3])));
            }
        }
        static void saveAccounts() throws IOException {
            if(JdbcStore.isActive()) { try { JdbcStore.saveAccounts(); return; } catch(Exception ex) { throw new IOException(ex); } }
            List<String> lines=new ArrayList<>();
            ACCOUNTS_MAP.forEach((id,a)->lines.add(esc(id)+"|"+esc(a.salt)+"|"+esc(a.passwordHash)+"|"+esc(a.role)));
            write(ACCOUNTS,lines);
        }
        static void loadCity() throws IOException {
            if(JdbcStore.isActive()) { try { JdbcStore.loadCity(); return; } catch(Exception ex) { throw new IOException(ex); } }
            CITY.adj.clear();
            for(String line: Files.readAllLines(AREAS,StandardCharsets.UTF_8)) {
                if(line.isBlank()) continue;
                String[] parts=line.split("\\|",-1);
                String area=unesc(parts[0]);
                CITY.addArea(area);
                if(parts.length==3) {
                    try { CITY.coordinates.put(area,new GeoPoint(Double.parseDouble(parts[1]),Double.parseDouble(parts[2]))); }
                    catch(NumberFormatException ignored) { /* Legacy/invalid coordinates are omitted from map. */ }
                }
            }
            for(String line: Files.readAllLines(ROADS,StandardCharsets.UTF_8)) {
                String[] x=line.split("\\|",-1);
                if(x.length==3) {
                    String a=unesc(x[0]), b=unesc(x[1]);
                    int d=Integer.parseInt(x[2]);
                    if(CITY.adj.containsKey(a)&&CITY.adj.containsKey(b)&&d>0) CITY.addRoad(a,b,d);
                }
            }
        }
        static void saveAreasAndRoads() throws IOException {
            if(JdbcStore.isActive()) { try { JdbcStore.saveCity(); return; } catch(Exception ex) { throw new IOException(ex); } }
            List<String> areas=new ArrayList<>();
            for(String a:sorted(CITY.adj.keySet())) {
                GeoPoint point=CITY.coordinates.get(a);
                areas.add(point==null ? esc(a) : esc(a)+"|"+point.lat+"|"+point.lng);
            }
            write(AREAS,areas);
            List<String> roads=new ArrayList<>();
            for(String a:CITY.adj.keySet()) for(Map.Entry<String,Integer> e:CITY.adj.get(a).entrySet())
                if(a.compareTo(e.getKey())<0) roads.add(esc(a)+"|"+esc(e.getKey())+"|"+e.getValue());
            write(ROADS,roads);
        }
        static void loadResources() throws IOException {
            if(JdbcStore.isActive()) { try { JdbcStore.loadResources(); return; } catch(Exception ex) { throw new IOException(ex); } }
            CITY.resources.clear();
            for(String line: Files.readAllLines(RESOURCES,StandardCharsets.UTF_8)) {
                String[] x=line.split("\\|",-1);
                if(x.length==5) {
                    String area=unesc(x[0]);
                    if(CITY.adj.containsKey(area)) {
                        CITY.resources.putIfAbsent(area,new ArrayList<>());
                        CITY.resources.get(area).add(new Resource(unesc(x[1]),unesc(x[2]),unesc(x[3]),Boolean.parseBoolean(x[4])));
                    }
                } else if(x.length==1 && !line.isBlank()) {
                    String area=unesc(line);
                    if(CITY.adj.containsKey(area)) CITY.resources.putIfAbsent(area,new ArrayList<>());
                }
            }
        }
        static void saveResources() throws IOException {
            if(JdbcStore.isActive()) { try { JdbcStore.saveResources(); return; } catch(Exception ex) { throw new IOException(ex); } }
            List<String> lines=new ArrayList<>();
            for(String area:sorted(CITY.resources.keySet())) {
                List<Resource> rs=CITY.resources.get(area);
                if(rs.isEmpty()) lines.add(esc(area));
                for(Resource r:rs) lines.add(esc(area)+"|"+esc(r.type)+"|"+esc(r.id)+"|"+esc(r.driver)+"|"+r.available);
            }
            write(RESOURCES,lines);
        }
        static void loadRequests() throws IOException {
            if(JdbcStore.isActive()) { try { JdbcStore.loadRequests(); return; } catch(Exception ex) { throw new IOException(ex); } }
            REQUESTS_LIST.clear();
            for(String line:Files.readAllLines(REQUESTS,StandardCharsets.UTF_8)) {
                String[] x=line.split("\\|",-1);
                if(x.length==8) REQUESTS_LIST.add(new EmergencyRequest(unesc(x[0]),unesc(x[1]),unesc(x[2]),
                    unesc(x[3]),Integer.parseInt(x[4]),unesc(x[5]),unesc(x[6]),unesc(x[7])));
                else if(x.length==12) REQUESTS_LIST.add(new EmergencyRequest(unesc(x[0]),unesc(x[1]),unesc(x[2]),
                    unesc(x[3]),Integer.parseInt(x[4]),unesc(x[5]),unesc(x[6]),unesc(x[7]),unesc(x[8]),
                    Double.parseDouble(x[9]),Double.parseDouble(x[10]),unesc(x[11])));
            }
        }
        static void saveRequests() {
            if(JdbcStore.isActive()) { try { JdbcStore.saveRequests(); return; } catch(Exception ex) { throw new RuntimeException(ex); } }
            try {
                List<String> lines=new ArrayList<>();
                for(EmergencyRequest r:REQUESTS_LIST)
                    lines.add(esc(r.id)+"|"+esc(r.requester)+"|"+esc(r.type)+"|"+esc(r.location)+"|"+
                        r.priority+"|"+esc(r.status)+"|"+esc(r.resourceId)+"|"+esc(r.route)+"|"+esc(r.description)+"|"+
                        r.latitude+"|"+r.longitude+"|"+esc(r.createdAt));
                write(REQUESTS,lines);
            } catch(IOException ex) { throw new RuntimeException(ex); }
        }
        static void write(Path path,List<String> lines) throws IOException {
            Path temp=path.resolveSibling(path.getFileName()+".tmp");
            Files.write(temp,lines,StandardCharsets.UTF_8);
            try { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
            catch(AtomicMoveNotSupportedException ex) { Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING); }
        }
        static String esc(String s) { return s.replace("\\","\\\\").replace("|","\\p").replace("\n","\\n").replace("\r",""); }
        static String unesc(String s) {
            StringBuilder b=new StringBuilder(); boolean slash=false;
            for(char ch:s.toCharArray()) {
                if(slash) { b.append(ch=='n'?'\n':ch=='p'?'|':ch); slash=false; }
                else if(ch=='\\') slash=true; else b.append(ch);
            }
            if(slash)b.append('\\'); return b.toString();
        }
    }
    static class Account {
        String salt,passwordHash,role;
        Account(String salt,String hash,String role) { this.salt=salt; passwordHash=hash; this.role=role; }
    }
    static class Passwords {
        static Account create(String id,char[] password,String role) throws Exception {
            byte[] salt=new byte[16]; SecureRandom.getInstanceStrong().nextBytes(salt);
            String saltText=Base64.getEncoder().encodeToString(salt);
            return new Account(saltText,hash(password,salt),role);
        }
        static boolean verify(char[] password,String saltText,String expected) {
            try {
                byte[] salt=Base64.getDecoder().decode(saltText);
                return MessageDigest.isEqual(hash(password,salt).getBytes(StandardCharsets.UTF_8),
                                             expected.getBytes(StandardCharsets.UTF_8));
            } catch(Exception ex) { return false; }
        }
        static String hash(char[] password,byte[] salt) throws Exception {
            PBEKeySpec spec=new PBEKeySpec(password,salt,120000,256);
            try {
                byte[] result=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
                return Base64.getEncoder().encodeToString(result);
            } finally { spec.clearPassword(); }
        }
    }
    static class GraphPanel extends JPanel {
        private static final long serialVersionUID = 1L;
        transient CityModel city;
        GraphPanel(CityModel c) { city=c; setBackground(new Color(250,251,254)); }
        @Override public Dimension getPreferredSize() { return new Dimension(650,270); }
        @Override protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2=(Graphics2D)g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            List<String> nodes=sorted(city.adj.keySet()); int n=nodes.size();
            if(n==0) {
                g2.setColor(MUTED); g2.drawString("City graph will appear here after areas are added.",20,35);
                g2.dispose(); return;
            }
            int w=getWidth(),h=getHeight(),radius=Math.max(35,Math.min(w,h)/3),cx=w/2,cy=h/2;
            Map<String,Point> pts=new HashMap<>();
            for(int i=0;i<n;i++) {
                double angle=2*Math.PI*i/n-Math.PI/2;
                pts.put(nodes.get(i),new Point(cx+(int)(radius*Math.cos(angle)),cy+(int)(radius*.72*Math.sin(angle))));
            }
            g2.setStroke(new BasicStroke(2f));
            for(String a:nodes) for(Map.Entry<String,Integer> e:city.adj.get(a).entrySet()) if(a.compareTo(e.getKey())<0) {
                Point p=pts.get(a),q=pts.get(e.getKey()); if(p==null||q==null)continue;
                g2.setColor(new Color(180,190,207));g2.drawLine(p.x,p.y,q.x,q.y);
                g2.setColor(MUTED);g2.setFont(new Font("Segoe UI",Font.PLAIN,10));
                g2.drawString(e.getValue()+" km",(p.x+q.x)/2+3,(p.y+q.y)/2-3);
            }
            for(String a:nodes) {
                Point p=pts.get(a); g2.setColor(new Color(218,231,250));g2.fillOval(p.x-18,p.y-18,36,36);
                g2.setColor(BLUE);g2.drawOval(p.x-18,p.y-18,36,36);
                g2.setColor(TEXT);g2.setFont(new Font("Segoe UI",Font.BOLD,10));
                FontMetrics fm=g2.getFontMetrics();g2.drawString(a,p.x-fm.stringWidth(a)/2,p.y+30);
            }
            g2.dispose();
        }
    }
}
