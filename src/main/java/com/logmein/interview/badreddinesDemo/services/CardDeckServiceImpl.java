package com.logmein.interview.badreddinesDemo.services;

import com.logmein.interview.badreddinesDemo.constants.AppResponses;
import com.logmein.interview.badreddinesDemo.dao.GameDeckCardRepo;
import com.logmein.interview.badreddinesDemo.dao.GameRepo;
import com.logmein.interview.badreddinesDemo.dao.model.Game;
import com.logmein.interview.badreddinesDemo.dao.model.GameDeckCard;
import com.logmein.interview.badreddinesDemo.dao.model.GameDeckCardPk;
import com.logmein.interview.badreddinesDemo.exceptions.AppException;
import com.logmein.interview.badreddinesDemo.services.beans.card.Card;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardCount;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardCountComparator;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardSuit;
import com.logmein.interview.badreddinesDemo.services.beans.deck.DeckOfCards;
import com.logmein.interview.badreddinesDemo.utilities.CardUtils;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service(value = "cardDeckService")
public class CardDeckServiceImpl implements CardDeckService {

    public static final String SAME_GAME_NAME_MESSAGE = "More than one game have the same 'gameName'!";
    private DeckCardCreatorService deckCardCreatorService;
    private GameRepo gameRepo;
    private GameDeckCardRepo gameDeckCardRepo;

    @Override
    public Game addDeckToGameDeck(@NonNull String gameName) {

        final List<Game> lstGames = this.gameRepo.findByName(gameName);
        if (lstGames.size() > 1) {
            throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, SAME_GAME_NAME_MESSAGE);
        } else if (lstGames.size() == 1) {
            final Game game = lstGames.get(0);
            final List<GameDeckCard> lstGameDeckCard = createGameDeckCardEntities(game);
            this.addTheDeckGameCard(game, lstGameDeckCard);
            return game;
        } else {
            throw new AppException(AppResponses.ENTITY_NOT_FOUND, "No Game with name[%s]!".formatted(gameName));
        }

    }

    /**
     * create a list of 'GameDeckCard' entities.
     *
     * @param game
     * @return
     */
    List<GameDeckCard> createGameDeckCardEntities(@NonNull final Game game) {
        final DeckOfCards deckOfCards = this.deckCardCreatorService.createDeckOfCards();
        final List<Card> cards = deckOfCards.getCards();
        final List<GameDeckCard> lstGameDeckCard = new ArrayList<>();
        cards.forEach(card -> {
            final GameDeckCard gameDeckCard = createGameDeckCard(game, card);
            lstGameDeckCard.add(gameDeckCard);
        });
        updateGameDeckCardOrder(lstGameDeckCard);
        return lstGameDeckCard;
    }

    @Override
    public void updateGameDeckCardOrder(@NonNull List<GameDeckCard> lstGameDeckCard) {
        int cardOrder = 0;
        for (GameDeckCard gameDeckCard : lstGameDeckCard) {
            gameDeckCard.setCardOrder(cardOrder++);
        }
    }

    GameDeckCard createGameDeckCard(@NonNull final Game savedGame, Card card) {
        final int suiteId = card.getCardSuit().getId();
        final int gameId = savedGame.getGameId();
        final Integer cardNumber = card.getCardNumber();

        final GameDeckCardPk gameDeckCardPk = new GameDeckCardPk();
        gameDeckCardPk.setGameId(gameId);
        final GameDeckCard gameDeckCard = new GameDeckCard();
        gameDeckCard.setId(gameDeckCardPk);
        gameDeckCard.setCardSuit(suiteId);
        gameDeckCard.setCardNumber(cardNumber);
        return gameDeckCard;
    }

    void addTheDeckGameCard(@NonNull final Game game, @NonNull final List<GameDeckCard> lstGameDeckCard) {
        Assert.notEmpty(lstGameDeckCard, "[Assertion failed] - 'lstGameDeckCard' collection must not be empty: it must contain at least 1 element");
        final int gameId = game.getGameId();
        updateCardsIds(lstGameDeckCard, gameId);
        this.gameDeckCardRepo.saveAll(lstGameDeckCard);
        final int decksNumber = game.getDecksNumber();
        game.setDecksNumber(decksNumber + 1);
        this.gameRepo.save(game);
    }

    /**
     * @param lstGameDeckCard
     * @param gameId
     * @deprecated TODO: make 'cardId' auto incremental.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    void updateCardsIds(@NonNull final List<GameDeckCard> lstGameDeckCard, final int gameId) {
        int maxCardId = getMaxDeckGameCardId(gameId);
        for (GameDeckCard gameDeckCard : lstGameDeckCard) {
            gameDeckCard.getId().setCardId(++maxCardId);
        }
    }

    /**
     * @param gameId
     * @return
     * @deprecated TODO: make 'cardId' auto incremental.
     */
    @Deprecated(since = "1.0", forRemoval = true)
    int getMaxDeckGameCardId(final int gameId) {
        final Optional<Integer> maxCardId = this.gameDeckCardRepo.findMaxIdCardIdByIdGameId(gameId);
        return maxCardId.orElse(0);
    }

    @Override
    public Map<CardSuit, Long> numberOfCardsPerSuitInGameDeck(String gameName) {
        final List<Game> lstGames = this.gameRepo.findByName(gameName);
        if (lstGames.size() > 1) {
            throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, SAME_GAME_NAME_MESSAGE);
        } else if (lstGames.size() == 1) {
            final Game game = lstGames.get(0);
            final int gameId = game.getGameId();
            final List<GameDeckCard> findByIdGameId = this.gameDeckCardRepo.findByIdGameId(gameId);
            final Map<Integer, Long> collect = findByIdGameId.stream().collect(Collectors.groupingBy(GameDeckCard::getCardSuit, Collectors.counting()));
            final Map<CardSuit, Long> result = new EnumMap<>(CardSuit.class);
            collect.forEach((key, value) -> result.put(CardSuit.valueOf(key), value));
            return result;
        } else {
            throw new AppException(AppResponses.ENTITY_NOT_FOUND, "There is no game with name[" + gameName + "]!");
        }

    }

    @Override
    public Set<CardCount> remainingCardsInGameDeck(String gameName) {

        final List<Game> lstGames = this.gameRepo.findByName(gameName);
        if (lstGames.size() > 1) {
            throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, SAME_GAME_NAME_MESSAGE);
        } else if (lstGames.size() == 1) {
            final Game game = lstGames.get(0);
            final int gameId = game.getGameId();
            final List<GameDeckCard> findByIdGameId = this.gameDeckCardRepo.findByIdGameId(gameId);
            // CardSuit/CardNumber/Count
            final Map<Integer, Map<Integer, Long>> mapCardSuitCardNumberCount = findByIdGameId.stream().collect(Collectors.groupingBy(GameDeckCard::getCardSuit, Collectors.groupingBy(GameDeckCard::getCardNumber, Collectors.counting())));

            final Set<CardCount> result = new TreeSet<>(new CardCountComparator());
            mapCardSuitCardNumberCount.forEach((key, mapCardNumberCount) -> {
                final CardSuit cardSuit = CardSuit.valueOf(key);
                mapCardNumberCount.forEach((cardNumber, value) -> {
                    final CardCount cardCount = new CardCount(cardSuit, cardNumber, CardUtils.getCardType(cardNumber), value);
                    result.add(cardCount);
                });
            });
            return result;
        } else {
            throw new AppException(AppResponses.ENTITY_NOT_FOUND, "There is no game with name[" + gameName + "]!");
        }

    }
}
