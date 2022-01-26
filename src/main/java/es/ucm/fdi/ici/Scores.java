package es.ucm.fdi.ici;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.Vector;

import pacman.game.util.Stats;

public class Scores {

	
	Vector<String> list_pacMan; 
	Vector<String> list_ghosts;
	Stats[][] stats;
	Vector<ScorePair> pacManRanking;
	Vector<ScorePair> ghostsRanking;
	Vector<ScorePair> globalRanking;

	public Scores(Vector<String> list_pacMan,Vector<String> list_ghosts)
	{
		this.list_pacMan = list_pacMan;
		this.list_ghosts = list_ghosts;
		stats = new Stats[list_pacMan.size()][list_ghosts.size()];
		for(int pc = 0; pc<list_pacMan.size(); pc++)
			for(int g=0; g<list_ghosts.size(); g++)
			{
				stats[pc][g] = new Stats("empty");
			}
	}

	synchronized void put(String pacMan, String ghosts, Stats score) {
		int posPacMan = list_pacMan.indexOf(pacMan);
		int posGhosts = list_ghosts.indexOf(ghosts);
		if((posPacMan ==-1)||(posGhosts==-1))
			System.err.println("error");
		stats[posPacMan][posGhosts] = score;	
		System.out.println(String.format("Scores.put %s, %s, %s",pacMan,ghosts,score.toString()));
		computeRanking();
	}
	
	public Vector<ScorePair> getMsPacManRanking() {
		return pacManRanking;
	}
	
	public Vector<ScorePair> getGhostsRanking() {
		return ghostsRanking;
	}

	public Vector<ScorePair> getGlobalRanking() {
		return globalRanking;
	}
	
	public void printScoreAndRanking()
	{
		System.out.println("Scores Table");
        for(Stats[] result_pacman : stats) {
        	for(Stats s: result_pacman)
        		System.out.print(s.getDescription()+": "+s.getAverage()+";");
        	System.out.println();
        }      
        System.out.println("MsPacMan Ranking");
        for(ScorePair sp: pacManRanking)
        	System.out.println(sp);

        System.out.println("Ghosts Ranking");
        for(ScorePair sp: ghostsRanking)
        	System.out.println(sp);

	}
	
	public void exportToFile(String file,int trials) {
		FileWriter myWriter;
	    try {
	    	myWriter = new FileWriter(file);
	    	myWriter.write("Trials: "+trials+"\n");
	    	//quien ha luchado con quien
	    	myWriter.write("Scores Table\n");
	    	for(Stats[] result_pacman : stats)
	    		for(Stats s: result_pacman)
	    			myWriter.write(s.getDescription()+": "+s.getAverage()+";\n");
	    	
	    	myWriter.write("MsPacMan Ranking\n");
	    	for(ScorePair sp: pacManRanking)
	    		myWriter.write(sp.toString()+"\n");
	    	
	    
	    	
	    	myWriter.close();
	    } catch (IOException e) {
	    	e.printStackTrace();
	    }
	}
	
	synchronized void computeRanking()
	{
		double[] pacManScores = new double[list_pacMan.size()]; 
		double[] ghostScores = new double[list_ghosts.size()];
		
		for(int pc = 0; pc<pacManScores.length; pc++)
		{
			double score = 0;
			for(int g=0; g<ghostScores.length; g++) {
				double avg = stats[pc][g].getAverage();
				if(Double.isNaN(avg))
					avg = 0;
				score+= avg;
			}
			pacManScores[pc] = score/(double)ghostScores.length;
		}
		
		for(int g = 0; g<ghostScores.length; g++)
		{
			double score = 0;
			for(int pc=0; pc<pacManScores.length; pc++)
			{
				double avg = stats[pc][g].getAverage();
				if(Double.isNaN(avg))
					avg = 0;
				score+= avg;
			}			
			ghostScores[g] = score/(double)pacManScores.length;
		}

		
		pacManRanking = new Vector<ScorePair>();
		int pos = 0;
		for(String c: list_pacMan)
			pacManRanking.add(new ScorePair(c,pacManScores[pos++]));
		
		ghostsRanking = new Vector<ScorePair>();
		pos = 0;
		for(String c: list_ghosts)
			ghostsRanking.add(new ScorePair(c,ghostScores[pos++]));
		
		globalRanking = new Vector<ScorePair>();
		if(list_ghosts.size()==list_pacMan.size())
		{		
			pos = 0;
			for(String c: list_pacMan) {
				globalRanking.add(new ScorePair(c+" & "+list_ghosts.get(pos),pacManScores[pos]-ghostScores[pos]));
				pos++;
			}
		} else {
			System.err.println("WARNING: Global ranking not computed (#MsPacManTeams != #GhostsTeams)");
		}
		
		
		Collections.sort(pacManRanking);
		Collections.sort(ghostsRanking);
		Collections.reverse(ghostsRanking);
		Collections.sort(globalRanking);

	}
	
	public class ScorePair implements Comparable<ScorePair>{
		String name;
		Double score;
		public ScorePair(String name, Double score) {
			super();
			this.name = name;
			this.score = score;
		}

		public Double getScore() {
			return score;
		}
		
		public String getName() {
			return name;
		}

		public int compareTo(ScorePair o) {
			ScorePair other = (ScorePair)o;
			return (int) (other.getScore()-this.score);
		}
		public String toString()
		{
			return this.name+": "+String.format("%.2f", this.score);
		}
		
	}

	public Vector<String> getList_pacMan() {
		return list_pacMan;
	}

	public Vector<String> getList_ghosts() {
		return list_ghosts;
	}

	public Stats[][] getStats() {
		return stats;
	}

	
	
}