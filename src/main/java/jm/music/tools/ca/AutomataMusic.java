package jm.music.tools.ca;

import jm.JMC;
import jm.util.*;
import jm.music.data.*;
import jm.inst.*;

/* --------------------
 * A class for making music from Cellular Automata clculations
 * @author Andrew Troedson and Andrew Brown
 * ---------------------
 */
public class AutomataMusic implements JMC {
    static boolean[][] cells = new boolean[10][10];
    int maxNotes = 100;
    Score score = new Score("The Cellular Drunk");
    Part piano = new Part("piano", 0, 0);
    Phrase phr1 = new Phrase();
    Phrase phr2 = new Phrase();
    // jMusic audio instrument specifications
    TriangleInst triInst = new TriangleInst(44100);

    public static void main(String[] args) {
        new AutomataMusic();
    }

    public AutomataMusic() {
        int liveCellCounter;
        int noteNumber;
        //arguments are width, height, number of seed cells,and wrap around?
        CellularAutomata CA = new CellularAutomata(cells.length, cells[0].length, 0.2, false);
        cells = CA.getAllStates();
        DrawCA DCA = new DrawCA(CA, "CA Music");
        // Another window with cell size and last 2 ints as x & y pos
        DrawCA DCA2 = new DrawCA(CA, "Big Cellular Automata Music", 30, 10, 100);
        // do the iterations
        for (int i = 0; i < maxNotes; i++) {
            CA.evolve();
            cells = CA.getAllStates();
            //do jMusic
            //counts the number of live cells in the current iteration
            liveCellCounter = 0;
            for (int k = 0; k < cells.length; k++) {
                for (int j = 0; j < cells[0].length; j++) {
                    if (CA.getState(k, j) == true) {
                        liveCellCounter++;
                    }
                }
            }
            noteNumber = liveCellCounter + (127 - cells.length * cells[0].length);
            //pitch offset
            Note n = new Note(noteNumber, SQ);
            phr1.addNote(n);
            //wait a bit to make it look more pretty
            try {
                Thread.sleep(100);
                System.out.println("Iteration " + i + " of " + maxNotes);
            } catch (Exception e) {
            }
            DCA.repaint();//Andrew this is what we had wrong - repaint the DrawArea! not this object.
            DCA2.repaint();
        }
        piano.addPhrase(phr1);
        score.addPart(piano);
        Write.midi(score, "AutomataMusic.mid");
        View.show(score, 330, 10);
        Write.au(score, "AutomataMusic.au", triInst);
        System.out.println(CA.toString());
    }
}
