package edu.nwmissouri.springbeam.bishop;
import java.io.Serializable;

public class VotingPage implements Serializable {
    String voteName = "unknown.md";
    Double voteRank = 1.0;
    Integer votes = 0;
    /**
     * Constructor for page name and count of votes made
     * @param voteNameIn
     * @param votesIn
     */
    public VotingPage(String voteNameIn, Integer votesIn){
        this.voteName = voteNameIn;
        this.votes = votesIn;
    }
    /**
     * Constructor for page, rank, and count of votes made
     * 
     * @param voteNameIn    - page name
     * @param voteRankIn    - page rank
     * @param votesIn       - count of votes made
     */

    public VotingPage(String, voteNameIn, Double voteRankIn, Integer votesIn){
        this.voteName = voteNameIn;
        this.voteRank = voteRankIn;
        this.votes = votesIn;
    }
    public Double getRank(){
        return this.pageRank;
    }

    public Integer getVotes(){
        return this.votes;
    }

    public String getName(){
        return this.voteName;
    }

    @Override
    public String toString(){
        return String.format("%s, %.5ft, %d", this.voteName, this.voteRank, this.votes);
    }
}