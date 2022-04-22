package events;

import java.util.Date;

public class MemberEntered implements Event {
    private final int membershipId;
    private final Date enterDate;

    public MemberEntered(int membershipId, Date enterDate) {
        this.membershipId = membershipId;
        this.enterDate = enterDate;
    }

    @Override
    public int getMembershipId() {
        return membershipId;
    }

    public Date getEnterDate() {
        return enterDate;
    }
}
