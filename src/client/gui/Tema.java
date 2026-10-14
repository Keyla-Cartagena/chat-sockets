package client.gui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Paleta de colores, tipografía y utilidades de dibujo de toda la interfaz. */
public final class Tema {

    private Tema() { }

    // Fondos (azul noche)
    public static final Color FONDO = new Color(0x0F1226);
    public static final Color PANEL = new Color(0x151936);
    public static final Color SUPERFICIE = new Color(0x222850);
    public static final Color SUPERFICIE_CLARA = new Color(0x2E3568);
    public static final Color BORDE = new Color(0x2A3064);

    // Acentos (violeta suave)
    public static final Color ACENTO = new Color(0x7C83FF);
    public static final Color ACENTO_2 = new Color(0xB06CFF);

    // Mensajes privados (ámbar)
    public static final Color PRIVADO = new Color(0xFFB86B);
    public static final Color PRIVADO_FONDO = new Color(0x3A2C5C);

    // Estados
    public static final Color EN_LINEA = new Color(0x3DDC97);
    public static final Color ERROR = new Color(0xFF6B8A);

    // Texto
    public static final Color TEXTO = new Color(0xE8EAFF);
    public static final Color TEXTO_SUAVE = new Color(0x8E94C2);

    private static final Color[] AVATARES = {
        new Color(0x6C8CFF), new Color(0xB06CFF), new Color(0xFF7AB6), new Color(0xFF9F6B),
        new Color(0x35C9B0), new Color(0x4FB6F5), new Color(0xE0C455), new Color(0x8BD76B)
    };

    /** Cada nickname tiene siempre el mismo color. */
    public static Color colorAvatar(String nick) {
        return AVATARES[Math.floorMod(nick.hashCode(), AVATARES.length)];
    }

    private static final String FAMILIA = elegirFamilia();

    private static String elegirFamilia() {
        String[] preferidas = {"Segoe UI", "Inter", "SF Pro Text", "Helvetica Neue", "Ubuntu", "Noto Sans", "DejaVu Sans"};
        Set<String> disponibles = new HashSet<>(Arrays.asList(
                GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()));
        for (String f : preferidas) {
            if (disponibles.contains(f)) {
                return f;
            }
        }
        return Font.SANS_SERIF;
    }

    public static Font fuente(int estilo, float tamano) {
        return new Font(FAMILIA, Font.PLAIN, 12).deriveFont(estilo, tamano);
    }

    /** Copia del Graphics con suavizado de bordes y de texto (hay que hacer dispose() al final). */
    public static Graphics2D suavizar(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        return g2;
    }

    public static Color conAlfa(Color c, int alfa) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alfa);
    }
}