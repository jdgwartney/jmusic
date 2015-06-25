/*
 
 <This Java Class is part of the jMusic API version 1.5, March 2004.>
 
 Copyright (C) 2000 Andrew Sorensen & Andrew Brown
 
 This program is free software; you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation; either version 2 of the License, or any
 later version.
 
 This program is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 GNU General Public License for more details.
 
 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.

 */ 

package jm.util;

import jm.midi.MidiSynth;
import jm.music.data.*;
import jm.JMC;
import jm.audio.*;
//import javax.sound.midi.InvalidMidiDataException;
import javax.sound.sampled.*;
import java.io.File;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Vector;

/*  Enhanced by the Derryn McMaster 2003 */
/*  Further updated by Andrew R Brown 2011 and 2012*/

public class Play implements JMC {
	/**
     * True at the index of a midiCycle that is currently playing
     */
	private static Vector cyclePlaying = new Vector();
    /** A thread started to time the duration of playbnack */
    private static Thread pauseThread;
	/** A instance of the jMusic's JavaSound MIDI playback class */
	private static Vector ms = new Vector();
	// for reading audio files for playback
	private static AudioInputStream audioInputStream;
	// mixer for real time audio playback
	private static Vector mixerList = new Vector();
	/** A flag for real time audio currently playing */
	private static boolean audioPlaying = false;
	/** A flag to indicate audio playback has been initiated but halted */
	private static boolean audioPaused = false;
	//
	private static RTMixer mixer;
	private static MidiSynth midiSynth1 = new MidiSynth();
	private static MidiSynth midiSynth2 = new MidiSynth();
	private static int msCnt = 0;
	
	/**
     * Constructor
     */
	public Play() {	}
	
	private static void msFill(int numb){
		if(ms.size() < numb) {
			for(int i=ms.size(); i<numb; i++){
				ms.add(new MidiSynth());
			}
		}
	}
	/**
     * Used by infinite cycle player threads to check
     * cyclePlaying status.
     */
	public static boolean cycleIsPlaying(){
		return cycleIsPlaying(0);
	}
	
	/**
	* Used by infinite cycle player threads to check
	* cyclePlaying status.
	* @index The id of the midiCycle to check.
	*/
	public static boolean cycleIsPlaying(int index){
		//System.out.println("in cycleIsPlaying(int index)");
		if (cyclePlaying.size() < index + 1) {
			for(int i=cyclePlaying.size(); i < index + 1; i++) {
				cyclePlaying.addElement(false);
			}
			cyclePlaying.add(index, false);
		}
		return ((Boolean) cyclePlaying.elementAt(index)).booleanValue();
	}
	
    /**
     * Thread.sleeps for a period of 1 score play length
     * (i.e. the time it would take for the specified 
     * score to play).
     * Can be used in conjunction with midiCycle() if the 
     * score requires a re-compute just before being
     * replayed.  (i.e. sleeps for one loop)  Should 
     * be placed immediately after the Play.midiCycle() 
     * command to ensure that the time waited is for 
     * the currently playing score, and not subject 
     * to any changes made to the score since.
     * @param score The score used for timing the sleep.
     */
	public static void waitCycle(Score score, int decayAllowance){
		try{
			Thread.sleep((int)(1000.0 * 60.0 / score.getTempo() * score.getEndTime())
				+ decayAllowance);
		}catch(Exception e){e.printStackTrace();}
	}
	/**
     * Playback a MIDI file from disk.
     * @param fileName The name of the file to play back.
     */
    public static void mid(String fileName) {
        Score score = new Score();
        Read.midi(score, fileName);
        Play.midi(score);
    }
	
	/**
     * Playback the jMusic score JavaSound MIDI
     * @param Note The note to be played
     */
	public static void midi(Note n) {
		//System.out.println("in midi(Note n)");
        midi(n, true);
	}	
    
