package client.gui;

import client.gui.Componentes.BotonSuave;
import client.gui.Componentes.CampoRedondeado;
import client.gui.Componentes.Logo;
import client.gui.Componentes.PanelRedondeado;
import common.Protocolo;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RadialGradientPaint;
import java.awt.geom.Point2D;
import java.util.function.Consumer;

/** Pantalla de inicio: pide nickname, servidor y puerto. */
public class PanelLogin extends JPanel {

    public record DatosLogin(String host, int puerto, String nick) { }

    private static final String TEXTO_BOTON = "Entrar al chat";

    private final Consumer<DatosLogin> alEntrar;
    private final CampoRedondeado campoNick = new CampoRedondeado("Cómo quieres que te vean", 18);
    private final CampoRedondeado campoHost = new CampoRedondeado("localhost", 18);
    private final CampoRedondeado campoPuerto = new CampoRedondeado("5000", 18);
    private final JLabel error = new JLabel(" ");
    private final BotonSuave boton = new BotonSuave(TEXTO_BOTON, BotonSuave.Estilo.PRIMARIO);

    public PanelLogin(Consumer<DatosLogin> alEntrar) {
        this.alEntrar = alEntrar;
        setLayout(new GridBagLayout());
        setBackground(Tema.FONDO);

        campoHost.texto.setText(Protocolo.HOST);
        campoPuerto.texto.setText(String.valueOf(Protocolo.PUERTO));

        PanelRedondeado tarjeta = new PanelRedondeado(Tema.PANEL, 34);
        tarjeta.setBorde(Tema.BORDE);
        tarjeta.setLayout(new GridBagLayout());
        tarjeta.setBorder(new EmptyBorder(38, 40, 34, 40));

        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;

        // Logo + títulos
        JPanel cabecera = new JPanel(new BorderLayout(0, 14));
        cabecera.setOpaque(false);
        JPanel logoCentrado = new JPanel();
        logoCentrado.setOpaque(false);
        logoCentrado.add(new Logo(68));
        cabecera.add(logoCentrado, BorderLayout.NORTH);

        JLabel titulo = new JLabel("Chat en línea", JLabel.CENTER);
        titulo.setFont(Tema.fuente(Font.BOLD, 26f));
        titulo.setForeground(Tema.TEXTO);
        JLabel subtitulo = new JLabel("Conéctate al servidor y empieza a conversar", JLabel.CENTER);
        subtitulo.setFont(Tema.fuente(Font.PLAIN, 13.5f));
        subtitulo.setForeground(Tema.TEXTO_SUAVE);
        JPanel textos = new JPanel(new BorderLayout(0, 4));
        textos.setOpaque(false);
        textos.add(titulo, BorderLayout.NORTH);
        textos.add(subtitulo, BorderLayout.CENTER);
        cabecera.add(textos, BorderLayout.CENTER);

        c.gridy = 0;
        c.insets = new Insets(0, 0, 26, 0);
        tarjeta.add(cabecera, c);

        // Nickname
        c.gridy = 1;
        c.insets = new Insets(0, 0, 14, 0);
        tarjeta.add(bloque("Nickname", campoNick), c);

        // Servidor + puerto en una fila
        JPanel fila = new JPanel(new GridBagLayout());
        fila.setOpaque(false);
        GridBagConstraints f = new GridBagConstraints();
        f.fill = GridBagConstraints.HORIZONTAL;
        f.gridy = 0;
        f.gridx = 0;
        f.weightx = 1;
        f.insets = new Insets(0, 0, 0, 12);
        fila.add(bloque("Servidor", campoHost), f);
        f.gridx = 1;
        f.weightx = 0;
        f.insets = new Insets(0, 0, 0, 0);
        campoPuerto.setPreferredSize(new Dimension(104, campoPuerto.getPreferredSize().height));
        fila.add(bloque("Puerto", campoPuerto), f);

        c.gridy = 2;
        c.insets = new Insets(0, 0, 8, 0);
        tarjeta.add(fila, c);

        // Mensaje de error (reserva su espacio para que la tarjeta no "salte")
        error.setFont(Tema.fuente(Font.PLAIN, 13f));
        error.setForeground(Tema.ERROR);
        c.gridy = 3;
        c.insets = new Insets(4, 2, 14, 0);
        tarjeta.add(error, c);

        // Botón
        c.gridy = 4;
        c.insets = new Insets(0, 0, 18, 0);
        tarjeta.add(boton, c);

        JLabel pie = new JLabel("Java · Sockets TCP · Hilos", JLabel.CENTER);
        pie.setFont(Tema.fuente(Font.PLAIN, 12f));
        pie.setForeground(Tema.conAlfa(Tema.TEXTO_SUAVE, 170));
        c.gridy = 5;
        c.insets = new Insets(0, 0, 0, 0);
        tarjeta.add(pie, c);

        Dimension pref = tarjeta.getPreferredSize();
        tarjeta.setPreferredSize(new Dimension(460, pref.height));
        add(tarjeta);

        boton.addActionListener(e -> intentar());
        campoNick.texto.addActionListener(e -> intentar());
        campoHost.texto.addActionListener(e -> intentar());
        campoPuerto.texto.addActionListener(e -> intentar());
    }

