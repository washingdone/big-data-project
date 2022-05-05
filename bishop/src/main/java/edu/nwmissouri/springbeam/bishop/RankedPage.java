package edu.nwmissouri.springbeam.bishop;

import java.util.ArrayList;
import java.io.Serializable;

public class RankedPage implements Serializable {
    String name = "unknown.md";
    Double rank = 1.000;
    ArrayList<VotingPage> voters = new ArrayList<VotingPage>();
    /**
     * Constructors with name and list of pages point to voting for this page are as follows:
     *
     * @param nameIn    - this page name
     * @param votersIn  - array list of pages pointing to this page
     */

     RankedPage(String nameIn, ArrayList<VotingPage> votersIn){
         this.name = nameIn;
         this.voters = votersIn;
     }
     /**
     * Constructor with name, rank, and list of pages pointing to voting for this page are as follows:
     * 
     * @param nameIn    - this page name
     * @param rankIn    - this page's rank
     * @param votersIn - array list of pages pointing to this page
     */

    public RankedPage (String nameIn, Double rankIn, ArrayList<VotingPage> votersIn) {
        this.name = nameIn;
        this.rank = rankIn;
        this.voters = votersIn;
    }

    public Double getRank(){
        return this.rank;
    }

    public ArrayList<VotingPage> getVoters(){
        return this.voters;
    } 
    @Override
    public String toString(){
        return String.format("%s, %.5f, %s", this.name, this.rank, this.voters.toString());
    }
}