	/**
     * Playback the jMusic score JavaSound MIDI
     * @param phr The Phrase to be played
     */
	public static void midi(Phrase phr) {
		//System.out.println("in midi(Phrase phr)");
        midi(phr, true);
	}
    
	/**
     * Playback the jMusic score JavaSound MIDI
     * @param part The Part to be played
     */
	public static void midi(Part part) {
        midi(part, true);
	}
		
	/**
     * Playback the jMusic score JavaSound MIDI using the default value of 
     * true for 'exit' - See Play.midi(Score,boolean)
     * @param score The score to be played.
     */ 
	public static void midi(Score score) {
		midi(score, true);
	}
	
    /**
     * Playback the jMusic score JavaSound MIDI
     * @param n The note to be played
     * @param exit Hold program open for the duration of playback, then close ready to exit? true or false. 
     */
	public static void midi(Note n, boolean exit) {
		//System.out.println("in midi(Note n, boolean exit)");
		Score s = new Score("One note score", 60);
        s.addPart(new Part(new Phrase(n)));
        midi(s, exit);
	}	
    
	/**
     * Playback the jMusic score JavaSound MIDI
     * @param phr The Phrase to be played
     * @param exit Hold program open for the duration of playback, then close ready to exit? true or false. 
     */
	public static void midi(Phrase phr, boolean exit) {
		double tempo = 60;
		if(phr.getTempo() != Phrase.DEFAULT_TEMPO) tempo = phr.getTempo();
		Score s = new Score(phr.getTitle() + " score", tempo);
        if (phr.getTempo() != Phrase.DEFAULT_TEMPO) s.setTempo(phr.getTempo());
        s.addPart(new Part(phr));
        midi(s,exit);
	}
    
	/**
     * Playback the jMusic score JavaSound MIDI
     * @param p The Part to be played
     * @param exit Hold program open for the duration of playback, then close ready to exit? true or false. 
     */
	public static void midi(Part p, boolean exit) {
		double tempo = 60;
		if(p.getTempo() != Part.DEFAULT_TEMPO) tempo = p.getTempo();
		Score s = new Score(p.getTitle() + " score", tempo);
        if (p.getTempo() != Part.DEFAULT_TEMPO) s.setTempo(p.getTempo());
        s.addPart(p);
        midi(s,exit);
	}
	
    /**
     * Playback the jMusic score JavaSound MIDI.
     * This method exits the application on completion.
     * To avoid this exit call, pass false as the second argument.
     * @param score The score to be played.
     * @param exit Hold program open for the duration of playback, then close ready to exit? true or false. 
     */ 
	public static void midi(Score score, boolean exit) {
		//System.out.println("in midi(Score s, boolean exit)");
		midi(score, exit, true, 1, 0);
	}
		
 	/**
     * Playback the jMusic score JavaSound MIDI.
     * This method can exit the application on completion id the exit argument is true.
     * To avoid this exit call, pass false as the second argument.
	* To have jMusic hold up the program while music plays set wait to true,
	* otherwise if the program has persistence (for example a GUI is open) 
	* wait can be set to false.
     * @param score The score to be played.
     * @param exit Close playback when done? true or false. 
	* @param wait Hold program open for the duration of playback? True of false
	* @param synths The number of the MidiSynths to use - default is 1.
     */ 
	public static void midi(Score score, boolean exit, boolean wait, int synths, int decayAllowance) {
		System.out.println("jMusic Play: Playing score " + score.getTitle() + " using JavaSound General MIDI soundbank.");
		msFill(synths); // add instances of MidSynths if required
		MidiSynth currentMidiSynth = (MidiSynth) ms.elementAt(msCnt);
		if (currentMidiSynth.isPlaying()) currentMidiSynth.stop();
		try {
			currentMidiSynth.play(score);
			if (wait) {
				System.out.println("jMusic Play: Waiting for the end of "+ score.getTitle() + ".");
				if (exit && decayAllowance == 0) { // allow reverb trail to end
					waitCycle(score, 200);
				} else waitCycle(score, decayAllowance);
			}
		} catch (Exception e) {
			System.err.println("jMusic Play: MIDI Playback Error:" + e);
			return;
		}
		if (exit) closeAll();
		msCnt = (msCnt+1) % synths;
	}
	
