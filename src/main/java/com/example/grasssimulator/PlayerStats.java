package com.example.grasssimulator;

import java.math.BigDecimal;
import java.util.Set;

public class PlayerStats {
    private String username;
    private int rebirths;
    private BigDecimal balance;
    private BigDecimal tokens;
    private int hoeLevel;
    private String activeHoe;
    private Set<String> purchasedHoes; // Добавили список купленных мотыг

    public PlayerStats(String username, int rebirths, BigDecimal balance, BigDecimal tokens, int hoeLevel, String activeHoe, Set<String> purchasedHoes) {
        this.username = username;
        this.rebirths = rebirths;
        this.balance = balance;
        this.tokens = tokens;
        this.hoeLevel = hoeLevel;
        this.activeHoe = activeHoe;
        this.purchasedHoes = purchasedHoes;
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
    public Set<String> getPurchasedHoes() {
        return purchasedHoes;
    }
}
