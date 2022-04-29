package edu.nwmissouri.springbeam.bishop;
import java.io.Serializable;

public class VotingPage implements Serializable {
    String name = "unknown";
    Double rank = 1.0;
    Integer votes = 0;
    /**
     * Constructor for page name and count of votes made
     * @param nameIn
     * @param votesIn
     */
    public VotingPage(String nameIn, Integer votesIn){
        this.nameIn = nameIn;
        this.votes = votesIn;
    }
    /**
     * Constructor for page, rank, and count of votes made
     * 
     * @param nameIn    - page name
     * @param rankIn    - page rank
     * @param votesIn       - count of votes made
     */

    public VotingPage(String, nameIn, Double rankIn, Integer votesIn){
        this.name = nameIn;
        this.rank = rankIn;
        this.votes = votesIn;
    }
    public Double getRank(){
        return this.rank;
    }

    public Integer getVotes(){
        return this.votes;
    }

    public String getName(){
        return this.name;
    }

    @Override
    public String toString(){
        return String.format("%s, %.5f, %d", this.name, this.rank, this.votes);
    }
}