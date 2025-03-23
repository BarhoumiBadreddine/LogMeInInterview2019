package com.logmein.interview.badreddinesDemo.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.StreamSupport;

import lombok.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.logmein.interview.badreddinesDemo.constants.AppResponses;
import com.logmein.interview.badreddinesDemo.dao.GameDeckCardRepo;
import com.logmein.interview.badreddinesDemo.dao.GamePlayerCardRepo;
import com.logmein.interview.badreddinesDemo.dao.GamePlayerRepo;
import com.logmein.interview.badreddinesDemo.dao.GameRepo;
import com.logmein.interview.badreddinesDemo.dao.model.Game;
import com.logmein.interview.badreddinesDemo.dao.model.GameDeckCard;
import com.logmein.interview.badreddinesDemo.dao.model.GamePlayer;
import com.logmein.interview.badreddinesDemo.dao.model.GamePlayerCard;
import com.logmein.interview.badreddinesDemo.dao.model.GamePlayerCardPk;
import com.logmein.interview.badreddinesDemo.dao.model.Player;
import com.logmein.interview.badreddinesDemo.exceptions.AppException;
import com.logmein.interview.badreddinesDemo.services.beans.card.Card;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardSuit;
import com.logmein.interview.badreddinesDemo.services.beans.card.CardType;
import com.logmein.interview.badreddinesDemo.services.beans.player.PlayerBean;
import com.logmein.interview.badreddinesDemo.utilities.CardUtils;

@Service(value = "playerService")
public class PlayerServiceImpl implements PlayerService {
	@Autowired
	@Qualifier("deckCardCreatorService")
	private DeckCardCreatorService deckCardCreatorService;
	@Autowired
	@Qualifier("shuffleDeckGameService")
	private ShuffleDeckGameService shuffleDeckGameService;
	@Autowired
	@Qualifier("gameService")
	private GameService gameService;

	@Autowired
	private GameRepo gameRepo;
	@Autowired
	private GamePlayerRepo gamePlayerRepo;
	@Autowired
	private GameDeckCardRepo gameDeckCardRepo;
	@Autowired
	private GamePlayerCardRepo gamePlayerCardRepo;

	@Transactional
	@Override
	public GamePlayerCard dealToPlayer(@NonNull String gameName, @NonNull String playerName) {
		Assert.hasText(gameName,
				"[Assertion failed] - 'gameName' String argument must have text; it must not be null, empty, or blank");
		final List<Game> lstGames = this.gameRepo.findByName(gameName);
		if (lstGames.size() > 1) {
			throw new IllegalStateException("More than one game have the same 'gameName'!");
		} else if (lstGames.size() == 1) {
			final Game game = lstGames.get(0);
			final int gameId = game.getGameId();

			final Player player = this.gameService.findRegistredPlayerOrRegisterThis(playerName);
			final int playerId = player.getPlayerId();
			final Optional<GamePlayer> oGamePlayer = this.gamePlayerRepo.findByIdGameIdAndIdPlayerId(gameId, playerId);
			if (oGamePlayer.isPresent()) {
				final List<GameDeckCard> findByIdGameId = this.gameDeckCardRepo.findByIdGameId(gameId);
				final long exactSizeIfKnown = findByIdGameId.spliterator().getExactSizeIfKnown();
				if (exactSizeIfKnown == 0) {
					throw new AppException(AppResponses.ENTITY_NOT_FOUND,
                            "Deck game is empty for game[%s]!".formatted(gameName));
				}

				final GameDeckCard removedGameDeckCard = findByIdGameId.remove(0);
				final int cardSuit = removedGameDeckCard.getCardSuit();
				final int cardNumber = removedGameDeckCard.getCardNumber();
				final int cardId = removedGameDeckCard.getId().getCardId();
				final GamePlayerCard gamePlayerCard = create(gameId, cardSuit, cardNumber, cardId, playerId);

				final GamePlayerCard savedGamePlayerCard = this.gamePlayerCardRepo.save(gamePlayerCard);
				this.gameDeckCardRepo.delete(removedGameDeckCard);
				return savedGamePlayerCard;
			} else {
				throw new AppException(AppResponses.ENTITY_NOT_FOUND,
                        "Player[%s] Is not playing with Game[%s]!".formatted(playerName, gameName));
			}
		} else {
			throw new AppException(AppResponses.ENTITY_NOT_FOUND, "No Game with name[%s]!".formatted(gameName));
		}

	}

	@Transactional
	@Override
	public List<Card> shuffleTheGameDeck(String gameName) {
		Assert.hasText(gameName,
				"[Assertion failed] - 'gameName' String argument must have text; it must not be null, empty, or blank");
		final List<Game> lstGames = this.gameRepo.findByName(gameName);
		if (lstGames.size() > 1) {
			throw new IllegalStateException("More than one game have the same 'gameName'!");
		} else if (lstGames.size() == 1) {
			final Game game = lstGames.get(0);
			final int gameId = game.getGameId();
			final List<GameDeckCard> lstGameDeckCards = this.gameDeckCardRepo.findByIdGameId(gameId);
			if (lstGameDeckCards.isEmpty()) {
				throw new AppException(AppResponses.ENTITY_NOT_FOUND, "Deck game is empty for game[" + gameName + "]!");
			}
			this.shuffleDeckGameService.shuffleGameDeck(lstGameDeckCards);

			this.updateGameDeckCardOrder(lstGameDeckCards);

			final Iterable<GameDeckCard> saveAll = this.gameDeckCardRepo.saveAll(lstGameDeckCards);
			return createCards(saveAll);

		} else {
			throw new AppException(AppResponses.ENTITY_NOT_FOUND, "No Game with name[" + gameName + "]!");
		}

	}

