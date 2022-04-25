package edu.nwmissouri.springbeam.marceline;

import java.io.Serializable;
import java.util.ArrayList;

public class RankedPage implements Serializable {
  String pageName;
  double rank;
  ArrayList<VotingPage> voters;
  int numVoters;

  public String getPageName() {
    return pageName;
  }

  public ArrayList<VotingPage> getVoters() {
    return voters;
  }

  public int getNumVoters() {
    return numVoters;
  }

  public double getRank() {
    return rank;
  }

  RankedPage(String pageName, ArrayList<VotingPage> voters) {
    this.pageName = pageName;
    this.voters = voters;
    this.numVoters = voters.size();
    this.rank = 1.0;
  }

  RankedPage(String pageName, ArrayList<VotingPage> voters, double rank) {
    this.pageName = pageName;
    this.voters = voters;
    this.rank = rank;
    this.numVoters = voters.size();
  }

  public String toString() {
    String pageString = this.pageName + ", VT:[";
    for (VotingPage voterName : this.voters) {
      pageString += voterName.toString() + ", ";
    }
    pageString += "], RK:" + this.rank;

    return pageString;
  }
}
