package grp3.java.booknet.dto.member;

/** Aggregate projection used to enrich one directory page without per-user queries. */
public interface MemberLoanTotals {
    Long getMemberId();
    long getOnLoan();
    long getOverdue();
}
