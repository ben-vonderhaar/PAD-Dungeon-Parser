package com.vonderhaar.dungeonparser.executor;

import java.net.URL;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.vonderhaar.dungeonparser.util.DungeonParserUtil;

public class DungeonParserRetrievalTask implements Runnable {

	private long dungeonId;
	private String dungeonLinkHref;
	
	private JsonObject dungeonJSON;
	
	public DungeonParserRetrievalTask(long dungeonId, String dungeonLinkHref) {
		this.dungeonId = dungeonId;
		this.dungeonLinkHref = dungeonLinkHref;
		
		dungeonJSON = new JsonObject();
	}
	
	public JsonObject getDungeonJSON() {
		return this.dungeonJSON;
	}
	
	@Override
	public void run() {
		
		for (int i = 0; i < 5; i++) {
			try {
						
				JsonArray floorsJSON = new JsonArray(), invadesJSON = new JsonArray();
			
				Document missionHTML = Jsoup.parse(new URL("http://m.puzzledragonx.com/" + this.dungeonLinkHref), 20000);
	
				// Initial dungeon data parsing
				this.dungeonJSON.addProperty("dungeonId", this.dungeonId);
				this.dungeonJSON.addProperty("dungeonName", DungeonParserUtil.getOnlyElement(missionHTML, 
						"div#header > div.title-wrapper > div.title > h1").text());
				this.dungeonJSON.addProperty("subDungeonName", DungeonParserUtil.getOnlyElement(missionHTML, 
						"div#mission1 > div.info > span.large > span.xlarge").text());

				// TODO handle invades that could happen on any floor.
				
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
							Element enemy = DungeonParserUtil.getOnlyElement(missionPart,
									"div.monster > div.detail-wrapper > div.stat-wrapper > div.name > a");
							
							JsonObject enemyJSON = new JsonObject();
							JsonArray loots = new JsonArray();
							
							enemyJSON.addProperty("monsterId", DungeonParserUtil.getMonsterIdFromAnchor(enemy));
							
							for (Element loot : missionPart.select("div.monster > div.loot > a")) {
								loots.add(new JsonPrimitive(DungeonParserUtil.getMonsterIdFromAnchor(loot)));
							}
						
							enemyJSON.add("loots", loots);
							
							if (DungeonParserUtil.getOnlyElement(missionPart, "div.monster > div.avatar > img").attr("style").contains("border-color: #6e4070")) {
								invadesJSON.add(enemyJSON);
							} else {
								enemiesJSON.add(enemyJSON);
							}
							
							
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
				this.dungeonJSON.add("floors", floorsJSON);
				this.dungeonJSON.add("invades", invadesJSON);
				
				// Once dungeon is successfully retrieved, break out of loop
				break;
			
			} catch (Exception e) {
				e.printStackTrace();
				
				System.out.println("Attempting to retrieve dungeon \"" + this.dungeonId + "\" again");
			}
		}
	}

}
