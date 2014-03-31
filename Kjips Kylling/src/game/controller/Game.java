package game.controller;

import game.controller.input.SimpleKeyboard;
import game.entity.SimplePlayer;
import game.entity.types.Level;
import game.entity.types.Player;
import game.io.BuildLevelException;
import game.io.LevelNotFoundException;
import game.io.ResourceLoader;
import game.io.SpriteSheetNotFoundException;
import game.util.Direction;
import game.view.GameWindow;

/**
 * Denne klassen er en kontroller (of sorts) for spillet.
 * Den laster inn data vha. en ResourceLoader, og starter ting.
 * To instanser anses å være like hviss de er samme objekt.
 * 
 * @author Haakon Løtveit (haakon.lotveit@student.uib.no)
 *
 */
public class Game {
	protected Player player;
	protected SimpleKeyboard keyboard;
	protected ResourceLoader loader;
	protected GameWindow window;
	protected Level level;
	protected int currentLevel = 0;

	/**
	 * Skaper en ny kontroller.
	 * Game er ansvarlig for å starte ting, og å holde orden på brett og slikt.
	 * Game har derimot intet ansvar for hvordan ting blir malt til skjermen.
	 * En mulighet for å laste inn flere monstre kan være å utvide ResourceLoader til å holde orden på monstre, og la game registrere dem hos brettene.
	 * @param loader en ResourceLoader som Game kan bruke til å laste inn ting til senere bruk.
	 * @throws SpriteSheetNotFoundException Dersom spillerens spritesheet ikke kan lastes inn.
	 */
	public Game(ResourceLoader loader) throws SpriteSheetNotFoundException {
		this.loader = loader;
		this.keyboard = new SimpleKeyboard(this);
		SimplePlayer.init(loader.getSpriteLoader("player"));
		this.player = SimplePlayer.getInstance();		
		window = new GameWindow(keyboard, player);
	}

	/**
	 * Lar deg flytte brukeren. Til dømes kan en tastaturlytter kalle denne metoden. (SimpleKeyboard gjør dette)
	 * Playerobjektet er selv ansvarlig for å sjekke om den kan gå dit eller ikke.
	 * @param where retningen spilleren skal bevege seg i.
	 */
	public void movePlayer(Direction where){
		player.move(where, level);
	}

	/**
	 * Starter spillet. For hvert brett laster det inn brettet vha. ResourceLoaderen og lar deg spille det brettet til du vinner.
	 * Hvis du dør kan du restarte, etc.
	 * Når du har rundet spillet, avslutter start();
	 * @throws LevelNotFoundException Dersom et av brettene ikke blir funnet.
	 * 	Nåværende start() vil begynne på første brett (1) og deretter inkremementere til siste brett. 
	 *  (Siste brett blir funnet vha. et kall til {@link ResourceLoader#getNumLevels()}. Hvis dere ikke følger konvensjonen med brett 1,2,…,n vil ikke denne metoden virke korrekt.) 
	 * @throws BuildLevelException Dersom lastingen av et brett går galt. Se: {@link ResourceLoader#getLevel(int)} for detaljene
	 */
	public void start() throws LevelNotFoundException, BuildLevelException {
		currentLevel = 1;

		while(currentLevel <= loader.getNumLevels()){
			/* Sett brettet til nytt brett hver gang du klarer ett brett.
			 * Her er det plenty med muligheter til utvidelse av logikken. */
			
			level = loader.getLevel(currentLevel);
			startLevel();

			long timestamp = System.nanoTime(); 				  /* Aldri bruk System.currentTimeMillis() til denne type ting. Tenk om du sitter på et tog, krysser en tidssone, og så krasjer spillet. */
			int timesPerSecond = 120;                            /* Hvor mange ganger i sekundet entiteter som ikke er spilleren får gjøre noe. */
			long tickFrequency = 1_000_000_000L / timesPerSecond; /* Her har vi tap av presisjon grunnet heltallsdivisjon. For vårt bruk er dette greit. */
			long levelStartedAt = timestamp; 
			
			/* Sjekker at du ikke har vunnet. Da skal spillet laste neste brett. */
			while(!(player.getColumn() == level.getGoalColumn() && 
					player.getRow()    == level.getGoalRow())){
				
				/* Sjekker om spilleren har gått på noe dødelig */
				if(level.isPlaceDeadly(player.getColumn(), player.getRow())){
					boolean restart = window.popupDeath();
					if(restart){
						level.reset(player);
						window.loadLevel(level);
						continue;
					}
					else{
						return;
					}
				}
				
				long timeSinceLastOp = System.nanoTime() - timestamp;
				
				if(timeSinceLastOp >= tickFrequency){
					level.tick();
					timestamp = System.nanoTime();
				}
			}
			
			long timeTaken = (System.nanoTime() - levelStartedAt) / 100_000_000L;

			window.popupVictory();
			window.popupGeneric("Tid brukt", String.format("Du har brukt %d tidels sekunder", timeTaken));
			currentLevel++;
		}
		window.popupGameComplete();
	}
	
	private void startLevel(){
		level.reset(player);
		window.loadLevel(level);
		/* TODO: En ting som kunne vært artig hadde vært en spesifikk tekst til hvert brett */
		window.popupGeneric("Velkomsttekst, placeholder", "Placeholdertekst som du bør bytte ut");
	}
	
	public void shutdown(){
		window.dispose();
		window = null;
	}
}
