package havis.app.modbus.reader.api;

public class Field {
	private int id;

	public Field(int id) {
		this.id = id;
	}

	public int getId() {
		return id;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof Field))
			return false;
		Field other = (Field) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return Integer.toString(id);
	}
}
