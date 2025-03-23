package com.logmein.interview.badreddinesDemo.services;

import com.logmein.interview.badreddinesDemo.constants.AppResponses;
import com.logmein.interview.badreddinesDemo.dao.*;
import com.logmein.interview.badreddinesDemo.dao.model.Game;
import com.logmein.interview.badreddinesDemo.dao.model.GamePlayer;
import com.logmein.interview.badreddinesDemo.dao.model.GamePlayerPk;
import com.logmein.interview.badreddinesDemo.dao.model.Player;
import com.logmein.interview.badreddinesDemo.exceptions.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.rest.webmvc.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Optional;

@Service(value = "gameService")
public class GameServiceImpl implements GameService {
    @Autowired
    @Qualifier("deckCardCreatorService")
    private DeckCardCreatorService deckCardCreatorService;

    @Autowired
    private GameRepo gameRepo;
    @Autowired
    private PlayerRepo playerRepo;
    @Autowired
    private GamePlayerRepo gamePlayerRepo;
    @Autowired
    private GamePlayerCardRepo gamePlayerCardRepo;
    @Autowired
    private GameDeckCardRepo gameDeckCardRepo;

    @Override
    public Iterable<Game> findAll() {
        return this.gameRepo.findAll();
    }

    @Transactional
    @Override
    public Game createGame(String gameName) {
        final List<Game> opGame = this.gameRepo.findByName(gameName);
        if (!opGame.isEmpty()) {
            throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "There is an existing game with the same name!");
        }
        return this.dbInsertGame(gameName);
    }

    @Override
    public GamePlayer addPlayerToGame(String gameName, String playerName) {
        final List<Game> opGame = this.gameRepo.findByName(gameName);
        if (opGame.size() > 1) {
            throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "More than one game have the same 'gameName'!");
        } else if (opGame.size() == 1) {
            final Game game = opGame.get(0);
            final Player player = findRegistredPlayerOrRegisterThis(playerName);
            // check if the player is already playing
            final GamePlayer gamePlayer;
            final Optional<GamePlayer> dbGamePlayer = this.gamePlayerRepo.findByIdPlayerId(player.getPlayerId());
            if (dbGamePlayer.isPresent()) {
                final GamePlayer foundGamePlayer = dbGamePlayer.get();
                if (foundGamePlayer.getId().getGameId() == game.getGameId()) {
                    throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "Player[%s] is already playing on this game[%s]!".formatted(playerName, gameName));
                } else {
                    throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "Player[%s] is already playing on another game!".formatted(playerName));
                }
            } else {
                gamePlayer = assignPlayerToGame(player, game);
            }
            return gamePlayer;
        } else {
            throw new AppException(AppResponses.ENTITY_NOT_FOUND, "No Game with name[%s]!".formatted(gameName));
        }
    }

    @Transactional
    @Override
    public boolean removePlayerFromGame(String gameName, String playerName) {
        final List<Game> lstGames = this.gameRepo.findByName(gameName);
        if (lstGames.size() > 1) {
            throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "More than one game have the same 'gameName'!");
        } else if (lstGames.size() == 1) {
            final Game game = lstGames.get(0);
            final int gameId = game.getGameId();
            final Player player = findRegistredPlayerOrRegisterThis(playerName);
            if (player == null) {
                throw new AppException(AppResponses.ERROR_SAVING_ENTITY, "Player does not exist and he can't be registred!");
            }
            // check if the player is already playing
            final Optional<GamePlayer> dbGamePlayer = this.gamePlayerRepo.findByIdPlayerId(player.getPlayerId());
            if (dbGamePlayer.isPresent()) {
                final GamePlayer gamePlayer = dbGamePlayer.get();
                final GamePlayerPk playerPk = gamePlayer.getId();
                final int playerId = playerPk.getPlayerId();
                final int playerGameId = playerPk.getGameId();
                if (playerGameId == gameId) {
                    this.gamePlayerCardRepo.deleteByIdPlayerId(playerId);
                    this.gamePlayerRepo.deleteByIdPlayerId(playerId);
                } else {
                    throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "user is playing, but not in this game!");
                }
            } else {
                throw new AppException(AppResponses.ENTITY_NOT_FOUND, "user is not playing any game!");
            }
            return true;
        } else {
            throw new AppException(AppResponses.ENTITY_NOT_FOUND, "There is no game with name[" + gameName + "]!");
        }

    }

    GamePlayer assignPlayerToGame(final Player player, final Game game) {
        Assert.notNull(player, "[Assertion failed] - 'player' argument is required; it must not be null");
        Assert.notNull(game, "[Assertion failed] - 'game' argument is required; it must not be null");
        final int gameId = game.getGameId();
        final int maxPlayerOrder = getMaxPlayerOrder(gameId);
        final GamePlayerPk gamePlayerPk = new GamePlayerPk();
        gamePlayerPk.setPlayerId(player.getPlayerId());
        gamePlayerPk.setGameId(gameId);
        final GamePlayer _gamePlayer = new GamePlayer();
        _gamePlayer.setId(gamePlayerPk);
        _gamePlayer.setPlayerOrder(maxPlayerOrder + 1);
        final GamePlayer gamePlayer = this.gamePlayerRepo.save(_gamePlayer);
        return gamePlayer;
    }

    /**
     * @param gameId
     * @return
     */
    int getMaxPlayerOrder(final int gameId) {
        final Optional<Integer> opMaxPlayerOrder = this.gamePlayerRepo.getMaxPlayerOrder(gameId);
        if (opMaxPlayerOrder.isPresent()) {
            return opMaxPlayerOrder.get();
        } else {
            return 0;
        }
    }


    /**
     * check if the player is already registered, if not register him/here.
     *
     * @param playerName the player to search or to register
     * @return the registered player.
     */
    @Override
    public Player findRegistredPlayerOrRegisterThis(final String playerName) {
        final Player player;
        final Optional<Player> dbPlayer = this.playerRepo.findByPlayerName(playerName);
        if (dbPlayer.isPresent()) {
            player = dbPlayer.get();
        } else {
            final Player _player = new Player();
            _player.setPlayerName(playerName);
            player = this.playerRepo.save(_player);
        }
        return player;
    }

    @Override
    public Game findById(int id) {
        return this.gameRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No Game with id[%d]!".formatted(id)));
    }

    @Override
    public Game findByName(String name) {
        return this.gameRepo.findByName(name)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("No Game with name[%s]!".formatted(name)));
    }

    @Transactional
    Game dbInsertGame(String gameName) {
        Assert.hasText(gameName, "[Assertion failed] - 'gameName' String argument must have text; it must not be null, empty, or blank");
        final Game newGame = new Game();
        newGame.setName(gameName);
        return this.gameRepo.save(newGame);
    }

    @Transactional
    @Override
    public boolean deleteGame(final String gameName) {
        Assert.hasText(gameName, "[Assertion failed] - 'gameName' String argument must have text; it must not be null, empty, or blank");
        final List<Game> lstGames = this.gameRepo.findByName(gameName);
        if (lstGames.size() > 1) {
            throw new IllegalStateException("More than one game have the same 'gameName'!");
        } else if (lstGames.size() == 1) {
            final Game game = lstGames.get(0);
            final int gameId = game.getGameId();
            this.gamePlayerCardRepo.deleteByIdGameId(gameId);
            this.gameDeckCardRepo.deleteByIdGameId(gameId);
            this.gamePlayerRepo.deleteByIdGameId(gameId);
            this.gameRepo.deleteById(gameId);
            return true;
        } else {
            throw new AppException(AppResponses.ENTITY_NOT_FOUND, "No Game with name[" + gameName + "]!");
        }
    }


    @Override
    public void deleteById(final int id) {
        this.gameRepo.deleteById(id);
    }
}
