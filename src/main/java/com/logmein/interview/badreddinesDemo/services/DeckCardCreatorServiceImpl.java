package com.logmein.interview.badreddinesDemo.services;

import com.logmein.interview.badreddinesDemo.services.beans.deck.DeckOfCards;
import com.logmein.interview.badreddinesDemo.utilities.CardUtils;
import org.springframework.stereotype.Service;

@Service(value = "deckCardCreatorService")
public class DeckCardCreatorServiceImpl implements DeckCardCreatorService {

    @Override
    public DeckOfCards createDeckOfCards() {
        final DeckOfCards deckOfCards = new DeckOfCards();
        CardUtils.fillCards(deckOfCards);
        return deckOfCards;
    }
}
