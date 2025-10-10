package huytq.example;

public class InsuranceClaim {

    private final String claimId;
    private double amount;
    private String claimStatus;

    private static final double PAYOUT_RATE = 0.85;

    public InsuranceClaim(String id, double claimAmount) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("Claim ID cannot be null or empty");
        }

        if (claimAmount <= 0) {
            throw new IllegalArgumentException("Claim amount must be positive.");
        }
        this.claimId = id;
        this.amount = claimAmount;
        this.claimStatus = "Pending";

    }

    public boolean processClaim(String statusUpdate) {
        if (statusUpdate == null || statusUpdate.isEmpty()) {
            throw new IllegalArgumentException("Status update cannot be null or empty");
        }

        if ("Pending".equals(claimStatus)) {
            this.claimStatus = statusUpdate;
            return true;
        }
        return false;
    }

    public double calculatePayout() {
        if ("Approved".equals(this.claimStatus)) {
            return amount * PAYOUT_RATE;
        } else {
            return 0;
        }
    }

    public void updateClaimAmount(double newAmount) {
        if (newAmount <= 0) {
            throw new IllegalArgumentException("New amount must be positive.");
        }
        this.amount = newAmount;
    }

    // Getters
    public String getClaimId() {
        return claimId;
    }

    public double getAmount() {
        return amount;
    }

    public String getClaimStatus() {
        return claimStatus;
    }

    @Override
    public String toString() {
        return "InsuranceClaim{" +
                "claimId='" + claimId + '\'' +
                ", amount=" + amount +
                ", claimStatus='" + claimStatus + '\'' +
                '}';
    }

}
