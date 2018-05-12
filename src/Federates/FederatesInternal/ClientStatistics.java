package Federates.FederatesInternal;

public class ClientStatistics {

    private double arrivalTime;
    private double seatTakenTime;

    public ClientStatistics(double arrivalTime) {
        this.arrivalTime = arrivalTime;
    }

    public ClientStatistics(double arrivalTime, double seatTakenTime) {
        this.arrivalTime = arrivalTime;
        this.seatTakenTime = seatTakenTime;
    }

    public double getArrivalTime() {
        return arrivalTime;
    }

    public double getSeatTakenTime() {
        return seatTakenTime;
    }

    public void setSeatTakenTime(double seatTakenTime) {
        this.seatTakenTime = seatTakenTime;
    }
}
