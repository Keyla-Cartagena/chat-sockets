package client.gui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GradientPaint;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/** Componentes visuales propios (dibujados a mano) que le dan el look moderno a la app. */
public final class Componentes {

    private Componentes() { }

    // ------------------------------------------------------------------ Botón

    public static class BotonSuave extends JButton {
        public enum Estilo { PRIMARIO, FANTASMA, ENVIAR }

        private final Estilo estilo;
        private boolean encima;

        public BotonSuave(String texto, Estilo estilo) {
            super(texto);
            this.estilo = estilo;
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setForeground(Color.WHITE);
            setFont(Tema.fuente(java.awt.Font.BOLD, 14f));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            if (estilo == Estilo.ENVIAR) {
                setPreferredSize(new Dimension(46, 46));
            } else {
                setBorder(new EmptyBorder(12, 26, 12, 26));
            }
            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) { encima = true; repaint(); }
                @Override public void mouseExited(MouseEvent e) { encima = false; repaint(); }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Tema.suavizar(g);
            int w = getWidth();
            int h = getHeight();
            float alfa = isEnabled() ? 1f : 0.45f;
            boolean presionado = getModel().isPressed();

            if (estilo == Estilo.FANTASMA) {
                g2.setColor(encima || presionado ? Tema.SUPERFICIE_CLARA : Tema.SUPERFICIE);
                g2.fillRoundRect(0, 0, w, h, h, h);
                g2.setColor(Tema.TEXTO);
            } else {
                Color c1 = Tema.ACENTO;
                Color c2 = Tema.ACENTO_2;
                if (encima && isEnabled()) { c1 = c1.brighter(); c2 = c2.brighter(); }
                if (presionado) { c1 = c1.darker(); c2 = c2.darker(); }
                g2.setPaint(new GradientPaint(0, 0, Tema.conAlfa(c1, (int) (255 * alfa)),
                        w, h, Tema.conAlfa(c2, (int) (255 * alfa))));
                if (estilo == Estilo.ENVIAR) {
                    int d = Math.min(w, h);
                    g2.fill(new Ellipse2D.Float((w - d) / 2f, (h - d) / 2f, d, d));
                } else {
                    g2.fillRoundRect(0, 0, w, h, h, h);
                }
                g2.setColor(Tema.conAlfa(Color.WHITE, (int) (255 * alfa)));
            }

