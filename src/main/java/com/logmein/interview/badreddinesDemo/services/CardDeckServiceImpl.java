package com.logmein.interview.badreddinesDemo.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

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

@Service(value = "cardDeckService")
public class CardDeckServiceImpl implements CardDeckService {
	@Autowired
	@Qualifier("deckCardCreatorService")
	private DeckCardCreatorService deckCardCreatorService;

	@Autowired
	private GameRepo gameRepo;
	@Autowired
	private GameDeckCardRepo gameDeckCardRepo;

	@Override
	public Game addDeckToGameDeck(String gameName) {

		final List<Game> lstGames = this.gameRepo.findByName(gameName);
		if (lstGames.size() > 1) {
			throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "More than one game have the same 'gameName'!");
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
	List<GameDeckCard> createGameDeckCardEntities(final Game game) {
		Assert.notNull(game, "[Assertion failed] - 'game' argument is required; it must not be null");
		final DeckOfCards deckOfCards = this.deckCardCreatorService.createDeckOfCards();
		final List<Card> cards = deckOfCards.getCards();
		final List<GameDeckCard> lstGameDeckCard = new ArrayList<GameDeckCard>();
		cards.forEach(card -> {
			final GameDeckCard gameDeckCard = createGameDeckCard(game, card);
			lstGameDeckCard.add(gameDeckCard);
		});
		updateGameDeckCardOrder(lstGameDeckCard);
		return lstGameDeckCard;
	}

	@Override
	public void updateGameDeckCardOrder(List<GameDeckCard> lstGameDeckCard) {
		int cardOrder = 0;
		for (GameDeckCard gameDeckCard : lstGameDeckCard) {
			gameDeckCard.setCardOrder(cardOrder++);
		}
	}

	GameDeckCard createGameDeckCard(final Game savedGame, Card card) {
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

	void addTheDeckGameCard(final Game game, final List<GameDeckCard> lstGameDeckCard) {
		Assert.notNull(game, "[Assertion failed] - 'game' argument is required; it must not be null");
		Assert.notEmpty(lstGameDeckCard,
				"[Assertion failed] - 'lstGameDeckCard' collection must not be empty: it must contain at least 1 element");
		final int gameId = game.getGameId();
		updateCardsIds(lstGameDeckCard, gameId);
		this.gameDeckCardRepo.saveAll(lstGameDeckCard);
		final int decksNumber = game.getDecksNumber();
		game.setDecksNumber(decksNumber + 1);
		this.gameRepo.save(game);
	}

	/**
	 * 
	 * @param lstGameDeckCard
	 * @param gameId
	 * @deprecated TODO: make 'cardId' auto incremental.
	 */
	void updateCardsIds(final List<GameDeckCard> lstGameDeckCard, final int gameId) {
		int maxCardId = getMaxDeckGameCardId(gameId);
		for (GameDeckCard gameDeckCard : lstGameDeckCard) {
			gameDeckCard.getId().setCardId(++maxCardId);
		}
	}

	/**
	 * 
	 * @param gameId
	 * @return
	 * @deprecated TODO: make 'cardId' auto incremental.
	 */
	int getMaxDeckGameCardId(final int gameId) {
		final Optional<Integer> maxCardId = this.gameDeckCardRepo.findMaxIdCardIdByIdGameId(gameId);
        return maxCardId.orElse(0);
	}

	@Override
	public Map<CardSuit, Long> numberOfCardsPerSuitInGameDeck(String gameName) {
		final List<Game> lstGames = this.gameRepo.findByName(gameName);
		if (lstGames.size() > 1) {
			throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "More than one game have the same 'gameName'!");
		} else if (lstGames.size() == 1) {
			final Game game = lstGames.get(0);
			final int gameId = game.getGameId();
			final List<GameDeckCard> findByIdGameId = this.gameDeckCardRepo.findByIdGameId(gameId);
			final Map<Integer, Long> collect = findByIdGameId.stream()
					.collect(Collectors.groupingBy(GameDeckCard::getCardSuit, Collectors.counting()));
			final Map<CardSuit, Long> result = new HashMap<>();
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
			throw new AppException(AppResponses.ENTITY_ALREADY_EXIST, "More than one game have the same 'gameName'!");
		} else if (lstGames.size() == 1) {
			final Game game = lstGames.get(0);
			final int gameId = game.getGameId();
			final List<GameDeckCard> findByIdGameId = this.gameDeckCardRepo.findByIdGameId(gameId);
			// CardSuit/CardNumber/Count
			final Map<Integer, Map<Integer, Long>> mapCardSuitCardNumberCount = findByIdGameId.stream()
					.collect(Collectors.groupingBy(GameDeckCard::getCardSuit,
							Collectors.groupingBy(GameDeckCard::getCardNumber, Collectors.counting())));

			final Set<CardCount> result = new TreeSet<>(new CardCountComparator());
			mapCardSuitCardNumberCount.forEach((key, mapCardNumberCount) -> {
                final CardSuit cardSuit = CardSuit.valueOf(key);
                mapCardNumberCount.forEach((cardNumber, value) -> {
                    final CardCount cardCount = new CardCount(cardSuit, cardNumber, CardUtils.getCardType(cardNumber),
                            value);
                    result.add(cardCount);
                });
            });
			return result;
		} else {
			throw new AppException(AppResponses.ENTITY_NOT_FOUND, "There is no game with name[" + gameName + "]!");
		}

	}
}