	/**
	* Refresh the default JavaSound MIDI playback with a new score.
	* Only works when midiCycle() is operating and 
	* updates take effect at the start of the next cycle.
     * @param s The score to be used as the update.
	*/	
	public static void updateScore(Score s){
		updateScore(s, 0);
	}
	
	/**
	* Refresh the JavaSound MIDI playback with a new score.
	* Only works when midiCycle() is operating and 
	* updates take effect at the start of the next cycle.
    * @param s The score to be used as the update.
	* @param index The id of the MidiSynth to update.
	*/	
	public static void updateScore(Score s, int index){
		//System.out.println("update score. ms.size() = " + ms.size() + " index = " + index);
		try {
			((MidiSynth) ms.elementAt(index)).updateSeq(s);
		} catch (Exception e) {
			System.err.println("jMusic Play class can't update MIDI sequence:" + e);
			return;
		}
	}
	
	/**
	* Halt real time audio playback.
	*/
	public static void pauseAudio() {
		for(int i=0; i<mixerList.size(); i++) {
			((RTMixer)mixerList.elementAt(i)).pause();
		}
		audioPaused = true;
	}
	
	/**
	* Restart real time audio playback after being paused.
	*/
	public static void unPauseAudio() {
		if (audioPaused) {
			for(int i=0; i<mixerList.size(); i++) {
				((RTMixer)mixerList.elementAt(i)).unPause();
			}
			audioPaused = false;
		} else {
			System.err.println("Play.unPauseAudio error: audio playback was not previously paused.");
		}
	}
	
	/**
	* End real time audio playback immediatly.
	*/
	public static void stopAudio() {
		for(int i=0; i<mixerList.size(); i++) {
			((RTMixer)mixerList.elementAt(i)).stop();
		}
		audioPaused = false;
		audioPlaying = false;
	}
	
	/**
	* End JavaSound MIDI playback immediatly.
	* For Play.stopMidi() to be able to take effect you need to add a flag 
	* when calling Play.midi to tell it not to create a Thread that holds open 
	* the program for the duration of playback - so this assumes you have some  
	* other persistent activity in your program (such as a GUI). 
	* e.g., Play.midi(myScore, false); later... Play.midiStop();
	* Call closeAll() after stopping if ready to exit application.
	*/
    public static void stopMidi() {
		for(int i=0; i<ms.size(); i++) {
			stopMidi(i);
		}
	}

	/**
	* End JavaSound MIDI playback immediatly.
	* For Play.stopMidi() to be able to take effect you need to add a flag 
	* when calling Play.midi to tell it not to create a Thread that holds open 
	* the program for the duration of playback - so this assumes you have some  
	* other persistent activity in your program (such as a GUI). 
	* e.g., Play.midi(myScore, false); later... Play.midiStop();
	* Call closeAll() after stopping if ready to exit application.
	* @param index The id of the MidiSynth to stop.
	*/
    public static void stopMidi(int index) {
		System.out.println("jMusic Play: Stopping JavaSound MIDI playback of " + index);
		if (cyclePlaying.size() < index + 1) {
			for(int i=cyclePlaying.size(); i < index + 1; i++) {
				cyclePlaying.addElement(false);
			}
		}
		cyclePlaying.set(index, false);
		((MidiSynth) ms.elementAt(index)).stop();
    }	

	/**
     * Halt the infinite midiCycle() at the end of the next cycle.
	* Call closeAll() after stopping if ready to exit application.
     */
	public static void stopMidiCycle() {
		stopMidiCycle(0);
	}
	
