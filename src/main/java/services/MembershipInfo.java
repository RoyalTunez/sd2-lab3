package services;

import events.*;

import java.time.Duration;
import java.util.Date;
import java.util.List;

public class MembershipInfo {
    private final int membershipId;
    private final Date registerDate, expireDate;
    private int attendanceCount;
    private boolean entered;

    public MembershipInfo(int membershipId, List<Event> history) {
        this.membershipId = membershipId;
        this.registerDate = new Date(0);
        this.expireDate = new Date(0);
        this.attendanceCount = 0;
        this.entered = false;

        history.forEach((event) -> {
            if (event instanceof MembershipSetUp) {
                this.setRegisterDate(((MembershipSetUp) event).getRegisterDate());
                this.setExpireDate(((MembershipSetUp) event).getExpireDate());
            } else if (event instanceof MembershipExtended) {
                this.extendExpireDate(((MembershipExtended) event).getDuration());
            } else if (event instanceof MemberEntered) {
                this.addAttendance();
                this.setEntered(true);
            } else if (event instanceof MemberLeft) {
                this.setEntered(false);
            }
        });
    }

    public int getMembershipId() {
        return membershipId;
    }

    public Date getRegisterDate() {
        return registerDate;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public int getAttendanceCount() {
        return attendanceCount;
    }

    public boolean isEntered() {
        return entered;
    }

    public void setRegisterDate(Date registerDate) {
        if (this.registerDate.getTime() == new Date(0).getTime()) {
            this.registerDate.setTime(registerDate.getTime());
        }
    }

    public void setExpireDate(Date expireDate) {
        if (this.expireDate.getTime() == new Date(0).getTime()) {
            this.expireDate.setTime(expireDate.getTime());
        }
    }

    public void extendExpireDate(Duration duration) {
        this.expireDate.setTime(this.expireDate.getTime() + duration.toMillis());
    }

    public void addAttendance() {
        attendanceCount++;
    }

    public void setEntered(boolean entered) {
        this.entered = entered;
    }
}