    private JPanel bloque(String etiqueta, CampoRedondeado campo) {
        JPanel p = new JPanel(new BorderLayout(0, 7));
        p.setOpaque(false);
        JLabel l = new JLabel(etiqueta);
        l.setFont(Tema.fuente(Font.BOLD, 12.5f));
        l.setForeground(Tema.TEXTO_SUAVE);
        l.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 0));
        p.add(l, BorderLayout.NORTH);
        p.add(campo, BorderLayout.CENTER);
        return p;
    }

    private void intentar() {
        if (!boton.isEnabled()) {
            return;
        }
        String nick = campoNick.texto.getText().trim();
        String host = campoHost.texto.getText().trim();
        if (host.isEmpty()) {
            host = Protocolo.HOST;
        }
        if (!Protocolo.nickValido(nick)) {
            mostrarError("El nickname debe tener de 1 a 15 letras, números o _ (sin espacios).");
            return;
        }
        int puerto;
        try {
            puerto = Integer.parseInt(campoPuerto.texto.getText().trim());
            if (puerto < 1 || puerto > 65535) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e) {
            mostrarError("El puerto debe ser un número entre 1 y 65535.");
            return;
        }
        mostrarError(null);
        alEntrar.accept(new DatosLogin(host, puerto, nick));
    }

    /** null limpia el mensaje. Se escapa el HTML porque el texto puede venir del servidor. */
    public void mostrarError(String mensaje) {
        if (mensaje == null || mensaje.isBlank()) {
            error.setText(" ");
        } else {
            String seguro = mensaje.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
            error.setText("<html><body style='width:340px'>" + seguro + "</body></html>");
        }
    }

    public void setCargando(boolean cargando) {
        boton.setEnabled(!cargando);
        boton.setText(cargando ? "Conectando…" : TEXTO_BOTON);
        campoNick.texto.setEnabled(!cargando);
        campoHost.texto.setEnabled(!cargando);
        campoPuerto.texto.setEnabled(!cargando);
    }

    public void enfocar() {
        campoNick.texto.requestFocusInWindow();
    }

    /** Fondo con degradado y dos "resplandores" de color. */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = Tema.suavizar(g);
        int w = getWidth();
        int h = getHeight();
        g2.setPaint(new GradientPaint(0, 0, Tema.FONDO, w, h, new Color(0x1A1546)));
        g2.fillRect(0, 0, w, h);

        float r1 = Math.max(w, h) * 0.55f;
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.12f, h * 0.10f), r1,
                new float[] {0f, 1f}, new Color[] {Tema.conAlfa(Tema.ACENTO, 70), Tema.conAlfa(Tema.ACENTO, 0)}));
        g2.fillRect(0, 0, w, h);

        float r2 = Math.max(w, h) * 0.50f;
        g2.setPaint(new RadialGradientPaint(new Point2D.Float(w * 0.92f, h * 0.95f), r2,
                new float[] {0f, 1f}, new Color[] {Tema.conAlfa(Tema.ACENTO_2, 65), Tema.conAlfa(Tema.ACENTO_2, 0)}));
        g2.fillRect(0, 0, w, h);
        g2.dispose();
    }
}