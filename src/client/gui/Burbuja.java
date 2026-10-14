package client.gui;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.BasicStroke;
import java.awt.geom.Area;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Burbuja de mensaje. El texto se parte en líneas a mano (con FontMetrics) en lugar de usar
 * HTML: así soporta palabras larguísimas y un mensaje como "<b>hola</b>" se muestra tal cual.
 */
class Burbuja extends JComponent {

    private static final int PAD_X = 14;
    private static final int PAD_Y = 9;
    private static final int RADIO = 20;
    private static final int ESQUINA = 6;

    private static final Font FUENTE_TEXTO = Tema.fuente(Font.PLAIN, 14.5f);
    private static final Font FUENTE_CABECERA = Tema.fuente(Font.BOLD, 12f);
    private static final Font FUENTE_HORA = Tema.fuente(Font.PLAIN, 10.5f);

    private final String texto;
    private final String cabecera;   // nombre del autor o "Privado · de X" (puede ser null)
    private final String hora;
    private final Color colorCabecera;
    private final boolean propia;
    private final boolean privada;

    private int anchoMax = 480;
    private int anchoCalculado = -1;
    private List<String> lineas = new ArrayList<>();
    private Dimension tamano = new Dimension(10, 10);

    Burbuja(String texto, String cabecera, String hora, Color colorCabecera, boolean propia, boolean privada) {
        this.texto = texto;
        this.cabecera = cabecera;
        this.hora = hora;
        this.colorCabecera = colorCabecera;
        this.propia = propia;
        this.privada = privada;
        setOpaque(false);
    }

    void setAnchoMax(int ancho) {
        if (ancho != anchoMax) {
            anchoMax = ancho;
            anchoCalculado = -1;
            revalidate();
            repaint();
        }
    }

    private void calcular() {
        if (anchoCalculado == anchoMax) {
            return;
        }
        FontMetrics fmTexto = getFontMetrics(FUENTE_TEXTO);
        FontMetrics fmCab = getFontMetrics(FUENTE_CABECERA);
        FontMetrics fmHora = getFontMetrics(FUENTE_HORA);

        lineas = envolver(texto, fmTexto, Math.max(80, anchoMax - 2 * PAD_X));

        int ancho = fmHora.stringWidth(hora);
        if (cabecera != null) {
            ancho = Math.max(ancho, fmCab.stringWidth(cabecera));
        }
        for (String l : lineas) {
            ancho = Math.max(ancho, fmTexto.stringWidth(l));
        }
        int alto = 2 * PAD_Y + lineas.size() * fmTexto.getHeight() + fmHora.getHeight() + 2;
        if (cabecera != null) {
            alto += fmCab.getHeight() + 2;
        }
        tamano = new Dimension(ancho + 2 * PAD_X, alto);
        anchoCalculado = anchoMax;
    }

    @Override
    public Dimension getPreferredSize() {
        calcular();
        return tamano;
    }

    @Override
    public Dimension getMinimumSize() {
        return getPreferredSize();
    }

    @Override
    public Dimension getMaximumSize() {
        return getPreferredSize();
    }

    @Override
    protected void paintComponent(Graphics g) {
        calcular();
        Graphics2D g2 = Tema.suavizar(g);
        int w = getWidth();
        int h = getHeight();

        // Forma: rectángulo redondeado con una esquina "puntiaguda" del lado de quien habla
        Area forma = new Area(new RoundRectangle2D.Float(0, 0, w, h, RADIO * 2, RADIO * 2));
        forma.add(new Area(new Rectangle2D.Float(propia ? w - ESQUINA * 2 : 0, h - ESQUINA * 2, ESQUINA * 2, ESQUINA * 2)));
        Shape recorte = forma;

        if (privada) {
            g2.setColor(Tema.PRIVADO_FONDO);
            g2.fill(recorte);
            g2.setColor(Tema.conAlfa(Tema.PRIVADO, 170));
            g2.setStroke(new BasicStroke(1.2f));
            g2.draw(recorte);
        } else if (propia) {
            g2.setPaint(new GradientPaint(0, 0, Tema.ACENTO, w, h, Tema.ACENTO_2));
            g2.fill(recorte);
        } else {
            g2.setColor(Tema.SUPERFICIE);
            g2.fill(recorte);
        }

        FontMetrics fmTexto = g2.getFontMetrics(FUENTE_TEXTO);
        FontMetrics fmCab = g2.getFontMetrics(FUENTE_CABECERA);
        FontMetrics fmHora = g2.getFontMetrics(FUENTE_HORA);

        int y = PAD_Y;
        if (cabecera != null) {
            g2.setFont(FUENTE_CABECERA);
            g2.setColor(colorCabecera);
            g2.drawString(cabecera, PAD_X, y + fmCab.getAscent());
            y += fmCab.getHeight() + 2;
        }

        g2.setFont(FUENTE_TEXTO);
        g2.setColor(propia && !privada ? Color.WHITE : Tema.TEXTO);
        for (String linea : lineas) {
            g2.drawString(linea, PAD_X, y + fmTexto.getAscent());
            y += fmTexto.getHeight();
        }

        g2.setFont(FUENTE_HORA);
        g2.setColor(propia && !privada ? Tema.conAlfa(Color.WHITE, 185) : Tema.TEXTO_SUAVE);
        g2.drawString(hora, w - PAD_X - fmHora.stringWidth(hora), y + 2 + fmHora.getAscent());
        g2.dispose();
    }

    /** Parte el texto en líneas que quepan en 'ancho'; si una palabra no cabe, la corta por letras. */
    static List<String> envolver(String texto, FontMetrics fm, int ancho) {
        List<String> resultado = new ArrayList<>();
        StringBuilder linea = new StringBuilder();
        for (String palabra : texto.split(" ", -1)) {
            String candidata = linea.length() == 0 ? palabra : linea + " " + palabra;
            if (fm.stringWidth(candidata) <= ancho) {
                linea = new StringBuilder(candidata);
                continue;
            }
            if (linea.length() > 0) {
                resultado.add(linea.toString());
                linea = new StringBuilder();
            }
            String resto = palabra;
            while (fm.stringWidth(resto) > ancho && resto.length() > 1) {
                int n = 1;
                while (n < resto.length() - 1 && fm.stringWidth(resto.substring(0, n + 1)) <= ancho) {
                    n++;
                }
                resultado.add(resto.substring(0, n));
                resto = resto.substring(n);
            }
            linea.append(resto);
        }
        resultado.add(linea.toString());
        return resultado;
    }
}