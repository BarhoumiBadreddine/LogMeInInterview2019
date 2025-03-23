package com.logmein.interview.badreddinesDemo.services;

import com.logmein.interview.badreddinesDemo.dao.model.Game;
import com.logmein.interview.badreddinesDemo.dao.model.GamePlayer;
import com.logmein.interview.badreddinesDemo.dao.model.Player;

public interface GameService {

    Game createGame(String gameName);

    boolean deleteGame(String gameName);

    Iterable<Game> findAll();

    GamePlayer addPlayerToGame(String gameName, String playerName);

    boolean removePlayerFromGame(String gameName, String playerName);

    Player findRegistredPlayerOrRegisterThis(String playerName);

    Game findById(int id);

    Game findByName(String name);
    void deleteById(int id);
}
