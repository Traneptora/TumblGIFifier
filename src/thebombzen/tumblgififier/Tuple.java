package thebombzen.tumblgififier;


public class Tuple<E, F> {
	private E e;
	private F f;
	public Tuple(E e, F f){
		this.e = e;
		this.f = f;
	}
	
	public E getE() {
		return e;
	}
	
	public void setE(E e) {
		this.e = e;
	}
	
	public F getF() {
		return f;
	}
	
	public void setF(F f) {
		this.f = f;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((e == null) ? 0 : e.hashCode());
		result = prime * result + ((f == null) ? 0 : f.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Tuple<?, ?> other = (Tuple<?, ?>) obj;
		if (e == null) {
			if (other.e != null)
				return false;
		} else if (!e.equals(other.e))
			return false;
		if (f == null) {
			if (other.f != null)
				return false;
		} else if (!f.equals(other.f))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Tuple [e=" + e + ", f=" + f + "]";
	}
	
}