            if (estilo == Estilo.ENVIAR) {
                // avioncito de papel
                float cx = w / 2f;
                float cy = h / 2f;
                Path2D.Float avion = new Path2D.Float();
                avion.moveTo(cx - 8, cy - 9);
                avion.lineTo(cx + 10, cy);
                avion.lineTo(cx - 8, cy + 9);
                avion.lineTo(cx - 4, cy);
                avion.closePath();
                g2.fill(avion);
            } else {
                g2.setFont(getFont());
                FontMetrics fm = g2.getFontMetrics();
                int x = (w - fm.stringWidth(getText())) / 2;
                int y = (h - fm.getHeight()) / 2 + fm.getAscent();
                g2.drawString(getText(), x, y);
            }
            g2.dispose();
        }
    }

    // ------------------------------------------------------- Paneles y campos

    public static class PanelRedondeado extends JPanel {
        private final Color fondo;
        private final int radio;
        private Color borde;

        public PanelRedondeado(Color fondo, int radio) {
            this.fondo = fondo;
            this.radio = radio;
            setOpaque(false);
        }

        public void setBorde(Color borde) {
            this.borde = borde;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Tema.suavizar(g);
            g2.setColor(fondo);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radio, radio);
            if (borde != null) {
                g2.setColor(borde);
                g2.setStroke(new BasicStroke(1.4f));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, radio, radio);
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    /** JTextField sin borde ni fondo, con texto de ayuda (placeholder) cuando está vacío. */
    public static class CampoTexto extends JTextField {
        private String pista;

        public CampoTexto(String pista) {
            this.pista = pista;
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder());
            setFont(Tema.fuente(java.awt.Font.PLAIN, 15f));
            setForeground(Tema.TEXTO);
            setCaretColor(Tema.ACENTO);
            setDisabledTextColor(Tema.TEXTO_SUAVE);
            setSelectionColor(Tema.conAlfa(Tema.ACENTO, 120));
            setSelectedTextColor(Color.WHITE);
        }

        public void setPista(String pista) {
            this.pista = pista;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (getText().isEmpty() && pista != null) {
                Graphics2D g2 = Tema.suavizar(g);
                g2.setFont(getFont());
                g2.setColor(Tema.conAlfa(Tema.TEXTO_SUAVE, 190));
                FontMetrics fm = g2.getFontMetrics();
                g2.drawString(pista, getInsets().left, (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
                g2.dispose();
            }
        }
    }

    /** Campo de texto dentro de un contenedor redondeado que se ilumina al tener el foco. */
    public static class CampoRedondeado extends PanelRedondeado {
        public final CampoTexto texto;

        public CampoRedondeado(String pista, int radio) {
            super(Tema.SUPERFICIE, radio);
            setLayout(new BorderLayout());
            setBorder(new EmptyBorder(12, 18, 12, 18));
            texto = new CampoTexto(pista);
            add(texto, BorderLayout.CENTER);
            texto.addFocusListener(new FocusAdapter() {
                @Override public void focusGained(FocusEvent e) { setBorde(Tema.ACENTO); }
                @Override public void focusLost(FocusEvent e) { setBorde(null); }
            });
            addMouseListener(new MouseAdapter() {
                @Override public void mousePressed(MouseEvent e) { texto.requestFocusInWindow(); }
            });
        }
    }

    // ------------------------------------------------- Avatar, logo y píldora

    public static void dibujarAvatar(Graphics2D g2, String nick, int x, int y, int d) {
        Color c = Tema.colorAvatar(nick);
        g2.setPaint(new GradientPaint(x, y, c, x + d, y + d, c.darker()));
        g2.fill(new Ellipse2D.Float(x, y, d, d));
        g2.setColor(Color.WHITE);
        g2.setFont(Tema.fuente(java.awt.Font.BOLD, d * 0.42f));
        FontMetrics fm = g2.getFontMetrics();
        String inicial = nick.isEmpty() ? "?" : new String(Character.toChars(nick.codePointAt(0))).toUpperCase();
        g2.drawString(inicial, x + (d - fm.stringWidth(inicial)) / 2.0f,
                y + (d - fm.getHeight()) / 2.0f + fm.getAscent());
    }

    public static class Avatar extends JComponent {
        private final String nick;
        private final int d;

        public Avatar(String nick, int d) {
            this.nick = nick;
            this.d = d;
            setOpaque(false);
            setPreferredSize(new Dimension(d, d));
            setMinimumSize(new Dimension(d, d));
            setMaximumSize(new Dimension(d, d));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Tema.suavizar(g);
            dibujarAvatar(g2, nick, 0, 0, d);
            g2.dispose();
        }
    }

    public static void dibujarLogo(Graphics2D g2, int x, int y, int d) {
        g2.setPaint(new GradientPaint(x, y, Tema.ACENTO, x + d, y + d, Tema.ACENTO_2));
        g2.fillRoundRect(x, y, d, d, d * 3 / 10, d * 3 / 10);

        float bw = d * 0.58f;
        float bh = d * 0.40f;
        float bx = x + (d - bw) / 2f;
        float by = y + d * 0.22f;
        g2.setColor(Color.WHITE);
        g2.fill(new RoundRectangle2D.Float(bx, by, bw, bh, bh * 0.7f, bh * 0.7f));

        Path2D.Float cola = new Path2D.Float();
        cola.moveTo(bx + bw * 0.20f, by + bh - 2);
        cola.lineTo(bx + bw * 0.16f, by + bh + d * 0.14f);
        cola.lineTo(bx + bw * 0.46f, by + bh - 2);
        cola.closePath();
        g2.fill(cola);

        g2.setColor(Tema.ACENTO);
        float r = d * 0.045f;
        for (int i = 0; i < 3; i++) {
            float cx = bx + bw * (0.27f + 0.23f * i);
            g2.fill(new Ellipse2D.Float(cx - r, by + bh / 2f - r, 2 * r, 2 * r));
        }
    }

    public static class Logo extends JComponent {
        private final int d;

        public Logo(int d) {
            this.d = d;
            setOpaque(false);
            setPreferredSize(new Dimension(d, d));
            setMinimumSize(new Dimension(d, d));
            setMaximumSize(new Dimension(d, d));
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Tema.suavizar(g);
            dibujarLogo(g2, 0, 0, d);
            g2.dispose();
        }
    }

    /** Icono de la ventana generado por código (sin archivos de imagen). */
    public static Image icono(int d) {
        BufferedImage img = new BufferedImage(d, d, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = Tema.suavizar(img.getGraphics());
        dibujarLogo(g2, 0, 0, d);
        g2.dispose();
        return img;
    }

    /** Etiqueta pequeña centrada con forma de píldora, para avisos del sistema. */
    public static class Pildora extends JComponent {
        private final String texto;
        private final Color color;

        public Pildora(String texto, Color color) {
            this.texto = texto;
            this.color = color;
            setOpaque(false);
            setFont(Tema.fuente(java.awt.Font.PLAIN, 12.5f));
        }

        @Override
        public Dimension getPreferredSize() {
            FontMetrics fm = getFontMetrics(getFont());
            return new Dimension(fm.stringWidth(texto) + 30, fm.getHeight() + 12);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = Tema.suavizar(g);
            g2.setColor(Tema.conAlfa(color, 30));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
            g2.setColor(color);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2,
                    (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
        }
    }

    // --------------------------------------------------------- Barra de scroll

    public static class BarraSuave extends BasicScrollBarUI {
        @Override
        protected JButton createDecreaseButton(int orientation) {
            return botonVacio();
        }

        @Override
        protected JButton createIncreaseButton(int orientation) {
            return botonVacio();
        }

        private JButton botonVacio() {
            JButton b = new JButton();
            Dimension cero = new Dimension(0, 0);
            b.setPreferredSize(cero);
            b.setMinimumSize(cero);
            b.setMaximumSize(cero);
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            // sin pista visible
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            if (r.isEmpty() || !scrollbar.isEnabled()) {
                return;
            }
            Graphics2D g2 = Tema.suavizar(g);
            g2.setColor(isThumbRollover() ? Tema.ACENTO : Tema.SUPERFICIE_CLARA);
            g2.fillRoundRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4, r.width - 4, r.width - 4);
            g2.dispose();
        }

        @Override
        public Dimension getPreferredSize(JComponent c) {
            return new Dimension(10, 10);
        }
    }
}