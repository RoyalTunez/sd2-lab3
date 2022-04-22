package events;

import java.util.Date;

public class MembershipSetUp implements Event {
    private final int membershipId;
    private final Date registerDate, expireDate;

    public MembershipSetUp(int membershipId, Date registerDate, Date expireDate) {
        this.membershipId = membershipId;
        this.registerDate = registerDate;
        this.expireDate = expireDate;
    }

    @Override
    public int getMembershipId() {
        return membershipId;
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public Date getExpireDate() {
        return expireDate;
    }
}
