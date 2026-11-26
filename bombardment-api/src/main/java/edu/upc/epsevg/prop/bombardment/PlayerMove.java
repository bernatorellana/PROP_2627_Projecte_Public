package edu.upc.epsevg.prop.bombardment;

/**
 * Una jugada més les dades informatives de la cerca que l'ha produïda.
 *
 * @author bernat
 */
public class PlayerMove {

    private final Move move;
    private long numberOfNodesExplored;
    private int maxDepthReached;
    private final SearchType searchType;
    private double heuristic;

    /**
     * @param move                  la jugada
     * @param numberOfNodesExplored nodes explorats (els que heu avaluat o expandit, sigueu coherents)
     * @param maxDepthReached       profunditat de la darrera iteració <b>completada</b>
     * @param searchType            tipus de cerca
     */
    public PlayerMove(Move move, long numberOfNodesExplored, int maxDepthReached, SearchType searchType) {
        this.move = move;
        this.numberOfNodesExplored = numberOfNodesExplored;
        this.maxDepthReached = maxDepthReached;
        this.searchType = searchType;
    }

    public Move getMove() { return move; }
    public long getNumberOfNodesExplored() { return numberOfNodesExplored; }
    public int getMaxDepthReached() { return maxDepthReached; }
    public SearchType getSearchType() { return searchType; }
    public double getHeuristic() { return heuristic; }

    public void setNumberOfNodesExplored(long n) { this.numberOfNodesExplored = n; }
    public void setMaxDepthReached(int d) { this.maxDepthReached = d; }

    /** Valor heurístic (des del punt de vista del jugador que mou) de la jugada triada. Opcional. */
    public void setHeuristic(double h) { this.heuristic = h; }

    @Override
    public String toString() {
        return move + " [" + searchType + ", depth=" + maxDepthReached + ", nodes=" + numberOfNodesExplored
                + ", h=" + heuristic + "]";
    }
}
