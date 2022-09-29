package org.ibsenc;

public class LiftRide {
  private Integer resortID;
  private String seasonID;
  private String dayID;
  private Integer skierID;
  private Integer time;
  private Integer liftID;

  public LiftRide() {
  }

  public LiftRide(Integer time, Integer liftId) {
    this.time = time;
    this.liftID = liftId;
  }

  public LiftRide(Integer resortId, String seasonId, String dayId, Integer skierId, Integer time,
      Integer liftId) {
    this.resortID = resortId;
    this.seasonID = seasonId;
    this.dayID = dayId;
    this.skierID = skierId;
    this.time = time;
    this.liftID = liftId;
  }

  public Integer getResortID() {
    return resortID;
  }

  public void setResortID(Integer resortID) {
    this.resortID = resortID;
  }

  public String getSeasonID() {
    return seasonID;
  }

  public void setSeasonID(String seasonID) {
    this.seasonID = seasonID;
  }

  public String getDayID() {
    return dayID;
  }

  public void setDayID(String dayID) {
    this.dayID = dayID;
  }

  public Integer getSkierID() {
    return skierID;
  }

  public void setSkierID(Integer skierID) {
    this.skierID = skierID;
  }

  public Integer getTime() {
    return time;
  }

  public void setTime(Integer time) {
    this.time = time;
  }

  public Integer getLiftID() {
    return liftID;
  }

  public void setLiftID(Integer liftID) {
    this.liftID = liftID;
  }
}
