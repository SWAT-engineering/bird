package engineering.swat.nest.nescio;

public class Location {
	
	private int offset;
	private int length;

	public Location(int offset, int length) {
		this.offset = offset;
		this.length = length;
	}

	public int getOffset() {
		return offset;
	}

	public int getLength() {
		return length;
	}
	
	@Override
	public String toString() {
		return "<" + offset + ", " + length + ">";
	}
}