	/**
	* Halt the infinite midiCycle() at the end of the next cycle.
	* Call closeAll() after stopping if ready to exit application.
	* @param index The midiCycle id to stop
	*/
	public static void stopMidiCycle(int index) {
		System.out.println("jMusic Play: Stopping cycle playback at end of next sequence");
		cyclePlaying.set(index, false);
		((MidiSynth) ms.elementAt(index)).setCycle(false);
	}
	
	/**
    * Repeated playback the jMusic note via the JavaSound MIDI synthesizer
     * @param n The note to be played. See midiCycle(Score s)
     */
	public static void midiCycle(Note n) {
		midiCycle(n, 0);
	}
	
	/**
    * Repeated playback the jMusic note via the JavaSound MIDI synthesizer
    * @param n The note to be played. See midiCycle(Score s)
	* @param index The midiCycle id to be used - default is 0.
    */
	public static void midiCycle(Note n, int index) {
		Score s = new Score("One note score");
        s.addPart(new Part(new Phrase(n)));
        midiCycle(s, index);
	}	
    
	/**
     * Repeated playback the jMusic phrase via the JavaSound MIDI synthesizer
     * @param phr The Phrase to be played. See midiCycle(Score s)
     */
	public static void midiCycle(Phrase phr) {
		midiCycle(phr, 0);
	}
	
	/**
     * Repeated playback the jMusic phrase via the JavaSound MIDI synthesizer
     * @param phr The Phrase to be played. See midiCycle(Score s)
	* @param index The midiCycle id to be used - default is 0.
     */
	public static void midiCycle(Phrase phr, int index) {
		Score s = new Score(phr.getTitle() + " score");
        s.addPart(new Part(phr));
        midiCycle(s, index);
	}
    
	/**
     * Repeated playback the jMusic part via the JavaSound MIDI synthesizer
     * @param part The Part to be played. See midiCycle(Score s)
     */
	public static void midiCycle(Part part) {
		midiCycle(part, 0);
	}
		
	/**
     * Repeated playback the jMusic part via the JavaSound MIDI synthesizer
     * @param part The Part to be played. See midiCycle(Score s)
	* @param index The midiCycle id to be used - default is 0.
     */
	public static void midiCycle(Part part, int index) {
		Score s = new Score(part.getTitle() + " score");
        s.addPart(part);
        midiCycle(s, index);
	}
    
	/**
     * Continually repeat playback of a Score object (i.e., loop playback).
	* @param score The score to played back repeatedly.
     */
	public static void midiCycle(Score score){
		midiCycle(score, 0);
	}
	
	/**
     * Continually repeat playback of a Score object (i.e., loop playback).
	* @param score The score to played back repeatedly.
     */
	public static void midiCycle(Score score, int index){
		if (ms.size() < index + 1) {
			for(int i=ms.size(); i < index + 1; i++) {
				ms.addElement(new MidiSynth());
			}
			//ms.add(index, new MidiSynth());
		}
		if (cyclePlaying.size() < index + 1) {
			for(int i=cyclePlaying.size(); i < index + 1; i++) {
				cyclePlaying.addElement(false);
			}
			//cyclePlaying.add(index, false);
		}
		if (((Boolean) cyclePlaying.elementAt(index)).booleanValue() == true) {
			stopMidiCycle(index);
			((MidiSynth) ms.elementAt(index)).stop();
		}
        cyclePlaying.set(index, true);
		System.out.println("jMusic Play: Starting cycle playback");
		try {
			((MidiSynth) ms.elementAt(index)).play(score);
			((MidiSynth) ms.elementAt(index)).setCycle(true);
		}
		catch (Exception e) {
			System.err.println("MIDI Playback Error:" + e);
			return;
		}
	}
	
    /**
     * Playback an audio file via javaSound.
     * This method requires the javax.sound packages in Java 1.3 or higher.
     * @param fileName The name of the file to be played.
     */ 
    public static void au(String fileName) {
        au(fileName, true);
    }
	
