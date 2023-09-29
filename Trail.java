import java.util.ArrayList;

public class Trail {

	// other trail references
	private ArrayList<Trail> fromTrails = new ArrayList<Trail>(); // trails before this trail
	private Trail toTrail = null; // trail after this trail

	// trail itself
	private ArrayList<double[]> trail = new ArrayList<double[]>();

	public Trail() {
	}
	
	// lengthen the trail by one point
	public void lengthen(double[] newPoint) {
		trail.add(newPoint);
	}
	
	// shorten the trail by one point
	public void shorten() {
		trail.remove(0);
	}
	
	// returns the points of the trail
	public ArrayList<double[]> getTrail() {
		return trail;
	}
	
	// attaches a trail from before
	public void attachFrom(Trail fromTrail) {
		fromTrails.add(fromTrail);
	}
	
	// attaches to a trail after
	public void attachTo(Trail toTrail) {
		this.toTrail = toTrail;
	}

	// returns the number of trail points
	public int length() {
		return trail.size();
	}
	
	// returns trails from before
	public ArrayList<Trail> getFromTrails() {
		return fromTrails;
	}
	
	// returns trails after
	public Trail getToTrail() {
		return toTrail;
	}

}
