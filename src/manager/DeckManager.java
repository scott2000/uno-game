package manager;

import card.CardObject;
import card.UnoCard;
import display.UnoMain;
import display.UnoPanel;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DeckManager {
    public static final int CARDS_PER_DECK = 108;
    public static final int CARDS_PER_HAND = 7;
    public static final int INITIAL_DECK_COUNT = CARDS_PER_DECK - CARDS_PER_HAND*2 - 1;

    private final File saveFile;
    private final Path savePath;

    private List<UnoCard> deck;

    public DeckManager(String gameType) {
        saveFile = new File(UnoMain.UNO_DIRECTORY, gameType+"Game");
        savePath = saveFile.toPath();
        if (saveFile.exists()) {
            int result = JOptionPane.showOptionDialog(
                    null,
                    "Do you want to resume the previous game or start a new one?",
                    "Resume Game?",
                    JOptionPane.DEFAULT_OPTION,
                    JOptionPane.PLAIN_MESSAGE,
                    null,
                    new String[] {"Resume", "New Game"},
                    "Resume");
            switch (result) {
            case 0:
                break;
            case 1:
                saveFile.delete();
                break;
            default:
                System.exit(0);
            }
        }
    }

    int count() {
        return deck.size();
    }

    public UnoCard draw() {
        // If the deck is empty, use discarded cards
        if (deck.isEmpty()) {
            deck = UnoPanel.newDeckFromDiscard();
        }
        return deck.remove(deck.size() - 1);
    }

    public void startGame() {
        if (loadGame()) {
            return;
        }
        deck = UnoCard.newDeck();
        UnoCard[] playerHand = new UnoCard[CARDS_PER_HAND];
        UnoCard[] opponentHand = new UnoCard[CARDS_PER_HAND];
        int playerAdvantage = 0;
        for (int i = 0; i < CARDS_PER_HAND; i++) {
            UnoCard a = draw();
            UnoCard b = draw();
            int diff = a.getPowerCode()-b.getPowerCode();
            if (Math.abs(playerAdvantage+diff) > UnoCard.MAX_POWER_DIFF) {
                opponentHand[i] = a;
                playerHand[i] = b;
                playerAdvantage -= diff;
            } else {
                playerHand[i] = a;
                opponentHand[i] = b;
                playerAdvantage += diff;
            }
        }
        for (int i = deck.size()-1; ; i--) {
            if (deck.get(i).isNumeric()) {
                UnoPanel.newGame(deck.remove(i), playerHand, opponentHand);
                return;
            }
        }
    }

    public void deleteSave() {
        saveFile.delete();
    }

    public void saveGame() {
        ArrayList<String> saveData = new ArrayList<>();
        saveData.add(saveCards(deck));
        UnoPanel.saveState(saveData);
        try {
            Files.write(savePath, saveData, Charset.forName("utf-8"));
        } catch (IOException ignored) {}
    }

    private boolean loadGame() {
        try {
            List<String> loadData = Files.readAllLines(savePath, Charset.forName("utf-8"));
            deck = new ArrayList<>(Arrays.asList(loadCards(loadData.get(0))));
            UnoPanel.loadState(loadData);
            return true;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static String saveCards(List<UnoCard> cards) {
        StringBuilder output = new StringBuilder();
        boolean first = true;
        for (UnoCard card : cards) {
            if (first) {
                first = false;
            } else {
                output.append(' ');
            }
            output.append(card.encode());
        }
        return output.toString();
    }

    public static String saveCardObjects(List<CardObject> cardObjects) {
        StringBuilder output = new StringBuilder();
        boolean first = true;
        for (CardObject cardObject : cardObjects) {
            if (first) {
                first = false;
            } else {
                output.append(' ');
            }
            output.append(cardObject.getCard().encode());
        }
        return output.toString();
    }

    public static UnoCard[] loadCards(String loadData) {
        String[] cardStrings = loadData.split(" ");
        int length = cardStrings.length;
        if (length == 1 && cardStrings[0].isEmpty()) {
            length = 0;
        }
        UnoCard[] result = new UnoCard[length];
        for (int i = 0; i < length; i++) {
            result[i] = UnoCard.decode(cardStrings[i]);
        }
        return result;
    }
}
