# PAD Dungeon Parser

This repository primarily serves the purpose of providing dungeon data to another of my projects, [PAD Material Calculator](https://github.com/ben-vonderhaar/PAD-Material-Calculator).  This information is 100% scraped from [PuzzleDragonX](http://www.puzzledragonx.com) and is not supplemented in any way.  If you would like to use this information without generating it yourself, the latest dungeon information can be downloaded as JSON at this link:

https://raw.githubusercontent.com/ben-vonderhaar/PAD-Dungeon-Parser/master/special-dungeons.json
https://raw.githubusercontent.com/ben-vonderhaar/PAD-Dungeon-Parser/master/technical-dungeons.json
https://raw.githubusercontent.com/ben-vonderhaar/PAD-Dungeon-Parser/master/normal-dungeons.json
https://raw.githubusercontent.com/ben-vonderhaar/PAD-Dungeon-Parser/master/multiplayer-dungeons.json

Currently this script is best run in Eclipse.  Run the gradlew script to bring down dependencies and import the project.  Run "DungeonParser.java" as a Java Application.  Delete the HTML files to force the script to re-parse the available dungeons before parsing the dungeon information.
