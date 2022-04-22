package services;

import events.MemberEntered;
import events.MemberLeft;
import storage.EventStorage;

import java.util.Date;

public class EnterService extends EventHandlingService {
    public EnterService(EventStorage storage) {
        super(storage);
    }

    public boolean enter(int membershipId, Date enterDate) {
        return storage.transactionOn(membershipId, (history) -> {
           MembershipInfo info = new MembershipInfo(membershipId, history);

           if (!info.isEntered() && enterDate.before(info.getExpireDate())) {
               var event = new MemberEntered(membershipId, enterDate);
               history.add(event);
               storage.applyEvent(event);
               return true;
           }

           return false;
        });
    }

    public boolean exit(int membershipId, Date exitDate) {
        return storage.transactionOn(membershipId, (history) -> {
            MembershipInfo info = new MembershipInfo(membershipId, history);

            if (info.isEntered()) {
                var event = new MemberLeft(membershipId, exitDate);
                history.add(event);
                storage.applyEvent(event);
                return true;
            }

            return false;
        });
    }
}
