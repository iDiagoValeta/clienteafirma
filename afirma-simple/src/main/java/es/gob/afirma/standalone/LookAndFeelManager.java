/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.standalone;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import es.gob.afirma.core.misc.Platform;
import es.gob.afirma.core.misc.Platform.OS;

/** Maneja el LookAndFeel aplicado al aplicativo.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s.
 * @author Carlos Gamuci. */
public final class LookAndFeelManager {

    private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	private LookAndFeelManager() {
		// No permitimos la instanciacion
	}

    /** Color de fondo por defecto para los JPanel, JFrame y Applet.
     * Se recalcula desde el tema activo al aplicar el Look&amp;Feel. */
    public static Color DEFAULT_COLOR;

    /** Color de fondo secundario (superficie elevada) para los JPanel, JFrame y Applet.
     * Se recalcula desde el tema activo al aplicar el Look&amp;Feel. */
    public static Color SECUNDARY_COLOR;

    /** Color transparente. */
    public static final Color TRANSPARENT_COLOR = new Color(255, 255, 255, 0);

    /** Indica si se detecta el uso de Windows en modo alto contraste. */
    public static final boolean WINDOWS_HIGH_CONTRAST;

    /** Indica si se detecta un sistema linux con GNOME y un tema oscuro habilitado. */
    public static final boolean GNOME_DARK_MODE;

    /** Tama&ntilde;o m&aacute;ximo de las fuentes por defecto antes de considerarse grandes. */
    private static final int LARGE_FONT_LIMIT = 13;

    /** Indica si el sistema operativo tiene activada una combinaci&oacute;n de colores de alto contraste. */
    private static final boolean LARGE_FONT;

    private static Dimension screenSize = null;

    private static boolean applied = false;

    static {

    	// Obtenemos el color de la ventanas. Se protege porque puede producir errores en
    	// algunas distribuciones de Linux
    	Color windowColor;
    	try {
	    	 windowColor = UIManager.getColor("window") != null ? //$NON-NLS-1$
	    			 new Color(UIManager.getColor("window").getRGB()) : //$NON-NLS-1$
	    			 new Color(238, 238, 238);
    	}
    	catch (final Throwable e) {
    		windowColor = new Color(238, 238, 238);
		}
    	DEFAULT_COLOR = windowColor;

        final Object highContrast = Toolkit.getDefaultToolkit().getDesktopProperty("win.highContrast.on"); //$NON-NLS-1$
        if (highContrast instanceof Boolean) {
            WINDOWS_HIGH_CONTRAST = ((Boolean) highContrast).booleanValue();
        }
        else {
            WINDOWS_HIGH_CONTRAST = false;
        }

        SECUNDARY_COLOR = WINDOWS_HIGH_CONTRAST ? Color.BLACK : Color.WHITE;

        final  Object defaultFontHeight = Toolkit.getDefaultToolkit().getDesktopProperty("win.defaultGUI.font.height"); //$NON-NLS-1$
        if (defaultFontHeight instanceof Integer) {
           LARGE_FONT = ((Integer) defaultFontHeight).intValue() > LARGE_FONT_LIMIT;
        }
        // En Linux usamos siempre la misma configuracion que al detectar un tamano de fuente grande
        else if (Platform.OS.LINUX.equals(Platform.getOS())) {
            LARGE_FONT = true;
        }
        else {
            LARGE_FONT = false;
        }

        if (Platform.getOS() == OS.LINUX) {
        	GNOME_DARK_MODE = isGnomeDarkMode();
        }
        else {
        	GNOME_DARK_MODE = false;
        }

    }

    /** Establece el decorado de la aplicaci&oacute;n si no se ha hecho ya. */
    public static void applyLookAndFeel() {
    	applyLookAndFeel(false);
    }

