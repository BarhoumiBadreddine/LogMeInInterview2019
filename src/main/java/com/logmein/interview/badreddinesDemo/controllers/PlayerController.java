package com.logmein.interview.badreddinesDemo.controllers;

import com.logmein.interview.badreddinesDemo.dao.model.GamePlayer;
import com.logmein.interview.badreddinesDemo.dao.model.GamePlayerCard;
import com.logmein.interview.badreddinesDemo.services.GameService;
import com.logmein.interview.badreddinesDemo.services.PlayerService;
import com.logmein.interview.badreddinesDemo.services.beans.card.Card;
import com.logmein.interview.badreddinesDemo.services.beans.player.PlayerBean;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Set;

@AllArgsConstructor
@Slf4j
@RestController
@RequestMapping(value = "/player")
public class PlayerController {

    private final GameService gameService;

    private final PlayerService playerService;

    @PostMapping(value = "/addToGame")
    public ResponseEntity<Object> addPlayerToGame(@RequestParam() String gameName, @RequestParam() String playerName, final UriComponentsBuilder uriBuilder) {
        final GamePlayer gamePlayer = this.gameService.addPlayerToGame(gameName, playerName);
        final URI locationOfNewCashCard = uriBuilder.path("addToGame/{gameId}/players/{playerId}").buildAndExpand(gamePlayer.getId().getGameId(), gamePlayer.getId().getPlayerId()).toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();
    }


    @DeleteMapping(value = "/removeFromGame")
    public ResponseEntity<Void> removePlayerFromGame(@RequestParam String gameName, @RequestParam String playerName) {

        this.gameService.removePlayerFromGame(gameName, playerName);

        return new ResponseEntity<>(HttpStatus.NOT_MODIFIED);

    }

    @PostMapping(value = "/deal/games/{gameName}/players/{playerName}")
    public ResponseEntity<Object> deal(@PathVariable String gameName, @PathVariable() String playerName, final UriComponentsBuilder uriBuilder) {

        final GamePlayerCard dealtCard = this.playerService.dealToPlayer(gameName, playerName);

        // Build the URI with the path variables
        final URI locationUri = uriBuilder.path("/deal/games/{id}").buildAndExpand(dealtCard.getId()).toUri();

        // Return a ResponseEntity with the created URI
        return ResponseEntity.created(locationUri).build();
    }

    @PostMapping(value = "/shuffle")
    public ResponseEntity<Object> shuffleTheGameDeck(@RequestParam() String gameName, final UriComponentsBuilder uriBuilder) {

        final Iterable<Card> lstCards = this.playerService.shuffleTheGameDeck(gameName);
        final URI locationOfNewCashCard = uriBuilder.path("shuffle/{id}").buildAndExpand("").toUri();
        return ResponseEntity.created(locationOfNewCashCard).build();

    }

    @GetMapping(value = "/listOfCards")
    public ResponseEntity<List<Card>> listOfCards(@RequestParam String gameName, @RequestParam String playerName) {

        final List<Card> lstCard = this.playerService.listOfPlayerCards(gameName, playerName);
        return ResponseEntity.ok(lstCard);

    }


    @GetMapping(value = "/totalAddedValue")
    public ResponseEntity<Set<PlayerBean>> playersValues(@RequestParam() String gameName) {

        final Set<PlayerBean> playersValues = this.playerService.playersTotalHoldValues(gameName);
        return ResponseEntity.ok(playersValues);

    }

}
