package com.example.grasssimulator;

import java.math.BigDecimal;

public class PlayerStats {
    private String username;
    private int rebirths;
    private BigDecimal balance;
    private BigDecimal tokens;
    private int hoeLevel;
    private String activeHoe;

    public PlayerStats(String username, int rebirths, BigDecimal balance, BigDecimal tokens, int hoeLevel, String activeHoe) {
        this.username = username;
        this.rebirths = rebirths;
        this.balance = balance;
        this.tokens = tokens;
        this.hoeLevel = hoeLevel;
        this.activeHoe = activeHoe;
    }

    public String getUsername() {
        return username;
    }

    public int getRebirths() {
        return rebirths;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public BigDecimal getTokens() {
        return tokens;
    }

    public int getHoeLevel() {
        return hoeLevel;
    }

    public String getActiveHoe() {
        return activeHoe;
    }
}
