package edu.nwmissouri.springbeam.marceline;

import java.io.Serializable;

public class VotingPage implements Serializable {
  String voterName;
  double rank;
  int contributorVotes;

  public int getContributorVotes() {
    return contributorVotes;
  }

  public String getVoterName() {
    return voterName;
  }

  public double getRank() {
    return rank;
  }

  /**
   * 
   * @param voterName
   * @param contributorVotes
   */
  VotingPage(String voterName, int contributorVotes) {
    this.voterName = voterName;
    this.contributorVotes = contributorVotes;
    this.rank = 1.0;
  }

  VotingPage(String voterName, int contributorVotes, double rank) {
    this.voterName = voterName;
    this.contributorVotes = contributorVotes;
    this.rank = rank;
  }

  public String toString() {
    String data = this.voterName;
    data += ", CV:" + this.contributorVotes;
    data += ", RK:" + this.rank;
    return data;
  }
}
