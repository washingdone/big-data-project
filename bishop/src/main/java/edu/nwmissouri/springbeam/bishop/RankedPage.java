package edu.nwmissouri.springbeam.bishop;

import java.util.ArrayList;
import java.io.Serializable;

public class RankedPage implements Serializable {
    String pageName = "unknown.md";
    Double pageRank = 1.000;
    ArrayList<VotingPage> pageVoters = new ArrayList<VotingPage>();
    /**
     * Constructors with name and list of pages point to voting for this page are as follows:
     *
     * @param pageNameIn    - this page name
     * @param pageVotersIn  - array list of pages pointing to this page
     */

     RankedPage(String pageNameIn, ArrayList<VotingPage> pageVotersIn){
         this.pageName = pageNameIn;
         this.pageVoters = pageVotersIn;
     }
     /**
     * Constructor with name, rank, and list of pages pointing to voting for this page are as follows:
     * 
     * @param pageNameIn    - this page name
     * @param pageRankIn    - this page's rank
     * @param pageVotersIn - array list of pages pointing to this page
     */

    public RankedPage (String pageNameIn, Double pageRankIn, ArrayList<VotingPage> pageVotersIn) {
        this.pageName = pageNameIn;
        this.pageRank = pageRankIn;
        this.pageVoters = pageVotersIn;
    }

    public Double getRank(){
        return this.rank;
    }

    public ArrayList<VotingPage> getVoters(){
        return this.pageVoters;
    } 
    @Override
    public String toString(){
        return String.format("%s, %.5f, %s", this.pageName, this.pageRank, this.pageVoters.toString());
    }
}