/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package common;

import editor.Main;
import editor.domain.ProjectResource;
import editor.domain.ResourceHolder;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import static java.awt.Frame.MAXIMIZED_BOTH;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.prefs.Preferences;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.GrayFilter;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JToolBar;
import javax.swing.MenuElement;
import javax.swing.UIManager;

/**
 *
 * @author Elvio
 */
public final class Util {
    
    // Loading images from the application jar
    public static Image loadImage(String imageName) {
        Image image = null;
        try {
            image = ImageIO.read(Main.class.getResource(imageName));
        }
        catch (IOException | IllegalArgumentException ex) {
            System.out.println("Unable to load image: " + imageName);
            Main.logException(ex, true);
        }
        return image;
    }
    public static ImageIcon loadIcon(String iconName) {
        return new ImageIcon(loadImage(iconName));
    }

    // Loading images from the application jar
    public static Image tryLoadImageFromFile(String imageName) {
        try {
            return ImageIO.read(new File(imageName));
        }
        catch (IOException | IllegalArgumentException ex) {
            return null;
        }
    }
    
    
    // Load a string from a text file inside the application Jar
    public static String loadTextDoc(String fileName, String defaultText) {
        String text;
        try {
            URL url = Main.class.getResource(fileName);
            Reader reader = new InputStreamReader(url.openStream());
            final char[] buffer = new char[1024];
            final StringBuilder out = new StringBuilder();
            for (;;) {
                int rsz = reader.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
            text = out.toString();
        }
        catch (IOException e) {
            text = defaultText;            
        }
        return text;
    }
    
//    public static void main(String[] args) throws Exception {
    // combine two images into a single one
//        ImageIcon icon1 = new ImageIcon(ImageIO.read(new File("/home/elvio/GreatSPN/SOURCES/JavaGUI/Editor/src/editor/gui/icons/page_net16.png")));
//        ImageIcon icon2 = new ImageIcon(ImageIO.read(new File("/home/elvio/GreatSPN/SOURCES/JavaGUI/Editor/src/editor/gui/icons/overlay_plus16.png")));
//        
//        ImageIcon icon3 = new ImageIcon(icon1.getImage());
//        Graphics2D g2 = (Graphics2D)icon3.getImage().getGraphics();
//        g2.drawImage(icon2.getImage(), 0, 0, null);
//        g2.dispose();
//        
//        Image img = icon3.getImage();
//        BufferedImage bi = new BufferedImage(img.getWidth(null), img.getHeight(null), BufferedImage.TYPE_INT_ARGB);
//        Graphics2D g3 = bi.createGraphics();
//        g3.drawImage(img, 0, 0, null);
//        g3.dispose();
//        ImageIO.write(bi, "png", new File("/home/elvio/test.png"));
//    }

    
    // Prepara l'avvio dell'applicazione
    public static void initApplication(String appName, String preferencesRootNode) {
        prefRootNode = preferencesRootNode;
        if (isOSX()) {
            System.setProperty("apple.laf.useScreenMenuBar","true");
            System.setProperty("com.apple.macos.useScreenMenuBar","true");
//            System.setProperty("com.apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
//            System.setProperty("com.apple.mrj.application.apple.menu.about.name", appName);
//            System.setProperty("apple.awt.brushMetalLook","true");
        }
        
        if (isWindows()) {
            // Correzione Known BUG del jdk per Windows -> causa il flickering delle finestre
            System.setProperty("sun.awt.noerasebackground", "true");
        }
        try {
            String laf = System.getProperty("swing.defaultlaf");
            if (laf == null)
                laf = UIManager.getSystemLookAndFeelClassName();
            Main.UiSize uiSize = Main.getUiSize();
            if (uiSize.getScaleMultiplier() > 1.0f)
                setDefaultUIFontSize(uiSize.getScaleMultiplier());
//            switch (Main.getUiSize()) {
//                case LARGE:
//                    setDefaultUIFontSize(1.25f);
//                    break;
//                case LARGER:
//                    setDefaultUIFontSize(1.5f);
//                    break;
//            }
            UIManager.setLookAndFeel(laf);
        } catch (Exception ex) { }
        
        if (isLinuxGTK()) {
            UIManager.put("Slider.paintValue", Boolean.FALSE);
        }
    }
    
    // Preferences
    static String prefRootNode;
    public static Preferences getPreferences() {
        assert prefRootNode != null;
        Preferences root = Preferences.userRoot();
        return root.node(prefRootNode);
    }
    
    // save/load JFrame positions between sessions
    public static void loadFramePosition(JFrame frame, String propName) {
        Preferences prefs = getPreferences();
        frame.setBounds(prefs.getInt(propName+"frame-x", 200),
                        prefs.getInt(propName+"frame-y", 100),
                        prefs.getInt(propName+"frame-width", 800),
                        prefs.getInt(propName+"frame-height", 500));
        frame.setExtendedState(prefs.getInt(propName+"frame-extstate", 
                                            frame.getExtendedState() & MAXIMIZED_BOTH));
    }
    public static void saveFramePosition(JFrame frame, String propName) {
        Preferences prefs = getPreferences();
        boolean isMaximized = (frame.getExtendedState() & MAXIMIZED_BOTH) == MAXIMIZED_BOTH;
        if (!isMaximized) {
            prefs.putInt(propName+"frame-x", frame.getBounds().x);
            prefs.putInt(propName+"frame-y", frame.getBounds().y);
            prefs.putInt(propName+"frame-width", frame.getBounds().width);
            prefs.putInt(propName+"frame-height", frame.getBounds().height);
        }
        prefs.putInt(propName+"frame-extstate", frame.getExtendedState() & MAXIMIZED_BOTH);
    }

    // Integrazione con OSX
    private static Boolean platformOSX = null;
    public static boolean isOSX() { 
        if (platformOSX == null)
            platformOSX = System.getProperty("os.name").equals("Mac OS X"); 
        return platformOSX;
    }
    
    
    public static final Color UNIFIED_GRAY_PANEL_BKGND = new Color(237, 237, 237);
    private static final boolean unifiedToolbar = false;
    public static boolean useUnifiedToolbar() {
        return unifiedToolbar && isOSX();
    }
    
    // Remove excessive layout spacing on OSX.
    public static void reformatPanelsForPlatformLookAndFeel(Container panel) {
        if (Util.useUnifiedToolbar()) {
            panel.setBackground(UNIFIED_GRAY_PANEL_BKGND);
//            ((javax.swing.JComponent)panel).setOpaque(false);   
        }
        GridBagLayout gbl = null;
        if (panel.getLayout() instanceof GridBagLayout)
            gbl = (GridBagLayout)panel.getLayout();
        for (int n=0; n<panel.getComponentCount(); n++) {
            Component c = panel.getComponent(n);
            if (gbl != null && isOSX()) {
                GridBagConstraints gbc = gbl.getConstraints(c);
                gbc.insets.bottom /= 3;
                gbc.insets.top /= 3;
                gbc.insets.left /= 3;
                gbc.insets.right /= 3;
                gbl.setConstraints(c, gbc);
            }
            if (gbl != null && (c instanceof JComboBox) && isLinuxGTK()) {
                GridBagConstraints gbc = gbl.getConstraints(c);
                gbc.ipadx = 8;
                gbc.ipady = 8;
                gbl.setConstraints(c, gbc);
//                ((JLabel)((JComboBox)c).getRenderer()).setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
            }
            // Descend recursively
            boolean descend = false; // = (c instanceof Container);
            descend = descend || (c instanceof JPanel);
            descend = descend || (c instanceof JSplitPane);
            descend = descend || (c instanceof JToolBar);
            descend = descend || (c instanceof JTabbedPane);
            if (descend)
                reformatPanelsForPlatformLookAndFeel((Container)c);
        }
    }
    
    // Reformat menu panels
    public static void reformatMenuPanels(Component c) {
        if (c instanceof JMenuBar) {
            JMenuBar mbar = (JMenuBar)c;
            for (int n=0; n<mbar.getMenuCount(); n++) {
                reformatMenuPanels(mbar.getMenu(n));
            }
        }
        else if (c instanceof JPopupMenu) {
            JPopupMenu popop = (JPopupMenu)c;
            if (isLinuxGTK()) {
                popop.setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
            }
            for (MenuElement elem : popop.getSubElements()) {
                reformatMenuPanels(elem.getComponent());
            }
        }
        else if (c instanceof JMenu) {
            JMenu menu = (JMenu)c;
            if (isLinuxGTK()) {
                menu.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
                menu.getPopupMenu().setBorder(BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY));
            }
            for (int n=0; n<menu.getMenuComponentCount(); n++) {
                reformatMenuPanels(menu.getMenuComponent(n));
            }
        }
    }
    
