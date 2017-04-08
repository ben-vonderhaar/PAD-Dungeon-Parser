package com.vonderhaar.dungeonparser.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.jsoup.nodes.Element;

public class DungeonParserUtil {
	
	/**
	 * 
	 * @param anchor
	 * @return
	 */
	public static Long getMonsterIdFromAnchor(Element anchor) {
		return getSomeIdFromAnchor(anchor, "n=");
	}
	
	/**
	 * 
	 * @param anchor
	 * @return
	 */
	public static Long getDungeonIdFromAnchor(Element anchor) {
		return getSomeIdFromAnchor(anchor, "m=");
	}
	
	/**
	 * 
	 * @param anchor
	 * @param idPrefix
	 * @return
	 */
	public static Long getSomeIdFromAnchor(Element anchor, String idPrefix) {
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
	public static Element getOnlyElement(Element parentElement, String cssQuery) {
		return parentElement.select(cssQuery).get(0);
	}

	/**
	 * @param URL
	 * @return
	 * @throws IOException
	 */
	public static HttpURLConnection getPreparedPADXConnection(String URL) throws IOException {
		HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
        
		connection.setRequestMethod("GET");
		connection.setRequestProperty("User-agent", "Dungeon Parser");
        
        return connection;
	}
}
