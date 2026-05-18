import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.BorderFactory;
import java.awt.Color;
import java.util.*;

/*
 * Created on 29.04.2007
 */
/**Die gesamte Oberfläche des Sudokus (leider momentan mit allem Daten drum und dran
 * in einem.*/
public class SudokuMainframe extends JFrame implements ActionListener, 
	FocusListener, KeyListener, Runnable {

	JPanel block[][] = new JPanel [3][3];
	JTextField stf [][] = new JTextField [9][9];
	JButton jbtn[] = new JButton[14];
	JComboBox jcombo;
	JCheckBox jcheck;
	JToggleButton jtbtn;
	JLabel statusRow = new JLabel ("Willkommen bei Danis Sudoku-Programm! Für Hilfe drück bitte den Button links oben! ");

	private HelpPanel helpPanel = null;
	public boolean helpPanelVisible = false;

	int shadow[] = new int [81]; // Zwischenablage
	int buffer[][] = new int [9][9];

	SudokuItem sudoku [][] = new SudokuItem [9][9];
	boolean sudokuJustStarted = true;

	/** Sudoku Vektor */
	Vector sv [][] = new Vector[9][9];
	Random r = new Random ( System.currentTimeMillis() );
	static boolean solved = true, solveStep;
	int unknown=0, possibilities=0, harakiri=0; // loops wird berechnet (aber nicht ausgewertet)
	int stepZ=-1, stepS=-1; // mit possibilities habe ich ab und zu neg. Werte gesehen aber egal, keine
											// Berechnung ist davon betroffen nur die Anzeige ist nicht korrekt
	int zbuff=0, sbuff=0;
	
	int mode;  	// Modes für tae
	static final int TRYMODE = 1;
	static final int SOLVEMODE = 2;

	private Thread solving = null;
	
	int countOfTryModes = 0;
	boolean tryModeOnceActivated;
	int firstTimeCountOfSolvesInTry = 0;
	
	int[][] generatedMatrix = new int[9][9];
	int trapezHöhe = 0;// für Animation 

	
	public SudokuMainframe(String version) {
		
		super (SC.APP_TITLE + " - " + version);

		// Hauptfenster
		JPanel pane0 = new JPanel ();
		pane0.setLayout (new BorderLayout ());

		// Tabellenbereich
		JPanel pane1 = new JPanel ();
		pane1.setBorder (new EmptyBorder (10, 40, 10, 40));
		pane1.setLayout (new GridLayout (3, 3));

		// Sudoku-Tabelle
		for (int i=0; i<3; i++) {
			for (int j=0; j<3; j++) { 
				block[i][j] = new JPanel(); // Blöcke 3x3
				// block[i][j].setBorder ( new CompoundBorder ( BorderFactory.createEtchedBorder(),
					// new EmptyBorder (getBlockInsets()) ) );
				block[i][j].setBorder ( new EmptyBorder (getBlockInsets()) );
				block[i][j].setLayout (new GridLayout (3, 3));
				block[i][j].getInsets ( getBlockInsets ());
				
				for (int k=0; k<3; k++)
					for (int l=0; l<3; l++) {
						stf[i*3+k][j*3+l] = new JTextField (2);
						stf[i*3+k][j*3+l].setHorizontalAlignment(SwingConstants.CENTER);
						stf[i*3+k][j*3+l].addFocusListener(this);
						stf[i*3+k][j*3+l].addKeyListener(this);
						block[i][j].add (stf [i*3+k][j*3+l]);
					}
				pane1.add ( block[i][j] );
			}
		}
		
		//Action-Bereich
		JPanel leftActionArea = new JPanel ();
		leftActionArea.setBorder (new EmptyBorder (10, 0, 10, 0));
		leftActionArea.setLayout (new GridLayout (7, 0));
		JPanel rightActionArea = new JPanel ();
		rightActionArea.setBorder (new EmptyBorder (10, 0, 10, 0));
		rightActionArea.setLayout (new GridLayout (7, 0));
		
		jcombo = new JComboBox ();
		jcombo.setEditable(false);
		for (int i=0; i<SudokuExamples.size(); ) jcombo.addItem ("   Beispiel " + ++i);
		
		jcheck = new JCheckBox ("Zeige Möglichkeiten");
		jcheck.setMnemonic (SC.mnemos[6]);
		jcheck.setToolTipText (SC.toolTips[6]);
		jcheck.addActionListener (this);
		jcheck.setEnabled(false);
		
		for (int i=0; i<14; i++) {
			switch (i) {
				case 1: leftActionArea.add (new JLabel()); break;
				case 2: leftActionArea.add (jcombo); break;
				//case 4: leftActionArea.add (new JLabel()); break;
				case 5: leftActionArea.add (new JLabel()); break;
				case 6: leftActionArea.add (jcheck); break;
				case 10: rightActionArea.add (new JLabel()); break;
				case 12: { 
					jtbtn = new JToggleButton(SC.btnTexte[i]);
					jtbtn.setMargin( getButtonInsets() );
					jtbtn.setMnemonic (SC.mnemos[i]);
					jtbtn.setToolTipText (SC.toolTips[i]);
					jtbtn.addActionListener (this);
					rightActionArea.add ( jtbtn ); break;
				}
				default: {
					jbtn[i] = new JButton (SC.btnTexte[i]);
					jbtn[i].setMargin( getButtonInsets() );
					jbtn[i].setMnemonic (SC.mnemos[i]);
					jbtn[i].setToolTipText (SC.toolTips[i]);
					jbtn[i].addActionListener (this);
					if (i<7) leftActionArea.add ( jbtn[i] );
						else rightActionArea.add ( jbtn[i] );
				}
			}
		}

		// Statusbereich
		JPanel statusArea = new JPanel ();
		statusArea.setLayout (new FlowLayout (FlowLayout.LEFT));
		statusArea.add(statusRow);
		statusArea.setBorder (BorderFactory.createLoweredBevelBorder());
		
		pane0.add ("Center", pane1);
		pane0.add ("West", leftActionArea);
		pane0.add ("East", rightActionArea);
		pane0.add ("South", statusArea);

		setContentPane(pane0);
		resetSudoku();
		startAnimation();
		
	}

	public Insets getButtonInsets () { return new Insets (5, 10, 5, 10); }
	
	public Insets getBlockInsets () { return new Insets (3, 5, 3, 5); }
	
	@Override
	public void actionPerformed(ActionEvent evt) {
		Object src = evt.getSource();

		setStatusRow(Color.BLACK, " ");
				
		if (src instanceof JCheckBox) {
			if (jcheck.isSelected()) {
				writeVectors();
			} else {
				eraseVectors();
			}
		}

		if (src instanceof JButton) {
			if (evt.getActionCommand() == SC.HILFE) {
				showHelpDialog();
			}
			if (evt.getActionCommand() == SC.FUELLEN) {
				resetSudoku();
				int selected = jcombo.getSelectedIndex();
				fillSudoku (SudokuExamples.getExample(selected) );
			}
			if (evt.getActionCommand() == SC.GENERIEREN) {
				resetSudoku(); generateSudoku();
			}
			if (evt.getActionCommand() == SC.MERKEN) {
				shadowSudoku();
			}
			if (evt.getActionCommand() == SC.LOESCHEN) {
				resetSudoku();
			}
			if (evt.getActionCommand() == SC.EINSETZEN) {
				resetSudoku(); fillinShadow();
			}
			if (evt.getActionCommand() == SC.LESEN) {
				if (initFields()) {
					jcheck.setEnabled(true); jcheck.setSelected(false);
				}
			}
			if (evt.getActionCommand() == SC.SCHRITT) {
				startSolvingStepwise();
			}
		} else {
			if (src instanceof JToggleButton) {
				if (evt.getActionCommand() == SC.LOESEN) {
					startSolving();
				}
			}
		}
	}

	@Override
	public void focusGained (FocusEvent e) {}
	@Override
	public void focusLost (FocusEvent e) { getFields(); }

	@Override
	public void keyPressed (KeyEvent e) {
		if (e.getKeyCode() == KeyEvent.VK_ENTER) getFields();
	}
	@Override
	public void keyReleased (KeyEvent e) {}
	@Override
	public void keyTyped (KeyEvent e) {}

	/** alle Aktionen zum Zurücksetzen der Sudoku-Oberfläche und des Sudukos sind hierdrin*/
	private void resetSudoku() {
		jcheck.setEnabled(false); jcheck.setSelected(false);
		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				stf[i][j].setText("");
				stf[i][j].setBackground(Color.WHITE);
				stf[i][j].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, SC.fineBoarderWidth));				
				stf[i][j].setEditable(true);
			}
		}
		resetSudokuMatrix();
	}

	/** alle Aktionen zum Zurücksetzen des Sudukos (nur Variablen) sind hierdrin*/
	void resetSudokuMatrix() {
		unknown=81; possibilities=0;
		harakiri=0; 
		solved=true;
		stepZ=-1; stepS=-1;
		sudokuJustStarted = true;
		tryModeOnceActivated = false;
		firstTimeCountOfSolvesInTry = -1;

		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				sudoku[i][j] = new SudokuItem ();
				try {
					sv[i][j].removeAllElements();
				} 
				catch (NullPointerException npe) { };
			}
		}
	}

	/**setzt farbigen Text in die Statuszeile*/
	void setStatusRow(Color color, String text) {
		statusRow.setForeground(color);
		statusRow.setText(text);
	}

	/** zeigt ein Hilfe-Fenster an*/
	private void showHelpDialog() {
		if (!helpPanelVisible) { // TODO Visibility in Help-Panel selber lösen??
			helpPanel = new HelpPanel();
			helpPanel.parent = this;
			helpPanel.setBounds(getBounds().x + getBounds().width/6, getBounds().y + getBounds().height/6, 
				getBounds().width*7/6, getBounds().height*3/2);
			helpPanel.validate();

			// So we only create one search panel.
			helpPanelVisible = true;
		} else {
			helpPanel.show();
		}
	}

	/** fügt eine Beispiel Sudoku-Matrix (aus der Klasse SudokuExamples) in die Tabelle ein*/
	void fillSudoku (int[] values) {
		for (int i=0; i<81; i++) {
			if (values[i] != 0) {
				stf[i/9][i%9].setText("" + values[i]);
			} else {
				stf[i/9][i%9].setText("");
			}
		}
	}

	/** speichert die Werte aus dem Sudoku für eine spätere Verwendung zwischen*/
	void shadowSudoku() { //TODO all diese Funktionen sollten mind. raus in z.B. SudokuTools
											// dort dann z.B. shadow auch speichern (static??)
		for (int i=0; i<81; i++) {
			try {
				shadow[i] = Integer.parseInt(stf[i/9][i%9].getText());
			} catch (NumberFormatException e) {
				shadow[i] = 0;
			}
		}
		setStatusRow(SC.DARKGREEN, "Werte wurden zur späteren Verwendung gemerkt!");
	}

	/** fügt die zwischengespeicherten Werte wieder in die Matrix ein*/
	void fillinShadow() {
		fillSudoku(shadow);
	}

	/** initialisiert (bei Start) die Felder des Sudokus nach Vorgaben aus dem Table der Oberfläche - 
	 * In dieser Funktion eine interne Buffer-Matrix mit den Werte der Textfields aus der Oberfläche 
	 * befüllt
	 * @return: true, wenn Sudoku korrekt, andernfalls false*/
	boolean initFields() {
		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				stf[i][j].setBackground(Color.WHITE);
				stf[i][j].setBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY, SC.boarderWidth));				
				try {
					buffer[i][j] = Integer.parseInt(stf[i][j].getText()); // Prüfung 0-9 im Konstruktor 
				} catch (NumberFormatException e) {
					buffer[i][j] = 0;
					stf[i][j].setText("");
				}
			}
		}
		if ( getFieldsAtFirst(buffer) ) {
			lockAndColorizeFields();
			return true;
		} else return false;
	}

	/** hier wird mit der Buffer-Matrix (int) eine Matrix aus SudokuItems ... gebildet
	* @return: true, wenn Sudoku korrekt, andernfalls false*/
	boolean getFieldsAtFirst( int my[][] ) {
		unknown=0; possibilities=0;
		harakiri = 0;
		tryModeOnceActivated = false; countOfTryModes = 0;
		firstTimeCountOfSolvesInTry = 0;

		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				sudoku[i][j] = new SudokuItem ( my[i][j] ); // Prüfung 0-9 im Konstruktor 

				if ( ! sudoku[i][j].isKnown() ) { // besser als / in Klasse unsolved impl.
					unknown++; possibilities += 9;
					sv[i][j] = new Vector(9);
					for (int k = 1; k < 10; k++)
						sv[i][j].addElement(new Integer (k));
				}
			}
		}
		
		if (! pruefeSudoku (sudoku)) {
			setStatusRow (SC.DARKRED, "Einige Felder sind nicht korrekt befüllt.");
			return false;
		} 

		if (unknown < 1) return true;
		initVectors();

		TryAndErrorTester tae = new TryAndErrorTester(sudoku, sv);

		if (tae.getSolution() != 0) {
			if (tae.getCountOfSolutionsWithMax100() > 1) {
				setStatusRow (SC.DARKRED, 
				"Es gibt mehrere Lösungen dieses Sudokus. Bitte weiter einschränken!");
				return false;
			}
		} else { 
			setStatusRow(SC.DARKRED, "Es gibt keine Lösung für dieses Sudoku!");
			return false;
		}

		sudokuJustStarted = false; solved = false;
		lockAndColorizeFields();
		setStatusRow (Color.BLACK, 
			"Leere Felder gibt es noch: " + unknown + " mit " + possibilities + " Einsetzungsmöglichkeiten.");
		return true;
	}

	/** verriegelt die Felder und färbt sie ein*/
	private void lockAndColorizeFields() {
		
		for (int zeile=0; zeile<9; zeile++) {
			for (int spalte=0; spalte<9; spalte++) {
				if (sudoku[zeile][spalte].wasGiven()) {
					stf[zeile][spalte].setBackground(SC.LIGHTGREEN); // weil Hintergrund in Linux niocht mehr funktioniert ein Versuch nur die boarder zu färben
					stf[zeile][spalte].setBorder(BorderFactory.createLineBorder(SC.LIGHTGREEN, SC.boarderWidth));
					stf[zeile][spalte].setEditable(false);
				}
			} 
		}
	}

	/** bereinigt am Beginn des Sudokus die gegebenen Zahlen aus Möglichkeits-Vektoren*/
	private void initVectors() {
		for (int zeile=0; zeile<9; zeile++) {
			for (int spalte=0; spalte<9; spalte++) {

				if ( sudoku[zeile][spalte].isKnown() ) continue;

				// Zeile & Spalte
				for (int i=0; i<9; i++) {
					bereinige (zeile, spalte, sudoku[zeile][i].getValue());
					bereinige (zeile, spalte, sudoku[i][spalte].getValue());
				}

				// Quadrat
				int tripleX = ( zeile/3 ) *3; int tripleY = ( spalte/3 ) *3;
				for (int j1=0; j1<3; j1++)
					for (int j2=0; j2<3; j2++)
						bereinige (zeile, spalte, sudoku[tripleX+j1][tripleY+j2].getValue());

			}
		}
	}

	/** liest beim Spielen das Feld ein und prüft es*/
	private void getFields() {
		
		if ( solved ) return;

		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				try {

					int buffer = Integer.parseInt (stf[i][j].getText());
					if ( (buffer < 1) || (buffer > 9) ) throw new NumberFormatException(); // Prüfung 0-9 hier, da kein Konstruktor verwendet
					if ( buffer != (sudoku[i][j].getValue()) ) {
						sudoku[i][j].setValue(buffer);
						TryAndErrorTester tae = new TryAndErrorTester(sudoku, sv);
						if (tae.isAnySolution(i, j, buffer))
							setFieldSolved (i, j, sv[i][j].size(), new Integer(buffer));
						else {
							//	pruefeSudoku (sudoku);
							setStatusRow (SC.DARKRED, "Feld ist nicht korrekt befüllt.");
							stf[i][j].setBackground(SC.SIGNALRED);
							stf[i][j].setBorder(BorderFactory.createLineBorder(SC.SIGNALRED, SC.boarderWidth));
							return;
						} 
					}
 
				} catch (NumberFormatException e) {
					sudoku[i][j].setComment ( stf[i][j].getText() );
					if (! sudoku[i][j].isComment()) {
						stf[i][j].setText("");
						float f = calculateColor ( sv[i][j].size() );
						stf[i][j].setBackground(new Color (1f, f, f));
						stf[i][j].setBorder(BorderFactory.createLineBorder(new Color (1f, f, f), SC.boarderWidth));
					} 
				}
			}
		}
		
		lockAndColorizeFields();
		jcheck.setSelected(false);

		for (int i = 0; i < 9; i++) {
			for (int j = 0; j < 9; j++) {
				if (sudoku[i][j].isComment()) {
					stf[i][j].setBackground(SC.LIGHTBLUE);
					stf[i][j].setBorder(BorderFactory.createLineBorder(SC.LIGHTBLUE, SC.boarderWidth));					
				}
				
			}
		}
		
		setStatusRow (Color.BLACK, 
			"Leere Felder gibt es noch: " + unknown + " mit " + possibilities + " Einsetzungsmöglichkeiten.");
		
		if (unknown == 0) showAfterSolving();
	}

	/** startet das vollständige (nicht schrittweise) Lösen des Sudokus*/
	void startSolving() {
		
		if (solving == null) {
			solving = new Thread (this);
			solving.start();
		} else {
			try {
				solving.interrupt();
				solving = null;
			} catch (Exception ex) { }
		}
	}

	/** löst im thread das Sudoku vollständig (nicht schrittweise)*/
	@Override
	public void run() {
		solved = false; harakiri = 0;
		mode = SOLVEMODE;

		if (jcheck.isSelected() == true) eraseVectors();

		while ( (solving != null) && (solved == false) ) {
			
			if (unknown < 1) solved = true;
			
			int zeile = r.nextInt(9); // mit for-Schleifen wäre es noch schneller, das ist aber schöner
			int spalte = r.nextInt(9);
			
			if ( sudoku[zeile][spalte].isNotInitialized() ) {
				setStatusRow(SC.DARKRED, "Werte noch nicht initialisiert - Vor Lösen bitte auf WERTE EINLESEN klicken!");
				solved = true;
				try {solving.interrupt(); solving = null;
				} catch (Exception ex) { }
				jtbtn.setSelected(false);
				jcheck.setSelected(false); // falls man während lösen das angewählt hatte
		
				return;
			}
			
			if ( sudoku[zeile][spalte].isKnown() ) continue;
		
			runSolve(zeile, spalte);
		}
		showAfterSolving();
	}

	/** startet das schrittweise Lösen des Sudokus*/
	void startSolvingStepwise() {
		
		solveStep = false; solved = false; harakiri = 0;
		mode = SOLVEMODE;

		if (jcheck.isSelected() == true) eraseVectors();

		while (solved == false) {

			if (unknown < 1) solved = true;
			
			int zeile = r.nextInt(9);
			int spalte = r.nextInt(9);

			if ( sudoku[zeile][spalte].isKnown() ) continue;
			
			runSolve(zeile, spalte);
			
			if (solveStep) break; 
		}
		showAfterSolving();
	}

	/** Diese Funktion verwaltet die Lösealgorithmen verschiedener Art (und Qalität) und 
	 * ruft sie nach Erfordernis auf.*/
	private void runSolve (int zeile, int spalte) {

		harakiri ++;
		solve (zeile, spalte);
			
		if ( (harakiri == 200) && (unknown > 0) ) {
			mode = TRYMODE; countOfTryModes++;
		}
		
		if (mode == TRYMODE) {
			if (SC.debug) {
				pruefeSudoku(sudoku);
				writeVectors();
			}
			TryAndErrorTester tae = new TryAndErrorTester(sudoku, sv);

			if (tae.getSolution() != 0) { // sofort anwendbar ohne taeSolve-Aufruf, Lösung gefunden
				zbuff = tae.getRowOfSolution(); sbuff = tae.getColumnOfSolution();
				Integer buffer = new Integer (tae.getSolution());
				setFieldSolved(zbuff, sbuff, sv[zbuff][sbuff].size(), buffer);
			} else { 
				if ( (unknown > 0) && (unknown < 81) )
					setStatusRow(SC.DARKRED, "Es gibt keine Lösung für dieses Sudoku!");
				solved = true;
			}

			if ( ! tryModeOnceActivated) {
				tryModeOnceActivated = true;
				firstTimeCountOfSolvesInTry = tae.getCountOfSolutionsWithMax100();
			}

		}
	}

	private void showAfterSolving () {

		if (unknown == 0) {
			try {Thread.sleep(500);} // nach Lösen Ergebnis anzeigen, davor Pause
				catch (InterruptedException ie) {};
			
			if (firstTimeCountOfSolvesInTry > 99) setStatusRow (SC.DARKRED, 
				"Es gibt mehr als 100 Lösungen. Diese ist nur exemplarisch! ( " +
				countOfTryModes + "x Testen verwendet.)");
			else if (firstTimeCountOfSolvesInTry > 1) setStatusRow (SC.DARKRED, 
				"Es gibt "+ firstTimeCountOfSolvesInTry +" Lösungen. Diese ist nur exemplarisch! ( " +
				countOfTryModes + "x Testen verwendet.)");
			else if (firstTimeCountOfSolvesInTry >= 0) {
				if (tryModeOnceActivated) setStatusRow (SC.DARKGREEN,
					"Es gibt genau eine Lösung dieses Sudokus. Das Sudoku ist damit korrekt! ( " +
					countOfTryModes + "x Testen verwendet.)");
				else setStatusRow (SC.DARKGREEN,
					"Es gibt genau eine Lösung dieses Sudokus. Das Sudoku ist damit korrekt!");
			}
		}
		try {solving.interrupt(); solving = null;
		} catch (Exception ex) { }

		jtbtn.setSelected(false);
		
		if (SC.debug) { // BUG mit debuggen in Datei schreiben um, falsche Lösungen zu debuggen
			pruefeSudoku(sudoku);
			writeVectors();
		}
	}

	/**Diese Funktion implementiert den einfachsten Lösealgorithmus. Er entspricht der Art,
	 * wie ich an Sudoku-Lösen als erstes herangehe.*/ 
	private void solve(int zeile, int spalte) {
		if (harakiri > 250)	{
			solved = true;
		} 
		
		int svSize=0;
		//try {
			svSize = sv[zeile][spalte].size();
		/*} catch (NullPointerException npe) {
			setStatusRow(SC.DARKRED, "Vor Lösen bitte auf WERTE EINLESEN klicken!");
			solved = true;
			return;
		}*/

		int tripleZ = ( zeile/3 ) *3; int tripleS = ( spalte/3 ) *3; // Tripel-Ecke oben links

		if (svSize == 1) { // nur noch eine Möglichkeit, Feld gelöst
			Integer wert = (Integer) sv[zeile][spalte].get(0);
			setFieldSolved(zeile, spalte, 1, wert);
		} else {
			//  ab hier gehen die eigentlichen Löse-Algorithmen los
			for (int i=0; i < svSize; i++) {
				Integer buffer = (Integer) sv[zeile][spalte].get(i);
				
				// 1. suche einzeln vorkommende Zahlen in Vektoren einer Einheit und trage diese ein
				boolean foundSolo = false;
				if ( oneInRow(zeile, spalte, svSize, buffer) ) {foundSolo = true;} 
				else if ( oneInColumn(zeile, spalte, svSize, buffer))  {foundSolo = true;}
				else if ( oneInTriple(zeile, spalte, svSize, tripleZ, tripleS, buffer) ) {foundSolo = true;} 
					
				if (foundSolo) {
					cleanRow(zeile, buffer);
					cleanColumn(spalte, buffer);
					cleanTriple(tripleZ, tripleS, buffer);
					break;
				}
			}
			
			// 2. n Vektoren einer Sudoku-Einheit suchen mit n gleichen members, dann können diese 
			// members aus anderen Vektoren derselben Sudoku-Einheit gestrichen werden (z.B. n=2),
			// wenn 3er Vektoren reichen muss der 3. nur 2 Zahlen enthalten un es geht trotzdem
			// erstmal für 2-er Vektoren impl.
			svSize = sv[zeile][spalte].size(); // neu lesen, wegen Bereinigung
			if (sv[zeile][spalte].size()==2) { // if:svSize ist doppelt in den Funktionen nochmal (egal)
				sameDoubleVectorInRow (zeile, spalte, svSize);
				sameDoubleVectorInColumn (zeile, spalte, svSize);
				sameDoubleVectorInTriple(zeile, spalte, svSize, tripleZ, tripleS);
			}
		}
		
	}

	/** Vectors auf Einzelmeldungen hin absuchen.*/
	private boolean oneInRow (int zeile, int spalte, int svSize, Integer buffer) {
		int found=0;
		for (int s=0; s<9; s++) {
			if (sudoku[zeile][s].isKnown() ) continue;
			if ( sv[zeile][s].contains(buffer) ) {
				found++; 
			}
			if (found > 1) return false;
		}
		// if (found == 1) 1x findet er sich immer selbst (wenn s = spalte)
		showSolve (zeile, spalte, buffer.toString(), svSize, true);
		return true;
	}

	private boolean oneInColumn (int zeile, int spalte, int svSize, Integer buffer) {
		int found=0;
		for (int z=0; z<9; z++) {
			if (sudoku[z][spalte].isKnown() ) continue;
			if ( sv[z][spalte].contains(buffer) ) {
				found++; 
			}
			if (found > 1) return false;
		}
		showSolve (zeile, spalte, buffer.toString(), svSize, true);
		return true;
	}

	private boolean oneInTriple (int zeile, int spalte, int svSize, int xCorner, int yCorner, Integer buffer) {
		int found=0;
		for (int j1 = 0; j1 < 3; j1++) {
			for (int j2 = 0; j2 < 3; j2++) {
				if (sudoku[xCorner + j1][yCorner + j2].isKnown() ) continue;
				if ( sv[xCorner + j1][yCorner + j2].contains(buffer) ) {
					found++; 
				}
				if (found > 1) return false;
			}
		}
		showSolve (zeile, spalte, buffer.toString(), svSize, true);
		return true;
	}

	/** Wert value aus der Zeile row löschen (wenn vorhanden). */
	private void cleanRow (int row, Integer value) {
		for (int i=0; i<9; i++) {
			if (sudoku[row][i].isKnown() ) continue;
			bereinige (row, i, value);
		}
	}
			
	private void cleanColumn (int column, Integer value) {
		for (int i=0; i<9; i++) {
			if (sudoku[i][column].isKnown() ) continue;
			bereinige (i, column, value );
		}
	}
			
	private void cleanTriple (int z, int s, Integer value) {
		for (int j1 = 0; j1 < 3; j1++) {
			for (int j2 = 0; j2 < 3; j2++) {
				if (sudoku[z + j1][s + j2].isKnown() ) continue;
				bereinige(z + j1, s + j2, value);
			}
		}
	}

	/** Vector auf Gleichheit prüfen, wenn ja andere Vektoren bereinigen
	 *  erstmal nur für 2er Vektoren	 */
	private void sameDoubleVectorInRow (int zeile, int spalte, int svSize) {
		int found=0;
		for (int s=0; s<9; s++) {
			if (s == spalte) continue;
			if (sudoku[zeile][s].isKnown() ) continue;
			if ( (svSize ==2) && (sv[zeile][s].equals(sv[zeile][spalte])) ) found++;
			if (found > 0) {
				for (int i=0; i<9; i++) {
					if (i == spalte) continue; // aktive Zelle NICHT bereinigen
					if (i == s) continue; // gefundenes Double NICHT bereinigen
					if (sudoku[zeile][i].isKnown() ) continue;

					bereinige (zeile, i, (Integer) sv[zeile][spalte].get(0));
					bereinige (zeile, i, (Integer) sv[zeile][spalte].get(1));
				}
				break; // Such-Schleife kann bei einmaligem Fund abgebrochen werden
			}
		}
	}

	private void sameDoubleVectorInColumn (int zeile, int spalte, int svSize) {
		int found=0;
		for (int z=0; z<9; z++) {
			if (z == zeile) continue;
			if (sudoku[z][spalte].isKnown() ) continue;
			if ( (svSize ==2) && (sv[z][spalte].equals(sv[zeile][spalte])) ) found++;
			if (found > 0) {
				for (int i=0; i<9; i++) {
					if (i == zeile) continue; // aktive Zelle NICHT bereinigen
					if (i == z) continue; // gefundenes Double NICHT bereinigen
					if (sudoku[i][spalte].isKnown() ) continue;

					bereinige (i, spalte, (Integer) sv[zeile][spalte].get(0));
					bereinige (i, spalte, (Integer) sv[zeile][spalte].get(1));
				}
				break; // Such-Schleife kann bei einmaligem Fund abgebrochen werden
			}
		}
	}

	/** sucht 2er-Vektoren durch ...	 */
	private void sameDoubleVectorInTriple (int zeile, int spalte, int svSize, int zc, int sc) {
		int found=0;
		for (int z = 0; z < 3; z++) {
			for (int s = 0; s < 3; s++) {
				if ( ((zc+z) == zeile) && ((sc+s) == spalte) ) continue;
				if (sudoku[zc+z][sc+s].isKnown() ) continue;
				if ( (svSize ==2) && (sv[zc+z][sc+s].equals(sv[zeile][spalte])) ) found++;
				if (found > 0) {
					for (int zi = 0; zi < 3; zi++) {
						for (int si = 0; si < 3; si++) {
							int zw = zc+zi; int sw = sc+si; // Zelleadresse im Triple vorab rechnen (Performance)
							if ( (zw == zeile) && (sw == spalte) ) continue; // aktive Zelle NICHT bereinigen
							if ( (zw == (zc+z)) && (sw == (sc+s)) ) continue; // gefundenes Double NICHT bereinigen
							if (sudoku[zc+zi][sc+si].isKnown() ) continue;

							bereinige (zw, sw, (Integer) sv[zeile][spalte].get(0));
							bereinige (zw, sw, (Integer) sv[zeile][spalte].get(1));
						}
					}
					return; // Such-Schleife kann bei einmaligem Fund abgebrochen werden, 
								 // hier return wg. Doppelschleife
				}
			}
		}
	}

	/** Alle Arbeiten, wenn ein Feld gelöst wurde, werden hier gemacht. */
	private void setFieldSolved(int zeile, int spalte, int possibilitiesLost, Integer wert) {

		int tripleZ = ( zeile/3 ) *3; int tripleS = ( spalte/3 ) *3; // Tripel-Ecke oben links

		showSolve(zeile, spalte, wert.toString(), possibilitiesLost, true);
		cleanRow(zeile, wert); // Wert entfernen aus Zeile, Spalte, Tripel
		cleanColumn(spalte, wert);
		cleanTriple (tripleZ, tripleS, wert);
	}

	/** Zeigt ein gelöstes Feld an*/
	private void showSolve(int zeile, int spalte, String wert, int possibilitiesLost, boolean valid) {

		if ( (stepZ>=0) && (stepS>=0) ) stf[stepZ][stepS].setBackground(Color.WHITE);
		stf[zeile][spalte].setBackground(SC.LIGHTYELLOW);
		stf[zeile][spalte].setBorder(BorderFactory.createLineBorder(SC.LIGHTYELLOW, SC.boarderWidth));
		stepZ=zeile; stepS=spalte;
		
		if (valid) {
			try {Thread.sleep(150);} // künstlich langsamer gemacht zu Show-Zwecken :-)
				catch (InterruptedException ie) {};
			
			sv[zeile][spalte].clear();
			unknown--; 
			possibilities -= possibilitiesLost;
			harakiri = 0;
			solveStep=true;
			mode = SOLVEMODE;
			setStatusRow(Color.BLACK, 
				"Leere Felder gibt es noch: " + unknown + " mit " + possibilities + " Einsetzungsmöglichkeiten.");
		}

		stf[zeile][spalte].setText( wert );
		sudoku[zeile][spalte].setValue( Integer.parseInt(wert) );
	}

	/** Das Löschen der Werte passiert hier drin.*/
	private void bereinige (int zeile, int spalte, int wert) {
		if (wert != 0) {
			boolean check = sv[zeile][spalte].remove( new Integer (wert) );
			if (check) {
				possibilities--;
			} 
		}
		float f = calculateColor ( sv[zeile][spalte].size() );
		// stf[zeile][spalte].setOpaque(true); war ein Versuch hat aber nichts gebracht
		stf[zeile][spalte].setBackground(new Color (1f, f, f));
		stf[zeile][spalte].setBorder(BorderFactory.createLineBorder(new Color (1f, f, f), SC.boarderWidth));
	}
	
	private void bereinige (int zeile, int spalte, Integer wert) {
		if (wert.intValue() != 0) {
			boolean check = sv[zeile][spalte].remove(wert);
			if (check) {
				possibilities--;
			} 
		}
		float f = calculateColor ( sv[zeile][spalte].size() );
		// stf[zeile][spalte].setOpaque(true);
		stf[zeile][spalte].setBackground(new Color (1f, f, f));
		stf[zeile][spalte].setBorder(BorderFactory.createLineBorder(new Color (1f, f, f), SC.boarderWidth));
	}

	private boolean pruefeSudoku( SudokuItem matrix[][]) {
		
		boolean status = true;
		 
		if ( ! proofRows(matrix) ) status = false;
		if ( ! proofColumns(matrix) ) status = false;
		if ( ! proofTriples(matrix) ) status = false;
		
		return status;
	}
		
		
	private boolean proofRows( SudokuItem matrix[][]) {
		Vector checker = new Vector(9);
		boolean okay = true;

		for (int r = 0; r < 9; r++) { // zeilenweise
			for (int c = 0; c < 9; c++) {
				if ( matrix[r][c].isKnown() )
					checker.add(new Integer (matrix[r][c].getValue()));
			}
			for (int wert=1; wert<10; wert++) {
				if ( checker.indexOf(new Integer(wert)) != checker.lastIndexOf(new Integer(wert))) {
					for (int c=0;c<9; c++)
						if (matrix[r][c].getValue() == wert) {
							stf[r][c].setBackground(SC.SIGNALRED);
							stf[r][c].setBorder(BorderFactory.createLineBorder(SC.SIGNALRED, SC.boarderWidth));
						}
					okay = false;
				}
			}
			checker.clear();
		}
		return okay;
	}

	private boolean proofColumns( SudokuItem matrix[][]) {
		Vector checker = new Vector(9);
		boolean okay = true;

		for (int c = 0; c < 9; c++) { // spaltenweise
			for (int r = 0; r < 9; r++) {
				if ( matrix[r][c].isKnown() )
					checker.add(new Integer (matrix[r][c].getValue()));
			}
			for (int wert=1; wert<10; wert++) {
				if ( checker.indexOf(new Integer(wert)) != checker.lastIndexOf(new Integer(wert))) {
					for (int r=0;r<9; r++)
						if (matrix[r][c].getValue() == wert) {
							stf[r][c].setBackground(SC.SIGNALRED);
							stf[r][c].setBorder(BorderFactory.createLineBorder(SC.SIGNALRED, SC.boarderWidth));
						}
					okay = false;
				}
			}
			checker.clear();
		}
		return okay;
	}

	private boolean proofTriples( SudokuItem matrix[][]) {
		Vector checker = new Vector(9);
		boolean okay = true;

		for (int br = 0; br < 3; br++) { // triple-weise
			for (int bc = 0; bc < 3; bc++) {
				for (int r = 0; r < 3; r++) {
					for (int c = 0; c < 3; c++) {
						if ( matrix[br*3+r][bc*3+c].isKnown() )
							checker.add(new Integer (matrix[br*3+r][bc*3+c].getValue()));
					}
				}
				for (int wert=1; wert<10; wert++) {
					if ( checker.indexOf(new Integer(wert)) != checker.lastIndexOf(new Integer(wert))) { 
						for (int r=0;r<3; r++)
							for (int c=0;c<3; c++)
								if (matrix[br*3+r][bc*3+c].getValue() == wert) {
									stf[r][c].setBackground(SC.SIGNALRED);
									stf[r][c].setBorder(BorderFactory.createLineBorder(SC.SIGNALRED, SC.boarderWidth));
								}
						okay = false;
					}
				}
				checker.clear();
			}
		}
		return okay;
	}

	void writeVectors() {
		
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				if ( ! sudoku[r][c].isKnown() )
					stf[r][c].setText (sv[r][c].toString());
			}
		}
	}

	void eraseVectors() {
		
		for (int r = 0; r < 9; r++) {
			for (int c = 0; c < 9; c++) {
				if ( ! sudoku[r][c].isKnown() ) {
					stf[r][c].setText ("");
					if ( sudoku[r][c].isComment() ) stf[r][c].setText (sudoku[r][c].getComment());
				}
			}
		}
		jcheck.setSelected (false); 
	}

	/** Diese Funktion ist für die optimierte Farbgestaltung der unbekannt-Felder verantwortlich: 
	 * Um den Nullpunkt zu heben, und die Wurzel um anfänglicher schneller zu steigen und 
	 * am Schluss weniger stark zu steigen: 
	 * Formel: f(size)=(sqrt(8-(size-1))/y + 1/5) * 5/6; 
	 * size kann 1-9 sein; y muss größer 8 sein, desto größer über 8, desto besser ist weiss von 
	 * size=1 (hellstrot) zu unterscheiden, aber Vorsicht size=8-9 werden damit wieder schlechter 
	 * unterscheidbar*/
	private float calculateColor (int x) {
		float y = (float) ( ( (Math.sqrt ((9f-x) / 9.2f) ) + 1f/5 ) * 5f/6);
		return y;
	}

	/** Generiert ein zufälliges (eigenes) Sudoku.*/
	synchronized void  generateSudoku () {
		trapezHöhe = 0;
		SGStarter sgs = new SGStarter();
		sgs.start (generatedMatrix, this);
	}

	/** Farbliche Animation beim Rückkehren aus dem Sudoku-Generator. Diese Methode baut 
	 * das Trapez auf! 
	 * Sie wird beim Löschen der Zahlen zum Schluss eingesetzt, da Java hier etwas unterwegs 
	 * sein kann, um den Nutzer zu zeigen, dass die Maschine noch lebt. :-) 
	 * Die Methode wird aus dem Generator selber aufgerufen, was nicht optimal ist (Durchgriff).
	 * - Mal bei Gelegenheit ändern ...*/
	public void startAnimation () {
		if (trapezHöhe > 8) return;
		if (trapezHöhe < 0) return;
		int posY= trapezHöhe, breite = 0;
		Color randomColor = new Color (0.7f+r.nextFloat()/4, 0.7f+r.nextFloat()/4, 0.7f+r.nextFloat()/4);

		for (; posY>=0; breite++, posY--) { // Trapez zeichnen
			if ( (4-posY) >= 0) {
				if ( (4-breite) >= 0) {
					stf[4-posY][4-breite].setBackground(randomColor);
					stf[4-posY][4-breite].setBorder(BorderFactory.createLineBorder(randomColor, SC.boarderWidth));
					stf[4-posY][4+breite].setBackground(randomColor);
					stf[4-posY][4+breite].setBorder(BorderFactory.createLineBorder(randomColor, SC.boarderWidth));
					stf[4+posY][4-breite].setBackground(randomColor);
					stf[4+posY][4-breite].setBorder(BorderFactory.createLineBorder(randomColor, SC.boarderWidth));
					stf[4+posY][4+breite].setBackground(randomColor);
					stf[4+posY][4+breite].setBorder(BorderFactory.createLineBorder(randomColor, SC.boarderWidth));
				}
			}
		}
		trapezHöhe++;
	}

	/** Farbliche Animation beim Rückkehren aus dem Sudoku-Generator. Diese Methode 
	 * baut das Trapez wieder ab! Die Methode wird aus dem Generator selber 
	 * aufgerufen, was nicht optimal ist (Durchgriff). - Mal bei Gelegenheit ändern ...
	 * */
	public void endAnimation() {
		trapezHöhe--;
		if (trapezHöhe > 8) return;
		if (trapezHöhe < 0) return;
		int posY= trapezHöhe, breite = 0;

		for (; posY>=0; breite++, posY--) {
			if ( (4-posY) >= 0) {
				if ( (4-breite) >= 0) {
					stf[4-posY][4-breite].setBackground(Color.WHITE);
					stf[4-posY][4-breite].setBorder(BorderFactory.createLineBorder(Color.WHITE, SC.boarderWidth));
					stf[4-posY][4+breite].setBackground(Color.WHITE);
					stf[4-posY][4+breite].setBorder(BorderFactory.createLineBorder(Color.WHITE, SC.boarderWidth));
					stf[4+posY][4-breite].setBackground(Color.WHITE);
					stf[4+posY][4-breite].setBorder(BorderFactory.createLineBorder(Color.WHITE, SC.boarderWidth));
					stf[4+posY][4+breite].setBackground(Color.WHITE);
					stf[4+posY][4+breite].setBorder(BorderFactory.createLineBorder(Color.WHITE, SC.boarderWidth));
				}
			}
		}
	}
	
	/** Das war mal eine andere Animation des SudokuGenerators. Felder werden wild colorisiert
	 * und mit zufälligen Zahlen befüllt. Kann vor trapez-Darstellung kommen, war mir aber zuviel ...*/
	public void animateColoredMatrix () {
		int ze = r.nextInt(9), sp = r.nextInt(9);
		stf[ze][sp].setText("" + (r.nextInt(9)+1));
		stf[ze][sp].setBackground( 
			new Color (0.75f+r.nextFloat()/4, 0.75f+r.nextFloat()/4, 0.75f+r.nextFloat()/4) );
	}
	
	/** Hiermit sollte die animateColoredMatrix-Animation wieder allmählich zurückgenommen 
	 * werden. Felder werden geleert und geweißt. ...*/
	public void animateWhiteMatrix () {
		int ze = r.nextInt(9), sp = r.nextInt(9);
		stf[ze][sp].setText("" + (r.nextInt(9)+1));
		stf[ze][sp].setBackground( Color.WHITE );
	}

	/** Fügt die Werte des generierten Sudokus in die Matrix-Zellen ein.
	 * TODO: vereinheitlichen mit Matrix in fillSudoku (1- oder 2-dimensional) und dann 
	 * zu einer Funktion zusammenfassen*/ 
	public void fillGeneratedSudoku () {
		resetSudoku();
		for (int i=0; i<9; i++) {
			for (int j=0; j<9; j++) {
				if (generatedMatrix[i][j] != 0) {
					stf[i][j].setText("" + generatedMatrix[i][j]);
				} else {
					stf[i][j].setText("");
				}
			}
		}
	}



}