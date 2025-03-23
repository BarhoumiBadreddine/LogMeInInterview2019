package com.logmein.interview.badreddinesDemo.controllers;

import com.logmein.interview.badreddinesDemo.dao.model.Game;
import com.logmein.interview.badreddinesDemo.services.CardDeckService;
import com.logmein.interview.badreddinesDemo.services.DeckCardCreatorService;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardCount;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardSuit;
import com.logmein.interview.badreddinesDemo.services.beans.deck.DeckOfCards;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;
import java.util.Set;

@AllArgsConstructor
@RestController
@RequestMapping(value = "/deck")
public class CardDeckController {

    private DeckCardCreatorService deckCardCreatorService;


    private CardDeckService cardDeckService;

    @GetMapping()
    public ResponseEntity<DeckOfCards> getDeckOfCards() {
        final DeckOfCards createDeckOfCards = this.deckCardCreatorService.createDeckOfCards();
        return ResponseEntity.ok(createDeckOfCards);
    }

    @PostMapping(value = "/games/{gameName}")
    public ResponseEntity<Object> postAddDeckToGameDeck(@PathVariable() String gameName, final UriComponentsBuilder uriBuilder) {

        final Game game = this.cardDeckService.addDeckToGameDeck(gameName);
        final URI location = uriBuilder.path("/games/definition/{id}").buildAndExpand(game.getGameId()).toUri();
        return ResponseEntity.created(location).build();
    }

    @GetMapping(value = "/cardsPerSuit")
    public ResponseEntity<Map<CardSuit, Long>> getNumberOfCardsPerSuitInGameDeck(@RequestParam() String gameName) {
        // Get the count of how many cards per suit are left undealt in the game deck

        final Map<CardSuit, Long> cardsPerSuit = this.cardDeckService.numberOfCardsPerSuitInGameDeck(gameName);
        return ResponseEntity.ok(cardsPerSuit);

    }

    @GetMapping(value = "/remaining")
    public ResponseEntity<Set<CardCount>> getRemainingCardsInGameDeck(@RequestParam() String gameName) {
        // Get the count of how many cards per suit are left undealt in the game deck

        final Set<CardCount> setOfCardCount = this.cardDeckService.remainingCardsInGameDeck(gameName);
        return ResponseEntity.ok(setOfCardCount);

    }

}
