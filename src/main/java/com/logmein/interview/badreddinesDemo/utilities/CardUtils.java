package com.logmein.interview.badreddinesDemo.utilities;

import com.logmein.interview.badreddinesDemo.services.beans.card.Card;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardSuit;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardType;
import com.logmein.interview.badreddinesDemo.services.beans.deck.DeckOfCards;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

public class CardUtils {

    public static CardType getCardType(final Integer cardNumber) {
        Assert.notNull(cardNumber, "[Assertion failed] - 'cardNumber' argument is required; it must not be null");

        final CardType[] values = CardType.values();
        for (CardType cardType : values) {
            final Integer min = cardType.getMin();
            final Integer max = cardType.getMax();
            if (cardNumber >= min && cardNumber <= max) {
                return cardType;
            }
        }
        throw new IllegalStateException("No card type for cardNumber[" + cardNumber + "]!");
    }

    public static ArrayList<Card> createCardsForEachSuite(Integer min, Integer max) {
        Assert.notNull(min, "[Assertion failed] - 'min' argument is required; it must not be null");
        Assert.notNull(max, "[Assertion failed] - 'max' argument is required; it must not be null");
        Assert.isTrue(min <= max, "'min' argument should be <= 'max'");

        final ArrayList<Card> arrayList = new ArrayList<>();
        for (CardSuit cardSuit : CardSuit.values()) {
            for (int cardNumber = min; cardNumber <= max; cardNumber++) {
                final CardType cardType = CardUtils.getCardType(cardNumber);
                arrayList.add(new Card(cardSuit, cardNumber, cardType));
            }
        }
        return arrayList;
    }

    public static ArrayList<Card> createCardsForEachSuite(CardType cardType) {
        Assert.notNull(cardType, "[Assertion failed] - 'cardType' argument is required; it must not be null");
        final Integer min = cardType.getMin();
        final Integer max = cardType.getMax();
        return CardUtils.createCardsForEachSuite(min, max);

    }

    public static void fillCards(DeckOfCards deckOfCards) {
        Assert.notNull(deckOfCards, "[Assertion failed] - 'deckOfCards' argument is required; it must not be null");
        final List<Card> newListOnExistingCards = deckOfCards.getCards();
        if (!newListOnExistingCards.isEmpty()) {
            throw new IllegalStateException("'deckOfCards.getCards()' is not clear!, may be it's already filled!");
        }
        final CardType[] values = CardType.values();
        for (CardType cardType : values) {
            final List<Card> cards = CardUtils.createCardsForEachSuite(cardType);
            deckOfCards.addCards(cards);
        }
    }
}
