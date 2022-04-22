package edu.nwmissouri.springbeam.bishop;

import java.util.ArrayList;

public class RankedPage {

    public RankedPage (String nameIn, Double rankIn, ArrayList<VotingPage> voters) {
        this.name = nameIn;
        this.rank = rankIn;
        this.voters = votersIn;
    }

    public Double getRank(){
        return this.rank;
    }

    //public ArrayList<VotingPage>(voters)
    
}