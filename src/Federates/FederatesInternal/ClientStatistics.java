package Federates.FederatesInternal;

public class ClientStatistics {

    private Double arrivalTime;
    private Double awaitEndTime;

    public ClientStatistics(Double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public ClientStatistics(Double arrivalTime, Double awaitEndTime) {
        this.arrivalTime = arrivalTime;
        this.awaitEndTime = awaitEndTime;
    }

    public Double getArrivalTime() {
        return arrivalTime;
    }

    public Double getAwaitEndTime() {
        return awaitEndTime;
    }

    public void setAwaitEndTime(Double awaitEndTime) {
        this.awaitEndTime = awaitEndTime;
    }


}