    /**
     * Establece el decorado de la aplicaci&oacute;n.
     * @param force {@code true} para aplicar el estilo en cualquier caso,
     * {@code false} para evitar aplicarlo si ya se hizo anteriormente.
     */
    public static void applyLookAndFeel(final boolean force) {

    	// Si ya se aplico el estilo y no se esta forzando a que
    	// se vuelva a aplicar, no se aplica
    	if (applied && !force) {
    		return;
    	}
    	// En Windows con alto contraste respetamos el Look&Feel del sistema por accesibilidad
    	if (WINDOWS_HIGH_CONTRAST) {
    		try {
    			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    		}
    		catch (final Exception e2) {
    			LOGGER.warning("No se ha podido establecer el 'Look&Feel' del sistema: " + e2); //$NON-NLS-1$
    		}
    		refreshThemeColors();
    		applied = true;
    		return;
    	}

    	// Tema plano y moderno (FlatLaf), en version clara u oscura segun el tema del sistema.
    	// FlatLaf ademas escala correctamente en pantallas HiDPI.
    	try {
    		if (GNOME_DARK_MODE) {
    			FlatDarkLaf.setup();
    		}
    		else {
    			FlatLightLaf.setup();
    		}
    		// Ajustes para dar aire a la interfaz
    		UIManager.put("Button.arc", Integer.valueOf(6)); //$NON-NLS-1$
    		UIManager.put("Component.arc", Integer.valueOf(6)); //$NON-NLS-1$
    		UIManager.put("ScrollBar.showButtons", Boolean.FALSE); //$NON-NLS-1$
    		UIManager.put("Button.defaultButtonFollowsFocus", Boolean.TRUE); //$NON-NLS-1$
    	}
    	catch (final Exception e) {
    		LOGGER.warning("No se ha podido aplicar FlatLaf, se usara Nimbus: " + e); //$NON-NLS-1$
    		setLookAndFeel("Nimbus"); //$NON-NLS-1$
    	}

    	// Dejamos que el sistema operativo dibuje las decoraciones de ventana (integracion nativa)
    	JFrame.setDefaultLookAndFeelDecorated(false);
    	JDialog.setDefaultLookAndFeelDecorated(false);

    	// Propiedades especificas para Mac OS X
    	if (Platform.OS.MACOSX.equals(Platform.getOS())) {
    		System.setProperty("apple.awt.antialiasing", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    		System.setProperty("apple.awt.textantialiasing", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    		System.setProperty("apple.laf.useScreenMenuBar", "true"); //$NON-NLS-1$ //$NON-NLS-2$
    	}

    	refreshThemeColors();
    	applied = true;
    }

    /** Recalcula los colores base a partir del tema actualmente activo, de modo que
     * los paneles que los usan se integren con el Look&amp;Feel claro u oscuro. */
    private static void refreshThemeColors() {
    	final Color panelBg = UIManager.getColor("Panel.background"); //$NON-NLS-1$
    	if (panelBg != null) {
    		DEFAULT_COLOR = new Color(panelBg.getRGB());
    	}
    	// Superficie elevada: fondo de los campos de texto (claro sobre fondo claro, oscuro sobre fondo oscuro)
    	final Color surface = UIManager.getColor("TextField.background"); //$NON-NLS-1$
    	if (surface != null) {
    		SECUNDARY_COLOR = new Color(surface.getRGB());
    	}
    }

    //Define el look and feel
    private static void setLookAndFeel(final String lookandfeelName) {
    	try {
            for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (lookandfeelName.equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                	return;
             	}
             }

    	}
    	 catch (final Exception e) {
             LOGGER.warning(
                    "No se ha podido establecer el 'Look&Feel' " + lookandfeelName + ": " + e //$NON-NLS-1$ //$NON-NLS-2$
             );
         }
    }

    /**
     * Obtiene el tama&ntilde;o de pantalla.
     * @return Tama&ntilde;o de pantalla.
     */
    public static Dimension getScreenSize() {
    	if (screenSize == null) {
    		screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    	}
    	return screenSize;
    }

	/**
	 * Indica si es necesario maximizar las ventanas para asegurar una buena visualizaci&oacute;n.
	 * @return {@code true} si se debe maximizar la pantalla, {@code false} en caso contrario.
	 */
	public static boolean needMaximizeWindow() {
		return getScreenSize().height <= 600;
	}



	/**
	 * C&Oacute;DIGO EXTRA&Iacute;DO DEL PROYECTO jSystemThemeDetector (https://github.com/Dansoftowner/jSystemThemeDetector).
	 * Detecta si nos encontramos en un entorno con GNOME y un tema declarado como oscuro.
	 * @return {@code true} si encontramos configurado un tema oscuro de GNOME, {@code false} en caso contrario.
	 */
	private static boolean isGnomeDarkMode() {
        return
             (queryResultContains("echo $XDG_CURRENT_DESKTOP", "gnome") || //$NON-NLS-1$ //$NON-NLS-2$
             queryResultContains("echo $XDG_DATA_DIRS | grep -Eo 'gnome'", "gnome") || //$NON-NLS-1$ //$NON-NLS-2$
             queryResultContains("ps -e | grep -E -i \"gnome\"", "gnome")) //$NON-NLS-1$ //$NON-NLS-2$
             	&& queryResultContains("gsettings get org.gnome.desktop.interface gtk-theme", "dark") //$NON-NLS-1$ //$NON-NLS-2$
        ;
    }

	private static boolean queryResultContains(final String cmd, final String subResult) {
		return query(cmd).toLowerCase().contains(subResult);
	}

	private static String query(final String cmd) {
		try {
			final Process process = Runtime.getRuntime().exec(cmd);
			final StringBuilder stringBuilder = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String actualReadLine;
				while ((actualReadLine = reader.readLine()) != null) {
					if (stringBuilder.length() != 0) {
						stringBuilder.append('\n');
					}
					stringBuilder.append(actualReadLine);
				}
			}
			return stringBuilder.toString();
		} catch (final IOException e) {
			return ""; //$NON-NLS-1$
		}
	}
}
