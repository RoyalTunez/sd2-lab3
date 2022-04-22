package events;

import java.util.Date;

public class MemberLeft implements Event {
    private final int membershipId;
    private final Date exitDate;

    public MemberLeft(int membershipId, Date exitDate) {
        this.membershipId = membershipId;
        this.exitDate = exitDate;
    }

    @Override
    public int getMembershipId() {
        return membershipId;
    }

    public Date getExitDate() {
        return exitDate;
    }
}
