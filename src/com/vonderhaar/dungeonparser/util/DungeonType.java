package com.vonderhaar.dungeonparser.util;

public enum DungeonType {
	SPECIAL ("special-dungeons"), TECHNICAL ("technical-dungeons"), NORMAL ("normal-dungeons"), MULTIPLAYER ("multiplayer-dungeons");
	
	private final String dungeonType;
	
	DungeonType(String dungeonType) {
		this.dungeonType = dungeonType;
	}
	
	@Override
	public String toString() {
		return this.dungeonType;
	}
}
