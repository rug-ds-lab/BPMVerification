package nl.rug.ds.bpm.verification.stepper;

/**
 * Created by Heerko Groefsema on 21-Apr-17.
 */
public class Marking {
	private int[] orderedTokensAtPlaces;
	private static int maximumTokensAtPlaces = 9;
	
	public Marking(int placeCount) {
		orderedTokensAtPlaces = new int[placeCount];
	}
	
	public Marking(int[] orderedTokensAtPlaces) {
		this.orderedTokensAtPlaces = orderedTokensAtPlaces;
	}
	
	public void setTokens(int index, int amount) {
		orderedTokensAtPlaces[index] = amount;
	}
	
	public int[] getOrderedTokensAtPlaces() {
		return orderedTokensAtPlaces;
	}
	
	public void setOrderedTokensAtPlaces(int[] orderedTokensAtPlaces) {
		this.orderedTokensAtPlaces = orderedTokensAtPlaces;
	}
	
	@Override
	public String toString() {
		String s = "";
		for (int i = 0; i < orderedTokensAtPlaces.length; i++)
			if (orderedTokensAtPlaces[i] > 0) {
				int tokens = orderedTokensAtPlaces[i];
				if (tokens > maximumTokensAtPlaces)
					tokens = maximumTokensAtPlaces;
				s = s + "+" + tokens + "p" + i;
			}
		return (s.length() > 0 ? s.substring(1) : "");
	}
	
	public static void setMaximumTokensAtPlaces(int maximum) {
		maximumTokensAtPlaces = maximum;
	}
}
