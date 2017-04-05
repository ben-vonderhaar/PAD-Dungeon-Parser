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

import jdk.nashorn.api.scripting.JSObject;

public class DungeonParser {

	private static HttpURLConnection getPreparedPADXConnection(String URL) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
        
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-agent", "Dungeon Parser");
        
        return connection;
	}
	
	public static File writeURLToFile(String URL, String file) throws IOException {
		
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
		File specialDungeonsHTML = 
				writeURLToFile("http://www.puzzledragonx.com/en/special-dungeons.asp", "special-dungeons.html");
		
		Document doc = Jsoup.parse(specialDungeonsHTML, "UTF-8");
		
//		System.out.println(doc);
		
		Elements dungeons = doc.select("td.dungeon > div.dungeon > a");
		
		JsonArray dungeonsJSON = new JsonArray();
		
		
//		int i = 0;
		
		
		
		for (Element dungeon : dungeons) {
			
			try {
		
			JsonObject dungeonJSON = new JsonObject();
		
			JsonArray floorsJSON = new JsonArray();
		
			Document missionHTML = Jsoup.parse(new URL("http://m.puzzledragonx.com/" + dungeon.attr("href")), 20000);
//			e.attr("href")
			System.out.println(missionHTML.select("div#mission1 > div.info > span.large > span.xlarge").get(0).text());
			
			dungeonJSON.addProperty("dungeonId", Long.valueOf(dungeon.attr("href").substring(dungeon.attr("href").indexOf("m=") + 2)));
			dungeonJSON.addProperty("dungeonName", missionHTML.select("div#mission1 > div.info > span.large > span.xlarge").get(0).text());
			
			Elements missionContainer = missionHTML.select("div#mission2");
			
			if (missionContainer.size() == 1) {
				
				JsonObject floorJSON = null;
				JsonArray enemiesJSON = null;
				
				for (Element missionPart : missionContainer.select("div")) {
					
					if (missionPart.attr("class").contains("floornum")) {
						
//						System.out.println(missionPart.select("div.floornum").get(0));
						
						if (null != floorJSON) {
							floorJSON.add("enemies", enemiesJSON);
							floorsJSON.add(floorJSON);
						}
						
						floorJSON = new JsonObject();
						floorJSON.addProperty("floorNum", floorsJSON.size() + 1);
						
						enemiesJSON = new JsonArray();
						
					} else if (missionPart.attr("class").contains("monster")) {
						
						Element enemy = missionPart.select("div.monster > div.detail-wrapper > div.stat-wrapper > div.name > a").get(0);
						
						JsonObject enemyJSON = new JsonObject();
						JsonArray loots = new JsonArray();
						enemyJSON.addProperty("monsterId", Long.valueOf(enemy.attr("href").substring(enemy.attr("href").indexOf("n=") + 2)));
						
//						System.out.println("Loots:");
						
						for (Element loot : missionPart.select("div.monster > div.loot > a")) {
							
							loots.add(new JsonPrimitive(Long.valueOf(loot.attr("href").substring(loot.attr("href").indexOf("n=") + 2))));
							
//							System.out.println("\t" + loot.attr("href").substring(loot.attr("href").indexOf("n=") + 2, loot.attr("href").length()));
						}
					
						enemyJSON.add("loots", loots);
						enemiesJSON.add(enemyJSON);
					}
				}

				if (null != floorJSON) {
					floorJSON.add("enemies", enemiesJSON);
					floorsJSON.add(floorJSON);
				}
				
			} else {
				System.out.println("Malformed mission");
			}

			dungeonJSON.add("floors", floorsJSON);
			dungeonsJSON.add(dungeonJSON);
			
			} catch (Exception e) {
				e.printStackTrace();
			}

//			i++;
//			if (i > 20) {
//				break;
//			}
		}
			
		System.out.println(dungeonsJSON);
		
	}
	
}
