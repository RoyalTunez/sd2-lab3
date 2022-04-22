package events;

import java.time.Duration;

public class MembershipExtended implements Event {
    private final int membershipId;
    private final Duration duration;

    public MembershipExtended(int membershipId, Duration duration) {
        this.membershipId = membershipId;
        this.duration = duration;
    }

    @Override
    public int getMembershipId() {
        return membershipId;
    }

    public Duration getDuration() {
        return duration;
    }
}