    /**
     * Playback an audio file via javaSound.
     * jMusic currently supports playback of .au, .wav and .aif file formats.
     * This method requires the javax.sound packages in Java 1.3 or higher.
     * By default this method, when complete, will continue the application. 
	* Careful that the application does not end, preventing the file from playing.
	* To keep the application open during playback pass 'false' as the autoClose argument.
     * @param filename The name of the file to be played.
     * @param autoClose A flag for exiting java after the file has played.
     */ 
    public static void au(String fileName, boolean autoClose) {
        jm.gui.wave.WaveFileReader afr = new jm.gui.wave.WaveFileReader(fileName);
        jm.music.rt.RTLine[] lineArray = {new jm.util.AudioRTLine(fileName)};	
        jm.audio.RTMixer mixer = new jm.audio.RTMixer(lineArray) ;//, 4096, si.getSampleRate(), si.getChannels(), 0.01);	
            mixer.begin();
            System.out.println("---------- Playing '" + fileName + "'... Sample rate = "
                               + afr.getSampleRate() + " Channels = " + afr.getChannels() + " ----------");
            if (autoClose) {
                java.io.File audioFile = new java.io.File(fileName);
                try {
                    int byteSize = afr.getBits() - 1;
                    // bytes, sample rate, channels, milliseconds, cautious buffer
                    Thread.sleep((int)((double)audioFile.length() / byteSize / 
                                       afr.getSampleRate() / afr.getChannels() * 1000.0));
                } catch (InterruptedException e) {
                    System.err.println("jMusic play.au error: Thread sleeping interupted");
                }
                System.out.println("-------------------- Completed Audio Playback ----------------------");
                System.exit(0); // horrid, but less confusing for beginners
            }
    }
	
	// audio file playback classes adapted from the SoundCipher library; http:soundcipher.com

   /**
     * Playback a specified audio file using JavaSound.
	* Audio files are presumed tobe in teh same folder as the program source.
     * @param fileName Name of the audio file to play.
     */
	public static void audioFile(String fileName) {
		try {
			audioInputStream = AudioSystem.getAudioInputStream(new File(fileName));//bis);
			new AudioFilePlayThread(audioInputStream).start();
			System.out.println("Playing audio file " + fileName);
		} catch (IOException ioe) {
			System.err.println("Play audioFile error: in playAudioFile(): " + ioe.getMessage());
		}
		catch (UnsupportedAudioFileException uafe) {
			System.err.println("Unsupported Audio File error: in Play.audioFile():" + uafe.getMessage());
		}
	}
    
	/**
     * Playback a note as real time audio.
     * @param note The phrase to be played.
     * @param inst An instrument to play the note with
     */
    public static void audio(Note note, Instrument inst) {
        audio(new Phrase(note), inst);
    }

    /**
     * Playback a phrase as real time audio.
     * @param phrase The phrase to be played.
     * @param insts An array of instruments to play the phrase with
     */
    public static void audio(Phrase phrase, Instrument[] insts) {
        audio(new Score(new Part(phrase)), insts);
    }
    
    /**
     * Playback a phrase as real time audio.
     * @param phrase The phrase to be played.
     * @param inst An instrument to play the phrase with
     */
    public static void audio(Phrase phrase, Instrument inst) {
        Part part = new Part(phrase);
        if(phrase.getTempo() != Phrase.DEFAULT_TEMPO) part.setTempo(phrase.getTempo());
        audio(part, new Instrument[] {inst});
    }
    
    /**
     * Playback a part as real time audio.
     * @param part The part to be played.
     * @param insts An array of instruments to play the part with
     */
    public static void audio(Part part, Instrument[] insts) {
        Score score = new Score(part);
        if(part.getTempo() != Part.DEFAULT_TEMPO) score.setTempo(part.getTempo());
        audio(score, insts);
    }

