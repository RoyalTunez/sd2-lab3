package services;

import events.MembershipExtended;
import events.MembershipSetUp;
import storage.EventStorage;

import java.time.Duration;
import java.util.Date;

public class ManagerService extends EventHandlingService {
    public ManagerService(EventStorage storage) {
        super(storage);
    }

    public int registerMembership(Date registerDate, Date expireDate) {
        int membershipId = storage.registerEventList();
        storage.saveEvent(new MembershipSetUp(membershipId, registerDate, expireDate));

        return membershipId;
    }

    public void extendMembership(int membershipId, Duration duration) {
        storage.saveEvent(new MembershipExtended(membershipId, duration));
    }

    public MembershipInfo getMembershipInfo(int membershipId) {
        return storage.transactionOn(membershipId, (history) -> new MembershipInfo(membershipId, history));
    }
}
