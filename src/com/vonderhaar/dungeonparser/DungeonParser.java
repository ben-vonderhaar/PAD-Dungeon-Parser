package com.vonderhaar.dungeonparser;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
	
	/**
	 * @param limit
	 * @throws IOException
	 */
	public DungeonParser(int limit) throws IOException { 
		
		File specialDungeonsHTML = 
				writeURLToFile("http://www.puzzledragonx.com/en/special-dungeons.asp", "special-dungeons.html");
		
		Document doc = Jsoup.parse(specialDungeonsHTML, "UTF-8");
		Elements dungeons = doc.select("td.dungeon > div.dungeon > a");
		
		JsonArray dungeonsJSON = new JsonArray();
		
		int i = 0;
		
		for (Element dungeon : dungeons) {
			
			try {
		
				JsonObject dungeonJSON = new JsonObject();			
				JsonArray floorsJSON = new JsonArray();
			
				Document missionHTML = Jsoup.parse(new URL("http://m.puzzledragonx.com/" + dungeon.attr("href")), 30000);
	
				// Initial dungeon data parsing
				String dungeonName = getOnlyElement(missionHTML, "div#mission1 > div.info > span.large > span.xlarge").text();
				dungeonJSON.addProperty("dungeonId", getDungeonIdFromAnchor(dungeon));
				dungeonJSON.addProperty("dungeonName", dungeonName);
				
				// TODO handle invades that could happen on any floor.
				
				System.out.println(dungeonName);
				
				// div with id=mission2 is always present and contains floor information.
				Elements missionContainer = missionHTML.select("div#mission2");
				
				if (missionContainer.size() == 1) {
					
					JsonObject floorJSON = null;
					JsonArray enemiesJSON = null;
					
					// Beneath the missionContainer are a number of divs, alternating between floor indicators and 
					// 1+ enemy information containers.
					for (Element missionPart : missionContainer.select("div")) {
						
						if (missionPart.attr("class").contains("floornum")) {
							
							// Add existing floor data (if any) to the list of floors processed so far.
							if (null != floorJSON) {
								floorJSON.add("enemies", enemiesJSON);
								floorsJSON.add(floorJSON);
							}
							
							// Stub out new floor.
							floorJSON = new JsonObject();
							
							// TODO parse actual floor number, as PADX sometimes skips floors in new/low difficulty dungeons
							floorJSON.addProperty("floorNum", floorsJSON.size() + 1);
							
							enemiesJSON = new JsonArray();
							
						} else if (missionPart.attr("class").contains("monster")) {
							
							// Parse information about the enemy, including their ID and any loot that can drop from them
							Element enemy = getOnlyElement(missionPart,
									"div.monster > div.detail-wrapper > div.stat-wrapper > div.name > a");
							
							JsonObject enemyJSON = new JsonObject();
							JsonArray loots = new JsonArray();
							
							enemyJSON.addProperty("monsterId", getMonsterIdFromAnchor(enemy));
							
							for (Element loot : missionPart.select("div.monster > div.loot > a")) {
								loots.add(new JsonPrimitive(getMonsterIdFromAnchor(loot)));
							}
						
							enemyJSON.add("loots", loots);
							enemiesJSON.add(enemyJSON);
						}
					}
	
					// Handle last floor
					if (null != floorJSON) {
						floorJSON.add("enemies", enemiesJSON);
						floorsJSON.add(floorJSON);
					}
					
				} else {
					System.out.println("Malformed mission");
				}
	
				// Add floors to the dungeon object, then add the dungeon to the list of dungeons processed so far.
				dungeonJSON.add("floors", floorsJSON);
				dungeonsJSON.add(dungeonJSON);
			
			} catch (Exception e) {
				e.printStackTrace();
			}

			// Break after specified limit
			i++;
			if (i > limit) {
				break;
			}
		}
			
		// Temporarily print to terminal for collection
		// TODO pipe this into a file
		System.out.println(dungeonsJSON);
		
	}
	
	/**
	 * 
	 * @param anchor
	 * @return
	 */
	private static Long getMonsterIdFromAnchor(Element anchor) {
		return getSomeIdFromAnchor(anchor, "n=");
	}
	
	/**
	 * 
	 * @param anchor
	 * @return
	 */
	private static Long getDungeonIdFromAnchor(Element anchor) {
		return getSomeIdFromAnchor(anchor, "m=");
	}
	
	/**
	 * 
	 * @param anchor
	 * @param idPrefix
	 * @return
	 */
	private static Long getSomeIdFromAnchor(Element anchor, String idPrefix) {
		String href = anchor.attr("href");
		return Long.valueOf(href.substring(href.indexOf(idPrefix) + 2));
	}
	
	/**
	 * TODO make this safe with proper error handling
	 * 
	 * @param parentElement the Element on which the cssQuery should selected from
	 * @param cssQuery a String adhering to jsoup's cssQuery syntax
	 * @return if only one Element is found given the parameters, that Element
	 */
	private static Element getOnlyElement(Element parentElement, String cssQuery) {
		return parentElement.select(cssQuery).get(0);
	}

	/**
	 * @param URL
	 * @return
	 * @throws IOException
	 */
	private static HttpURLConnection getPreparedPADXConnection(String URL) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
        
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-agent", "Dungeon Parser");
        
        return connection;
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
			
			System.out.println(file + " already in place");
			 
			return new File(file);
		} catch (FileNotFoundException e) {
			System.out.println("Cannot find " + file + ", loading from PADX");
		} 
		
		HttpURLConnection connection = getPreparedPADXConnection(URL);
        
		connection.connect();
		FileUtils.copyInputStreamToFile(connection.getInputStream(), new File(file));
		
		connection.disconnect();
		
		return new File(file);
			
	}
	
	public static void main(String[] args) throws IOException {
		new DungeonParser(Integer.MAX_VALUE);
	}
	
}