    /**
     * Playback a score as real time audio.
     * @param score The score to be played.
     * @param insts An array of instrument to play the score with
     */
    public static void audio(Score score, Instrument[] insts) {
		//System.out.println("audioPlaying = "+ audioPlaying);
		//if(audioPlaying) {
		//	stopAudio();
		//}
		audioPlaying = true;
        System.out.print("Playing Score as Audio... ");
        // make all instrument real time
        for(int i=0; i<insts.length; i++) {
            insts[i].setOutput(Instrument.REALTIME);
        }
        // get all the phrases in a vector
        java.util.Vector v = new java.util.Vector();
        for(int i=0; i<score.size(); i++) {
            Part p = score.getPart(i);
            for(int j=0; j<p.size(); j++) {
                Phrase phr = p.getPhrase(j);
                if(phr.getInstrument() == Phrase.DEFAULT_INSTRUMENT) 
                    phr.setInstrument(p.getInstrument());
                if(phr.getTempo() == Phrase.DEFAULT_TEMPO) 
                    phr.setTempo(p.getTempo());
                v.addElement(phr);
            }
        }
        // create RTPhrases for each phrase
        jm.music.rt.RTLine[] lines = new jm.music.rt.RTLine[v.size()];
        for(int i=0; i<v.size(); i++) {
            Phrase phr = (Phrase)(v.elementAt(i));
            lines[i] = new jm.music.rt.RTPhrase(phr, insts[phr.getInstrument()]);
        }        
        // create mixer and wait for the end then pause the mixer
		//RTMixer mixer = new RTMixer(lines);
		if (mixer == null) {
			mixer = new RTMixer(lines);
			mixer.begin();
		} else {
			mixer.addLines(lines);
		}
        //mixerList.add(mixer);
        //audioWait(score, mixer);
    }
    
	// Spawn a thread to keep app alive during playback
    private static void audioWait(final Score score, final RTMixer mixer) {
        pauseThread = new Thread( new Runnable() {
            public void run() {
                try {
                    pauseThread.sleep((int)(score.getEndTime() * 60.0 / score.getTempo() * 1000.0));
                } catch (Exception e) {System.out.println("jMusic Play.audioWait error in pauseThread");}
                System.out.println("Completed audio playback.");
				//mixer.pause();
				audioPaused = true;
				try {
					Thread.sleep(500); // stop abrupt cutoff buzz
				} catch (InterruptedException e) {};
				//mixer.stop();
				//mixerList.remove(mixer);
				//audioPaused = false;
				//audioPlaying = false;
            }});
        pauseThread.start();
    }
    
    /**
     * Playback an audio file using Java Applet audioclip playback.
     * A audioClip limitation is that the file must be small enough to fit into RAM.
     * This method is compatibl with Java 1.1 and higher.
     * @param fileName The name of the file to be played.
     */ 
    public static void audioClip(String fileName) {
        System.out.println("-------- Playing an audio file ----------");
        System.out.println("Loading sound into memory, please wait...");
        java.io.File audioFile = new java.io.File(fileName);
        try {
	    java.net.URI tempURI = audioFile.toURI();
	    java.net.URL tempURL = tempURI.toURL();
            java.applet.AudioClip sound = java.applet.Applet.newAudioClip(tempURL);
            System.out.println("Playing '" + fileName + "' ...");
            sound.play();
        } catch (java.net.MalformedURLException e) {
            System.err.println("jMusic play.au error: malformed URL or filename");
        }
        try {
            // bytes, channels, sample rate, milliseconds, cautious buffer
            Thread.sleep((int)(audioFile.length() / 2.0 / 44100.0 / 2.0 * 1000.0) + 1000);
        } catch (InterruptedException e) {
            System.err.println("jMusic play.au error: Thread sleeping interupted");
        }
        System.out.println("-------------------- Completed Playback ----------------------");
        System.exit(0); // horrid but less confusing for beginners
	}
	
	/** Close all open resources when finished playing altogether. Call before exiting app. */
	public static void closeAll() {
		audioInputStream = null;
		for (int i=0; i< ms.size(); i++) {
			((MidiSynth) ms.elementAt(i)).finalize();
		}
		ms.clear();
	}
}


