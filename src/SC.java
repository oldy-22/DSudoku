import java.awt.Color;

/*
 * Created on 06.06.2007
 *
 */

/** SudokuConstants
 * @author heida
 *
 */
public class SC {
	
	// ------------ Änderbare KOSTANTENDEFINITIONEN ------------
	
	public static final String APP_TITLE = "Sudoku-Programm von Dani";
	// version nun in SudokuApplication.java
	public static final boolean debug = false;
	
	
	// ------------ Nicht änderbare KOSTANTENDEFINITIONEN ------------

	// Texte auf den Buttons
	static final String HILFE			= " Hilfe - Text! ";
	static final String FUELLEN	= " Beispiel ausfüllen! ";
	static final String GENERIEREN	= " Sudoku generieren! ";
	
	static final String MERKEN	= " Werte merken! ";
	static final String LOESCHEN= " Werte löschen! ";
	static final String EINSETZEN= " Werte einsetzen! ";
	
	static final String LESEN		= " Werte einlesen! ";
	static final String LOESEN		= " Vollständig Lösen ! ";
	static final String SCHRITT	= " Schrittweise Lösen ! ";
	
	static final Color DARKGREEN = new Color(0, 102, 0);
	static final Color LIGHTGREEN = new Color(230, 255, 153);
	static final Color LIGHTBLUE = new Color(102, 255, 255);
	static final Color VIOLETT = new Color(120, 0, 120);
	static final Color LIGHTYELLOW = new Color(255,  255, 51);
	static final Color DARKRED = new Color(204, 0, 0);
	static final Color SIGNALRED = new Color(204, 0, 102);
	
	static final int boarderWidth = 3;
	static final int fineBoarderWidth = 1;

	static final String btnTexte[] = new String [] { HILFE, "", "", FUELLEN, GENERIEREN, "", "",
		MERKEN, LOESCHEN, EINSETZEN, "", LESEN, LOESEN, SCHRITT};
	static final char mnemos[] = {'h','1','2','b','g','4','z','m', 'l', 'e','x', 'w', 'v', 's'};
	static final String toolTips[] = new String [] {"Hilfetext zeigen!", "", "", 
		"Beispiel laden.", 
		"Ein Sudoku vom Programm selbst generieren lassen", "", 
		"Zeige die Eintragsmöglichkeiten offener Felder an.",
		"Werte aus Sudoku intern speichern.",
		"Sudoku-Zahlen löschen.",
		"Gemerkte Werte in Sudoku einsetzen.", "",
		"Eingetragene Werte einlesen und Sudoku aufbereiten.",
		"Sudoku lösen (alle Zahlen ausfüllen).",
		"Im Einzelschritt lösen (1 Zahl pro Click eintragen)."
	};

}
