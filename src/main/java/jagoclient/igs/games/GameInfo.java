package jagoclient.igs.games;

import jagoclient.Global;

import java.util.Scanner;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import rene.util.parser.StringParser;

/**
This is a SortObject for sorting games by W player rank.
@see jagoclient.sort.Sorter
*/

public class GameInfo
{
	int gameNumber;
	String whitePlayer;
	String whiteRank;
	String blackPlayer;
	String blackRank;
	int move;
	int size;
	int handicap;
	// This is the komi minus .5
	int komi;
	int byoYomi;
	int byoYomiMoves;
	int initialTime;

	private class GameInfoParser {
		int position;
		String line;
		private String parseBrackets() {
			while(line.charAt(position) != '[') {
				position++;
			}
			position++;
			StringBuilder sb = new StringBuilder();
			while (line.charAt(position) != ']') {
				sb.append(line.charAt(position));
				position++;
			}
			position++;
			return sb.toString();
		}

		private String parseParens() {
			while(line.charAt(position) != '(') {
				position++;
			}
			position++;
			StringBuilder sb = new StringBuilder();
			while (line.charAt(position) != ')') {
				sb.append(line.charAt(position));
				position++;
			}
			position++;
			return sb.toString();
		}

		private String parseWord() {
			while(Character.isWhitespace(line.charAt(position))) {
				position++;
			}
			StringBuilder sb = new StringBuilder();
			while (!Character.isWhitespace(line.charAt(position))) {
				sb.append(line.charAt(position));
				position++;
			}
			return sb.toString();
		}
		public void parse(String line) {
			this.line = line;
			gameNumber = Integer.parseInt(parseBrackets().trim());
			whitePlayer = parseWord();
			whiteRank = parseBrackets().trim();
			// Skip vs.
			parseWord();
			blackPlayer = parseWord();
			blackRank = parseBrackets().trim();

			String[] gameInfoFields = parseParens().trim().split(" +");
			move = Integer.parseInt(gameInfoFields[0]);
			size = Integer.parseInt(gameInfoFields[1]);
			handicap = Integer.parseInt(gameInfoFields[2]);
			komi = (int)Math.floor(Double.parseDouble(gameInfoFields[3]));

			String[] timingFields = gameInfoFields[4].split("/");
			initialTime = Integer.parseInt(timingFields[0]);
			byoYomi = Integer.parseInt(timingFields[1]);
			byoYomiMoves = Integer.parseInt(timingFields[2]);
		}
	}

	public GameInfo(String line)
	{
		new GameInfoParser().parse(line);
	}

	public int getGameNumber() {
		return gameNumber;
	}

	public void setGameNumber(int gameNumber) {
		this.gameNumber = gameNumber;
	}

	public String getWhitePlayer() {
		return whitePlayer;
	}

	public void setWhitePlayer(String whitePlayer) {
		this.whitePlayer = whitePlayer;
	}

	public String getWhiteRank() {
		return whiteRank;
	}

	public void setWhiteRank(String whiteRank) {
		this.whiteRank = whiteRank;
	}

	public String getBlackPlayer() {
		return blackPlayer;
	}

	public void setBlackPlayer(String blackPlayer) {
		this.blackPlayer = blackPlayer;
	}

	public String getBlackRank() {
		return blackRank;
	}

	public void setBlackRank(String blackRank) {
		this.blackRank = blackRank;
	}

	public int getMove() {
		return move;
	}

	public void setMove(int move) {
		this.move = move;
	}

	public int getSize() {
		return size;
	}

	public void setSize(int size) {
		this.size = size;
	}

	public int getHandicap() {
		return handicap;
	}

	public void setHandicap(int handicap) {
		this.handicap = handicap;
	}

	public int getKomi() {
		return komi;
	}

	public void setKomi(int komi) {
		this.komi = komi;
	}

	public int getByoYomi() {
		return byoYomi;
	}

	public void setByoYomi(int byoYomi) {
		this.byoYomi = byoYomi;
	}

	public int getByoYomiMoves() {
		return byoYomiMoves;
	}

	public void setByoYomiMoves(int byoYomiMoves) {
		this.byoYomiMoves = byoYomiMoves;
	}

	public int getInitialTime() {
		return initialTime;
	}

	public void setInitialTime(int initialTime) {
		this.initialTime = initialTime;
	}
}
