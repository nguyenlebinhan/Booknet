package grp3.java.booknet.dto.admin;

public record AdminDashboardSummary(
        long publishedTitles,
        long registeredMembers,
        long borrowedCopies,
        long overdueLoans,
        long draftTitles,
        long pendingReservations,
        long availableCopies
) {
    public long trackedCopies() {
        return borrowedCopies + availableCopies;
    }

    public int availableCopyPercentage() {
        long trackedCopies = trackedCopies();
        return trackedCopies == 0 ? 0 : (int) Math.round(availableCopies * 100.0 / trackedCopies);
    }
}
