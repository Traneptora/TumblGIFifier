package thebombzen.tumblgififier.util;

/**
 * This represents a tuple of two elements, i.e. an ordered pair. The purpose is to use a Tuple in a Map or a Set, so we can can create a set of ordered pairs or a Map whose domain is a set of ordered pairs.
 *
 * There are no restrictions on the components, other than restrictions enforced by the generic typing.
 * @param <E> This is the type of the first component of the ordered pair. 
 * @param <F> This is the type of the second component of the ordered pair.
 */
public class Tuple<E, F> {
	private E e;
	private F f;
	/**
	 * Construct an ordered pair of two elements.
	 * @param e the first component of the ordered pair
	 * @param f the second component of the ordered pair
	 */
	public Tuple(E e, F f){
		this.e = e;
		this.f = f;
	}
	
	/**
	 * This is the getter for the first component.
	 * @return
	 */
	public E getFirst() {
		return e;
	}
	
	/**
	 * This is the setter for the first component.
	 * @param e
	 */
	public void setFirst(E e) {
		this.e = e;
	}
	
	/**
	 * This is the getter for the second component.
	 * @return
	 */
	public F getSecond() {
		return f;
	}
	
	/**
	 * This is the setter for the second component.
	 * @param f
	 */
	public void setSecond(F f) {
		this.f = f;
	}

	/**
	 * The hash code function calls the hash code of its two components. Because this class is designed for easy usage in HashMaps and HashSets, it is advised to use E and F that implement some form of hashCode that isn't the identity hash code.
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((e == null) ? 0 : e.hashCode());
		result = prime * result + ((f == null) ? 0 : f.hashCode());
		return result;
	}

	/**
	 * The equals function calls the equals function of its two components. An ordered pair is equal to another if and only if the components equal their respective counterparts, according to E and F's equals() method. Please override those so they are realistic and do not use ==.
	 */
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

	/**
	 * This method returns a string of the form "Tuple [e=E, f=F]" where E and F are the first and second components, respectively.
	 */
	@Override
	public String toString() {
		return "Tuple [e=" + e + ", f=" + f + "]";
	}
	
}
