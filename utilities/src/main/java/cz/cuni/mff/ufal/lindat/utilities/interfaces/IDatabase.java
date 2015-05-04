package cz.cuni.mff.ufal.lindat.utilities.interfaces;

public interface IDatabase extends java.io.Closeable {

	/**
	 * Return number of opened connections globally (if a connection
	 * pool is used then the number of current open sessions can be considerably smaller).
	 */
	public long getGlobalConnectionsCount();

	/**
	 * Number of opened sessions so far.
	 */
	public long getSessionOpenCount();

	/**
	 * Number of closed sessions so far.
	 */
	public long getSessionCloseCount();


	void openSession();

	void closeSession();

	/**
	 * Allows IDE to track unclosed resources, implement as this.closeSession();
	 */
	public void close();

}
