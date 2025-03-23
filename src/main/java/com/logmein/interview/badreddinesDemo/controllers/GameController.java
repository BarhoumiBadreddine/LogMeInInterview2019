package com.logmein.interview.badreddinesDemo.controllers;

import com.logmein.interview.badreddinesDemo.dao.model.Game;
import com.logmein.interview.badreddinesDemo.exceptions.AppException;
import com.logmein.interview.badreddinesDemo.services.GameService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Slf4j
@RestController
@RequestMapping(value = "/games")
@AllArgsConstructor
@CrossOrigin(origins = "*")
public class GameController {

    private GameService gameService;

    @GetMapping(value = "/definition/{id}")
    public ResponseEntity<Game> getGameById(@PathVariable() int id) {
        log.info("Getting game with id: {}", id);
        final Game game = this.gameService.findById(id);
        return ResponseEntity.ok(game);
    }

    @GetMapping(value = "/search")
    public ResponseEntity<Game> getGameByName(@RequestParam() String q) {
        log.info("Searching for game with q: {}", q);
        try {
            final Game game = this.gameService.findById(Integer.parseInt(q));
            return ResponseEntity.ok(game);
        }
        catch (NumberFormatException e) {
            final Game game = this.gameService.findByName(q);
            return ResponseEntity.ok(game);
        }
    }

    @PostMapping("/definition")
    public ResponseEntity<Void> createGame(@RequestParam() String gameName, UriComponentsBuilder uriBuilder) {
        log.info("Creating game with name: {}", gameName);
        final Game newGame = this.gameService.createGame(gameName);
        final URI location = uriBuilder.path("/games/{id}").buildAndExpand(newGame.getGameId()).toUri();
        return ResponseEntity.created(location).build();
    }

    @DeleteMapping("/definition/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable int id) {
        log.info("Deleting game with id: {}", id);
        gameService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

}
