package jagoclient.igs.users;

import org.apache.commons.lang3.StringUtils;
import rene.util.parser.StringParser;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Scanner;

/**
 * Created by johndaniels on 7/21/17.
 */
public class UserInfo {
    String username;
    String country;
    String infoString;
    String rank;
    String idle;
    int wins;
    int losses;
    String flags;
    String language;

    public UserInfo(String userLine) {
        // The early fields in a userinfo are fixed width in terms of bytes, so we convert to a utf-8 byte array
        username = userLine.substring(0, 10).trim();
        infoString = userLine.substring(12, 27).trim();
        country = userLine.substring(28, 36).trim();
        String remaining = StringUtils.stripStart(userLine.substring(37), " ");
        Scanner s = new Scanner(remaining);
        rank = s.next();

        s.useDelimiter("( |/)+");
        try {
            wins = s.nextInt();
            losses = s.nextInt();
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
        s.reset();
        String observing = s.next();
        String playing = s.next();
        idle = s.next();
        flags = s.next();
        language = s.next();

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getInfoString() {
        return infoString;
    }

    public void setInfoString(String infoString) {
        this.infoString = infoString;
    }

    public String getRank() {
        return rank;
    }

    public void setRank(String rank) {
        this.rank = rank;
    }

    public int getRankValue() {
        int bonus = rank.contains("+") ? 1 : 0;
        if (rank.contains("k")) {
            int kyuRank = Integer.parseInt(rank.substring(0, rank.indexOf("k")));
            return (18 - kyuRank) * 2 + bonus;
        } else if (rank.contains("d")) {
            int danRank = Integer.parseInt(rank.substring(0, rank.indexOf("d")));
            return 34 + danRank * 2 + bonus;
        } else if (rank.contains("p")) {
            int proRank = Integer.parseInt(rank.substring(0, rank.indexOf("p")));
            return 54 + proRank * 2 + bonus;
        } else {
            return 0;
        }
    }

    public String getIdle() {
        return idle;
    }

    public void setIdle(String idle) {
        this.idle = idle;
    }

    public int getWins() {
        return wins;
    }

    public void setWins(int wins) {
        this.wins = wins;
    }

    public int getLosses() {
        return losses;
    }

    public void setLosses(int losses) {
        this.losses = losses;
    }

    public String getFlags() {
        return flags;
    }

    public void setFlags(String flags) {
        this.flags = flags;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }
}