	List<Card> createCards(Iterable<GameDeckCard> lstGameDeckCard) {
		final List<Card> cards = new ArrayList<>();
		StreamSupport.stream(lstGameDeckCard.spliterator(), false).forEach(gdc -> {
			final CardSuit cardSuit = CardSuit.valueOf(gdc.getCardSuit());
			final Integer cardNumber = gdc.getCardNumber();
			final CardType cardType = CardUtils.getCardType(cardNumber);
			final Card card = new Card(cardSuit, cardNumber, cardType);
			cards.add(card);
		});
		return cards;
	}

	GamePlayerCard create(final int gameId, final int cardSuit, final int cardNumber, final int cardId,
			final int playerId) {
		final GamePlayerCardPk gamePlayerCardPk = new GamePlayerCardPk();
		gamePlayerCardPk.setGameId(gameId);
		gamePlayerCardPk.setPlayerId(playerId);
		gamePlayerCardPk.setCardId(cardId);
		final GamePlayerCard gamePlayerCard = new GamePlayerCard();
		gamePlayerCard.setId(gamePlayerCardPk);
		gamePlayerCard.setCardSuit(cardSuit);
		gamePlayerCard.setCardNumber(cardNumber);
		return gamePlayerCard;
	}

	void updateGameDeckCardOrder(List<GameDeckCard> lstGameDeckCard) {
		int cardOrder = 0;
		for (GameDeckCard gameDeckCard : lstGameDeckCard) {
			gameDeckCard.setCardOrder(cardOrder++);
		}
	}

	@Override
	public List<Card> listOfPlayerCards(String gameName, String playerName) {

		Assert.hasText(gameName,
				"[Assertion failed] - 'gameName' String argument must have text; it must not be null, empty, or blank");
		final List<Game> lstGames = this.gameRepo.findByName(gameName);
		if (lstGames.size() > 1) {
			throw new IllegalStateException("More than one game have the same 'gameName'!");
		} else if (lstGames.size() == 1) {
			final Game game = lstGames.get(0);
			final int gameId = game.getGameId();
			final Player player = this.gameService.findRegistredPlayerOrRegisterThis(playerName);
			final int playerId = player.getPlayerId();
			final Optional<GamePlayer> oGamePlayer = this.gamePlayerRepo.findByIdGameIdAndIdPlayerId(gameId, playerId);
			if (oGamePlayer.isPresent()) {
//				final GamePlayer gamePlayer = oGamePlayer.get();
				final List<GamePlayerCard> lstGamePlayerCard = this.gamePlayerCardRepo
						.findByIdGameIdAndIdPlayerId(gameId, playerId);
				return createCards(lstGamePlayerCard);
			} else {
				throw new AppException(AppResponses.ENTITY_NOT_FOUND,
						"Player[" + playerName + "] Is not playing with Game[" + gameName + "]!");
			}
		} else {
			throw new AppException(AppResponses.ENTITY_NOT_FOUND, "No Game with name[" + gameName + "]!");
		}
	}

	public List<Card> createCards(final List<GamePlayerCard> lstGamePlayerCard) {
		final List<Card> cards = new ArrayList<>();
		lstGamePlayerCard.stream().forEach(gpc -> {
			final CardSuit cardSuit = CardSuit.valueOf(gpc.getCardSuit());
			final Integer cardNumber = gpc.getCardNumber();
			final CardType cardType = CardUtils.getCardType(cardNumber);
			final Card card = new Card(cardSuit, cardNumber, cardType);
			cards.add(card);
		});
		return cards;
	}

	@Override
	public Set<PlayerBean> playersTotalHoldValues(String gameName) {
		Assert.hasText(gameName,
				"[Assertion failed] - 'gameName' String argument must have text; it must not be null, empty, or blank");
		final List<Game> lstGames = this.gameRepo.findByName(gameName);
		if (lstGames.size() > 1) {
			throw new IllegalStateException("More than one game have the same 'gameName'!");
		} else if (lstGames.size() == 1) {
			final Game game = lstGames.get(0);
			final int gameId = game.getGameId();
			final List<PlayerBean> groupByPlayers = this.gamePlayerCardRepo.groupByPlayers(gameId);
			final TreeSet<PlayerBean> treeSet = new TreeSet<>(Collections.reverseOrder());
			treeSet.addAll(groupByPlayers);
			return treeSet;

		} else {
			throw new AppException(AppResponses.ENTITY_NOT_FOUND, "No Game with name[" + gameName + "]!");
		}

	}

}