    public static float UIscaleFactor = 1.0f;
    public static float origDefaultUiFontSize = 16;
    private static void setDefaultUIFontSize(float scaleFactor) {
        Set<Object> keySet = UIManager.getLookAndFeelDefaults().keySet();
        Object[] keys = keySet.toArray(new Object[keySet.size()]);
        
        for (Object key : keys) {

            if (key != null && key.toString().toLowerCase().contains("font")) {
//                System.out.println(key+" is now "+UIManager.get(key));
                Font font = UIManager.getDefaults().getFont(key);
                if (font != null) {
                    font = font.deriveFont((float)font.getSize() * scaleFactor);
                    UIManager.put(key, font);
                    // Take label font size as standard font size
                    if (key.toString().toLowerCase().equals("label.font"))
                        origDefaultUiFontSize = font.getSize();
                }
            }
//            if (key != null && key.toString().contains("UI")) {
//                System.out.println(key+"  =  "+UIManager.get(key));
//            }
        }
        if (isOSX()) {
            Color bg = (Color) UIManager.get("ComboBox.background");
            Color fg = (Color) UIManager.get("ComboBox.foreground");
            UIManager.put("ComboBoxUI", "javax.swing.plaf.metal.MetalComboBoxUI");
            UIManager.put("ComboBox.selectionBackground", bg);
            UIManager.put("ComboBox.selectionForeground", fg);
            UIManager.put("ComboBox.background", Color.WHITE);
            UIManager.put("ComboBox.foreground", fg);
        }
        UIscaleFactor = scaleFactor;
    }

    
    // Make a frame movable by dragging one of its panels
    public static void makeFrameDraggableByPanel(final JFrame frame, final JPanel panel) {
        final String START_COORDS = "mouseDownStartCoords";
        final String FRAME_COORDS = "mouseDownFrameCoords";
        panel.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                frame.getRootPane().putClientProperty(START_COORDS, null);
                frame.getRootPane().putClientProperty(FRAME_COORDS, null);
            }
            @Override public void mousePressed(MouseEvent e) {
                frame.getRootPane().putClientProperty(START_COORDS, e.getLocationOnScreen());
                frame.getRootPane().putClientProperty(FRAME_COORDS, frame.getLocation());
            }
        });
        panel.addMouseMotionListener(new MouseMotionAdapter(){
            @Override public void mouseDragged(MouseEvent e) {
                Point currCoords = e.getLocationOnScreen();
                Point startScreenCoords = (Point)frame.getRootPane().getClientProperty(START_COORDS);
                Point startFrameLoc = (Point)frame.getRootPane().getClientProperty(FRAME_COORDS);
                if (startScreenCoords == null)
                    return; // Possible ??
                int x = startFrameLoc.x + (currCoords.x - startScreenCoords.x);
                int y = startFrameLoc.y + (currCoords.y - startScreenCoords.y);
                frame.setLocation(x, y);
            }
        });
    }
    
    
    private static Boolean platformWindows = null;
    public static boolean isWindows() { 
        if (platformWindows == null)
            platformWindows = System.getProperty("os.name").toUpperCase().contains("WIN"); 
        return platformWindows;
    }
    
    private static Boolean platformLinux = null;
    public static boolean isLinux() { 
        if (platformLinux == null)
            platformLinux = System.getProperty("os.name").toUpperCase().contains("NUX"); 
        return platformLinux;
    }
    
    public static boolean isLinuxGTK() { 
        return isLinux() && UIManager.getLookAndFeel() != null &&
                UIManager.getLookAndFeel().getName().contains("GTK");
    }
    
    // Darken color
    public static Color darken(Color clr, float coeff) {
        return new Color((int)(clr.getRed() * coeff),
                         (int)(clr.getGreen() * coeff),
                         (int)(clr.getBlue() * coeff));
    }
    
    // Lighten color
    public static Color lighten(Color clr, float coeff) {
        coeff = 1.0f - coeff;
        return new Color(255 - (int)((255 - clr.getRed()) * coeff),
                         255 - (int)((255 - clr.getGreen()) * coeff),
                         255 - (int)((255 - clr.getBlue()) * coeff));
    }    
    // Gray color
    public static Color grayscale(Color clr) {
        int grayscale = clr.getRed() * 21 + clr.getGreen() * 71 + clr.getBlue() * 8;
        grayscale /= 256;
        return new Color(grayscale, grayscale, grayscale);
    }
    
    // Gray color, lighter
    public static Color lighterGrayscale(Color clr, float coeff) {
        int grayscale = clr.getRed() * 21 + clr.getGreen() * 71 + clr.getBlue() * 8;
        grayscale /= 256;
        grayscale = 255 - (int)((255 - grayscale) * coeff);
        return new Color(grayscale, grayscale, grayscale);
    }
    // mix colors
    public static Color mix(Color clr1, Color clr2, float coeff) {
        return new Color((int)(clr1.getRed() * coeff   + clr2.getRed() * (1-coeff)),
                         (int)(clr1.getGreen() * coeff + clr2.getGreen() * (1-coeff)),
                         (int)(clr1.getBlue() * coeff  + clr2.getBlue() * (1-coeff)));
    }
    // convert color in html hex form #RRGGBB
    public static String clrToHex(Color clr) {
        return String.format("#%06x", clr.getRGB() & 0xFFFFFF);
    }
    
    // Deep copy of objects through serialization
    public static Object deepCopy(Object obj) {
        if (obj == null)
            return null;
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(obj);
            oos.flush();
            oos.close();
            bos.close();
            byte[] byteData = bos.toByteArray();
            ByteArrayInputStream bais = new ByteArrayInputStream(byteData);
            Object newData = (Object) new ObjectInputStream(bais).readObject();
            return newData;
        }
        catch (Exception e) { Main.logException(e, true); return null; }
    }
    
    // Deep copy of object, with relink of the transient resource pointers
    public static ResourceHolder deepCopyRelink(ResourceHolder obj) {
        Map<UUID, ProjectResource> resources = new HashMap<>();
        obj.retrieveLinkedResources(resources);
        ResourceHolder newObj = (ResourceHolder)Util.deepCopy(obj);
        newObj.relinkTransientResources(resources);
        
        return newObj;
    }
    
    
    // Grayed Icons
    private static final Map<Icon, Icon> grayedIconMap = new HashMap<>();    
    public static Icon getGrayedIcon(Icon icon) {
        if (icon == null)
            return null;
        Icon grayed = grayedIconMap.get(icon);
        if (grayed != null)
            return grayed; // already computed
        
        final int w = icon.getIconWidth();
        final int h = icon.getIconHeight();
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();
        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        BufferedImage image = gc.createCompatibleImage(w, h);
        Graphics2D g2d = image.createGraphics();
        icon.paintIcon(null, g2d, 0, 0);
        Image gray = GrayFilter.createDisabledImage(image);
        
        grayed = new ImageIcon(gray);
        grayedIconMap.put(icon, grayed);
        return grayed;
    }
    
    public static Icon getGrayedIcon(ImageIcon icon) {
        if (icon == null)
            return null;
        Icon grayed = grayedIconMap.get(icon);
        if (grayed != null)
            return grayed; // already computed
        
        Image gray = GrayFilter.createDisabledImage(icon.getImage());
        
        grayed = new ImageIcon(gray);
        grayedIconMap.put(icon, grayed);
        return grayed;
    }

    public static boolean hasParentComponentOfClass(Component comp, Class cl) {
        while (comp != null) {
            if (cl.isInstance(comp))
                return true;
            comp = comp.getParent();
        }
        return false;
    }

    //==========================================================================
    // Support for Windows Subsystem for Linux
    //==========================================================================
    public static boolean useWSL() {
        return Util.isWindows() && !useAppImage();
    }

    // Verify that we can actually call WSL commands
    public static boolean checkWSL() {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{ 
                "wsl", "exit", "25"});
            int exitcode = p.waitFor();
            //            JOptionPane.showMessageDialog(null, "exit = "+exitcode);
            return exitcode == 25;
        } catch (IOException | InterruptedException e) {
            //            JOptionPane.showMessageDialog(null, "exception "+e);
            return false;
        }
    }

    // Verify the existance of a runnable file in the WSL subsystem
    public static boolean checkWSLcanExecute(String file) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{ 
                "wsl", "test", "-x", file});
            int exitcode = p.waitFor();
            return exitcode == 0; // 0 means it has the x flag, otherwise test returns 1
        } catch (IOException | InterruptedException e) {
            return false;
        }
    }

    // Should call the portable GreatSPN distribution inside the AppImage
    public static boolean useAppImage() {
        return Main.getUseAppImageGreatSPN_Distrib() && Main.isAppImageDistribution();
    }
}
