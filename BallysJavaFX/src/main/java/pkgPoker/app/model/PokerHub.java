package pkgPoker.app.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import netgame.common.Hub;
import pkgPokerBLL.Action;
import pkgPokerBLL.Card;
import pkgPokerBLL.CardDraw;
import pkgPokerBLL.Deck;
import pkgPokerBLL.GamePlay;
import pkgPokerBLL.GamePlayPlayerHand;
import pkgPokerBLL.Player;
import pkgPokerBLL.Rule;
import pkgPokerBLL.Table;

import pkgPokerEnum.eAction;
import pkgPokerEnum.eCardDestination;
import pkgPokerEnum.eDrawCount;
import pkgPokerEnum.eGame;
import pkgPokerEnum.eGameState;

public class PokerHub extends Hub {

	private Table HubPokerTable = new Table();
	private GamePlay HubGamePlay;
	private int iDealNbr = 0;

	public PokerHub(int port) throws IOException {
		super(port);
	}

	protected void playerConnected(int playerID) {

		if (playerID == 2) {
			shutdownServerSocket();
		}
	}

	protected void playerDisconnected(int playerID) {
		shutDownHub();
	}

	protected void messageReceived(int ClientID, Object message) {

		if (message instanceof Action) {
			Player actPlayer = (Player) ((Action) message).getPlayer();
			Action act = (Action) message;
			switch (act.getAction()) {
			case Sit:
				HubPokerTable.AddPlayerToTable(actPlayer);
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case Leave:			
				HubPokerTable.RemovePlayerFromTable(actPlayer);
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case TableState:
				resetOutput();
				sendToAll(HubPokerTable);
				break;
			case StartGame:
				// Get the rule from the Action object.
				Rule rle = new Rule(act.geteGame());
				
				UUID DealerID = null;
			
				for(UUID id:HubPokerTable.getHmPlayer().keySet()){
					if(id.equals(actPlayer.getPlayerID())){
						DealerID = id;
					}
				}
				
				if(DealerID==null){
					DealerID = HubPokerTable.getHmPlayer().keySet().iterator().next();
				}
				

				HubGamePlay = new GamePlay(rle,DealerID);
				

				HubGamePlay.setGamePlayers(HubPokerTable.getHmPlayer());
				
				int[] Order = null;
				
				for(Player p:HubPokerTable.getHmPlayer().values()){
					if(p.getPlayerID().equals(DealerID)){
						Order = GamePlay.GetOrder(p.getiPlayerPosition());
					}
				}
				HubGamePlay.setiActOrder(Order);

			case Draw:
				Rule drawrle = new Rule(act.geteGame());
				
				
				HubGamePlay.seteDrawCountLast(eDrawCount.geteDrawCount(HubGamePlay.geteDrawCountLast().getDrawNo()+1));
				
				CardDraw draw = drawrle.GetDrawCard(HubGamePlay.geteDrawCountLast());
				

				if(draw.getCardDestination()==eCardDestination.Player){
					for(int x:HubGamePlay.getiActOrder()){
						for(Player p:HubPokerTable.getHmPlayer().values()){
							if(x==p.getiPlayerPosition()){
								for(int i=0;i<draw.getCardCount().getCardCount();i++){
									HubGamePlay.drawCard(p, eCardDestination.Player);
								}
							}
						}
					}
				}
			
				else{
					for(int i=0;i<draw.getCardCount().getCardCount();i++){
						HubGamePlay.drawCard(null, eCardDestination.Community);
					}
				}
				
				if(HubGamePlay.geteDrawCountLast().getDrawNo()==drawrle.GetMaxDrawCount()){
					HubGamePlay.isGameOver();
				}
				
				resetOutput();
				sendToAll(HubGamePlay);
				break;
			case ScoreGame:
				HubGamePlay.ScoreGame();
				resetOutput();
				sendToAll(HubGamePlay);
				break;
			}
			
		}

	}

}