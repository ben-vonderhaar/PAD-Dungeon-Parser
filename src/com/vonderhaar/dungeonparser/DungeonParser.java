package com.vonderhaar.dungeonparser;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.vonderhaar.dungeonparser.executor.DungeonParserRetrievalTask;
import com.vonderhaar.dungeonparser.util.DungeonParserUtil;
import com.vonderhaar.dungeonparser.util.DungeonType;

/**
 * 
 * 
 * TODO:
 * Refactor all strings into constants
 * Handle normal/technical dungeons as well as special dungeons
 * Provide ability to specify configurations when invoking from command line
 * 
 * 
 * @author Ben
 */
public class DungeonParser {
	
	private static Logger _log = Logger.getLogger(DungeonParser.class);
	
	/**
	 * @param limit
	 * @throws IOException
	 */
	public DungeonParser(int limit, DungeonType dungeonType) throws IOException { 
		
		File specialDungeonsHTML = 
				writeURLToFile("http://www.puzzledragonx.com/en/" + dungeonType + ".asp", dungeonType + ".html");
		
		_log.info("Retrieving " + dungeonType);
		
		Document doc = Jsoup.parse(specialDungeonsHTML, "UTF-8");
		Elements dungeons = doc.select("td.dungeon > div.dungeon > a");
		
		if (limit > dungeons.size()) {
			limit = dungeons.size();
		}
		
		int i = 0;
		
		ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();
		executor.setCorePoolSize(5);
		executor.setMaximumPoolSize(50);
		
		List<DungeonParserRetrievalTask> tasks = new ArrayList<DungeonParserRetrievalTask>();
		
		for (Element dungeon : dungeons) {
			
			DungeonParserRetrievalTask task = new DungeonParserRetrievalTask(DungeonParserUtil.getDungeonIdFromAnchor(dungeon), 
					dungeon.attr("href"));
            
			tasks.add(task);
			
			// Break after specified limit
			if (++i >= limit) {
				break;
			}
		}
		
		int j = 0;
		
		_log.info("Starting dungeon retrieval tasks");
		
		while (j < tasks.size()) {
			if (executor.getActiveCount() < 0.8 * executor.getMaximumPoolSize()) {
				executor.execute(tasks.get(j++));
				_log.info((j / ((double) limit / 100)) + "% started");
			} else {
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
		_log.info("All retrieval tasks started");
		
        executor.shutdown();
        
        int lastSeenNumTasks = Integer.MAX_VALUE;
        
        while (!executor.isTerminated()) { 
        
        	if (executor.getActiveCount() < limit && executor.getActiveCount() < lastSeenNumTasks) {
        		lastSeenNumTasks = executor.getActiveCount();
        		_log.info((((double) limit - lastSeenNumTasks) / ((double) limit / 100)) + "% completed");
        	}
        }
        
        _log.info("All retrieval tasks completed");
			
        BufferedWriter bufferedWriter = null;
		FileWriter fileWriter = null;

		try {

			fileWriter = new FileWriter(dungeonType + ".json");
			bufferedWriter = new BufferedWriter(fileWriter);

			bufferedWriter.write("[");
			
			for (int k = 0; k < tasks.size(); k++) {
				
				if (k > 0) {
					bufferedWriter.write(",");
				}
				
				bufferedWriter.write(tasks.get(k).getDungeonJSON().toString());
			}

			bufferedWriter.write("]");


		} catch (IOException e) {

			e.printStackTrace();

		} finally {

			try {

				if (bufferedWriter != null)
					bufferedWriter.close();

				if (fileWriter != null)
					fileWriter.close();

			} catch (IOException ex) {

				ex.printStackTrace();

			}

		}
	}

	
	/**
	 * @param URL
	 * @param file
	 * @return
	 * @throws IOException
	 */
	private static File writeURLToFile(String URL, String file) throws IOException {
		
		FileReader fileReader = null;
		
		try {
			fileReader = new FileReader(file);
			fileReader.close();
			
			_log.info(file + " already in place");
			 
			return new File(file);
		} catch (FileNotFoundException e) {
			_log.info("Cannot find " + file + ", loading from PADX");
		} 
		
		HttpURLConnection connection = DungeonParserUtil.getPreparedPADXConnection(URL);
        
		connection.connect();
		FileUtils.copyInputStreamToFile(connection.getInputStream(), new File(file));
		
		connection.disconnect();
		
		return new File(file);
			
	}
	
	public static void main(String[] args) throws IOException {
		new DungeonParser(Integer.MAX_VALUE, DungeonType.SPECIAL);
//		new DungeonParser(5/*Integer.MAX_VALUE*/, DungeonType.TECHNICAL);
//		new DungeonParser(5/*Integer.MAX_VALUE*/, DungeonType.NORMAL);
//		new DungeonParser(1/*Integer.MAX_VALUE*/, DungeonType.MULTIPLAYER);

		//TODO coin dungeons
	}
	
}
