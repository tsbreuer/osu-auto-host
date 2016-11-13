package lt.ekgame.autohost;

import java.io.File;
import java.util.LinkedList;
import java.util.Queue;

import lt.ekgame.bancho.api.units.Beatmap;

public class BeatmapHandler {
	
	public Queue<Beatmap> beatmaps = new LinkedList<>();
	public Queue<Beatmap> completed = new LinkedList<>();
	private Beatmap current = null;
	
	public BeatmapHandler(String folder) {
		File beatmapFolder = new File(folder);
		if (beatmapFolder.exists() && beatmapFolder.isDirectory()) {
			File[] files = beatmapFolder.listFiles(i -> i.getName().toLowerCase().endsWith("osu"));
			for (File file : files) {
				try {
					Beatmap beatmap = new Beatmap(file);
					beatmaps.add(beatmap);
				}
				catch (Exception e) {
					System.err.println("Failed to load " + file.getName());
				}
			}
		}
	}
	
	public String getBeatmapsQueue()
	{
		String ReturnString = beatmaps.size() + " Beatmaps in Queue.";
		int i = 0;
			for (Beatmap beatmap : beatmaps) {
				if (beatmap.getDT()) {
					ReturnString = ReturnString+" || DT [http://osu.ppy.sh/b/"+beatmap.getId()+" "+ beatmap.getArtist()+" - " + beatmap.getTitle()+ "] ["+beatmap.getDiff()+"*]";
				}
				if (beatmap.getHT()) {
					ReturnString = ReturnString+" || HT [http://osu.ppy.sh/b/"+beatmap.getId()+" "+ beatmap.getArtist()+" - " + beatmap.getTitle()+ "] ["+beatmap.getDiff()+"*]";
				}
				if (!beatmap.getDT() && !beatmap.getHT()){
					ReturnString = ReturnString+" || [http://osu.ppy.sh/b/"+beatmap.getId()+" "+ beatmap.getArtist()+" - " + beatmap.getTitle()+ "] ["+beatmap.getDiff()+"*]";
				}
				
				i++;
		}
			if (ReturnString.equals("")){
				return "No beatmaps in queue";
			} else {
	return ReturnString;
			}
	}
	
	public boolean recentlyPlayed(Beatmap check, int recentness) {
		int i = 0;
		for (Beatmap beatmap : completed) {
			if (beatmap.getChecksum().toLowerCase().equals(check.getChecksum().toLowerCase()))
				if (completed.size() - i < recentness)
					return true;
			i++;
		}
		return false;
	}
	
	public boolean inQueue(Beatmap check) {
		if (current != null && current.getChecksum().toLowerCase().equals(check.getChecksum().toLowerCase()))
			return true;
		for (Beatmap beatmap : beatmaps) {
			if (beatmap.getChecksum().toLowerCase().equals(check.getChecksum().toLowerCase()))
				return true;
		}
		return false;
	}
	
	public boolean hasRequested(int userId){
	for (Beatmap beatmap : beatmaps) {
		if (beatmap.RequestedBy == userId)
			return true;
	}
	return false;
	}
	public Beatmap nextBeatmap() {
		if (current != null)
			completed.add(current);
		current = beatmaps.poll();
		return current;
	}
	
	public Beatmap getBeatmap() {
		return current;
	}

	public void reset() {
		beatmaps.addAll(completed);
		completed.clear();
		current = null;
	}

	public void push(Beatmap beatmap) {
		beatmaps.add(beatmap);
	}

	public int queueSize() {
		return beatmaps.size();
	}

}
